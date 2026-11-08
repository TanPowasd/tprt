package org.mylove.tprt.Modifiers.re;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.TraceableEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.entity.PartEntity;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mylove.tprt.Tprt;
import org.mylove.tprt.registries.ModifierIds;
import slimeknights.mantle.client.TooltipKey;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import net.minecraft.world.phys.EntityHitResult;
import slimeknights.tconstruct.library.modifiers.hook.behavior.ToolDamageModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeHitModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.display.TooltipModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.InventoryTickModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.ranged.ProjectileHitModifierHook;
import slimeknights.tconstruct.library.modifiers.impl.NoLevelsModifier;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.helper.ToolAttackUtil;
import slimeknights.tconstruct.library.tools.helper.ToolDamageUtil;
import slimeknights.tconstruct.library.tools.item.IModifiable;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;
import slimeknights.tconstruct.library.tools.nbt.ModifierNBT;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.library.tools.stat.ToolStats;

import java.util.Comparator;
import java.util.List;

/*
  ============================================================
  在消失之前，我想留在某个人心中 (before_i_vanish)
  —— 挚爱 (chtholly) · 顶端 (tconstruct:head) 专属词条

  核心机制 —— 【托付之剑 · 自律护主】 (非数值堆砌, 纯交互机制):
    1) 脱手伴行 · 自律御剑:
       - 当这把武器位于快捷栏 (0~8 槽) 或副手、且未握在主手时 (例如切出法术书施法、用弓射击或进食),
         它会化作一柄悬浮在你身后的「托付之剑」伴随左右:
       - 自动劈落弹射物: 每 0.2 秒扫描自身周围 4.0 格内飞来的敌对弹射物
         (兼容原版箭矢/火球与铁魔法、灾变、冰火传说等跨模组弹射物), 自动挥剑将其当场劈碎拦截
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
        ProjectileHitModifierHook,
        TooltipModifierHook {

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

    // ===== 工具持久化 NBT 键 =====
    private static final ResourceLocation KEY_VANISHING_TICKS = Tprt.getResource("vanish_grace_ticks");
    private static final ResourceLocation KEY_VANISHING_CD = Tprt.getResource("vanish_cooldown_ticks");
    private static final ResourceLocation KEY_VANISHING_HITS = Tprt.getResource("vanish_reforge_hits");
    private static final ResourceLocation KEY_GUARDIAN_ACTIVE = Tprt.getResource("vanish_guardian_active");

    /** 递归重入保护: 防止自律飞斩触发的 performAttack 再次引发嵌套飞斩 */
    private static boolean PERFORMING_AUTO_SLASH = false;

    public before_i_vanish() {
        MinecraftForge.EVENT_BUS.addListener(this::onLivingDeath);
    }

    @Override
    protected void registerHooks(ModuleHookMap.@NotNull Builder hookBuilder) {
        super.registerHooks(hookBuilder);
        hookBuilder.addHook(this,
                ModifierHooks.INVENTORY_TICK,
                ModifierHooks.TOOL_DAMAGE,
                ModifierHooks.MELEE_HIT,
                ModifierHooks.PROJECTILE_HIT,
                ModifierHooks.TOOLTIP);
    }

    // ============================================================
    //  1. 背包/快捷栏 Tick: 脱手悬浮护主 (劈弹射物 + 自律飞斩) & 消逝前夕计时
    // ============================================================

    @Override
    public void onInventoryTick(@NotNull IToolStackView tool, @NotNull ModifierEntry modifier, @NotNull Level world,
                                @NotNull LivingEntity holder, int itemSlot, boolean isSelected, boolean isCorrectSlot,
                                @NotNull ItemStack stack) {
        if (world.isClientSide || !(holder instanceof Player player) || !(world instanceof ServerLevel serverLevel)) {
            return;
        }
        if (!player.isAlive()) return;

        ModDataNBT data = tool.getPersistentData();

        // 1) 更新【消逝前夕】与冷却倒计时
        int vanishingTicks = data.getInt(KEY_VANISHING_TICKS);
        if (vanishingTicks > 0) {
            vanishingTicks--;
            if (vanishingTicks > 0) {
                data.putInt(KEY_VANISHING_TICKS, vanishingTicks);
            } else {
                data.remove(KEY_VANISHING_TICKS);
                data.remove(KEY_VANISHING_HITS);
            }
        }

        int cdTicks = data.getInt(KEY_VANISHING_CD);
        if (cdTicks > 0) {
            cdTicks--;
            if (cdTicks > 0) {
                data.putInt(KEY_VANISHING_CD, cdTicks);
            } else {
                data.remove(KEY_VANISHING_CD);
            }
        }

        if (tool.isBroken() && vanishingTicks <= 0) {
            if (data.getBoolean(KEY_GUARDIAN_ACTIVE)) {
                data.remove(KEY_GUARDIAN_ACTIVE);
            }
            return;
        }

        // 2) 判断是否处于「快捷栏 (0~8) 或副手」且「未握在主手」的自律护主状态
        boolean inHotbarOrOffhand = (itemSlot >= 0 && itemSlot < Inventory.getSelectionSize())
                || player.getOffhandItem() == stack;
        boolean heldInMainHand = isSelected || player.getMainHandItem() == stack;
        boolean guardianMode = inHotbarOrOffhand && !heldInMainHand && isFirstGuardianSwordInInventory(player, stack);

        if (data.getBoolean(KEY_GUARDIAN_ACTIVE) != guardianMode) {
            if (guardianMode) {
                data.putBoolean(KEY_GUARDIAN_ACTIVE, true);
            } else {
                data.remove(KEY_GUARDIAN_ACTIVE);
            }
        }

        if (!guardianMode) return;

        // 3) 身侧悬浮圣剑伴行视觉效果 (每 10 ticks 萦绕苍蓝星光与剑意)
        if (player.tickCount % 10 == 0) {
            spawnFloatingSwordAura(serverLevel, player, vanishingTicks > 0);
        }

        // 4) 自动劈落飞来的跨模组敌对弹射物 (每 4 ticks 扫描 4.0 格)
        if (player.tickCount % PARRY_INTERVAL_TICKS == 0) {
            parryIncomingProjectiles(serverLevel, player);
        }

        // 5) 自动出鞘飞斩靠近 5.0 格内的敌人 (常态 1.0 秒 / 消逝前夕 0.5 秒)
        int slashInterval = vanishingTicks > 0 ? VANISHING_SLASH_INTERVAL_TICKS : AUTO_SLASH_INTERVAL_TICKS;
        if (player.tickCount % slashInterval == 0 && !PERFORMING_AUTO_SLASH) {
            performAutonomousGuardianSlash(serverLevel, player, tool, stack);
        }
    }

    /**
     * 自动劈落周围飞向玩家的跨模组敌对弹射物
     */
    private static void parryIncomingProjectiles(ServerLevel serverLevel, Player player) {
        AABB box = player.getBoundingBox().inflate(PARRY_RADIUS);
        List<Projectile> projectiles = serverLevel.getEntitiesOfClass(Projectile.class, box,
                proj -> isHostileIncomingProjectile(player, proj));
        if (projectiles.isEmpty()) return;

        int parried = 0;
        for (Projectile proj : projectiles) {
            double px = proj.getX();
            double py = proj.getY() + proj.getBbHeight() * 0.5;
            double pz = proj.getZ();

            // 剑光斩碎弹射物
            serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK, px, py, pz, 1, 0.0, 0.0, 0.0, 0.0);
            serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, px, py, pz, 6, 0.15, 0.15, 0.15, 0.03);
            serverLevel.sendParticles(ParticleTypes.END_ROD, px, py, pz, 4, 0.1, 0.1, 0.1, 0.02);
            proj.discard();
            parried++;
        }

        if (parried > 0) {
            serverLevel.playSound(null, player.getX(), player.getY() + 1.0, player.getZ(),
                    SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 0.75f, 1.45f);
            serverLevel.playSound(null, player.getX(), player.getY() + 1.0, player.getZ(),
                    SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8f, 1.35f);
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

        // 计算武器自身的完整基础攻击力 (即使玩家主手拿着法术书或食物, 也能打出圣剑本身的真实面板)
        float toolAttack = tool.getStats().get(ToolStats.ATTACK_DAMAGE) + 1.0f;
        float playerAttack = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        float baseDamage = Math.max(toolAttack, playerAttack);

        ToolAttackContext context = ToolAttackContext.attacker(player)
                .target(target)
                .hand(InteractionHand.MAIN_HAND)
                .baseDamage(baseDamage)
                .baseKnockback(AUTO_SLASH_KNOCKBACK)
                .cooldown(1.0f)
                .extraAttack()
                .build();

        boolean wasSprinting = player.isSprinting();
        Vec3 prevVelocity = player.getDeltaMovement();
        ItemStack prevMainHand = player.getMainHandItem();
        PERFORMING_AUTO_SLASH = true;
        try {
            // 临时将圣剑映射至主手，兼容 SakuraTinker 等读取主手物品栈的外部 Mixin，避免非匠魂物品告警
            player.setItemInHand(InteractionHand.MAIN_HAND, swordStack);
            resetInvulnerabilityRecursive(target);
            ToolAttackUtil.performAttack(tool, context);
            spawnAutoSlashParticles(serverLevel, player, target);
        } finally {
            player.setItemInHand(InteractionHand.MAIN_HAND, prevMainHand);
            PERFORMING_AUTO_SLASH = false;
            // 保持玩家原本的冲刺与移动速度不被打断
            if (wasSprinting) {
                player.setSprinting(true);
            }
            player.setDeltaMovement(prevVelocity);
        }
    }

    // ============================================================
    //  2. 「在消失之前」—— 拦截耐久归零, 开启 15 秒【消逝前夕】并通过命中/击杀重铸剑身
    // ============================================================

    @Override
    public int onDamageTool(@NotNull IToolStackView tool, @NotNull ModifierEntry modifier, int amount,
                            @Nullable LivingEntity holder) {
        if (amount <= 0) return 0;
        ModDataNBT data = tool.getPersistentData();

        // 若已处于【消逝前夕】15 秒内, 完全免除一切耐久损耗, 绝不碎裂
        if (data.getInt(KEY_VANISHING_TICKS) > 0) {
            return 0;
        }

        int currentDurability = tool.getCurrentDurability();
        // 当本次耐久损耗足以使武器损坏归零 ("即将消失") 时:
        if (currentDurability - amount <= 0) {
            if (data.getInt(KEY_VANISHING_CD) <= 0) {
                data.putInt(KEY_VANISHING_TICKS, VANISHING_DURATION_TICKS);
                data.putInt(KEY_VANISHING_CD, VANISHING_COOLDOWN_TICKS);
                data.putInt(KEY_VANISHING_HITS, 0);

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
        LivingEntity target = context.getLivingTarget();
        if (attacker == null || target == null || attacker.level().isClientSide || damageDealt <= 0.0f) return;

        ModDataNBT data = tool.getPersistentData();
        if (data.getInt(KEY_VANISHING_TICKS) <= 0) return;

        // 【消逝前夕】期间每次命中累积重铸进度; 若直接斩杀目标或满 6 次命中则立即重铸剑身
        int hits = data.getInt(KEY_VANISHING_HITS) + 1;
        if (!target.isAlive() || target.isDeadOrDying() || hits >= REFORGE_REQUIRED_HITS) {
            reforgeSword(attacker, tool, data);
        } else {
            data.putInt(KEY_VANISHING_HITS, hits);
        }
    }

    @Override
    public boolean onProjectileHitEntity(@NotNull ModifierNBT modifiers, @NotNull ModDataNBT persistentData,
                                         @NotNull ModifierEntry modifier, @NotNull Projectile projectile,
                                         @NotNull EntityHitResult hit, @Nullable LivingEntity attacker,
                                         @Nullable LivingEntity target, boolean notBlocked) {
        if (attacker instanceof Player player && target != null && !player.level().isClientSide) {
            for (InteractionHand hand : InteractionHand.values()) {
                ItemStack held = player.getItemInHand(hand);
                if (!held.isEmpty() && held.getItem() instanceof IModifiable && ToolStack.isInitialized(held)) {
                    ToolStack bowTool = ToolStack.from(held);
                    if (bowTool.getModifierLevel(ModifierIds.before_i_vanish) > 0) {
                        ModDataNBT data = bowTool.getPersistentData();
                        if (data.getInt(KEY_VANISHING_TICKS) > 0) {
                            int hits = data.getInt(KEY_VANISHING_HITS) + 1;
                            if (!target.isAlive() || target.isDeadOrDying() || hits >= REFORGE_REQUIRED_HITS) {
                                reforgeSword(player, bowTool, data);
                            } else {
                                data.putInt(KEY_VANISHING_HITS, hits);
                            }
                            break;
                        }
                    }
                }
            }
        }
        return notBlocked;
    }

    private void onLivingDeath(LivingDeathEvent event) {
        LivingEntity dead = event.getEntity();
        if (dead == null || dead.level().isClientSide) return;
        Entity causer = event.getSource().getEntity();
        if (!(causer instanceof Player player)) return;

        // 若玩家身上的含本词条武器正处于【消逝前夕】, 击杀任意敌人立即重铸剑身
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            tryReforgeOnKill(player, stack);
        }
        tryReforgeOnKill(player, player.getOffhandItem());
    }

    private static void tryReforgeOnKill(Player player, ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof IModifiable) || !ToolStack.isInitialized(stack)) {
            return;
        }
        ToolStack tool = ToolStack.from(stack);
        if (tool.getModifierLevel(ModifierIds.before_i_vanish) <= 0) return;
        ModDataNBT data = tool.getPersistentData();
        if (data.getInt(KEY_VANISHING_TICKS) > 0) {
            reforgeSword(player, tool, data);
        }
    }

    /**
     * 铭心重铸: 恢复 35% 最大耐久度并结束消逝危局
     */
    private static void reforgeSword(Player player, IToolStackView tool, ModDataNBT data) {
        data.remove(KEY_VANISHING_TICKS);
        data.remove(KEY_VANISHING_HITS);

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
        ModDataNBT data = tool.getPersistentData();
        boolean guardianActive = data.getBoolean(KEY_GUARDIAN_ACTIVE);
        int vanishingTicks = data.getInt(KEY_VANISHING_TICKS);
        int cdTicks = data.getInt(KEY_VANISHING_CD);
        int hits = data.getInt(KEY_VANISHING_HITS);

        if (vanishingTicks > 0) {
            int sec = (vanishingTicks + 19) / 20;
            tooltip.add(Component.translatable("modifier.tprt.before_i_vanish.tooltip.vanishing",
                    sec, hits, REFORGE_REQUIRED_HITS));
        } else if (guardianActive) {
            tooltip.add(Component.translatable("modifier.tprt.before_i_vanish.tooltip.guardian_active"));
        } else {
            tooltip.add(Component.translatable("modifier.tprt.before_i_vanish.tooltip.held"));
        }

        if (cdTicks > 0 && vanishingTicks <= 0) {
            int cdSec = (cdTicks + 19) / 20;
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
    //  跨模组兼容辅助方法与视觉特效
    // ============================================================

    /** 确保快捷栏有多把同词条武器时只激活第一把作为自律浮游剑, 避免重复触发 */
    private static boolean isFirstGuardianSwordInInventory(Player player, ItemStack currentStack) {
        ItemStack mainHand = player.getMainHandItem();
        for (int i = 0; i < Inventory.getSelectionSize(); i++) {
            ItemStack candidate = player.getInventory().getItem(i);
            if (candidate == mainHand || candidate.isEmpty()) continue;
            if (candidate.getItem() instanceof IModifiable && ToolStack.isInitialized(candidate)) {
                ToolStack tool = ToolStack.from(candidate);
                if (!tool.isBroken() && tool.getModifierLevel(ModifierIds.before_i_vanish) > 0) {
                    return candidate == currentStack;
                }
            }
        }
        ItemStack offhand = player.getOffhandItem();
        if (offhand != mainHand && !offhand.isEmpty() && offhand.getItem() instanceof IModifiable && ToolStack.isInitialized(offhand)) {
            ToolStack tool = ToolStack.from(offhand);
            if (!tool.isBroken() && tool.getModifierLevel(ModifierIds.before_i_vanish) > 0) {
                return offhand == currentStack;
            }
        }
        return false;
    }

    /** 识别飞向玩家的跨模组敌对弹射物 (排除玩家自身及友方/宠物发射的弹射物) */
    private static boolean isHostileIncomingProjectile(Player player, Projectile proj) {
        if (proj == null || !proj.isAlive() || proj.onGround()) return false;
        Entity owner = proj.getOwner();
        if (owner == player) return false;
        if (owner != null) {
            if (owner.isAlliedTo(player)) return false;
            if (owner instanceof OwnableEntity ownable && player.getUUID().equals(ownable.getOwnerUUID())) return false;
            if (owner instanceof TraceableEntity traceable && traceable.getOwner() == player) return false;
        }
        // 判断弹射物是否正在靠近或处于玩家近身威胁范围内
        Vec3 toPlayer = player.getEyePosition().subtract(proj.position());
        Vec3 motion = proj.getDeltaMovement();
        if (motion.lengthSqr() < 1.0E-4D) return false;
        return toPlayer.lengthSqr() <= 4.0D || motion.dot(toPlayer) > -0.15D;
    }

    private static boolean isValidSlashTarget(Player player, LivingEntity candidate) {
        if (candidate == player || !candidate.isAlive() || candidate.isDeadOrDying() || candidate instanceof ArmorStand) {
            return false;
        }
        if (candidate.isAlliedTo(player) || player.isPassengerOfSameVehicle(candidate)) return false;
        if (candidate instanceof OwnableEntity ownable && player.getUUID().equals(ownable.getOwnerUUID())) {
            return false;
        }
        // 仅自动斩击敌对生物、正在攻击玩家的生物或与玩家交战中的目标, 不误伤温顺动物/村民
        if (candidate instanceof Mob mob && mob.getTarget() == player) return true;
        if (player.getLastHurtMob() == candidate || player.getLastHurtByMob() == candidate) return true;
        return !candidate.getType().getCategory().isFriendly();
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

        // 在玩家与目标之间绘制一道苍蓝飞剑轨迹
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
