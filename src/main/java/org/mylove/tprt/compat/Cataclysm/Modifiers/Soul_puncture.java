package org.mylove.tprt.compat.Cataclysm.Modifiers;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.mylove.tprt.hooks.Arrowmodifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.interaction.InventoryTickModifierHook;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;
import slimeknights.tconstruct.library.tools.nbt.ModifierNBT;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 灵魂穿刺 (Soul_puncture)
 * <p>
 * 原有效果: 命中时箭矢基础伤害 ×1.75, 并按词条等级增加穿透。<br>
 * 新增效果:
 * <ol>
 *     <li><b>50% 概率无视无敌帧</b>: 命中瞬间 (伤害结算之前) 清掉目标的无敌帧, 这一箭按满额结算;</li>
 *     <li><b>弹道修正式追踪</b>: 飞行途中把箭的朝向逐步修正到准心附近的生物身上;</li>
 *     <li><b>同一支箭对同一个单位只结算一次</b>: 重复命中会被整个取消 (箭穿过去), 也不会再被追踪拉回来。</li>
 * </ol>
 *
 * <b>转向模型</b> (离目标越远转角越大、箭速越快转角越大):
 * <pre>
 *   每 tick 转角 = min(还需要转的角度, 基准角 × 距离系数 × 速度系数)
 *   距离系数 = clamp(箭到目标的距离 / 8, 0.4, 2.5)
 *   速度系数 = clamp(箭速(格/tick) / 2, 0.5, 1.75)
 *   基准角   = 8°
 * </pre>
 * 即: 目标越远、箭飞得越快, 单 tick 允许转的角度就越大 (硬上限 26°); 但转向永远只朝目标方向转、
 * 不会转过头, 而且速度大小保持不变 (伤害与普通箭一致)。箭模型的朝向由原版 {@code AbstractArrow#tick}
 * 按速度重算并做 20% 插值, 所以即使某一 tick 转角较大, 视觉上仍是平滑弧线而不是折线急转。
 */
public class Soul_puncture extends Arrowmodifier implements InventoryTickModifierHook {

    /* ===================== 原有数值 ===================== */

    /** 命中伤害倍率 */
    private static final double DAMAGE_MULTIPLIER = 1.0D;

    /* ===================== 无敌帧 ===================== */

    /** 无视无敌帧的概率 */
    private static final float IFRAME_IGNORE_CHANCE = 1.0F;

    /* ===================== 追踪判定 ===================== */

    /** 准心周围的判定半角 (度): 只有落在这个锥形里的生物才会被追踪 */
    private static final double AIM_CONE = 22.0D;
    /** 玩家到目标的最大距离 (格) */
    private static final double TRACK_RANGE = 32.0D;
    /** 目标与箭飞行方向的最大夹角 (度): 保证目标在箭的前方 */
    private static final double MAX_FLIGHT_ANGLE = 75.0D;

    /* ===================== 转向模型 ===================== */

    /** 基准转角 (度/tick): 距离与速度都在基准值时的单 tick 转角 */
    private static final double TURN_BASE_DEGREES = 8.0D;
    /** 距离系数基准: 距离达到这个值时系数为 1, 更远则系数更大 */
    private static final double TURN_DISTANCE_REFERENCE = 8.0D;
    /** 距离系数下限 / 上限 */
    private static final double TURN_DISTANCE_MIN_FACTOR = 0.4D;
    private static final double TURN_DISTANCE_MAX_FACTOR = 2.5D;
    /** 速度系数基准: 箭速达到这个值 (格/tick) 时系数为 1, 更快则系数更大 */
    private static final double TURN_SPEED_REFERENCE = 2.0D;
    /** 速度系数下限 / 上限 */
    private static final double TURN_SPEED_MIN_FACTOR = 0.5D;
    private static final double TURN_SPEED_MAX_FACTOR = 1.75D;
    /** 单 tick 转角硬上限 (度) */
    private static final double TURN_MAX_DEGREES = 26.0D;

    /* ===================== 其它 ===================== */

    /** 对目标移动的预判比例 (0 = 不预判 / 1 = 完全预判) */
    private static final double LEAD_FACTOR = 0.5D;
    /** 与目标近于这个距离就不再修正, 避免绕着目标转圈 */
    private static final double MIN_CORRECT_DISTANCE = 1.2D;
    /** 单支箭最长追踪 (转向) 时间 (tick) */
    private static final int MAX_TRACK_TICKS = 100;
    /** 追踪状态最长保留时间 (tick): 远大于滞空时间, 只作兜底清理;
     *  这段时间内“同一支箭对同一单位只结算一次”的判定始终有效 */
    private static final int STATE_LIFETIME = 600;
    /** 箭速低于这个值就认为已经停下 (插在地上/掉落), 不再修正 */
    private static final double MIN_SPEED = 0.05D;

    /** 正在追踪的箭: 箭 UUID → 追踪状态 */
    private static final Map<UUID, Tracking> TRACKED = new HashMap<>();

    @Override
    public boolean havenolevel() {
        return true;
    }

    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        super.registerHooks(hookBuilder);
        hookBuilder.addHook(this, ModifierHooks.INVENTORY_TICK);
    }

    /* ============================================================
     *  命中: 同一支箭对同一单位只结算一次 + 原有加成 + 50% 无视无敌帧
     * ============================================================ */

    @Override
    public boolean onProjectileHitEntity(ModifierNBT modifiers, ModDataNBT persistentData, ModifierEntry modifier, Projectile projectile, EntityHitResult hit, LivingEntity attacker, LivingEntity target) {
        if (target != null) {
            Tracking tracking = TRACKED.get(projectile.getUUID());
            // Set#add 返回 false, 说明这支箭之前已经打过它了
            if (tracking != null && !tracking.hitEntities.add(target.getUUID())) {
                // 返回 true 会被匠魂接到 ProjectileImpactEvent#setCanceled 上, 这次命中被整个取消:
                // 不结算伤害、不消耗穿透, 箭直接穿过去。
                return true;
            }
        }
        // 等价于 Arrow 接口的默认实现: 命中交给 arrowhurt 处理
        if (target != null && attacker != null && projectile instanceof AbstractArrow arrow) {
            this.arrowhurt(modifiers, persistentData, modifier.getLevel(), projectile, hit, arrow, attacker, target);
        }
        return false;
    }

    @Override
    public void arrowhurt(ModifierNBT modifiers, ModDataNBT persistentData, int level, Projectile projectile, EntityHitResult hit, AbstractArrow arrow, LivingEntity attacker, LivingEntity target) {
        if (target != null) {
            arrow.setBaseDamage(arrow.getBaseDamage() * DAMAGE_MULTIPLIER);
            arrow.setPierceLevel((byte) (arrow.getPierceLevel() + level));
            // 这一步跑在 hurt() 之前 (匠魂的 PROJECTILE_HIT 挂在 ProjectileImpactEvent 上),
            // 所以清掉无敌帧之后, 这一箭会走完整伤害分支而不是被无敌帧吃掉。
            if (target.getRandom().nextFloat() < IFRAME_IGNORE_CHANCE) {
                target.invulnerableTime = 0;
            }
        }
    }

    /* ============================================================
     *  发射: 登记这支出膛的箭
     * ============================================================ */

    @Override
    public void onTinkerArrowShoot(IToolStackView tool, int level, LivingEntity shooter, Projectile projectile, AbstractArrow arrow, ModDataNBT namespacedNBT, boolean primary) {
        if (!(shooter.level() instanceof ServerLevel serverLevel) || !(shooter instanceof Player player)) {
            return;
        }
        long now = serverLevel.getGameTime();
        // 顺手清掉过期/已经消失的记录
        TRACKED.entrySet().removeIf(entry -> entry.getValue().isDone(now));
        TRACKED.put(arrow.getUUID(), new Tracking(player.getUUID(), arrow, now + MAX_TRACK_TICKS, now + STATE_LIFETIME));
    }

    /* ============================================================
     *  飞行: 每个游戏刻做一次弹道修正
     * ============================================================ */

    @Override
    public void onInventoryTick(IToolStackView tool, ModifierEntry modifier, Level world, LivingEntity holder, int itemSlot, boolean isSelected, boolean isCorrectSlot, ItemStack stack) {
        if (TRACKED.isEmpty() || !(world instanceof ServerLevel serverLevel) || !(holder instanceof Player player)) {
            return;
        }
        long now = serverLevel.getGameTime();
        Iterator<Map.Entry<UUID, Tracking>> iterator = TRACKED.entrySet().iterator();
        while (iterator.hasNext()) {
            Tracking tracking = iterator.next().getValue();
            // 只处理自己射出去的箭 (工具可能被别的玩家/生物持有)
            if (!tracking.ownerId.equals(player.getUUID())) {
                continue;
            }
            AbstractArrow arrow = tracking.arrow;
            if (tracking.isDone(now)) {
                iterator.remove();
                continue;
            }
            // 同一个游戏刻只修正一次 (背包里可能有多件带此词条的工具在 tick)
            if (tracking.lastArrowTick == arrow.tickCount) {
                continue;
            }
            // 箭已经插在地上/掉落时位置几乎不再变化, 直接结束追踪
            Vec3 pos = arrow.position();
            if (tracking.lastPos != null && tracking.lastPos.distanceToSqr(pos) < 0.0004D) {
                iterator.remove();
                continue;
            }
            tracking.lastArrowTick = arrow.tickCount;
            tracking.lastPos = pos;
            // 追踪只在前 MAX_TRACK_TICKS 内生效; 之后状态继续留着做“同一单位只打一次”的判定
            if (now <= tracking.homingUntil) {
                correct(arrow, player, tracking);
            }
        }
    }

    /** 弹道修正: 只改方向, 不改速度大小; 距离越远、速度越快, 单 tick 允许的转角越大 */
    private static void correct(AbstractArrow arrow, Player player, Tracking tracking) {
        Vec3 velocity = arrow.getDeltaMovement();
        double speed = velocity.length();
        if (speed < MIN_SPEED) {
            // 已经停下 (插在地上/掉落)
            return;
        }
        Vec3 arrowPos = arrow.position();
        Vec3 currentDir = velocity.scale(1.0D / speed);
        LivingEntity target = findAimTarget(player, arrowPos, currentDir, tracking.hitEntities);
        if (target == null) {
            return;
        }

        // 瞄准点 = 生物身体中心 + 一部分预判量
        Vec3 aim = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
        double distance = arrowPos.distanceTo(aim);
        if (distance < MIN_CORRECT_DISTANCE) {
            // 已经很近了, 再修正反而会绕着目标转
            return;
        }
        Vec3 lead = target.getDeltaMovement().scale(distance / speed * LEAD_FACTOR);
        Vec3 desired = aim.add(lead).subtract(arrowPos);
        if (desired.lengthSqr() < 1.0E-6D) {
            return;
        }
        desired = desired.normalize();

        // 距离越远 → 系数越大; 箭速越快 → 系数越大
        double distanceFactor = Mth.clamp(distance / TURN_DISTANCE_REFERENCE, TURN_DISTANCE_MIN_FACTOR, TURN_DISTANCE_MAX_FACTOR);
        double speedFactor = Mth.clamp(speed / TURN_SPEED_REFERENCE, TURN_SPEED_MIN_FACTOR, TURN_SPEED_MAX_FACTOR);
        double maxTurn = Math.min(TURN_MAX_DEGREES, TURN_BASE_DEGREES * distanceFactor * speedFactor);

        // 朝目标方向转, 最多 maxTurn 度, 且不会转过头
        Vec3 newDir = turnTowards(currentDir, desired, maxTurn);
        arrow.setDeltaMovement(newDir.scale(speed));
        // 同步速度给客户端: 否则客户端还按原来的直线速度算箭的朝向, 看起来会“横着飞”
        arrow.hurtMarked = true;
    }

    /** 把单位向量 from 朝 to 最多转 maxDegrees 度 (绕 from×to 轴旋转, 保持单位长度) */
    private static Vec3 turnTowards(Vec3 from, Vec3 to, double maxDegrees) {
        double remaining = angleBetween(from, to);
        if (remaining < 1.0E-3D) {
            return to;
        }
        double turn = Math.min(remaining, maxDegrees);
        if (turn <= 0.0D) {
            return from;
        }
        Vec3 axis = from.cross(to);
        if (axis.lengthSqr() < 1.0E-8D) {
            // 正好反向: 任取一条垂直轴, 避免除零
            axis = from.cross(new Vec3(0.0D, 1.0D, 0.0D));
            if (axis.lengthSqr() < 1.0E-8D) {
                axis = from.cross(new Vec3(1.0D, 0.0D, 0.0D));
            }
        }
        axis = axis.normalize();
        double radians = Math.toRadians(turn);
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        // 罗德里格斯旋转: v' = v·cosθ + (k×v)·sinθ + k·(k·v)·(1-cosθ)
        return from.scale(cos)
                .add(axis.cross(from).scale(sin))
                .add(axis.scale(axis.dot(from) * (1.0D - cos)))
                .normalize();
    }

    /** 在准心附近挑一个最值得追的生物 (已经命中过的排除掉, 免得把箭又拉回去) */
    private static LivingEntity findAimTarget(Player player, Vec3 arrowPos, Vec3 flightDir, Set<UUID> hitEntities) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        AABB area = player.getBoundingBox().inflate(TRACK_RANGE);
        LivingEntity best = null;
        double bestScore = Double.MAX_VALUE;
        for (LivingEntity candidate : player.level().getEntitiesOfClass(LivingEntity.class, area,
                e -> e != player && e.isAlive() && !e.isSpectator() && !e.isAlliedTo(player) && !hitEntities.contains(e.getUUID()))) {
            Vec3 point = candidate.position().add(0.0D, candidate.getBbHeight() * 0.5D, 0.0D);

            // 1) 必须落在准心周围的锥形里
            Vec3 toTarget = point.subtract(eye);
            double toTargetLength = toTarget.length();
            if (toTargetLength < 1.0E-4D) {
                continue;
            }
            double aimAngle = angleBetween(toTarget.scale(1.0D / toTargetLength), look);
            if (aimAngle > AIM_CONE) {
                continue;
            }

            // 2) 必须在箭的前方, 否则会出现“掉头追人”的怪动作
            Vec3 fromArrow = point.subtract(arrowPos);
            double fromArrowLength = fromArrow.length();
            if (fromArrowLength < 1.0E-4D || fromArrowLength > TRACK_RANGE) {
                continue;
            }
            if (angleBetween(fromArrow.scale(1.0D / fromArrowLength), flightDir) > MAX_FLIGHT_ANGLE) {
                continue;
            }

            // 3) 打分: 越贴近准心越好, 稍微偏好近一点的目标
            double score = aimAngle + fromArrowLength * 0.25D;
            if (score < bestScore) {
                bestScore = score;
                best = candidate;
            }
        }
        return best;
    }

    /** 两个单位向量的夹角 (度) */
    private static double angleBetween(Vec3 a, Vec3 b) {
        return Math.toDegrees(Math.acos(Mth.clamp(a.dot(b), -1.0D, 1.0D)));
    }

    /** 一支在追踪中的箭 */
    private static final class Tracking {
        private final UUID ownerId;
        private final AbstractArrow arrow;
        /** 转向生效截止时间 (游戏刻) */
        private final long homingUntil;
        /** 状态清理截止时间 (游戏刻) */
        private final long expireAt;
        /** 这支箭已经命中过的单位, 用于保证同一单位只结算一次 */
        private final Set<UUID> hitEntities = new HashSet<>();
        private int lastArrowTick = -1;
        private Vec3 lastPos;

        private Tracking(UUID ownerId, AbstractArrow arrow, long homingUntil, long expireAt) {
            this.ownerId = ownerId;
            this.arrow = arrow;
            this.homingUntil = homingUntil;
            this.expireAt = expireAt;
        }

        private boolean isDone(long now) {
            return now > expireAt || arrow.isRemoved() || !arrow.isAlive();
        }
    }
}
