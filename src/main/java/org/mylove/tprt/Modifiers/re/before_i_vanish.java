package org.mylove.tprt.Modifiers.re;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mylove.tprt.Tprt;
import org.mylove.tprt.common.chtholly.ChthollyCombatHelper;
import org.mylove.tprt.common.chtholly.ChthollyDataKeys;
import org.mylove.tprt.registries.ModifierIds;
import slimeknights.mantle.client.TooltipKey;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.behavior.ToolDamageModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeHitModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.display.TooltipModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.InventoryTickModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.ranged.ProjectileHitModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.ranged.ProjectileLaunchModifierHook;
import slimeknights.tconstruct.library.modifiers.impl.NoLevelsModifier;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.helper.ToolDamageUtil;
import slimeknights.tconstruct.library.tools.item.IModifiable;
import slimeknights.tconstruct.library.tools.nbt.IModDataView;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;
import slimeknights.tconstruct.library.tools.nbt.ModifierNBT;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.library.tools.stat.ToolStats;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/*
  ============================================================
  在消失之前，我想留在某个人心中 (before_i_vanish)
  —— 挚爱 (chtholly) · 手柄/绑定结等近战辅件专属词条

  核心机制 —— 【托付之剑 · 自律护主】 (非数值堆砌, 纯交互机制):
    1) 脱手伴行 · 自律御剑:
       - 当这把武器位于快捷栏 (0~8 槽) 或副手、且未握在主手时 (例如切出法术书施法、用弓射击或进食),
         它会化作一柄悬浮在你身后的「托付之剑」伴随左右:
       - 自动劈落弹射物: 每 0.2 秒扫描自身周围 4.0 格内飞来的敌对弹射物
         (保护联机队友、三叉戟与匠魂投掷武器/浮漂/珍珠不被销毁), 自动挥剑将其拦截
       - 自动出鞘飞斩: 每 1.0 秒自动对靠近你 5.0 格内的敌对生物发动一次完整的匠魂近战飞斩
         (直接调用本武器完整面板与全部词条如「瑟尼欧里斯」, 并击退逼开贴身敌人)
    2) 在消失之前 · 燃尽重铸:
       - 当武器耐久即将归零损毁时, 绝不损坏, 而是保留最后 1 点耐久进入持续 15 秒的【消逝前夕】 (冷却 45 秒):
       - 期间武器免除一切耐久消耗, 自律飞斩间隔缩短至 0.5 秒;
       - 在这 15 秒内击败任意 1 名敌人 (或累计命中 6 次), 即可重聚记忆重铸剑身, 立即恢复 35% 最大耐久度
  ============================================================
 */
