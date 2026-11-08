package org.mylove.tprt.Modifiers.re;

import com.ssakura49.sakuratinker.core.library.tools.modifier.hooks.STHooks;
import com.ssakura49.sakuratinker.core.library.tools.modifier.hooks.click.LeftClickModifierHook;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.entity.PartEntity;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraft.core.registries.Registries;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mylove.tprt.Tprt;
import org.mylove.tprt.registries.ModDamageSource;
import org.mylove.tprt.registries.ModifierIds;
import slimeknights.mantle.client.TooltipKey;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.display.TooltipModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.GeneralInteractionModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.InteractionSource;
import slimeknights.tconstruct.library.modifiers.hook.interaction.InventoryTickModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.ranged.ProjectileHitModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.ranged.ProjectileLaunchModifierHook;
import slimeknights.tconstruct.library.modifiers.impl.NoLevelsModifier;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.helper.ToolAttackUtil;
import slimeknights.tconstruct.library.tools.helper.ToolDamageUtil;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;
import slimeknights.tconstruct.library.tools.nbt.ModifierNBT;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.library.tools.stat.ToolStats;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/*
  ============================================================
  纵使日薄西山，即便看不到未来 (even_as_the_sun_sets)
  —— 挚爱 (chtholly) · 远程弓弩 (tconstruct:ranged) 专属词条

  核心机制 —— 【凝时守望 · 斜阳御剑 · 妖精归途】三位一体闭环:
    1) 望眼欲穿 · 凝时守望 (潜行松弦驻留 + 贴脸护驾):
       - 【潜行 + 松弦射箭】时, 射出的箭矢不会飞走, 而是凝固悬停在你身侧半空的星火法阵中,
         化作「守望之矢」(最多同时驻留 8 支, 持续 15 秒, 随身环绕);
       - 驻留期间若有敌对生物闯入周围 4.0 格贴脸突袭, 身侧会自动抽出一支「守望之矢」迎面瞬发钉射并将其击退。
    2) 太阳西斜 · 万箭御剑 (引信齐射 + 跨空间唤剑劈砍 + 斜阳折跃):
       - 【非潜行射出引信箭】命中任意目标时:
         a) 万箭齐发: 身侧所有悬停的「守望之矢」同时化作绯红流光, 跨空间锁定穿刺该目标 (无视无敌帧, 独立结算 100% 箭矢伤害);
         b) 斜阳御剑: 直接唤醒你快捷栏 (0~8 槽) 或副手中的【匠魂近战武器】跨越空间在远方目标头顶显形,
            自动对其劈下一记完整的匠魂近战斩击 (100% 触发该近战武器面板与全部词条! 本次齐射每倾泻 3 支守望之矢额外多劈 1 剑, 最多连劈 3 剑;
            若未携带近战武器则保底降下一道本弓星火剑气);
         c) 斜阳折跃: 若在跳跃/滞空状态下松弦射箭且周围 6 格内有敌对生物逼近, 借斜阳余晖向后上方轻盈折跃拉开距离并重置摔落高度。
    3) 不归之人 · 妖精归途 (穿透留标 + 倒飞折返二次穿刺 + 丝线聚怪 + 归箭续杯):
       - 引信箭与齐射倾泻的守望之矢均自带 +3 穿透, 穿过目标后在敌后终点插下持续 12 秒的「不归路标」(最多 8 枚);
       - 当场上「不归路标」达到 6 枚及以上时自动触发 (或手持弓随时按下【左键】主动召回 / 路标到期自动折返):
         所有「不归路标」拉出苍蓝与绯红交织的记忆丝线朝你高速倒飞折返, 无视无敌帧从背后二次贯穿沿途所有敌人 (85% 箭矢伤害),
         并将沿途敌人向丝线中心强力牵引聚拢;
       - 每枚折返归来的路标为你恢复本弓 2 点耐久, 并将其中一半 (向上取整, 最多 4 支) 直接重新凝聚为身侧悬停的「守望之矢」进入下一轮!
  ============================================================
 */
