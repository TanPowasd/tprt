package org.mylove.tprt.Modifiers.re;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.network.SyncManaPacket;
import io.redspace.ironsspellbooks.setup.PacketDistributor;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mylove.tprt.common.chtholly.ChthollyCombatHelper;
import org.mylove.tprt.common.chtholly.ChthollySyncData;
import org.mylove.tprt.registries.ModifierIds;
import org.mylove.tprt.utils.ModListUtil;
import slimeknights.mantle.client.TooltipKey;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.display.TooltipModifierHook;
import slimeknights.tconstruct.library.modifiers.impl.NoLevelsModifier;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.item.IModifiable;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/*
  ============================================================
  终末挽歌 (elegy_of_world_end) —— 挚爱 (chtholly) · 法术书/魔导工具核心词条

  单一核心机制 —— 【挽歌余音】:
    - 挂印记: 佩戴或手持含本词条的法术书/工具时, 造成的任何法术/魔法伤害
      (支持铁魔法 Iron's Spellbooks、诡厄巫法、新生魔艺、灾变法术与原版魔法等跨模组法术伤害),
      以及手持含本词条的激光枪/炼金手套/暗黑魔杖直接造成的攻击伤害,
      会在目标身上烙印持续 5 秒的「挽歌余音」
    - 全伤害汇入: 「挽歌余音」存在期间, 你对该目标造成的一切伤害 (包括法术、近战、远程、召唤物)
      都会把 40% 的伤害额存入余音之中
    - 每秒余音共振: 余音每秒对目标跳一次已存伤害 30% 的跨模组纯粹真伤 (无视无敌帧、护甲与魔抗)
    - 终末引爆与回温: 在 5 秒结束 (或目标当前生命值 ≤ 已存余音伤害足以斩杀) 时一次性全额引爆,
      造成 100% 已存余音伤害的纯粹真伤, 同时按引爆量的 20% 回复生命值、50% 恢复法力值 (含客户端法力同步)

  数值均为顶部常量, 可自由调整
  ============================================================
 */
public class elegy_of_world_end extends NoLevelsModifier implements TooltipModifierHook {

    // ===== 挽歌余音核心参数 =====
    public static final int ECHO_DURATION_TICKS = 5 * 20;            // 挽歌余音持续时间 (5 秒 = 100 ticks)
    public static final int ECHO_PULSE_INTERVAL_TICKS = 20;          // 余音每秒共振间隔 (1 秒 = 20 ticks)
    public static final float DAMAGE_STORE_RATIO = 0.40f;            // 全伤害存入余音的比例 (40%)
    public static final float PULSE_TRUE_DAMAGE_RATIO = 0.30f;       // 每秒共振真伤占已存伤害比例 (30%)
    public static final float DETONATE_TRUE_DAMAGE_RATIO = 1.00f;    // 终末引爆真伤占已存伤害比例 (100%)
    public static final float DETONATE_HEAL_RATIO = 0.20f;           // 引爆量转化为生命回复的比例 (20%)
    public static final float DETONATE_MANA_RATIO = 0.50f;           // 引爆量转化为法力回复的比例 (50%)
    public static final float MIN_MANA_RESTORE = 15.0f;              // 每次引爆保底恢复法力值
    public static final float MAX_MANA_RESTORE_RATIO = 0.35f;        // 单次引爆法力恢复上限占最大法力比例 (35%)

    /** 每个玩家当前维持的挽歌余音目标表 (Player UUID -> (Target UUID -> EchoMark)) */
    private static final Map<UUID, Map<UUID, EchoMark>> PLAYER_MARKS = new ConcurrentHashMap<>();

