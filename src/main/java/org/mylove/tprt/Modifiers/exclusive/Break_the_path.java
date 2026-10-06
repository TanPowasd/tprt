package org.mylove.tprt.Modifiers.exclusive;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.mylove.tprt.Tprt;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.interaction.GeneralInteractionModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.InteractionSource;
import slimeknights.tconstruct.library.modifiers.hook.interaction.InventoryTickModifierHook;
import slimeknights.tconstruct.library.modifiers.impl.NoLevelsModifier;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import java.util.List;
import java.util.Optional;

/**
 * 幻影投掷 (phantom_hurl)
 * <p>
 * 潜行 + 长按右键蓄力, 松手后把工具沿准心投掷出去。<br>
 * 投掷物在触碰到方块或者生物后: 把使用者传送到落点, 收回工具,
 * 并对落点半径 3 格内的生物造成 玩家攻击力 × 500% 的伤害 (伤害来源为玩家本人)。<br>
 * 整套行为拥有 5 秒冷却。
 * <p>
 * 实现说明:
 * <ul>
 *     <li>起手/蓄力用 {@link GeneralInteractionModifierHook}, 投掷物的飞行状态写在工具持久数据里;</li>
 *     <li>飞行由 {@link InventoryTickModifierHook} 按游戏刻推进 (每刻最多推进一次), 逐刻做
 *         方块射线检测 + 生物扫掠检测, 因此不存在穿模, 也无需注册新的投掷物实体;</li>
 *     <li>轨迹用粒子表现, 命中/传送/收回用音效表现。</li>
 * </ul>
 * 词条注册: ModifierRegistry 里加一行
 * {@code MODIFIERS.register("phantom_hurl", phantom_hurl::new)}
 */
public class Break_the_path extends NoLevelsModifier implements GeneralInteractionModifierHook, InventoryTickModifierHook {

    /* ===================== 数值 ===================== */

    /** 冷却: 5 秒 */
    private static final int COOLDOWN = 100;
    /** 最短蓄力: 0.5 秒, 低于这个时间松手不会投掷 */
    private static final int MIN_CHARGE = 10;
    /** 满蓄力: 1 秒, 超过后威力不再提升 */
    private static final int FULL_CHARGE = 20;
    /** 投掷速度 (格/tick), 蓄力越久越快 */
    private static final double MIN_SPEED = 0.8D;
    private static final double MAX_SPEED = 2.4D;
    /** 最大飞行距离 (格) */
    private static final double MIN_RANGE = 8.0D;
    private static final double MAX_RANGE = 40.0D;
    /** 命中判定膨胀 (格) */
    private static final double HIT_INFLATE = 0.35D;
    /** 落点伤害半径 (格) */
    private static final double AOE_RADIUS = 3.0D;
    /** 伤害倍率: 玩家攻击力 × 500% */
    private static final double DAMAGE_MULTIPLIER = 5.0D;
    /** 传送点沿投掷方向回退的距离, 避免把玩家塞进方块/生物里 */
    private static final double TELEPORT_BACK = 0.6D;
    /** 飞行状态最长存活时间 (tick), 用于清理掉线/崩溃留下的残留数据 */
    private static final long MAX_FLIGHT_LIFE = 200L;

    /* ===================== 工具持久数据 ===================== */

    /** 飞行状态整体存在一个 CompoundTag 里, 一次读写、一次清理 */
    private static final ResourceLocation FLIGHT = ResourceLocation.fromNamespaceAndPath(Tprt.MODID, "phantom_hurl_flight");
    private static final String ORIGIN_X = "ox";
    private static final String ORIGIN_Y = "oy";
    private static final String ORIGIN_Z = "oz";
    private static final String DIR_X = "dx";
    private static final String DIR_Y = "dy";
    private static final String DIR_Z = "dz";
    private static final String SPEED = "sp";
    private static final String RANGE = "rg";
    private static final String TRAVELED = "tr";
    private static final String START_TICK = "st";
    private static final String LAST_TICK = "tk";

    @Override
    protected void registerHooks(ModuleHookMap.@NotNull Builder hookBuilder) {
        super.registerHooks(hookBuilder);
        hookBuilder.addHook(this, ModifierHooks.GENERAL_INTERACT, ModifierHooks.INVENTORY_TICK);
    }

    /* ============================================================
     *  蓄力阶段
     * ============================================================ */

    @Override
    public int getUseDuration(@NotNull IToolStackView tool, @NotNull ModifierEntry modifier) {
        return 72000;
    }