public class even_as_the_sun_sets extends NoLevelsModifier implements
        ProjectileLaunchModifierHook,
        ProjectileHitModifierHook,
        GeneralInteractionModifierHook,
        LeftClickModifierHook,
        InventoryTickModifierHook,
        TooltipModifierHook {

    // ===== 1. 【凝时守望】参数 =====
    public static final int MAX_HOVER_ARROWS = 8;                    // 最多同时悬停的「守望之矢」数量 (8 支)
    public static final int HOVER_DURATION_TICKS = 15 * 20;          // 「守望之矢」持续时间 (15 秒 = 300 ticks)
    public static final double GUARD_RADIUS = 4.0D;                  // 贴身自动护驾扫描半径 (4.0 格)
    public static final int GUARD_INTERVAL_TICKS = 10;               // 贴身护驾扫描间隔 (0.5 秒 = 10 ticks)
    public static final float GUARD_KNOCKBACK = 0.85f;               // 贴身护驾钉射击退力度

    // ===== 2. 【万箭御剑 & 斜阳折跃】参数 =====
    public static final int BONUS_PIERCE_LEVEL = 3;                  // 箭矢额外穿透等级 (+3)
    public static final int ARROWS_PER_EXTRA_SWORD = 3;              // 每倾泻 3 支守望之矢额外多劈 1 剑
    public static final int MAX_SWORD_STRIKES = 3;                   // 单次命中最多连劈剑数 (3 剑)
    public static final double LEAP_THREAT_RADIUS = 6.0D;            // 空中松弦【斜阳折跃】感应周围敌人的半径 (6.0 格)
    public static final double LEAP_BACK_SPEED = 0.78D;              // 【斜阳折跃】后撤水平初速度
    public static final double LEAP_UP_SPEED = 0.44D;                // 【斜阳折跃】跃升垂直初速度

    // ===== 3. 【不归路标 & 妖精归途】参数 =====
    public static final int MAX_WAYMARKS = 8;                        // 场上最多同时存在的「不归路标」数量 (8 枚)
    public static final int AUTO_RECALL_THRESHOLD = 6;               // 自动触发【妖精归途】的路标数量阈值 (6 枚)
    public static final int WAYMARK_DURATION_TICKS = 12 * 20;        // 「不归路标」持续时间 (12 秒 = 240 ticks)
    public static final float RECALL_DAMAGE_RATIO = 0.85f;           // 路标倒飞折返背后穿刺伤害比例 (85%)
    public static final double RECALL_CORRIDOR_WIDTH = 1.85D;        // 路标倒飞折返穿刺与聚怪走廊宽度 (1.85 格)
    public static final double RECALL_PULL_STRENGTH = 0.45D;         // 记忆丝线向连线中心牵引聚拢敌人的力度
    public static final int RECALL_DURABILITY_REPAIR = 2;            // 每枚折返路标恢复的弓耐久 (2 点)
    public static final int MAX_REFILL_HOVER = 4;                    // 单次折返最多回填凝聚的「守望之矢」数量 (4 支)

    // ===== 投射物实体临时标记 =====
    private static final String TAG_CAPTURED_HOVER = "tprt:sun_sets_captured_hover";
    private static final String TAG_ESTIMATED_DAMAGE = "tprt:sun_sets_est_dmg";

    // ===== 同步至弓 NBT 的键 (供 Tooltip 与 Destiny 2 HUD 读取) =====
    public static final ResourceLocation SYNC_HOVER_COUNT = Tprt.getResource("ss_hover_count");
    public static final ResourceLocation SYNC_HOVER_SEC = Tprt.getResource("ss_hover_sec");
    public static final ResourceLocation SYNC_WAYMARK_COUNT = Tprt.getResource("ss_waymark_count");
    public static final ResourceLocation SYNC_WAYMARK_SEC = Tprt.getResource("ss_waymark_sec");
    public static final ResourceLocation SYNC_LINKED_SWORD = Tprt.getResource("ss_linked_sword");

    /** 跨空间唤剑递归重入保护 */
    private static boolean PERFORMING_REMOTE_SWORD = false;

    /** 单支悬停在身侧的「守望之矢」 */
    private static final class HoverArrow {
        final float damage;
        long expireTick;

        HoverArrow(float damage, long expireTick) {
            this.damage = damage;
            this.expireTick = expireTick;
        }
    }

    /** 单枚插在远方的「不归路标」 */
    private static final class Waymark {
        final Vec3 pos;
        final float damage;
        long expireTick;

        Waymark(Vec3 pos, float damage, long expireTick) {
            this.pos = pos;
            this.damage = damage;
            this.expireTick = expireTick;
        }
    }

    /** 玩家维度下的【守望之矢 + 不归路标】状态容器 */
    private static final class ArcherState {
        final List<HoverArrow> hoverArrows = new ArrayList<>();
        final List<Waymark> waymarks = new ArrayList<>();
        long lastRecallTick = -20L;
    }

    private static final Map<UUID, ArcherState> ARCHER_STATES = new ConcurrentHashMap<>();

    public even_as_the_sun_sets() {
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST, false, EntityJoinLevelEvent.class, this::onEntityJoinLevel);
    }

    @Override
    protected void registerHooks(ModuleHookMap.@NotNull Builder hookBuilder) {
        super.registerHooks(hookBuilder);
        hookBuilder.addHook(this,
                ModifierHooks.PROJECTILE_LAUNCH,
                ModifierHooks.PROJECTILE_HIT,
                ModifierHooks.GENERAL_INTERACT,
                STHooks.LEFT_CLICK,
                ModifierHooks.INVENTORY_TICK,
                ModifierHooks.TOOLTIP);
    }

    // ============================================================
    //  1. 射箭发射钩子: 潜行凝时驻留「守望之矢」 / 常态引信箭 & 空中「斜阳折跃」
    // ============================================================

    @Override
    @SuppressWarnings("deprecation")
    public void onProjectileLaunch(@NotNull IToolStackView tool, @NotNull ModifierEntry modifier,
                                   @NotNull LivingEntity shooter, @NotNull Projectile projectile,
                                   @Nullable AbstractArrow arrow, @NotNull ModDataNBT persistentData,
                                   boolean primary) {
        if (shooter.level().isClientSide || !(shooter instanceof Player player)) return;

        float estimatedDamage = estimateProjectileDamage(tool, projectile, arrow);
        projectile.getPersistentData().putFloat(TAG_ESTIMATED_DAMAGE, estimatedDamage);

        if (arrow != null) {
            int newPierce = Math.min(127, Math.max(0, arrow.getPierceLevel()) + BONUS_PIERCE_LEVEL);
            arrow.setPierceLevel((byte) newPierce);
        }

        ArcherState state = ARCHER_STATES.computeIfAbsent(player.getUUID(), k -> new ArcherState());
        long now = player.level().getGameTime();
        pruneExpired(player, tool, state, now);

        // 【潜行 + 松弦】: 箭矢不飞出, 而是凝固悬停在身侧化为「守望之矢」
        if (player.isShiftKeyDown() && state.hoverArrows.size() < MAX_HOVER_ARROWS) {
            state.hoverArrows.add(new HoverArrow(estimatedDamage, now + HOVER_DURATION_TICKS));
            // 刷新全部驻留箭的持续时间, 方便连射布阵
            for (HoverArrow ha : state.hoverArrows) {
                ha.expireTick = Math.max(ha.expireTick, now + HOVER_DURATION_TICKS);
            }
            projectile.getPersistentData().putBoolean(TAG_CAPTURED_HOVER, true);
            projectile.setDeltaMovement(Vec3.ZERO);

            if (player.level() instanceof ServerLevel serverLevel) {
                spawnHoverStoreParticles(serverLevel, player, state.hoverArrows.size());
            }
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS,
                    0.9f, 1.15f + 0.06f * state.hoverArrows.size());
            player.displayClientMessage(
                    Component.translatable("message.tprt.sun_sets.hover_stored",
                            state.hoverArrows.size(), MAX_HOVER_ARROWS),
                    true);
            syncBowNbt(player, tool.getPersistentData(), state, now);
            return;
        }

        // 【非潜行 + 滞空松弦】: 若周围 6 格内有敌对生物逼近, 触发【斜阳折跃】向后上方轻盈拉开距离
        if (primary && !player.onGround() && hasNearbyHostile(player, LEAP_THREAT_RADIUS)) {
            Vec3 look = player.getLookAngle();
            Vec3 horizBack = new Vec3(-look.x, 0.0D, -look.z);
            if (horizBack.lengthSqr() > 1.0E-4D) {
                horizBack = horizBack.normalize().scale(LEAP_BACK_SPEED);
            }
            Vec3 curVel = player.getDeltaMovement();
            player.setDeltaMovement(horizBack.x, Math.max(curVel.y, LEAP_UP_SPEED), horizBack.z);
            player.hurtMarked = true;
            player.resetFallDistance();

            if (player.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.END_ROD,
                        player.getX(), player.getY() + 0.5D, player.getZ(),
                        12, 0.35D, 0.15D, 0.35D, 0.06D);
                serverLevel.sendParticles(ParticleTypes.FLAME,
                        player.getX(), player.getY() + 0.5D, player.getZ(),
                        8, 0.30D, 0.15D, 0.30D, 0.04D);
            }
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ENDER_DRAGON_FLAP, SoundSource.PLAYERS, 0.55f, 1.65f);
        }

        syncBowNbt(player, tool.getPersistentData(), state, now);
    }

    /** 拦截潜行驻留箭的实体生成, 使其化作身侧星火悬停而不飞向地面 */
    private void onEntityJoinLevel(EntityJoinLevelEvent event) {
        Entity entity = event.getEntity();
        if (!event.getLevel().isClientSide && entity instanceof Projectile projectile) {
            if (projectile.getPersistentData().getBoolean(TAG_CAPTURED_HOVER)) {
                event.setCanceled(true);
                projectile.discard();
            }
        }
    }

    // ============================================================
    //  2. 箭矢命中钩子: 万箭齐发 + 斜阳御剑 (跨空间唤剑) + 敌后立下「不归路标」
    // ============================================================

    @Override
    public boolean onProjectileHitEntity(@NotNull ModifierNBT modifiers, @NotNull ModDataNBT persistentData,
                                         @NotNull ModifierEntry modifier, @NotNull Projectile projectile,
                                         @NotNull EntityHitResult hit, @Nullable LivingEntity attacker,
                                         @Nullable LivingEntity target, boolean notBlocked) {
        if (attacker == null || target == null || attacker.level().isClientSide) return notBlocked;
        if (!(attacker instanceof Player player) || !(player.level() instanceof ServerLevel serverLevel)) {
            return notBlocked;
        }
        if (projectile.getPersistentData().getBoolean(TAG_CAPTURED_HOVER)) {
            return false;
        }

        ArcherState state = ARCHER_STATES.computeIfAbsent(player.getUUID(), k -> new ArcherState());
        long now = serverLevel.getGameTime();
        ToolStack heldBow = findPlayerBow(player);
        pruneExpired(player, heldBow, state, now);

        float baseArrowDamage = projectile.getPersistentData().getFloat(TAG_ESTIMATED_DAMAGE);
        if (baseArrowDamage <= 0.0f) {
            baseArrowDamage = estimateProjectileDamage(heldBow, projectile,
                    projectile instanceof AbstractArrow aa ? aa : null);
        }

        // 1) 【万箭齐发】: 倾泻身侧全部悬停的「守望之矢」跨空间穿刺该目标
        int unleashedCount = 0;
        if (!state.hoverArrows.isEmpty()) {
            List<HoverArrow> toUnleash = new ArrayList<>(state.hoverArrows);
            state.hoverArrows.clear();
            unleashedCount = toUnleash.size();

            for (int i = 0; i < unleashedCount; i++) {
                HoverArrow ha = toUnleash.get(i);
                // 绘制从玩家身侧飞向目标的苍蓝/绯红流光弹道
                Vec3 startPos = getHoverOrbitPosition(player, i, unleashedCount, now);
                Vec3 endPos = target.position().add(0.0D, target.getBbHeight() * 0.55D, 0.0D);
                spawnStreamLineParticles(serverLevel, startPos, endPos, i % 2 == 0);

                // 无视无敌帧独立结算 100% 驻留箭伤害
                if (target.isAlive()) {
                    dealPurePierceDamage(player, target, ha.damage);
                }

                // 每支倾泻的守望之矢穿过目标后, 在目标身后扇形落点插下一枚「不归路标」
                Vec3 behindPos = computeBehindWaymarkPos(startPos, target, i, unleashedCount);
                addWaymark(state, behindPos, ha.damage, now);
            }

            serverLevel.playSound(null, target.getX(), target.getY(), target.getZ(),
                    SoundEvents.TRIDENT_RETURN, SoundSource.PLAYERS, 0.95f, 1.45f);
        }

        // 2) 【斜阳御剑】: 唤醒快捷栏/副手里的匠魂近战武器跨越空间在目标头顶劈下完整近战斩击
        int swordStrikes = Math.min(MAX_SWORD_STRIKES, 1 + (unleashedCount / ARROWS_PER_EXTRA_SWORD));
        triggerRemoteSwordStrikes(serverLevel, player, heldBow, target, swordStrikes, baseArrowDamage);

        // 3) 【不归路标】: 本支引信箭穿过目标后, 在其身后延伸处插下一枚「不归路标」
        Vec3 flightDir = projectile.getDeltaMovement();
        if (flightDir.lengthSqr() < 1.0E-4D) {
            flightDir = target.position().subtract(player.position());
        }
        Vec3 behindTarget = computeBehindWaymarkPos(player.getEyePosition(), target, 0, 1);
        addWaymark(state, behindTarget, baseArrowDamage, now);

        // 若场上「不归路标」达到自动折返阈值 (6 枚及以上), 立即触发【妖精归途】万矢倒飞折返!
        if (state.waymarks.size() >= AUTO_RECALL_THRESHOLD) {
            triggerWaymarkRecall(serverLevel, player, heldBow, state, now);
        } else if (heldBow != null) {
            syncBowNbt(player, heldBow.getPersistentData(), state, now);
        }

        return notBlocked;
    }

    @Override
    public boolean onProjectileHitsBlock(@NotNull ModifierNBT modifiers, @NotNull ModDataNBT persistentData,
                                         @NotNull ModifierEntry modifier, @NotNull Projectile projectile,
                                         @NotNull BlockHitResult hit, @Nullable LivingEntity attacker) {
        if (attacker == null || attacker.level().isClientSide || !(attacker instanceof Player player)) return false;
        if (!(player.level() instanceof ServerLevel serverLevel)) return false;
        if (projectile.getPersistentData().getBoolean(TAG_CAPTURED_HOVER)) return false;

        ArcherState state = ARCHER_STATES.computeIfAbsent(player.getUUID(), k -> new ArcherState());
        long now = serverLevel.getGameTime();
        ToolStack heldBow = findPlayerBow(player);
        pruneExpired(player, heldBow, state, now);

        float baseArrowDamage = projectile.getPersistentData().getFloat(TAG_ESTIMATED_DAMAGE);
        if (baseArrowDamage <= 0.0f) {
            baseArrowDamage = estimateProjectileDamage(heldBow, projectile,
                    projectile instanceof AbstractArrow aa ? aa : null);
        }

        // 箭矢落地的终点同样化作一枚「不归路标」
        Vec3 hitPos = hit.getLocation().add(0.0D, 0.35D, 0.0D);
        addWaymark(state, hitPos, baseArrowDamage, now);

        if (state.waymarks.size() >= AUTO_RECALL_THRESHOLD) {
            triggerWaymarkRecall(serverLevel, player, heldBow, state, now);
        } else if (heldBow != null) {
            syncBowNbt(player, heldBow.getPersistentData(), state, now);
        }
        return false;
    }

    // ============================================================
    //  3. 左键主动召回「不归路标」(兼容 SakuraTinker LeftClick 与 TConstruct GeneralInteract)
    // ============================================================

    @Override
    public @NotNull InteractionResult onToolUse(@NotNull IToolStackView tool, @NotNull ModifierEntry modifier,
                                                @NotNull Player player, @NotNull InteractionHand hand,
                                                @NotNull InteractionSource source) {
        if (source == InteractionSource.LEFT_CLICK) {
            if (!player.level().isClientSide && player.level() instanceof ServerLevel serverLevel) {
                ArcherState state = ARCHER_STATES.get(player.getUUID());
                if (state != null && !state.waymarks.isEmpty()) {
                    triggerWaymarkRecall(serverLevel, player, tool, state, serverLevel.getGameTime());
                    return InteractionResult.SUCCESS;
                }
            }
        }
        return InteractionResult.PASS;
    }

    @Override
    public void onLeftClickEmpty(IToolStackView tool, ModifierEntry entry, Player player,
                                 Level level, EquipmentSlot equipmentSlot) {
        tryManualRecall(tool, player, level);
    }

    @Override
    public void onLeftClickBlock(IToolStackView tool, ModifierEntry entry, Player player,
                                 Level level, EquipmentSlot equipmentSlot, BlockState state, BlockPos pos) {
        tryManualRecall(tool, player, level);
    }

    @Override
    public void onLeftClickEntity(IToolStackView tool, ModifierEntry entry, Player player,
                                  Level level, EquipmentSlot equipmentSlot, Entity target) {
        tryManualRecall(tool, player, level);
    }

    private void tryManualRecall(IToolStackView tool, Player player, Level level) {
        if (level.isClientSide || !(level instanceof ServerLevel serverLevel)) return;
        ArcherState state = ARCHER_STATES.get(player.getUUID());
        if (state != null && !state.waymarks.isEmpty()) {
            triggerWaymarkRecall(serverLevel, player, tool, state, serverLevel.getGameTime());
        }
    }

    // ============================================================
    //  4. 背包/手持常驻 Tick: 贴身护驾 + 悬停/路标粒子渲染 + 状态同步
    // ============================================================

    @Override
    public void onInventoryTick(@NotNull IToolStackView tool, @NotNull ModifierEntry modifier,
                                @NotNull Level level, @NotNull LivingEntity holder, int itemSlot,
                                boolean isSelected, boolean isCorrectSlot, @NotNull ItemStack stack) {
        if (level.isClientSide || !(holder instanceof Player player) || !(level instanceof ServerLevel serverLevel)) {
            return;
        }
        // 仅在手持或位于快捷栏/副手时驱动, 且每 tick 只由主手或第一个匹配槽位驱动一次
        boolean inHotbarOrOffhand = isSelected || player.getOffhandItem() == stack || (itemSlot >= 0 && itemSlot <= 8);
        if (!inHotbarOrOffhand) return;

        ArcherState state = ARCHER_STATES.get(player.getUUID());
        long now = serverLevel.getGameTime();

        if (state != null) {
            pruneExpired(player, tool, state, now);

            // 1) 【凝时守望 · 近身自动护驾】: 若身侧有「守望之矢」且 4 格内有敌对生物贴脸, 自动抽出一支迎面钉射逼退
            if (!state.hoverArrows.isEmpty() && now % GUARD_INTERVAL_TICKS == 0) {
                LivingEntity rusher = findNearestHostile(player, GUARD_RADIUS);
                if (rusher != null) {
                    HoverArrow guardArrow = state.hoverArrows.remove(state.hoverArrows.size() - 1);
                    Vec3 startPos = getHoverOrbitPosition(player, 0, 1, now);
                    Vec3 endPos = rusher.position().add(0.0D, rusher.getBbHeight() * 0.55D, 0.0D);
                    spawnStreamLineParticles(serverLevel, startPos, endPos, true);

                    dealPurePierceDamage(player, rusher, guardArrow.damage);
                    Vec3 pushDir = rusher.position().subtract(player.position());
                    if (pushDir.lengthSqr() > 1.0E-4D) {
                        pushDir = pushDir.normalize();
                        rusher.knockback(GUARD_KNOCKBACK, -pushDir.x, -pushDir.z);
                    }
                    // 护驾射出的箭矢同样在突袭者身后立下一枚「不归路标」
                    Vec3 behindPos = computeBehindWaymarkPos(startPos, rusher, 0, 1);
                    addWaymark(state, behindPos, guardArrow.damage, now);

                    serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 0.8f, 1.45f);

                    if (state.waymarks.size() >= AUTO_RECALL_THRESHOLD) {
                        triggerWaymarkRecall(serverLevel, player, tool, state, now);
                    }
                }
            }

            // 2) 渲染身侧悬停「守望之矢」与远方「不归路标」的星火粒子
            if (now % 5 == 0) {
                renderAmbientParticles(serverLevel, player, state, now);
            }
        }

        // 每 5 ticks 同步一次 NBT 供客户端 Tooltip 与 Destiny 2 HUD 读取
        if (now % 5 == 0) {
            syncBowNbt(player, tool.getPersistentData(), state, now);
        }
    }

    // ============================================================
    //  5. 【斜阳御剑】: 跨空间唤醒快捷栏/副手匠魂近战武器劈砍目标
    // ============================================================

    private void triggerRemoteSwordStrikes(ServerLevel serverLevel, Player player,
                                           @Nullable IToolStackView heldBow, LivingEntity target,
                                           int strikeCount, float arrowDamage) {
        if (PERFORMING_REMOTE_SWORD || !target.isAlive()) return;

        ItemStack linkedSword = findBestTinkerMeleeWeapon(player);
        if (!linkedSword.isEmpty()) {
            ToolStack swordTool = ToolStack.from(linkedSword);
            if (!swordTool.isBroken()) {
                boolean wasSprinting = player.isSprinting();
                Vec3 prevVelocity = player.getDeltaMovement();
                ItemStack prevMainHand = player.getMainHandItem();
                PERFORMING_REMOTE_SWORD = true;
                try {
                    player.setItemInHand(InteractionHand.MAIN_HAND, linkedSword);
                    for (int i = 0; i < strikeCount; i++) {
                        if (!target.isAlive() || target.isDeadOrDying()) break;
                        resetInvulnerabilityRecursive(target);
                        ToolAttackContext context = ToolAttackContext.attacker(player)
                                .target(target)
                                .hand(InteractionHand.MAIN_HAND)
                                .cooldown(1.0f)
                                .extraAttack()
                                .build();
                        ToolAttackUtil.performAttack(swordTool, context);
                        spawnRemoteSwordParticles(serverLevel, target, i);
                    }
                } finally {
                    player.setItemInHand(InteractionHand.MAIN_HAND, prevMainHand);
                    PERFORMING_REMOTE_SWORD = false;
                    if (wasSprinting) {
                        player.setSprinting(true);
                    }
                    player.setDeltaMovement(prevVelocity);
                }
                serverLevel.playSound(null, target.getX(), target.getY(), target.getZ(),
                        SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.85f, 1.30f);
                return;
            }
        }

        // 保底: 若玩家未携带匠魂近战武器, 则按【弓近战面板与本箭面板伤害的平均值】降下斩击
        float bowMeleeDamage = heldBow != null ? heldBow.getStats().get(ToolStats.ATTACK_DAMAGE) : arrowDamage;
        float avgPanelDamage = (bowMeleeDamage + arrowDamage) * 0.5f;
        for (int i = 0; i < strikeCount; i++) {
            if (!target.isAlive() || target.isDeadOrDying()) break;
            dealPurePierceDamage(player, target, avgPanelDamage);
            spawnRemoteSwordParticles(serverLevel, target, i);
        }
        serverLevel.playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.80f, 1.40f);
    }

    /** 查找玩家副手或快捷栏 (0~8 槽) 中面板攻击力最高的匠魂近战武器 */
    @NotNull
    public static ItemStack findBestTinkerMeleeWeapon(Player player) {
        ItemStack bestStack = ItemStack.EMPTY;
        float bestAttack = -1.0f;

        // 优先检查副手与快捷栏 0~8
        List<ItemStack> candidates = new ArrayList<>(10);
        candidates.add(player.getOffhandItem());
        for (int slot = 0; slot < 9; slot++) {
            candidates.add(player.getInventory().getItem(slot));
        }

        for (ItemStack candidate : candidates) {
            if (candidate.isEmpty() || candidate == player.getMainHandItem()) continue;
            if (!candidate.is(TinkerTags.Items.MELEE_WEAPON)) continue;
            // 排除远程弓弩自身
            if (candidate.is(TinkerTags.Items.RANGED)) continue;

            ToolStack tool = ToolStack.from(candidate);
            if (tool.isBroken()) continue;
            float attack = tool.getStats().get(ToolStats.ATTACK_DAMAGE);
            if (attack > bestAttack) {
                bestAttack = attack;
                bestStack = candidate;
            }
        }
        return bestStack;
    }

    // ============================================================
    //  6. 【不归路标 & 妖精归途】: 倒飞折返二次穿刺 + 丝线聚怪 + 归箭回耐续杯
    // ============================================================

    private void addWaymark(ArcherState state, Vec3 pos, float damage, long now) {
        while (state.waymarks.size() >= MAX_WAYMARKS) {
            state.waymarks.remove(0);
        }
        state.waymarks.add(new Waymark(pos, damage, now + WAYMARK_DURATION_TICKS));
    }

    private void triggerWaymarkRecall(ServerLevel serverLevel, Player player,
                                      @Nullable IToolStackView bowTool, ArcherState state, long now) {
        if (state.waymarks.isEmpty() || now - state.lastRecallTick < 3L) return;
        state.lastRecallTick = now;

        List<Waymark> recalledList = new ArrayList<>(state.waymarks);
        state.waymarks.clear();
        int recalledCount = recalledList.size();

        Vec3 playerCenter = player.position().add(0.0D, player.getBbHeight() * 0.6D, 0.0D);

        for (int i = 0; i < recalledCount; i++) {
            Waymark wm = recalledList.get(i);
            Vec3 start = wm.pos;
            Vec3 seg = playerCenter.subtract(start);
            double dist = seg.length();
            if (dist < 0.25D) continue;

            // 绘制从远方路标倒飞回玩家身侧的苍蓝-绯红记忆丝线
            spawnStreamLineParticles(serverLevel, start, playerCenter, i % 2 == 0);

            // 沿折返线段扫描走廊内的所有敌对生物: 从背后无视无敌帧二次贯穿 + 向连线中点牵引聚拢
            AABB scanBox = new AABB(start, playerCenter).inflate(RECALL_CORRIDOR_WIDTH);
            Vec3 pullCenter = start.lerp(playerCenter, 0.45D);
            List<LivingEntity> candidates = serverLevel.getEntitiesOfClass(
                    LivingEntity.class, scanBox, e -> isValidEnemy(player, e));

            Set<UUID> piercedThisLine = new HashSet<>();
            for (LivingEntity enemy : candidates) {
                LivingEntity root = resolveDamageTarget(enemy);
                if (piercedThisLine.contains(root.getUUID())) continue;

                Vec3 enemyCenter = root.position().add(0.0D, root.getBbHeight() * 0.5D, 0.0D);
                if (distanceToSegment(enemyCenter, start, playerCenter) <= RECALL_CORRIDOR_WIDTH + root.getBbWidth() * 0.5D) {
                    piercedThisLine.add(root.getUUID());
                    float pierceDamage = Math.max(2.0f, wm.damage * RECALL_DAMAGE_RATIO);
                    dealPurePierceDamage(player, root, pierceDamage);

                    // 记忆丝线牵引聚拢
                    Vec3 pullVec = pullCenter.subtract(root.position());
                    Vec3 horizPull = new Vec3(pullVec.x, 0.0D, pullVec.z);
                    if (horizPull.lengthSqr() > 0.04D) {
                        horizPull = horizPull.normalize().scale(RECALL_PULL_STRENGTH);
                        root.setDeltaMovement(
                                Mth.clamp(root.getDeltaMovement().x + horizPull.x, -1.2D, 1.2D),
                                Math.max(root.getDeltaMovement().y, 0.16D),
                                Mth.clamp(root.getDeltaMovement().z + horizPull.z, -1.2D, 1.2D));
                        root.hurtMarked = true;
                    }
                }
            }
        }

        // 归途回馈 1: 每枚折返归来的路标恢复本弓 2 点耐久
        if (bowTool != null) {
            int repairAmount = recalledCount * RECALL_DURABILITY_REPAIR;
            ToolDamageUtil.repair(bowTool, repairAmount);
        }

        // 归途回馈 2: 将折返路标的一半 (向上取整, 最多 4 支) 直接重新凝聚为身侧悬停的「守望之矢」
        int refillCount = Math.min(MAX_REFILL_HOVER, (recalledCount + 1) / 2);
        float avgDamage = (float) recalledList.stream().mapToDouble(w -> w.damage).average().orElse(12.0D);
        int actualRefilled = 0;
        for (int i = 0; i < refillCount && state.hoverArrows.size() < MAX_HOVER_ARROWS; i++) {
            state.hoverArrows.add(new HoverArrow(avgDamage, now + HOVER_DURATION_TICKS));
            actualRefilled++;
        }

        serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.TRIDENT_THUNDER, SoundSource.PLAYERS, 0.55f, 1.65f);
        serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS, 0.90f, 1.35f);

        player.displayClientMessage(
                Component.translatable("message.tprt.sun_sets.recall", recalledCount, actualRefilled),
                true);

        if (bowTool != null) {
            syncBowNbt(player, bowTool.getPersistentData(), state, now);
        }
    }

    /** 清理过期状态; 若「不归路标」自然到期, 也会自动触发折返归来而不浪费 */
    private void pruneExpired(Player player, @Nullable IToolStackView bowTool, ArcherState state, long now) {
        state.hoverArrows.removeIf(ha -> ha.expireTick <= now);

        boolean anyWaymarkExpired = false;
        for (Waymark wm : state.waymarks) {
            if (wm.expireTick <= now) {
                anyWaymarkExpired = true;
                break;
            }
        }
        if (anyWaymarkExpired && player.level() instanceof ServerLevel serverLevel) {
            triggerWaymarkRecall(serverLevel, player, bowTool, state, now);
        }
    }

    // ============================================================
    //  7. 伤害、索敌、粒子与 HUD 数据同步辅助方法
    // ============================================================

    private static float estimateProjectileDamage(@Nullable IToolStackView tool, Projectile projectile,
                                                  @Nullable AbstractArrow arrow) {
        if (arrow != null) {
            double speed = Math.max(1.2D, arrow.getDeltaMovement().length());
            float dmg = (float) (arrow.getBaseDamage() * speed);
            if (dmg > 1.0f) return dmg;
        }
        if (tool != null) {
            float vel = tool.getStats().get(ToolStats.VELOCITY);
            float projDmg = tool.getStats().get(ToolStats.PROJECTILE_DAMAGE);
            return Math.max(6.0f, projDmg * vel * 3.0f);
        }
        return 12.0f;
    }

    private static void dealPurePierceDamage(Player player, LivingEntity rawTarget, float amount) {
        if (amount <= 0.0f) return;
        LivingEntity target = resolveDamageTarget(rawTarget);
        if (target == null || !target.isAlive() || target.isDeadOrDying()) return;

        resetInvulnerabilityRecursive(target);
        DamageSource pureSource = new DamageSource(
                player.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                        .getHolderOrThrow(ModDamageSource.TPRT_PURE_DAMAGE_TYPE),
                player,
                player
        );
        target.hurt(pureSource, amount);
        resetInvulnerabilityRecursive(target);
    }

    private static Vec3 computeBehindWaymarkPos(Vec3 fromPos, LivingEntity target, int index, int total) {
        Vec3 dir = target.position().subtract(fromPos);
        Vec3 horiz = new Vec3(dir.x, 0.0D, dir.z);
        if (horiz.lengthSqr() < 1.0E-4D) {
            horiz = new Vec3(1.0D, 0.0D, 0.0D);
        } else {
            horiz = horiz.normalize();
        }
        // 多支齐射箭在目标身后呈优雅的扇形展开立标
        float spreadAngle = total > 1 ? ((index - (total - 1) * 0.5f) * 0.28f) : 0.0f;
        Vec3 rotated = horiz.yRot(spreadAngle);
        double behindDist = Math.max(2.2D, target.getBbWidth() + 1.8D);
        return target.position()
                .add(rotated.scale(behindDist))
                .add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
    }

    private static Vec3 getHoverOrbitPosition(Player player, int index, int total, long gameTime) {
        double baseAngle = (gameTime * 0.08D) + (2.0D * Math.PI * index / Math.max(1, total));
        double radius = 1.15D;
        double yOffset = 1.15D + 0.18D * Math.sin(gameTime * 0.12D + index);
        return player.position().add(
                Math.cos(baseAngle) * radius,
                yOffset,
                Math.sin(baseAngle) * radius);
    }

    private static double distanceToSegment(Vec3 p, Vec3 a, Vec3 b) {
        Vec3 ab = b.subtract(a);
        double lenSq = ab.lengthSqr();
        if (lenSq < 1.0E-6D) return p.distanceTo(a);
        double t = Mth.clamp(p.subtract(a).dot(ab) / lenSq, 0.0D, 1.0D);
        Vec3 proj = a.add(ab.scale(t));
        return p.distanceTo(proj);
    }

    @Nullable
    private static ToolStack findPlayerBow(Player player) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (!stack.isEmpty() && stack.is(TinkerTags.Items.MODIFIABLE)) {
                ToolStack tool = ToolStack.from(stack);
                if (tool.getModifierLevel(ModifierIds.even_as_the_sun_sets) > 0) {
                    return tool;
                }
            }
        }
        return null;
    }

    private static boolean hasNearbyHostile(Player player, double radius) {
        return findNearestHostile(player, radius) != null;
    }

    @Nullable
    private static LivingEntity findNearestHostile(Player player, double radius) {
        AABB box = player.getBoundingBox().inflate(radius, 3.0D, radius);
        List<LivingEntity> list = player.level().getEntitiesOfClass(
                LivingEntity.class, box, e -> isHostileToPlayer(player, e));
        if (list.isEmpty()) return null;
        list.sort(Comparator.comparingDouble(player::distanceToSqr));
        return list.get(0);
    }

    private static boolean isHostileToPlayer(Player player, LivingEntity entity) {
        if (!isValidEnemy(player, entity)) return false;
        if (entity instanceof Mob mob && mob.getTarget() == player) return true;
        return entity instanceof Enemy;
    }

    private static boolean isValidEnemy(Player player, LivingEntity entity) {
        if (entity == player || !entity.isAlive() || entity.isSpectator()) return false;
        if (entity.isAlliedTo(player)) return false;
        if (entity instanceof OwnableEntity ownable && player.getUUID().equals(ownable.getOwnerUUID())) {
            return false;
        }
        return true;
    }

    private static LivingEntity resolveDamageTarget(Entity entity) {
        if (entity instanceof PartEntity<?> part && part.getParent() instanceof LivingEntity parent) {
            return parent;
        }
        return entity instanceof LivingEntity living ? living : null;
    }

    private static void resetInvulnerabilityRecursive(Entity entity) {
        entity.invulnerableTime = 0;
        if (entity instanceof LivingEntity living) {
            living.hurtTime = 0;
        }
        if (entity.isMultipartEntity()) {
            PartEntity<?>[] parts = entity.getParts();
            if (parts != null) {
                for (PartEntity<?> part : parts) {
                    if (part != null) {
                        part.invulnerableTime = 0;
                    }
                }
            }
        }
    }

    private static void spawnHoverStoreParticles(ServerLevel serverLevel, Player player, int count) {
        Vec3 pos = getHoverOrbitPosition(player, count - 1, Math.max(1, count), serverLevel.getGameTime());
        serverLevel.sendParticles(ParticleTypes.END_ROD, pos.x, pos.y, pos.z, 6, 0.08D, 0.08D, 0.08D, 0.02D);
        serverLevel.sendParticles(ParticleTypes.FLAME, pos.x, pos.y, pos.z, 4, 0.06D, 0.06D, 0.06D, 0.01D);
    }

    private static void spawnStreamLineParticles(ServerLevel serverLevel, Vec3 from, Vec3 to, boolean azure) {
        Vec3 diff = to.subtract(from);
        double dist = diff.length();
        int steps = Mth.clamp((int) (dist * 2.5D), 4, 28);
        for (int s = 0; s <= steps; s++) {
            double ratio = (double) s / (double) steps;
            Vec3 p = from.lerp(to, ratio);
            serverLevel.sendParticles(
                    azure ? ParticleTypes.END_ROD : ParticleTypes.FLAME,
                    p.x, p.y, p.z, 1, 0.02D, 0.02D, 0.02D, 0.0D);
        }
    }

    private static void spawnRemoteSwordParticles(ServerLevel serverLevel, LivingEntity target, int strikeIdx) {
        double cx = target.getX();
        double cy = target.getY() + target.getBbHeight() * 0.65D;
        double cz = target.getZ();
        serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK, cx, cy, cz, 2, 0.25D, 0.15D, 0.25D, 0.0D);
        serverLevel.sendParticles(
                strikeIdx % 2 == 0 ? ParticleTypes.END_ROD : ParticleTypes.FLAME,
                cx, cy + 0.5D, cz, 8, 0.35D, 0.35D, 0.35D, 0.06D);
    }

    private static void renderAmbientParticles(ServerLevel serverLevel, Player player, ArcherState state, long now) {
        int hoverTotal = state.hoverArrows.size();
        for (int i = 0; i < hoverTotal; i++) {
            Vec3 p = getHoverOrbitPosition(player, i, hoverTotal, now);
            serverLevel.sendParticles(
                    i % 2 == 0 ? ParticleTypes.END_ROD : ParticleTypes.FLAME,
                    p.x, p.y, p.z, 1, 0.02D, 0.02D, 0.02D, 0.0D);
        }
        for (int i = 0; i < state.waymarks.size(); i++) {
            Waymark wm = state.waymarks.get(i);
            serverLevel.sendParticles(ParticleTypes.END_ROD,
                    wm.pos.x, wm.pos.y, wm.pos.z, 2, 0.08D, 0.25D, 0.08D, 0.01D);
            serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                    wm.pos.x, wm.pos.y, wm.pos.z, 1, 0.06D, 0.12D, 0.06D, 0.01D);
        }
    }

    private static void syncBowNbt(Player player, ModDataNBT data, @Nullable ArcherState state, long now) {
        if (state == null) {
            data.remove(SYNC_HOVER_COUNT);
            data.remove(SYNC_HOVER_SEC);
            data.remove(SYNC_WAYMARK_COUNT);
            data.remove(SYNC_WAYMARK_SEC);
        } else {
            int hoverCount = state.hoverArrows.size();
            int hoverSec = 0;
            for (HoverArrow ha : state.hoverArrows) {
                int sec = (int) ((ha.expireTick - now + 19L) / 20L);
                if (sec > hoverSec) hoverSec = sec;
            }

            int waymarkCount = state.waymarks.size();
            int waymarkSec = 0;
            for (Waymark wm : state.waymarks) {
                int sec = (int) ((wm.expireTick - now + 19L) / 20L);
                if (sec > waymarkSec) waymarkSec = sec;
            }

            if (hoverCount > 0) {
                data.putInt(SYNC_HOVER_COUNT, hoverCount);
                data.putInt(SYNC_HOVER_SEC, hoverSec);
            } else {
                data.remove(SYNC_HOVER_COUNT);
                data.remove(SYNC_HOVER_SEC);
            }

            if (waymarkCount > 0) {
                data.putInt(SYNC_WAYMARK_COUNT, waymarkCount);
                data.putInt(SYNC_WAYMARK_SEC, waymarkSec);
            } else {
                data.remove(SYNC_WAYMARK_COUNT);
                data.remove(SYNC_WAYMARK_SEC);
            }
        }

        ItemStack linkedSword = findBestTinkerMeleeWeapon(player);
        if (!linkedSword.isEmpty()) {
            String swordName = linkedSword.getHoverName().getString();
            data.putString(SYNC_LINKED_SWORD, swordName);
        } else {
            data.remove(SYNC_LINKED_SWORD);
        }
    }

    // ============================================================
    //  8. 匠魂工具栏动态 Tooltip 显示
    // ============================================================

    @Override
    public void addTooltip(@NotNull IToolStackView tool, @NotNull ModifierEntry modifier,
                           @Nullable Player player, @NotNull List<Component> tooltip,
                           @NotNull TooltipKey tooltipKey, @NotNull TooltipFlag tooltipFlag) {
        ModDataNBT data = tool.getPersistentData();
        int hoverCount = data.getInt(SYNC_HOVER_COUNT);
        int hoverSec = data.getInt(SYNC_HOVER_SEC);
        int waymarkCount = data.getInt(SYNC_WAYMARK_COUNT);
        int waymarkSec = data.getInt(SYNC_WAYMARK_SEC);
        String linkedSword = data.getString(SYNC_LINKED_SWORD);

        tooltip.add(Component.translatable("tooltip.tprt.sun_sets.hover_status",
                hoverCount, MAX_HOVER_ARROWS, hoverSec));
        tooltip.add(Component.translatable("tooltip.tprt.sun_sets.waymark_status",
                waymarkCount, MAX_WAYMARKS, waymarkSec));

        if (!linkedSword.isEmpty()) {
            tooltip.add(Component.translatable("tooltip.tprt.sun_sets.linked_sword", linkedSword));
        } else {
            tooltip.add(Component.translatable("tooltip.tprt.sun_sets.no_sword"));
        }

        if (tooltipKey == TooltipKey.SHIFT) {
            tooltip.add(Component.translatable("tooltip.tprt.sun_sets.detail_hover",
                    MAX_HOVER_ARROWS, HOVER_DURATION_TICKS / 20, String.format("%.1f", GUARD_RADIUS)));
            tooltip.add(Component.translatable("tooltip.tprt.sun_sets.detail_sword",
                    ARROWS_PER_EXTRA_SWORD, MAX_SWORD_STRIKES));
            tooltip.add(Component.translatable("tooltip.tprt.sun_sets.detail_waymark",
                    BONUS_PIERCE_LEVEL, AUTO_RECALL_THRESHOLD, Math.round(RECALL_DAMAGE_RATIO * 100.0f),
                    RECALL_DURABILITY_REPAIR));
        } else {
            tooltip.add(Component.translatable("tooltip.tprt.sun_sets.shift_hint")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
