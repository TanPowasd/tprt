package org.mylove.tprt.Modifiers.re;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.TraceableEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.entity.PartEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mylove.tprt.Tprt;
import org.mylove.tprt.registries.ModDamageSource;
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
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

/*
  ============================================================
  终末挽歌 (elegy_of_world_end) —— 挚爱 (chtholly) · 法术书核心词条

  单一核心机制 —— 【挽歌余音】:
    - 挂印记: 佩戴或手持含本词条的法术书/工具时, 造成的任何法术/魔法伤害
      (支持铁魔法 Iron's Spellbooks、诡厄巫法、新生魔艺、灾变法术与原版魔法等跨模组法术伤害)
      会在目标身上烙印持续 5 秒的「挽歌余音」
    - 全伤害汇入: 「挽歌余音」存在期间, 你对该目标造成的一切伤害 (包括法术、近战、远程、召唤物)
      都会把 40% 的伤害额存入余音之中
    - 每秒余音共振: 余音每秒对目标跳一次已存伤害 30% 的跨模组纯粹真伤 (无视无敌帧、护甲与魔抗)
    - 终末引爆与回温: 在 5 秒结束 (或目标当前生命值 ≤ 已存余音伤害足以斩杀) 时一次性全额引爆,
      造成 100% 已存余音伤害的纯粹真伤, 同时按引爆量的 20% 回复生命值、50% 恢复法力值

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

    // ===== 工具 NBT 同步键 (用于客户端法术书 Tooltip 实时展示) =====
    private static final ResourceLocation SYNC_ACTIVE_TARGETS = Tprt.getResource("elegy_active_targets");
    private static final ResourceLocation SYNC_MAX_STORED_TENTHS = Tprt.getResource("elegy_max_stored_tenths");
    private static final ResourceLocation SYNC_REMAIN_SEC = Tprt.getResource("elegy_remain_sec");

    /** 递归重入保护: 防止余音共振/终末引爆的纯粹真伤再次被存入余音 */
    private static final Set<Entity> REENTRANT_GUARD = Collections.newSetFromMap(new WeakHashMap<>());

    /** 每个玩家当前维持的挽歌余音目标表 (Player UUID -> (Target UUID -> EchoMark)) */
    private static final Map<UUID, Map<UUID, EchoMark>> PLAYER_MARKS = new HashMap<>();

    public elegy_of_world_end() {
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOW, this::onLivingDamage);
        MinecraftForge.EVENT_BUS.addListener(this::onPlayerTick);
        MinecraftForge.EVENT_BUS.addListener(this::onLivingDeath);
        MinecraftForge.EVENT_BUS.addListener(this::onPlayerLogout);
    }

    @Override
    protected void registerHooks(ModuleHookMap.@NotNull Builder hookBuilder) {
        super.registerHooks(hookBuilder);
        hookBuilder.addHook(this, ModifierHooks.TOOLTIP);
    }

    // ============================================================
    //  1. 挂印记与全伤害汇入 (法术开启余音 -> 全流派伤害 40% 存入 + 斩杀判定)
    // ============================================================

    private void onLivingDamage(LivingDamageEvent event) {
        LivingEntity rawTarget = event.getEntity();
        if (rawTarget == null || rawTarget.level().isClientSide) return;

        LivingEntity target = resolveActualLivingTarget(rawTarget);
        if (!target.isAlive() || target instanceof ArmorStand) return;
        if (REENTRANT_GUARD.contains(target) || REENTRANT_GUARD.contains(rawTarget)) return;

        float dealt = event.getAmount();
        if (dealt <= 0.0f) return;

        DamageSource source = event.getSource();
        Player attacker = resolvePlayerAttacker(source);
        if (attacker == null || attacker == target || !isWearingOrHoldingElegy(attacker)) return;
        if (!isValidTarget(attacker, target)) return;

        Map<UUID, EchoMark> marks = PLAYER_MARKS.computeIfAbsent(attacker.getUUID(), k -> new HashMap<>());
        EchoMark mark = marks.get(target.getUUID());
        boolean isSpell = isCrossModSpellOrMagicDamage(source);

        // 若目标尚未带有「挽歌余音」, 则必须由法术/魔法伤害首次挂上印记
        if (mark == null) {
            if (!isSpell) return;
            mark = new EchoMark(target.getUUID(), ECHO_DURATION_TICKS);
            marks.put(target.getUUID(), mark);
            spawnMarkImprintParticles(target);
        }

        // 余音存在期间, 玩家对该目标造成的一切伤害 (法术/近战/远程/召唤物) 均将 40% 存入余音
        float added = dealt * DAMAGE_STORE_RATIO;
        mark.storedDamage += added;

        // 检查本击结算后目标剩余生命值是否已进入余音斩杀线 (剩余生命 ≤ 已存余音伤害)
        float remainingHpAfterHit = target.getHealth() - dealt;
        if (remainingHpAfterHit > 0.0f && remainingHpAfterHit <= mark.storedDamage) {
            marks.remove(target.getUUID());
            detonateEchoMark(attacker, target, mark);
        }
        syncBookNbt(attacker, marks);
    }

    // ============================================================
    //  2. 每秒余音共振 (30% 纯粹真伤) 与 5 秒到期终末引爆 (100% 纯粹真伤)
    // ============================================================

    private void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Player player = event.player;
        if (player.level().isClientSide || !(player.level() instanceof ServerLevel serverLevel)) return;

        Map<UUID, EchoMark> marks = PLAYER_MARKS.get(player.getUUID());
        if (marks == null || marks.isEmpty()) return;

        if (!isWearingOrHoldingElegy(player) || !player.isAlive()) {
            marks.clear();
            syncBookNbt(player, marks);
            return;
        }

        Iterator<Map.Entry<UUID, EchoMark>> it = marks.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, EchoMark> entry = it.next();
            EchoMark mark = entry.getValue();
            Entity entity = serverLevel.getEntity(mark.targetUuid);
            if (!(entity instanceof LivingEntity target) || !target.isAlive() || target.isDeadOrDying()) {
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
                dealCrossModPureDamage(player, target, pulseDamage);
                spawnPulseParticles(target);

                // 共振后若目标跌入斩杀线则立即触发终末引爆
                if (target.isAlive() && !target.isDeadOrDying() && target.getHealth() <= mark.storedDamage) {
                    it.remove();
                    detonateEchoMark(player, target, mark);
                }
            }
        }

        syncBookNbt(player, marks);
    }

    /**
     * 终末引爆: 一次性结算 100% 已存余音伤害的跨模组纯粹真伤, 并按引爆量回复玩家生命值与法力值
     */
    private static void detonateEchoMark(Player player, LivingEntity target, EchoMark mark) {
        if (mark.storedDamage <= 0.0f) return;

        float detonateDamage = mark.storedDamage * DETONATE_TRUE_DAMAGE_RATIO;
        if (target.isAlive() && !target.isDeadOrDying()) {
            dealCrossModPureDamage(player, target, detonateDamage);
            spawnDetonateEffects(target);
        }

        // 按引爆伤害的 20% 回复生命值 (可自然触发护符溢疗转幸福护盾或护甲末日三问清延迟池)
        float healAmount = detonateDamage * DETONATE_HEAL_RATIO;
        if (healAmount > 0.0f && player.isAlive()) {
            player.heal(healAmount);
        }

        // 按引爆伤害的 50% 恢复法力值 (兼容 Iron's Spellbooks)
        if (ModListUtil.ISSLoaded && player.isAlive()) {
            float manaAmount = Math.max(MIN_MANA_RESTORE, detonateDamage * DETONATE_MANA_RATIO);
            IssManaBridge.restoreMana(player, manaAmount, MAX_MANA_RESTORE_RATIO);
        }
    }

    private void onLivingDeath(LivingDeathEvent event) {
        LivingEntity dead = event.getEntity();
        if (dead == null || dead.level().isClientSide) return;
        UUID deadId = dead.getUUID();

        // 若带余音的目标死亡, 仍为施法者返还余音已存伤害对应的生命与法力回温
        for (Map.Entry<UUID, Map<UUID, EchoMark>> entry : PLAYER_MARKS.entrySet()) {
            EchoMark removed = entry.getValue().remove(deadId);
            if (removed != null && removed.storedDamage > 0.0f && dead.level() instanceof ServerLevel serverLevel) {
                Player owner = serverLevel.getPlayerByUUID(entry.getKey());
                if (owner != null && owner.isAlive()) {
                    float detonateDamage = removed.storedDamage * DETONATE_TRUE_DAMAGE_RATIO;
                    owner.heal(detonateDamage * DETONATE_HEAL_RATIO);
                    if (ModListUtil.ISSLoaded) {
                        float manaAmount = Math.max(MIN_MANA_RESTORE, detonateDamage * DETONATE_MANA_RATIO);
                        IssManaBridge.restoreMana(owner, manaAmount, MAX_MANA_RESTORE_RATIO);
                    }
                    syncBookNbt(owner, entry.getValue());
                }
            }
        }
    }

    private void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        Player player = event.getEntity();
        if (player != null) {
            PLAYER_MARKS.remove(player.getUUID());
        }
    }

    // ============================================================
    //  3. 动态 Tooltip 面板展示
    // ============================================================

    @Override
    public void addTooltip(@NotNull IToolStackView tool, @NotNull ModifierEntry modifier, @Nullable Player player,
                           @NotNull List<Component> tooltip, @NotNull TooltipKey tooltipKey, @NotNull TooltipFlag tooltipFlag) {
        ModDataNBT data = tool.getPersistentData();
        int activeTargets = data.getInt(SYNC_ACTIVE_TARGETS);
        float maxStored = data.getInt(SYNC_MAX_STORED_TENTHS) / 10.0f;

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
    //  跨模组兼容辅助方法 (纯真伤穿透、法术伤害识别、召唤物归因、多部件 Boss)
    // ============================================================

    /**
     * 跨模组纯粹真伤结算:
     * 清除目标及多部件子实体无敌帧, 使用 TPRT_PURE_DAMAGE_TYPE 穿透护甲/魔抗/附魔/盾牌;
     * 若目标模组 Boss 拦截 hurt() 导致生命未扣减, 则保底扣减其有效生命值。
     */
    private static void dealCrossModPureDamage(Player attacker, LivingEntity target, float amount) {
        if (amount <= 0.0f || !target.isAlive() || target.isDeadOrDying()) return;

        DamageSource pureSource = new DamageSource(
                attacker.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                        .getHolderOrThrow(ModDamageSource.TPRT_PURE_DAMAGE_TYPE),
                attacker,
                attacker
        );

        REENTRANT_GUARD.add(target);
        try {
            int prevInvuln = target.invulnerableTime;
            int prevHurtTime = target.hurtTime;
            resetInvulnerabilityRecursive(target);

            float healthBefore = target.getHealth();
            target.hurt(pureSource, amount);
            float actualDrop = healthBefore - target.getHealth();

            // 针对强制锁伤或免疫常规 hurt 的跨模组 Boss 进行保底真伤结算
            if (target.isAlive() && actualDrop < amount * 0.05f) {
                float forcedHealth = Math.max(0.0f, healthBefore - amount);
                target.setHealth(forcedHealth);
                target.gameEvent(GameEvent.ENTITY_DAMAGE, attacker);
                if (forcedHealth <= 0.0f && !target.isDeadOrDying()) {
                    target.die(pureSource);
                }
            }

            target.invulnerableTime = prevInvuln;
            target.hurtTime = prevHurtTime;
        } finally {
            REENTRANT_GUARD.remove(target);
        }
    }

    /** 识别跨模组法术与魔法伤害 (Iron's Spellbooks / Goety / Ars Nouveau / Cataclysm / L2 / 原版魔法等) */
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
                || msgId.contains("sonic_boom") || msgId.contains("wither_skull") || msgId.contains("indirectmagic")) {
            return true;
        }
        return source.typeHolder().unwrapKey()
                .map(key -> {
                    String ns = key.location().getNamespace().toLowerCase(Locale.ROOT);
                    String path = key.location().getPath().toLowerCase(Locale.ROOT);
                    return ns.equals("irons_spellbooks")
                            || ns.equals("ars_nouveau")
                            || ns.equals("goety")
                            || path.contains("spell")
                            || path.contains("magic")
                            || path.contains("arcane")
                            || path.contains("eldritch")
                            || path.contains("holy");
                })
                .orElse(false);
    }

    /** 解析跨模组攻击源中的玩家归属 (支持直接施法、法术投射物、EchoCaster 残影、召唤物) */
    @Nullable
    private static Player resolvePlayerAttacker(DamageSource source) {
        if (source == null) return null;
        Entity causer = source.getEntity();
        if (causer instanceof Player p) return p;
        if (causer instanceof OwnableEntity ownable && ownable.getOwner() instanceof Player p) return p;
        if (causer instanceof TraceableEntity traceable && traceable.getOwner() instanceof Player p) return p;

        Entity direct = source.getDirectEntity();
        if (direct instanceof Player p) return p;
        if (direct instanceof OwnableEntity ownable && ownable.getOwner() instanceof Player p) return p;
        if (direct instanceof TraceableEntity traceable && traceable.getOwner() instanceof Player p) return p;
        return null;
    }

    /** 兼容多部件 Boss (PartEntity) 获取实际本体 LivingEntity */
    private static LivingEntity resolveActualLivingTarget(LivingEntity entity) {
        if (entity.isMultipartEntity() && entity.getParts() != null) {
            return entity;
        }
        return entity;
    }

    private static void resetInvulnerabilityRecursive(LivingEntity target) {
        target.invulnerableTime = 0;
        target.hurtTime = 0;
        if (target.isMultipartEntity() && target.getParts() != null) {
            for (PartEntity<?> part : target.getParts()) {
                if (part != null) {
                    part.invulnerableTime = 0;
                }
            }
        }
    }

    private static boolean isValidTarget(Player attacker, LivingEntity candidate) {
        if (candidate == attacker || !candidate.isAlive() || candidate instanceof ArmorStand) return false;
        if (candidate.isAlliedTo(attacker)) return false;
        if (candidate instanceof OwnableEntity ownable && attacker.getUUID().equals(ownable.getOwnerUUID())) {
            return false;
        }
        return true;
    }

    /** 检查玩家是否在 Curios 饰品栏 (如法术书槽) 或主副手装备了含「终末挽歌」的工具 */
    private static boolean isWearingOrHoldingElegy(Player player) {
        if (hasElegyModifier(player.getMainHandItem()) || hasElegyModifier(player.getOffhandItem())) {
            return true;
        }
        var inventory = CuriosApi.getCuriosInventory(player).resolve();
        if (inventory.isEmpty()) return false;
        for (var entry : inventory.get().getCurios().entrySet()) {
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

    private static void syncBookNbt(Player player, Map<UUID, EchoMark> marks) {
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
        int maxStoredTenths = Math.round(maxStored * 10.0f);
        int remainSec = (maxRemainTicks + 19) / 20;

        updateStackNbtIfElegy(player.getMainHandItem(), activeCount, maxStoredTenths, remainSec);
        updateStackNbtIfElegy(player.getOffhandItem(), activeCount, maxStoredTenths, remainSec);

        var inventory = CuriosApi.getCuriosInventory(player).resolve();
        if (inventory.isPresent()) {
            for (var entry : inventory.get().getCurios().entrySet()) {
                var stacks = entry.getValue().getStacks();
                for (int i = 0; i < stacks.getSlots(); i++) {
                    updateStackNbtIfElegy(stacks.getStackInSlot(i), activeCount, maxStoredTenths, remainSec);
                }
            }
        }
    }

    private static void updateStackNbtIfElegy(ItemStack stack, int activeCount, int maxStoredTenths, int remainSec) {
        if (!hasElegyModifier(stack)) return;
        ToolStack tool = ToolStack.from(stack);
        ModDataNBT data = tool.getPersistentData();
        if (data.getInt(SYNC_ACTIVE_TARGETS) != activeCount) {
            data.putInt(SYNC_ACTIVE_TARGETS, activeCount);
        }
        if (data.getInt(SYNC_MAX_STORED_TENTHS) != maxStoredTenths) {
            data.putInt(SYNC_MAX_STORED_TENTHS, maxStoredTenths);
        }
        if (data.getInt(SYNC_REMAIN_SEC) != remainSec) {
            data.putInt(SYNC_REMAIN_SEC, remainSec);
        }
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
            } catch (Throwable ignored) {
            }
        }
    }
}