    static {
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOW, false, LivingDamageEvent.class, elegy_of_world_end::onLivingDamage);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, TickEvent.PlayerTickEvent.class, elegy_of_world_end::onPlayerTick);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, LivingDeathEvent.class, elegy_of_world_end::onLivingDeath);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, PlayerEvent.PlayerLoggedOutEvent.class, elegy_of_world_end::onPlayerLogout);
    }

    public static void clearServerState() {
        PLAYER_MARKS.clear();
    }

    @Override
    protected void registerHooks(ModuleHookMap.@NotNull Builder hookBuilder) {
        super.registerHooks(hookBuilder);
        hookBuilder.addHook(this, ModifierHooks.TOOLTIP);
    }

    // ============================================================
    //  1. 挂印记与全伤害汇入 (法术或本词条载体开启余音 -> 全流派伤害 40% 存入 + 斩杀判定)
    // ============================================================

    private static void onLivingDamage(LivingDamageEvent event) {
        LivingEntity rawTarget = event.getEntity();
        if (rawTarget == null || rawTarget.level().isClientSide) return;

        // 防止挚爱体系纯粹真伤（余音共振/终末引爆/幸福星火）互相嵌套无限递归
        if (ChthollyCombatHelper.isDealingPureDamage() || ChthollyCombatHelper.isDealingPureDamage(rawTarget)) {
            return;
        }

        LivingEntity target = ChthollyCombatHelper.resolveLivingTarget(rawTarget);
        if (target == null || !target.isAlive()) return;

        float dealt = event.getAmount();
        if (dealt <= 0.0f || !Float.isFinite(dealt)) return;

        DamageSource source = event.getSource();
        Player attacker = ChthollyCombatHelper.resolvePlayerAttacker(source);
        if (attacker == null || attacker == target || !isWearingOrHoldingElegy(attacker)) return;
        if (!ChthollyCombatHelper.isValidHostileTarget(attacker, target)) return;

        Map<UUID, EchoMark> marks = PLAYER_MARKS.computeIfAbsent(attacker.getUUID(), k -> new ConcurrentHashMap<>());
        EchoMark mark = marks.get(target.getUUID());

        // 若目标尚未带有「挽歌余音」, 可由法术/魔法伤害或手持终末挽歌载体（激光枪/炼金手套/暗黑魔杖等）直接烙印
        if (mark == null) {
            boolean canImprint = isCrossModSpellOrMagicDamage(source) || isHoldingElegyInHands(attacker);
            if (!canImprint) return;
            mark = new EchoMark(target.getUUID(), ECHO_DURATION_TICKS);
            marks.put(target.getUUID(), mark);
            spawnMarkImprintParticles(target);
        }

        // 余音存在期间, 玩家对该目标造成的一切伤害 (法术/近战/远程/召唤物) 均将 40% 存入余音
        // 伤害累积交由 onPlayerTick 与余音共振统一判定斩杀与引爆，避免在 LivingDamageEvent 中过早预判引爆导致面对锁血/阶段保护 Boss 时空爆
        float added = dealt * DAMAGE_STORE_RATIO;
        mark.storedDamage += added;
    }

    // ============================================================
    //  2. 每秒余音共振 (30% 纯粹真伤) 与 5 秒到期终末引爆 (100% 纯粹真伤)
    // ============================================================

    private static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Player player = event.player;
        if (player.level().isClientSide || !(player.level() instanceof ServerLevel serverLevel)) return;

        Map<UUID, EchoMark> marks = PLAYER_MARKS.get(player.getUUID());
        if (marks == null || marks.isEmpty()) return;

        if (!isWearingOrHoldingElegy(player) || !player.isAlive()) {
            marks.clear();
            return;
        }

        Iterator<Map.Entry<UUID, EchoMark>> it = marks.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, EchoMark> entry = it.next();
            EchoMark mark = entry.getValue();
            Entity entity = serverLevel.getEntity(mark.targetUuid);
            LivingEntity target = ChthollyCombatHelper.resolveLivingTarget(entity);
            if (target == null || !target.isAlive() || target.isDeadOrDying()) {
                it.remove();
                continue;
            }

            mark.remainingTicks--;
            mark.ticksSinceLastPulse++;

            // 斩杀线实时检测: 目标当前生命值 ≤ 已存余音伤害时立即全额引爆
            if (mark.storedDamage > 0.0f && target.getHealth() <= mark.storedDamage) {
                it.remove();
                detonateEchoMark(player, target, mark);
                continue;
            }

            // 5 秒到期: 一次性全额引爆
            if (mark.remainingTicks <= 0) {
                it.remove();
                detonateEchoMark(player, target, mark);
                continue;
            }

            // 每满 1 秒 (20 ticks): 触发一次 30% 已存伤害的余音共振纯粹真伤
            if (mark.ticksSinceLastPulse >= ECHO_PULSE_INTERVAL_TICKS && mark.storedDamage > 0.0f) {
                mark.ticksSinceLastPulse = 0;
                float pulseDamage = mark.storedDamage * PULSE_TRUE_DAMAGE_RATIO;
                ChthollyCombatHelper.dealPureDamage(player, target, pulseDamage);
                spawnPulseParticles(target);

                // 共振后若目标跌入斩杀线则立即触发终末引爆
                if (target.isAlive() && !target.isDeadOrDying() && target.getHealth() <= mark.storedDamage) {
                    it.remove();
                    detonateEchoMark(player, target, mark);
                }
            }
        }
    }

    /**
     * 终末引爆: 一次性结算 100% 已存余音伤害的跨模组纯粹真伤, 并按引爆量回复玩家生命值与法力值
     */
    private static void detonateEchoMark(Player player, LivingEntity target, EchoMark mark) {
        if (mark.storedDamage <= 0.0f) return;

        float detonateDamage = mark.storedDamage * DETONATE_TRUE_DAMAGE_RATIO;
        if (target.isAlive() && !target.isDeadOrDying()) {
            ChthollyCombatHelper.dealPureDamage(player, target, detonateDamage);
            spawnDetonateEffects(target);
        }

        rewardOwnerOnDetonate(player, detonateDamage);
    }

    private static void rewardOwnerOnDetonate(Player player, float detonateDamage) {
        float healAmount = detonateDamage * DETONATE_HEAL_RATIO;
        if (healAmount > 0.0f && player.isAlive()) {
            player.heal(healAmount);
        }

        if (ModListUtil.ISSLoaded && player.isAlive()) {
            float manaAmount = Math.max(MIN_MANA_RESTORE, detonateDamage * DETONATE_MANA_RATIO);
            IssManaBridge.restoreMana(player, manaAmount, MAX_MANA_RESTORE_RATIO);
        }
    }

    private static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity dead = event.getEntity();
        if (dead == null || dead.level().isClientSide || event.isCanceled()) return;

        if (dead instanceof Player deadPlayer) {
            PLAYER_MARKS.remove(deadPlayer.getUUID());
        }

        if (!(dead.level() instanceof ServerLevel serverLevel)) return;
        MinecraftServer server = serverLevel.getServer();
        UUID deadId = dead.getUUID();

        // 若带余音的目标死亡, 仍为施法者（支持跨维度）返还余音已存伤害对应的生命与法力回温
        for (Map.Entry<UUID, Map<UUID, EchoMark>> entry : PLAYER_MARKS.entrySet()) {
            EchoMark removed = entry.getValue().remove(deadId);
            if (removed != null && removed.storedDamage > 0.0f) {
                ServerPlayer owner = server.getPlayerList().getPlayer(entry.getKey());
                if (owner != null && owner.isAlive()) {
                    rewardOwnerOnDetonate(owner, removed.storedDamage * DETONATE_TRUE_DAMAGE_RATIO);
                }
            }
        }
    }

    private static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        Player player = event.getEntity();
        if (player != null) {
            PLAYER_MARKS.remove(player.getUUID());
        }
    }

    // ============================================================
    //  3. 动态 Tooltip 面板展示 & HUD 快照导出 (零 ItemStack NBT 污染)
    // ============================================================

    public record ElegyHudSnapshot(boolean equipped, int activeTargets, int maxStoredTenths, int remainSec) {
    }

    public static ElegyHudSnapshot getHudSnapshot(Player player) {
        boolean equipped = isWearingOrHoldingElegy(player);
        Map<UUID, EchoMark> marks = PLAYER_MARKS.get(player.getUUID());
        if (!equipped || marks == null || marks.isEmpty()) {
            return new ElegyHudSnapshot(equipped, 0, 0, 0);
        }
        int activeCount = marks.size();
        float maxStored = 0.0f;
        int maxRemainTicks = 0;
        for (EchoMark mark : marks.values()) {
            if (mark.storedDamage > maxStored) {
                maxStored = mark.storedDamage;
            }
            if (mark.remainingTicks > maxRemainTicks) {
                maxRemainTicks = mark.remainingTicks;
            }
        }
        return new ElegyHudSnapshot(
                true,
                activeCount,
                Math.round(maxStored * 10.0f),
                (maxRemainTicks + 19) / 20
        );
    }

    @Override
    public void addTooltip(@NotNull IToolStackView tool, @NotNull ModifierEntry modifier, @Nullable Player player,
                           @NotNull List<Component> tooltip, @NotNull TooltipKey tooltipKey, @NotNull TooltipFlag tooltipFlag) {
        ChthollySyncData sync = ChthollySyncData.getClient();
        int activeTargets = sync.elActiveTargets();
        float maxStored = sync.elMaxStoredTenths() / 10.0f;

        if (activeTargets > 0) {
            tooltip.add(Component.translatable("modifier.tprt.elegy_of_world_end.tooltip.active",
                    activeTargets, String.format("%.1f", maxStored)));
        } else {
            tooltip.add(Component.translatable("modifier.tprt.elegy_of_world_end.tooltip.standby"));
        }

        if (tooltipKey == TooltipKey.SHIFT) {
            tooltip.add(Component.translatable("modifier.tprt.elegy_of_world_end.tooltip.detail_imprint",
                    (int) (ECHO_DURATION_TICKS / 20.0f), (int) (DAMAGE_STORE_RATIO * 100)));
            tooltip.add(Component.translatable("modifier.tprt.elegy_of_world_end.tooltip.detail_resonance",
                    (int) (PULSE_TRUE_DAMAGE_RATIO * 100),
                    (int) (DETONATE_TRUE_DAMAGE_RATIO * 100),
                    (int) (DETONATE_HEAL_RATIO * 100),
                    (int) (DETONATE_MANA_RATIO * 100)));
        } else {
            tooltip.add(Component.translatable("modifier.tprt.elegy_of_world_end.tooltip.shift_hint"));
        }
    }

    // ============================================================
    //  跨模组法术伤害识别与槽位检测
    // ============================================================

    private static boolean isCrossModSpellOrMagicDamage(DamageSource source) {
        if (source == null) return false;
        if (source.is(DamageTypeTags.WITCH_RESISTANT_TO) || source.is(DamageTypeTags.AVOIDS_GUARDIAN_THORNS)) {
            return true;
        }
        String className = source.getClass().getName().toLowerCase(Locale.ROOT);
        if (className.contains("spell") || className.contains("magic")) {
            return true;
        }
        String msgId = source.getMsgId().toLowerCase(Locale.ROOT);
        if (msgId.contains("spell") || msgId.contains("magic") || msgId.contains("arcane")
                || msgId.contains("eldritch") || msgId.contains("holy") || msgId.contains("ender")
                || msgId.contains("evocation") || msgId.contains("blood") || msgId.contains("nature")
                || msgId.contains("fire_field") || msgId.contains("poison_cloud") || msgId.contains("dragon_breath")
                || msgId.contains("sonic_boom") || msgId.contains("wither_skull") || msgId.contains("indirectmagic")
                || msgId.contains("laser") || msgId.contains("alchem") || msgId.contains("dark_wand")
                || msgId.contains("sakuratinker")) {
            return true;
        }
        return source.typeHolder().unwrapKey()
                .map(key -> {
                    String ns = key.location().getNamespace().toLowerCase(Locale.ROOT);
                    String path = key.location().getPath().toLowerCase(Locale.ROOT);
                    return ns.equals("irons_spellbooks")
                            || ns.equals("ars_nouveau")
                            || ns.equals("goety")
                            || ns.equals("sakuratinker")
                            || path.contains("spell")
                            || path.contains("magic")
                            || path.contains("arcane")
                            || path.contains("eldritch")
                            || path.contains("holy")
                            || path.contains("laser")
                            || path.contains("dark_wand")
                            || path.contains("alchemical");
                })
                .orElse(false);
    }

    private static boolean isHoldingElegyInHands(Player player) {
        return hasElegyModifier(player.getMainHandItem()) || hasElegyModifier(player.getOffhandItem());
    }

    public static boolean isWearingOrHoldingElegy(Player player) {
        if (isHoldingElegyInHands(player)) {
            return true;
        }
        var inventory = CuriosApi.getCuriosInventory(player).resolve();
        if (inventory.isEmpty()) return false;
        for (var entry : inventory.get().getCurios().entrySet()) {
            if (entry.getKey().toLowerCase(Locale.ROOT).contains("cosmetic")) continue;
            var stacks = entry.getValue().getStacks();
            for (int i = 0; i < stacks.getSlots(); i++) {
                if (hasElegyModifier(stacks.getStackInSlot(i))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean hasElegyModifier(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof IModifiable) || !ToolStack.isInitialized(stack)) {
            return false;
        }
        ToolStack tool = ToolStack.from(stack);
        return !tool.isBroken() && tool.getModifierLevel(ModifierIds.elegy_of_world_end) > 0;
    }

    private static void spawnMarkImprintParticles(LivingEntity target) {
        if (!(target.level() instanceof ServerLevel serverLevel)) return;
        double cx = target.getX();
        double cy = target.getY() + target.getBbHeight() * 0.6;
        double cz = target.getZ();
        serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, cx, cy, cz, 12, 0.35, 0.35, 0.35, 0.03);
        serverLevel.sendParticles(ParticleTypes.END_ROD, cx, cy, cz, 8, 0.3, 0.3, 0.3, 0.02);
        serverLevel.playSound(null, cx, cy, cz, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.9f, 1.25f);
    }

    private static void spawnPulseParticles(LivingEntity target) {
        if (!(target.level() instanceof ServerLevel serverLevel)) return;
        double cx = target.getX();
        double cy = target.getY() + target.getBbHeight() * 0.5;
        double cz = target.getZ();
        serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, cx, cy, cz, 8, 0.3, 0.3, 0.3, 0.02);
        serverLevel.sendParticles(ParticleTypes.NOTE, cx, cy + 0.6, cz, 3, 0.25, 0.15, 0.25, 0.5);
    }

    private static void spawnDetonateEffects(LivingEntity target) {
        if (!(target.level() instanceof ServerLevel serverLevel)) return;
        double cx = target.getX();
        double cy = target.getY() + target.getBbHeight() * 0.5;
        double cz = target.getZ();
        serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, cx, cy, cz, 24, 0.45, 0.45, 0.45, 0.08);
        serverLevel.sendParticles(ParticleTypes.FLAME, cx, cy, cz, 18, 0.45, 0.45, 0.45, 0.06);
        serverLevel.sendParticles(ParticleTypes.END_ROD, cx, cy, cz, 16, 0.4, 0.4, 0.4, 0.06);
        serverLevel.playSound(null, cx, cy, cz, SoundEvents.BELL_RESONATE, SoundSource.PLAYERS, 1.0f, 1.35f);
    }

    /** 挽歌余音印记数据 */
    private static final class EchoMark {
        final UUID targetUuid;
        int remainingTicks;
        int ticksSinceLastPulse;
        float storedDamage;

        EchoMark(UUID targetUuid, int remainingTicks) {
            this.targetUuid = targetUuid;
            this.remainingTicks = remainingTicks;
        }
    }

    /** Iron's Spellbooks 法力恢复软桥接 (仅在 ISSLoaded 时调用, 避免硬依赖类加载崩溃) */
    private static final class IssManaBridge {
        static void restoreMana(Player player, float rawManaRestore, float maxRatioCap) {
            try {
                MagicData magicData = MagicData.getPlayerMagicData(player);
                if (magicData == null) return;
                float maxMana = 100.0f;
                AttributeInstance maxManaAttr = player.getAttribute(AttributeRegistry.MAX_MANA.get());
                if (maxManaAttr != null && maxManaAttr.getValue() > 0) {
                    maxMana = (float) maxManaAttr.getValue();
                }
                float cappedRestore = Math.min(rawManaRestore, maxMana * maxRatioCap);
                float nextMana = Mth.clamp(magicData.getMana() + cappedRestore, 0.0f, maxMana);
                magicData.setMana(nextMana);
                // 修复: 同步服务端法力值到客户端，避免客户端法力条不同步与无法施法假象
                if (player instanceof ServerPlayer serverPlayer) {
                    PacketDistributor.sendToPlayer(serverPlayer, new SyncManaPacket(magicData));
                }
            } catch (Throwable ignored) {
            }
        }
    }
}