public class before_i_vanish extends NoLevelsModifier implements
        InventoryTickModifierHook,
        ToolDamageModifierHook,
        MeleeHitModifierHook,
        ProjectileLaunchModifierHook,
        ProjectileHitModifierHook,
        TooltipModifierHook {

    public static final ResourceLocation TOOL_UUID_KEY = ResourceLocation.fromNamespaceAndPath(Tprt.MODID, "bv_tool_uuid");

    public static String getOrCreateToolIdentifier(IToolStackView tool) {
        String id = tool.getPersistentData().getString(TOOL_UUID_KEY);
        if (id.isEmpty()) {
            id = java.util.UUID.randomUUID().toString();
            tool.getPersistentData().putString(TOOL_UUID_KEY, id);
        }
        return id;
    }

    // ===== 自律护主核心参数 =====
    public static final double PARRY_RADIUS = 4.0D;                  // 自动劈落敌对弹射物的扫描半径 (4.0 格)
    public static final int PARRY_INTERVAL_TICKS = 4;                // 弹射物拦截扫描间隔 (0.2 秒 = 4 ticks)
    public static final double AUTO_SLASH_RADIUS = 5.0D;             // 自律飞斩索敌半径 (5.0 格)
    public static final int AUTO_SLASH_INTERVAL_TICKS = 20;          // 常态自律飞斩间隔 (1.0 秒 = 20 ticks)
    public static final int VANISHING_SLASH_INTERVAL_TICKS = 10;     // 【消逝前夕】期间自律飞斩间隔 (0.5 秒 = 10 ticks)
    public static final float AUTO_SLASH_KNOCKBACK = 0.65f;          // 自律飞斩附加逼退力度

    // ===== 在消失之前 (耐久锁1 + 重铸剑身) 参数 =====
    public static final int VANISHING_DURATION_TICKS = 15 * 20;      // 【消逝前夕】持续时间 (15 秒 = 300 ticks)
    public static final int VANISHING_COOLDOWN_TICKS = 45 * 20;      // 【消逝前夕】冷却时间 (45 秒 = 900 ticks)
    public static final int REFORGE_REQUIRED_HITS = 6;               // 【消逝前夕】期间重铸剑身所需命中次数 (6 次, 或击杀 1 名敌人)
    public static final float REFORGE_DURABILITY_RATIO = 0.35f;      // 重铸剑身恢复的最大耐久比例 (35%)

    static {
        MinecraftForge.EVENT_BUS.addListener(before_i_vanish::onLivingDeath);
    }

    @Override
    protected void registerHooks(ModuleHookMap.@NotNull Builder hookBuilder) {
        super.registerHooks(hookBuilder);
        hookBuilder.addHook(this,
                ModifierHooks.INVENTORY_TICK,
                ModifierHooks.TOOL_DAMAGE,
                ModifierHooks.MELEE_HIT,
                ModifierHooks.PROJECTILE_LAUNCH,
                ModifierHooks.PROJECTILE_HIT,
                ModifierHooks.TOOLTIP);
    }

    // ============================================================
    //  1. 背包/快捷栏 Tick: 脱手悬浮护主 (劈弹射物 + 自律飞斩) & 时间戳到期单次清理
    // ============================================================

    @Override
    public void onInventoryTick(@NotNull IToolStackView tool, @NotNull ModifierEntry modifier, @NotNull Level world,
                                @NotNull LivingEntity holder, int itemSlot, boolean isSelected, boolean isCorrectSlot,
                                @NotNull ItemStack stack) {
        if (world.isClientSide || !(holder instanceof Player player) || !(world instanceof ServerLevel serverLevel)) {
            return;
        }
        if (!player.isAlive()) return;

        long now = serverLevel.getGameTime();
        ModDataNBT data = tool.getPersistentData();

        // 兼容旧存档一次性迁移 (彻底消除原先长达 60 秒每 tick 改 NBT 导致无法挖掘方块的问题)
        migrateLegacyTimersIfNeeded(data, now);

        long vanishUntil = ChthollyDataKeys.getLong(data, ChthollyDataKeys.BV_VANISH_UNTIL);
        boolean inGrace = ChthollyDataKeys.isTimerActive(now, vanishUntil, VANISHING_DURATION_TICKS);
        if (vanishUntil > 0L && !inGrace) {
            data.remove(ChthollyDataKeys.BV_VANISH_UNTIL);
            data.remove(ChthollyDataKeys.BV_VANISHING_HITS);
        }

        long cdUntil = ChthollyDataKeys.getLong(data, ChthollyDataKeys.BV_CD_UNTIL);
        if (cdUntil > 0L && !ChthollyDataKeys.isTimerActive(now, cdUntil, VANISHING_COOLDOWN_TICKS + VANISHING_DURATION_TICKS)) {
            data.remove(ChthollyDataKeys.BV_CD_UNTIL);
        }

        if (tool.isBroken() && !inGrace) {
            return;
        }

        // 判断是否处于「快捷栏 (0~8) 或副手」且「未握在主手」的自律护主状态（无状态纯函数判定，切栏零 NBT 写入）
        if (!isGuardianActive(player, stack)) {
            return;
        }

        // 身侧悬浮圣剑伴行视觉效果 (每 10 ticks 萦绕苍蓝星光与剑意)
        if (player.tickCount % 10 == 0) {
            spawnFloatingSwordAura(serverLevel, player, inGrace);
        }

        // 自动劈落飞来的跨模组敌对弹射物 (每 4 ticks 扫描 4.0 格，严格保护队友与三叉戟/投掷武器)
        if (player.tickCount % PARRY_INTERVAL_TICKS == 0) {
            ChthollyCombatHelper.parryIncomingProjectiles(serverLevel, player, PARRY_RADIUS);
        }

        // 自动出鞘飞斩靠近 5.0 格内的敌人 (常态 1.0 秒 / 消逝前夕 0.5 秒)
        int slashInterval = inGrace ? VANISHING_SLASH_INTERVAL_TICKS : AUTO_SLASH_INTERVAL_TICKS;
        if (player.tickCount % slashInterval == 0 && !ChthollyCombatHelper.isPerformingOffSlotAttack()) {
            performAutonomousGuardianSlash(serverLevel, player, tool, stack);
        }
    }

    /**
     * 自律飞斩: 选取 5.0 格内最具威胁的敌人, 调用本武器完整的匠魂近战攻击流水线出鞘斩击
     */
    private static void performAutonomousGuardianSlash(ServerLevel serverLevel, Player player, IToolStackView tool, ItemStack swordStack) {
        AABB searchBox = player.getBoundingBox().inflate(AUTO_SLASH_RADIUS, 3.0D, AUTO_SLASH_RADIUS);
        List<LivingEntity> candidates = serverLevel.getEntitiesOfClass(LivingEntity.class, searchBox,
                entity -> isValidSlashTarget(player, entity));
        if (candidates.isEmpty()) return;

        // 优先锁定正在以玩家为目标的怪物, 其次按距离最近排序
        candidates.sort(Comparator
                .<LivingEntity, Boolean>comparing(e -> !(e instanceof Mob mob && mob.getTarget() == player))
                .thenComparingDouble(player::distanceToSqr));

        LivingEntity target = candidates.get(0);
        ChthollyCombatHelper.performOffSlotAttacks(player, swordStack, tool, target, 1, AUTO_SLASH_KNOCKBACK,
                idx -> spawnAutoSlashParticles(serverLevel, player, target));
    }

    // ============================================================
    //  2. 「在消失之前」—— 拦截耐久归零, 开启 15 秒【消逝前夕】并通过命中/击杀重铸剑身
    // ============================================================

    @Override
    public int onDamageTool(@NotNull IToolStackView tool, @NotNull ModifierEntry modifier, int amount,
                            @Nullable LivingEntity holder) {
        if (amount <= 0) return 0;
        long now = holder != null ? holder.level().getGameTime() : 0L;
        ModDataNBT data = tool.getPersistentData();
        if (now > 0L) {
            migrateLegacyTimersIfNeeded(data, now);
        }

        // 若已处于【消逝前夕】15 秒内, 完全免除一切耐久损耗, 绝不碎裂
        if (now > 0L && isGraceActive(data, now)) {
            return 0;
        }

        int currentDurability = tool.getCurrentDurability();
        // 当本次耐久损耗足以使武器损坏归零 ("即将消失") 时:
        if (currentDurability - amount <= 0 && now > 0L) {
            if (!isCooldownActive(data, now)) {
                ChthollyDataKeys.putLong(data, ChthollyDataKeys.BV_VANISH_UNTIL, now + VANISHING_DURATION_TICKS);
                ChthollyDataKeys.putLong(data, ChthollyDataKeys.BV_CD_UNTIL,
                        now + VANISHING_DURATION_TICKS + VANISHING_COOLDOWN_TICKS);
                data.putInt(ChthollyDataKeys.BV_VANISHING_HITS, 0);

                if (holder instanceof Player player && !player.level().isClientSide) {
                    player.displayClientMessage(
                            Component.translatable("message.tprt.before_i_vanish.triggered"), true);
                    player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 0.7f, 1.35f);
                }
                // 保留最后 1 点耐久不损坏
                return Math.max(0, currentDurability - 1);
            }
        }
        return amount;
    }

    @Override
    public void afterMeleeHit(@NotNull IToolStackView tool, @NotNull ModifierEntry modifier,
                              @NotNull ToolAttackContext context, float damageDealt) {
        Player attacker = context.getPlayerAttacker();
        LivingEntity target = ChthollyCombatHelper.resolveLivingTarget(context.getTarget());
        if (target == null) {
            target = context.getLivingTarget();
        }
        if (attacker == null || target == null || attacker.level().isClientSide || damageDealt <= 0.0f) return;

        long now = attacker.level().getGameTime();
        ModDataNBT data = tool.getPersistentData();
        if (!isGraceActive(data, now)) return;

        // 【消逝前夕】期间每次命中累积重铸进度; 若直接斩杀目标或满 6 次命中则立即重铸剑身
        int hits = data.getInt(ChthollyDataKeys.BV_VANISHING_HITS) + 1;
        if (!target.isAlive() || target.isDeadOrDying() || hits >= REFORGE_REQUIRED_HITS) {
            reforgeSword(attacker, tool, data, now);
        } else {
            data.putInt(ChthollyDataKeys.BV_VANISHING_HITS, hits);
        }
    }

    @Override
    public void onProjectileLaunch(@NotNull IToolStackView tool, @NotNull ModifierEntry modifier,
                                   @NotNull LivingEntity shooter, @NotNull Projectile projectile,
                                   @Nullable AbstractArrow arrow, @NotNull ModDataNBT persistentData,
                                   boolean primary) {
        String id = getOrCreateToolIdentifier(tool);
        persistentData.putString(TOOL_UUID_KEY, id);
        long now = shooter.level().getGameTime();
        if (isGraceActive(tool.getPersistentData(), now)) {
            ChthollyDataKeys.putLong(persistentData, ChthollyDataKeys.BV_VANISH_UNTIL,
                    ChthollyDataKeys.getLong(tool.getPersistentData(), ChthollyDataKeys.BV_VANISH_UNTIL));
            persistentData.putInt(ChthollyDataKeys.BV_VANISHING_HITS,
                    tool.getPersistentData().getInt(ChthollyDataKeys.BV_VANISHING_HITS));
        }
    }

    @Override
    public boolean onProjectileHitEntity(@NotNull ModifierNBT modifiers, @NotNull ModDataNBT persistentData,
                                         @NotNull ModifierEntry modifier, @NotNull Projectile projectile,
                                         @NotNull EntityHitResult hit, @Nullable LivingEntity attacker,
                                         @Nullable LivingEntity target, boolean notBlocked) {
        if (attacker instanceof Player player && target != null && !player.level().isClientSide) {
            long now = player.level().getGameTime();
            if (isGraceActive(persistentData, now)) {
                int hits = persistentData.getInt(ChthollyDataKeys.BV_VANISHING_HITS) + 1;
                boolean shouldReforge = !target.isAlive() || target.isDeadOrDying() || hits >= REFORGE_REQUIRED_HITS;
                persistentData.putInt(ChthollyDataKeys.BV_VANISHING_HITS, hits);

                String toolId = persistentData.getString(TOOL_UUID_KEY);
                IToolStackView matchedTool = !toolId.isEmpty() ? findToolByUuid(player, toolId) : null;
                if (matchedTool != null) {
                    ModDataNBT toolData = matchedTool.getPersistentData();
                    if (isGraceActive(toolData, now)) {
                        int toolHits = toolData.getInt(ChthollyDataKeys.BV_VANISHING_HITS) + 1;
                        if (shouldReforge || toolHits >= REFORGE_REQUIRED_HITS) {
                            reforgeSword(player, matchedTool, toolData, now);
                            persistentData.remove(ChthollyDataKeys.BV_VANISH_UNTIL);
                            persistentData.remove(ChthollyDataKeys.BV_VANISHING_HITS);
                        } else {
                            toolData.putInt(ChthollyDataKeys.BV_VANISHING_HITS, toolHits);
                        }
                    }
                } else if (shouldReforge) {
                    persistentData.remove(ChthollyDataKeys.BV_VANISH_UNTIL);
                    persistentData.remove(ChthollyDataKeys.BV_VANISHING_HITS);
                    ChthollyDataKeys.putLong(persistentData, ChthollyDataKeys.BV_CD_UNTIL, now + VANISHING_COOLDOWN_TICKS);
                }
            }
        }
        return notBlocked;
    }

    private static IToolStackView findToolByUuid(Player player, String toolId) {
        for (int i = 0; i < player.getInventory().items.size(); i++) {
            ItemStack stack = player.getInventory().items.get(i);
            if (!stack.isEmpty() && stack.getItem() instanceof IModifiable && ToolStack.isInitialized(stack)) {
                ToolStack tool = ToolStack.from(stack);
                if (toolId.equals(tool.getPersistentData().getString(TOOL_UUID_KEY))) {
                    return tool;
                }
            }
        }
        ItemStack offhand = player.getOffhandItem();
        if (!offhand.isEmpty() && offhand.getItem() instanceof IModifiable && ToolStack.isInitialized(offhand)) {
            ToolStack tool = ToolStack.from(offhand);
            if (toolId.equals(tool.getPersistentData().getString(TOOL_UUID_KEY))) {
                return tool;
            }
        }
        return null;
    }

    private static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity dead = event.getEntity();
        if (dead == null || dead.level().isClientSide || event.isCanceled()) return;
        Player player = ChthollyCombatHelper.resolvePlayerAttacker(event.getSource());
        if (player == null || player == dead) return;

        long now = player.level().getGameTime();
        // 若玩家身上的含本词条武器正处于【消逝前夕】, 击杀任意敌人立即重铸剑身
        for (int i = 0; i < player.getInventory().items.size(); i++) {
            tryReforgeOnKill(player, player.getInventory().items.get(i), now);
        }
        tryReforgeOnKill(player, player.getOffhandItem(), now);
    }

    private static void tryReforgeOnKill(Player player, ItemStack stack, long now) {
        if (stack.isEmpty() || !(stack.getItem() instanceof IModifiable) || !ToolStack.isInitialized(stack)) {
            return;
        }
        ToolStack tool = ToolStack.from(stack);
        if (tool.getModifierLevel(ModifierIds.before_i_vanish) <= 0) return;
        ModDataNBT data = tool.getPersistentData();
        if (isGraceActive(data, now)) {
            reforgeSword(player, tool, data, now);
        }
    }

    /**
     * 铭心重铸: 恢复 35% 最大耐久度并开启 45 秒冷却
     */
    private static void reforgeSword(Player player, IToolStackView tool, ModDataNBT data, long now) {
        data.remove(ChthollyDataKeys.BV_VANISH_UNTIL);
        data.remove(ChthollyDataKeys.BV_VANISHING_HITS);
        ChthollyDataKeys.putLong(data, ChthollyDataKeys.BV_CD_UNTIL, now + VANISHING_COOLDOWN_TICKS);

        int maxDurability = tool.getStats().getInt(ToolStats.DURABILITY);
        int restore = Math.max(10, Math.round(maxDurability * REFORGE_DURABILITY_RATIO));
        ToolDamageUtil.repair(tool, restore);

        player.displayClientMessage(
                Component.translatable("message.tprt.before_i_vanish.reforged", restore), true);
        if (player.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                    player.getX(), player.getY() + 1.1, player.getZ(),
                    20, 0.4, 0.4, 0.4, 0.05);
            serverLevel.sendParticles(ParticleTypes.END_ROD,
                    player.getX(), player.getY() + 1.1, player.getZ(),
                    16, 0.4, 0.4, 0.4, 0.04);
            serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.CHORUS_FLOWER_GROW, SoundSource.PLAYERS, 0.9f, 1.3f);
        }
    }

    // ============================================================
    //  3. 动态 Tooltip 面板展示
    // ============================================================

    @Override
    public void addTooltip(@NotNull IToolStackView tool, @NotNull ModifierEntry modifier, @Nullable Player player,
                           @NotNull List<Component> tooltip, @NotNull TooltipKey tooltipKey, @NotNull TooltipFlag tooltipFlag) {
        getOrCreateToolIdentifier(tool);
        IModDataView data = tool.getPersistentData();
        long now = player != null ? player.level().getGameTime() : 0L;
        int graceSec = now > 0L ? getGraceRemainingSeconds(data, now) : 0;
        int cdSec = now > 0L ? getCooldownRemainingSeconds(data, now) : 0;
        int hits = data.getInt(ChthollyDataKeys.BV_VANISHING_HITS);
        boolean guardianActive = player != null && isGuardianActiveForTool(player, tool);

        if (graceSec > 0) {
            tooltip.add(Component.translatable("modifier.tprt.before_i_vanish.tooltip.vanishing",
                    graceSec, hits, REFORGE_REQUIRED_HITS));
        } else if (guardianActive) {
            tooltip.add(Component.translatable("modifier.tprt.before_i_vanish.tooltip.guardian_active"));
        } else if (isEligibleGuardianWeapon(tool)) {
            tooltip.add(Component.translatable("modifier.tprt.before_i_vanish.tooltip.held"));
        }

        if (cdSec > 0 && graceSec <= 0) {
            tooltip.add(Component.translatable("modifier.tprt.before_i_vanish.tooltip.cooldown", cdSec));
        }

        if (tooltipKey == TooltipKey.SHIFT) {
            tooltip.add(Component.translatable("modifier.tprt.before_i_vanish.tooltip.detail_guardian",
                    String.format("%.1f", PARRY_RADIUS), String.format("%.1f", AUTO_SLASH_RADIUS)));
            tooltip.add(Component.translatable("modifier.tprt.before_i_vanish.tooltip.detail_reforge",
                    VANISHING_DURATION_TICKS / 20, REFORGE_REQUIRED_HITS, (int) (REFORGE_DURABILITY_RATIO * 100)));
        } else {
            tooltip.add(Component.translatable("modifier.tprt.before_i_vanish.tooltip.shift_hint"));
        }
    }

    // ============================================================
    //  供客户端 HUD 与内部共用的无状态判定方法
    // ============================================================

    public static boolean isGraceActive(IModDataView data, long now) {
        long until = ChthollyDataKeys.getLong(data, ChthollyDataKeys.BV_VANISH_UNTIL);
        return ChthollyDataKeys.isTimerActive(now, until, VANISHING_DURATION_TICKS);
    }

    public static boolean isCooldownActive(IModDataView data, long now) {
        long until = ChthollyDataKeys.getLong(data, ChthollyDataKeys.BV_CD_UNTIL);
        return ChthollyDataKeys.isTimerActive(now, until, VANISHING_COOLDOWN_TICKS + VANISHING_DURATION_TICKS);
    }

    public static int getGraceRemainingTicks(IModDataView data, long now) {
        long until = ChthollyDataKeys.getLong(data, ChthollyDataKeys.BV_VANISH_UNTIL);
        return ChthollyDataKeys.getRemainingTicks(now, until, VANISHING_DURATION_TICKS);
    }

    public static int getGraceRemainingSeconds(IModDataView data, long now) {
        int ticks = getGraceRemainingTicks(data, now);
        return ticks > 0 ? (ticks + 19) / 20 : 0;
    }

    public static int getCooldownRemainingTicks(IModDataView data, long now) {
        if (isGraceActive(data, now)) return 0;
        long until = ChthollyDataKeys.getLong(data, ChthollyDataKeys.BV_CD_UNTIL);
        return ChthollyDataKeys.getRemainingTicks(now, until, VANISHING_COOLDOWN_TICKS + VANISHING_DURATION_TICKS);
    }

    public static int getCooldownRemainingSeconds(IModDataView data, long now) {
        int ticks = getCooldownRemainingTicks(data, now);
        return ticks > 0 ? (ticks + 19) / 20 : 0;
    }

    /** 判断指定 ItemStack 是否正作为玩家的第一把脱手悬浮守护圣剑生效 */
    public static boolean isGuardianActive(Player player, ItemStack currentStack) {
        if (currentStack.isEmpty() || currentStack == player.getMainHandItem()) {
            return false;
        }
        ItemStack first = findFirstGuardianSwordStack(player);
        return !first.isEmpty() && first == currentStack;
    }

    @NotNull
    public static ItemStack findFirstGuardianSwordStack(Player player) {
        ItemStack mainHand = player.getMainHandItem();
        long now = player.level().getGameTime();
        ItemStack bestCandidate = ItemStack.EMPTY;
        int bestPriority = -1;

        for (int i = 0; i < Inventory.getSelectionSize(); i++) {
            ItemStack candidate = player.getInventory().getItem(i);
            if (candidate == mainHand || candidate.isEmpty()) continue;
            int priority = getGuardianCandidatePriority(candidate, now);
            if (priority > bestPriority) {
                bestPriority = priority;
                bestCandidate = candidate;
                if (bestPriority >= 3) {
                    return bestCandidate;
                }
            }
        }
        ItemStack offhand = player.getOffhandItem();
        if (offhand != mainHand && !offhand.isEmpty()) {
            int priority = getGuardianCandidatePriority(offhand, now);
            if (priority > bestPriority) {
                bestCandidate = offhand;
            }
        }
        return bestCandidate;
    }

    /**
     * 计算脱手悬浮护主候选优先级（<=0 表示不可作为悬浮圣剑）：
     *   3 = 剑/劈刀类 (TinkerTags.Items.SWORD)
     *   2 = 主近战武器 (如短剑/斧/镰刀/标枪等 TinkerTags.Items.MELEE_PRIMARY)
     *   1 = 其他具备有效攻击面板的近战工具 (如镐/锄等)
     * 纯远程发射器（长弓/弩/钓竿）与弹药因弓把部件可能携带本词条，必须排除以防抢占近战圣剑护主槽位。
     */
    private static int getGuardianCandidatePriority(ItemStack stack, long now) {
        if (stack.isEmpty() || !(stack.getItem() instanceof IModifiable) || !ToolStack.isInitialized(stack)) {
            return 0;
        }
        if (stack.is(TinkerTags.Items.BOWS) || stack.is(TinkerTags.Items.FISHING_RODS)
                || stack.is(TinkerTags.Items.AMMO)) {
            return 0;
        }
        ToolStack tool = ToolStack.from(stack);
        if (tool.getModifierLevel(ModifierIds.before_i_vanish) <= 0) {
            return 0;
        }
        if (!isEligibleGuardianWeapon(tool)) {
            return 0;
        }
        if (tool.isBroken() && !isGraceActive(tool.getPersistentData(), now)) {
            return 0;
        }
        if (stack.is(TinkerTags.Items.SWORD)) return 3;
        if (stack.is(TinkerTags.Items.MELEE_PRIMARY)) return 2;
        return 1;
    }

    private static boolean isEligibleGuardianWeapon(IToolStackView tool) {
        if (tool.hasTag(TinkerTags.Items.BOWS) || tool.hasTag(TinkerTags.Items.FISHING_RODS)
                || tool.hasTag(TinkerTags.Items.AMMO)) {
            return false;
        }
        return tool.getStats().get(ToolStats.ATTACK_DAMAGE) > 0.0f
                || tool.hasTag(TinkerTags.Items.MELEE_PRIMARY);
    }

    private static boolean isGuardianActiveForTool(Player player, IToolStackView targetTool) {
        if (!isEligibleGuardianWeapon(targetTool)) return false;
        ItemStack activeStack = findFirstGuardianSwordStack(player);
        if (activeStack.isEmpty()) return false;
        return matchesToolView(activeStack, targetTool)
                && (player.getMainHandItem().isEmpty() || !matchesToolView(player.getMainHandItem(), targetTool));
    }

    private static boolean matchesToolView(ItemStack stack, IToolStackView targetTool) {
        if (stack.isEmpty() || stack.getItem() != targetTool.getItem()
                || !(stack.getItem() instanceof IModifiable) || !ToolStack.isInitialized(stack)) {
            return false;
        }
        ToolStack tool = ToolStack.from(stack);
        String id1 = tool.getPersistentData().getString(TOOL_UUID_KEY);
        String id2 = targetTool.getPersistentData().getString(TOOL_UUID_KEY);
        if (!id1.isEmpty() && !id2.isEmpty()) {
            return id1.equals(id2);
        }
        if (id1.isEmpty() && !tool.isBroken()) {
            id1 = getOrCreateToolIdentifier(tool);
        }
        if (id2.isEmpty()) {
            id2 = getOrCreateToolIdentifier(targetTool);
        }
        if (!id1.isEmpty() && !id2.isEmpty()) {
            return id1.equals(id2);
        }
        return tool.getDamage() == targetTool.getDamage()
                && tool.getModifierLevel(ModifierIds.before_i_vanish) == targetTool.getModifierLevel(ModifierIds.before_i_vanish)
                && Objects.equals(tool.getMaterials(), targetTool.getMaterials())
                && ChthollyDataKeys.getLong(tool.getPersistentData(), ChthollyDataKeys.BV_VANISH_UNTIL)
                == ChthollyDataKeys.getLong(targetTool.getPersistentData(), ChthollyDataKeys.BV_VANISH_UNTIL)
                && ChthollyDataKeys.getLong(tool.getPersistentData(), ChthollyDataKeys.BV_CD_UNTIL)
                == ChthollyDataKeys.getLong(targetTool.getPersistentData(), ChthollyDataKeys.BV_CD_UNTIL);
    }

    private static void migrateLegacyTimersIfNeeded(ModDataNBT data, long now) {
        int legacyGrace = data.getInt(ChthollyDataKeys.LEGACY_BV_VANISHING_TICKS);
        if (legacyGrace > 0) {
            data.remove(ChthollyDataKeys.LEGACY_BV_VANISHING_TICKS);
            ChthollyDataKeys.putLong(data, ChthollyDataKeys.BV_VANISH_UNTIL, now + legacyGrace);
        }
        int legacyCd = data.getInt(ChthollyDataKeys.LEGACY_BV_VANISHING_CD);
        if (legacyCd > 0) {
            data.remove(ChthollyDataKeys.LEGACY_BV_VANISHING_CD);
            ChthollyDataKeys.putLong(data, ChthollyDataKeys.BV_CD_UNTIL, now + legacyCd);
        }
        if (data.getBoolean(ChthollyDataKeys.LEGACY_BV_GUARDIAN_ACTIVE)) {
            data.remove(ChthollyDataKeys.LEGACY_BV_GUARDIAN_ACTIVE);
        }
    }

    private static boolean isValidSlashTarget(Player player, LivingEntity candidate) {
        if (!ChthollyCombatHelper.isValidHostileTarget(player, candidate)) {
            return false;
        }
        // 仅自动斩击敌对生物、正在攻击玩家的生物或与玩家交战中的目标, 不误伤温顺动物/村民/中立生物
        if (candidate instanceof Mob mob && mob.getTarget() == player) return true;
        if (player.getLastHurtMob() == candidate || player.getLastHurtByMob() == candidate) return true;
        return candidate instanceof Enemy;
    }

    private static void spawnFloatingSwordAura(ServerLevel serverLevel, Player player, boolean vanishing) {
        double angle = (player.tickCount * 0.18D) % (Math.PI * 2.0D);
        double ox = Math.cos(angle) * 0.75D;
        double oz = Math.sin(angle) * 0.75D;
        double x = player.getX() + ox;
        double y = player.getY() + 1.15D + Math.sin(angle * 2.0D) * 0.15D;
        double z = player.getZ() + oz;

        serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, x, y, z, 2, 0.05, 0.08, 0.05, 0.01);
        serverLevel.sendParticles(ParticleTypes.END_ROD, x, y, z, 1, 0.03, 0.05, 0.03, 0.005);
        if (vanishing) {
            serverLevel.sendParticles(ParticleTypes.FLAME, x, y, z, 2, 0.05, 0.08, 0.05, 0.01);
        }
    }

    private static void spawnAutoSlashParticles(ServerLevel serverLevel, Player player, LivingEntity target) {
        double tx = target.getX();
        double ty = target.getY() + target.getBbHeight() * 0.55D;
        double tz = target.getZ();

        Vec3 from = player.position().add(0.0D, 1.2D, 0.0D);
        Vec3 to = new Vec3(tx, ty, tz);
        Vec3 diff = to.subtract(from);
        int steps = 6;
        for (int i = 1; i <= steps; i++) {
            double ratio = (double) i / steps;
            Vec3 p = from.add(diff.scale(ratio));
            serverLevel.sendParticles(ParticleTypes.END_ROD, p.x, p.y, p.z, 1, 0.02, 0.02, 0.02, 0.0);
        }

        serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK, tx, ty, tz, 1, 0.0, 0.0, 0.0, 0.0);
        serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, tx, ty, tz, 10, 0.3, 0.3, 0.3, 0.04);
        serverLevel.playSound(null, tx, ty, tz, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.7f, 1.3f);
    }
}