    @Override
    public @NotNull UseAnim getUseAction(@NotNull IToolStackView tool, @NotNull ModifierEntry modifier) {
        // 三叉戟的举枪/投掷姿势, 最贴近“把工具掷出去”
        return UseAnim.SPEAR;
    }

    @Override
    public @NotNull InteractionResult onToolUse(@NotNull IToolStackView tool, @NotNull ModifierEntry modifier, @NotNull Player player, @NotNull InteractionHand hand, @NotNull InteractionSource source) {
        if (source != InteractionSource.RIGHT_CLICK || tool.isBroken() || modifier.intEffectiveLevel() <= 0) {
            return InteractionResult.PASS;
        }
        // 必须潜行才能起手
        if (!player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        // 冷却中无法再次蓄力
        if (player.getCooldowns().isOnCooldown(player.getItemInHand(hand).getItem())) {
            return InteractionResult.PASS;
        }
        // 上一次投掷的状态异常残留时先清掉, 避免叠加
        clearFlight(tool);
        GeneralInteractionModifierHook.startUsing(tool, modifier.getId(), player, hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public void onUsingTick(@NotNull IToolStackView tool, @NotNull ModifierEntry modifier, @NotNull LivingEntity entity, int timeLeft) {
        if (!(entity instanceof Player player) || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        int used = this.getUseDuration(tool, modifier) - timeLeft;
        if (used < 0 || used > FULL_CHARGE) {
            return;
        }
        double charge = Mth.clamp(used / (double) FULL_CHARGE, 0.0D, 1.0D);

        // 向准心汇聚的蓄力粒子
        Vec3 focus = player.getEyePosition().add(player.getLookAngle().scale(1.2D));
        int count = 2 + (int) (charge * 4.0D);
        for (int i = 0; i < count; i++) {
            double angle = level.getRandom().nextDouble() * Math.PI * 2.0D;
            double radius = 1.2D * (1.0D - charge * 0.6D);
            Vec3 from = new Vec3(focus.x + Math.cos(angle) * radius,
                    focus.y + Math.sin(angle) * radius,
                    focus.z + Math.sin(angle * 0.5D) * radius);
            spawnParticle(level, ParticleTypes.ENCHANTED_HIT, from, focus.subtract(from).scale(0.3D));
        }
        // 蓄力完成的提示音
        if (used == FULL_CHARGE) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.TRIDENT_RIPTIDE_3, SoundSource.PLAYERS, 0.7F, 1.4F);
        }
    }

    @Override
    public void onFinishUsing(@NotNull IToolStackView tool, @NotNull ModifierEntry modifier, @NotNull LivingEntity entity) {
        // 蓄力到 getUseDuration 上限时也按正常投掷结算
        this.onStoppedUsing(tool, modifier, entity, 0);
    }

    @Override
    public void onStoppedUsing(@NotNull IToolStackView tool, @NotNull ModifierEntry modifier, @NotNull LivingEntity entity, int timeLeft) {
        if (!(entity instanceof Player player)) {
            return;
        }
        int used = this.getUseDuration(tool, modifier) - timeLeft;
        if (used < MIN_CHARGE) {
            // 蓄力不足
            return;
        }
        double charge = Mth.clamp((used - MIN_CHARGE) / (double) (FULL_CHARGE - MIN_CHARGE), 0.0D, 1.0D);

        // 冷却: 客户端也要设一份。原版服务端的 ItemCooldowns 并不会同步给客户端,
        // 只设服务端的话客户端不知道自己处于冷却, 会继续做蓄力预测 (动作播了却不结算)。
        ItemStack stack = player.getUseItem();
        if (stack.isEmpty()) {
            stack = player.getMainHandItem();
        }
        player.getCooldowns().addCooldown(stack.getItem(), COOLDOWN);

        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        double speed = MIN_SPEED + (MAX_SPEED - MIN_SPEED) * charge;
        double range = MIN_RANGE + (MAX_RANGE - MIN_RANGE) * charge;
        Vec3 origin = player.getEyePosition();
        Vec3 dir = player.getLookAngle().normalize();

        // 写入飞行状态
        CompoundTag flight = new CompoundTag();
        flight.putDouble(ORIGIN_X, origin.x);
        flight.putDouble(ORIGIN_Y, origin.y);
        flight.putDouble(ORIGIN_Z, origin.z);
        flight.putDouble(DIR_X, dir.x);
        flight.putDouble(DIR_Y, dir.y);
        flight.putDouble(DIR_Z, dir.z);
        flight.putDouble(SPEED, speed);
        flight.putDouble(RANGE, range);
        flight.putDouble(TRAVELED, 0.0D);
        flight.putLong(START_TICK, level.getGameTime());
        flight.putLong(LAST_TICK, -1L);
        tool.getPersistentData().put(FLIGHT, flight);

        // 出手动作
        player.swing(InteractionHand.MAIN_HAND);

        // 出手表现
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.TRIDENT_THROW, SoundSource.PLAYERS, 1.0F, 0.8F + 0.4F * (float) charge);
        level.sendParticles(ParticleTypes.CRIT, origin.x, origin.y, origin.z, 6, 0.15D, 0.15D, 0.15D, 0.05D);
    }

    /* ============================================================
     *  飞行阶段: 每游戏刻推进一次
     * ============================================================ */

    @Override
    public void onInventoryTick(@NotNull IToolStackView tool, @NotNull ModifierEntry modifier, @NotNull Level world, @NotNull LivingEntity holder, int itemSlot, boolean isSelected, boolean isCorrectSlot, @NotNull ItemStack stack) {
        if (!(world instanceof ServerLevel level) || !(holder instanceof Player player)) {
            return;
        }
        if (!tool.getPersistentData().contains(FLIGHT)) {
            return;
        }
        CompoundTag flight = tool.getPersistentData().getCompound(FLIGHT);
        if (flight.isEmpty()) {
            clearFlight(tool);
            return;
        }
        long now = level.getGameTime();
        // 残留数据保护: 下线/崩溃留下的过期状态直接清掉
        if (now - flight.getLong(START_TICK) > MAX_FLIGHT_LIFE) {
            clearFlight(tool);
            return;
        }
        // 同一游戏刻只推进一次 (背包里可能有多把带此词条的工具在 tick)
        if (flight.getLong(LAST_TICK) == now) {
            return;
        }
        flight.putLong(LAST_TICK, now);
        tool.getPersistentData().put(FLIGHT, flight);
        tickFlight(tool, player, level, flight);
    }

    /** 推进一游戏刻的飞行, 命中则结算 */
    private void tickFlight(IToolStackView tool, Player player, ServerLevel level, CompoundTag flight) {
        Vec3 origin = new Vec3(flight.getDouble(ORIGIN_X), flight.getDouble(ORIGIN_Y), flight.getDouble(ORIGIN_Z));
        Vec3 dir = new Vec3(flight.getDouble(DIR_X), flight.getDouble(DIR_Y), flight.getDouble(DIR_Z));
        double speed = flight.getDouble(SPEED);
        double range = flight.getDouble(RANGE);
        double traveled = flight.getDouble(TRAVELED);

        // 本刻的推进段
        double step = Math.min(speed, range - traveled);
        if (step <= 0.0D) {
            land(tool, player, level, origin.add(dir.scale(range)), dir);
            return;
        }
        Vec3 from = origin.add(dir.scale(traveled));
        Vec3 to = origin.add(dir.scale(traveled + step));

        // 1) 方块碰撞: 整段射线检测, 不会穿模
        BlockHitResult blockHit = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        boolean hitBlock = blockHit.getType() != HitResult.Type.MISS;
        double blockDistSqr = hitBlock ? from.distanceToSqr(blockHit.getLocation()) : Double.MAX_VALUE;

        // 2) 生物碰撞: 扫掠盒内取最近的一个
        Vec3 entityPoint = null;
        double entityDistSqr = Double.MAX_VALUE;
        AABB sweep = new AABB(from, to).inflate(HIT_INFLATE);
        for (Entity candidate : level.getEntities(player, sweep, e -> e instanceof LivingEntity && e != player && e.isAlive() && !e.isSpectator())) {
            Optional<Vec3> point = candidate.getBoundingBox().inflate(HIT_INFLATE).clip(from, to);
            if (point.isPresent()) {
                double distSqr = from.distanceToSqr(point.get());
                if (distSqr < entityDistSqr) {
                    entityDistSqr = distSqr;
                    entityPoint = point.get();
                }
            }
        }

        // 3) 取更近的那个作为落点
        Vec3 impact = null;
        if (entityPoint != null && entityDistSqr <= blockDistSqr) {
            impact = entityPoint;
        } else if (hitBlock) {
            impact = blockHit.getLocation();
        }
        if (impact != null) {
            land(tool, player, level, impact, dir);
            return;
        }

        // 4) 什么都没碰到: 继续飞
        spawnTrail(level, from, to, dir);
        double next = traveled + step;
        if (next >= range - 1.0E-6D) {
            // 到达最大射程, 直接在终点结算
            land(tool, player, level, to, dir);
        } else {
            flight.putDouble(TRAVELED, next);
            tool.getPersistentData().put(FLIGHT, flight);
        }
    }

    /* ============================================================
     *  落地结算: 传送 + 收回工具 + 范围伤害
     * ============================================================ */

    private void land(IToolStackView tool, Player player, ServerLevel level, Vec3 impact, Vec3 dir) {
        // 工具收回: 清理飞行状态 (工具全程留在背包里, 这一步即“回到手上”)
        clearFlight(tool);

        // 落点略微回退, 避免把玩家塞进方块或者生物体内
        Vec3 dest = impact.subtract(dir.scale(TELEPORT_BACK));
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.teleportTo(level, dest.x, dest.y, dest.z, player.getYRot(), player.getXRot());
        } else {
            player.teleportTo(dest.x, dest.y, dest.z);
        }
        player.resetFallDistance();
        player.setDeltaMovement(Vec3.ZERO);
        player.hurtMarked = true;
        player.swing(InteractionHand.MAIN_HAND);

        // 表现: 命中、传送、收回
        level.playSound(null, impact.x, impact.y, impact.z, SoundEvents.TRIDENT_HIT_GROUND, SoundSource.PLAYERS, 1.0F, 1.0F);
        level.playSound(null, dest.x, dest.y, dest.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.8F, 1.2F);
        level.playSound(null, dest.x, dest.y, dest.z, SoundEvents.TRIDENT_RETURN, SoundSource.PLAYERS, 0.9F, 1.0F);
        impactBurst(level, impact);

        // 范围伤害: 玩家攻击力 × 500%, 伤害来源为玩家
        float damage = (float) (player.getAttributeValue(Attributes.ATTACK_DAMAGE) * DAMAGE_MULTIPLIER);
        DamageSource source = player.damageSources().playerAttack(player);
        AABB area = new AABB(impact.subtract(AOE_RADIUS, AOE_RADIUS, AOE_RADIUS),
                impact.add(AOE_RADIUS, AOE_RADIUS, AOE_RADIUS));
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, area,
                e -> e != player && e.isAlive() && !e.isSpectator());
        for (LivingEntity target : targets) {
            // 清掉无敌帧, 保证这一击的伤害不会被吞掉
            target.invulnerableTime = 0;
            if (target.hurt(source, damage)) {
                target.setLastHurtByMob(player);
            }
        }

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.8F, 1.2F );
    }

    /** 清理飞行状态 */
    private static void clearFlight(IToolStackView tool) {
        tool.getPersistentData().remove(FLIGHT);
    }

    /* ============================================================
     *  表现
     * ============================================================ */

    /** 按 count = 0 发包: 此时偏移量被客户端当作初速度使用, 可以得到带拖尾的粒子 */
    private static void spawnParticle(ServerLevel level, ParticleOptions type, Vec3 pos, Vec3 velocity) {
        level.sendParticles(type, pos.x, pos.y, pos.z, 0, velocity.x, velocity.y, velocity.z, 0.0D);
    }

    /** 飞行轨迹 */
    private static void spawnTrail(ServerLevel level, Vec3 from, Vec3 to, Vec3 dir) {
        double length = from.distanceTo(to);
        int points = Math.max(1, (int) Math.ceil(length / 0.5D));
        for (int i = 0; i <= points; i++) {
            Vec3 pos = from.add(dir.scale(length * i / points));
            spawnParticle(level, ParticleTypes.END_ROD, pos, Vec3.ZERO);
            spawnParticle(level, ParticleTypes.CRIT, pos, dir.scale(-0.1D));
        }
    }

    /** 落点冲击: 半径 3 格的冲击环 */
    private static void impactBurst(ServerLevel level, Vec3 center) {
        level.sendParticles(ParticleTypes.SWEEP_ATTACK, center.x, center.y, center.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        level.sendParticles(ParticleTypes.CRIT, center.x, center.y, center.z, 12, 0.5D, 0.5D, 0.5D, 0.1D);
        int points = 36;
        for (int i = 0; i < points; i++) {
            double angle = i * Math.PI * 2.0D / points;
            spawnParticle(level, ParticleTypes.CLOUD,
                    new Vec3(center.x + Math.cos(angle) * AOE_RADIUS, center.y + 0.15D, center.z + Math.sin(angle) * AOE_RADIUS),
                    new Vec3(0.0D, 0.02D, 0.0D));
        }
    }
}
