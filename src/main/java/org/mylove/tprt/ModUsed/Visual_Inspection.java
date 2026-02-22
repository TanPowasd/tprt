package org.mylove.tprt.ModUsed;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import java.util.ArrayList;
import java.util.List;

//视角检测算法 *射线检测
public class Visual_Inspection {
    //射线监测
    /**
     * 射线检测 - 获取玩家视线方向上的所有实体碰撞点
     *
     * @param player 进行检测的玩家实体
     * @param distance 检测的最大距离
     * @return 按距离排序的命中结果列表，最近的在前。如果没有命中则返回null
     */
    public static List<HitResult> Radiographic_detection_GetEntity(LivingEntity player, float distance) {
        Level level = player.level(); // 获取玩家所在的世界
        if (!level.isClientSide) {
            ItemStack mainHandItem = player.getMainHandItem();
            if (true) {
                // 如果手中的物品是MagicItem的物品
                // 执行射线检测逻辑
                Vec3 startPos = player.getEyePosition(); // 获取玩家眼睛位置
                Vec3 endPos = player.getLookAngle().normalize().scale(distance).add(startPos); // 计算结束向量
                // 如果视线?格内有方块，获取方块
                BlockHitResult blockHitResult = level.clip(new ClipContext(startPos, endPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player)); // 只检测方块，不检测结果
                endPos = blockHitResult.getLocation(); // 更新结束位置为第一个碰到的方块，若32格内没有碰到方块则不变
                // 以新的结束点开始检测是否在视线上有实体

                // 创建一个以玩家为中心，沿视线方向的包围盒
                AABB range = player.getBoundingBox().expandTowards(endPos.subtract(startPos)); // 包围盒
                List<HitResult> hitwhat = new ArrayList<>(); // 获取包围盒内的"命中结果"，用列表存储
                List<? extends Entity> entitiesList = level.getEntities(player, range, entity -> entity.isPickable() && entity.isAlive()); // 检测为活着的可以交互的实体（活的生物）
                for (var e : entitiesList) {
                    // 获取实体的包围盒与玩家视线方向向量的交点
                    Vec3 vec3 = e.getBoundingBox().clip(startPos, endPos).orElse(null);
                    if (vec3 != null) {
                        // 如果交点不为空，说明视线与实体的包围盒相交
                        EntityHitResult entityHitResult = new EntityHitResult(e, vec3);
                        hitwhat.add(entityHitResult); // 将实体命中结果添加到列表中
                    }
                }
                if (!hitwhat.isEmpty()) {
                    hitwhat.sort((o1, o2) -> o1.getLocation().distanceToSqr(startPos) < o2.getLocation().distanceToSqr(startPos) ? -1 : 1);
                    HitResult hitResult = hitwhat.get(0);
                    if (hitResult instanceof EntityHitResult entityHitResult && entityHitResult.getEntity() instanceof LivingEntity livingEntity) {
                        // DeBug.Logger.log("Radiographic_detection_GetEntity: " + livingEntity.getName().getString());
                        return hitwhat;
                    } else {
                        return null;
                    }
                }
            }
        }
        return null;
    }

    /**
     * 圆锥区域锁定检测 - 获取玩家视角圆锥范围内的所有实体
     *
     * @param player 进行检测的玩家实体
     * @param distance 检测的最大距离（圆锥高度）
     * @param coneRadius 在最大距离处的圆锥底面半径
     * @return 按距离排序的实体列表，最近的在前。如果没有实体则返回空列表
     */
    public static List<LivingEntity> Lock_detection_GetEntity(LivingEntity player, float distance, float coneRadius) {
        Level level = player.level();

        // 只在服务端执行
        if (level.isClientSide) {
            return new ArrayList<>();
        }

        Vec3 startPos = player.getEyePosition(); // 圆锥顶点：玩家眼睛位置
        Vec3 lookVec = player.getLookAngle().normalize(); // 圆锥方向：玩家视线方向

        // 获取圆锥范围内的所有实体
        List<LivingEntity> entitiesInCone = getEntitiesInCone(player, startPos, lookVec, distance, coneRadius);

        // 按距离从近到远排序
        entitiesInCone.sort((e1, e2) -> {
            double dist1 = e1.distanceToSqr(player);
            double dist2 = e2.distanceToSqr(player);
            return Double.compare(dist1, dist2);
        });

        return entitiesInCone;
    }

    /**
     * 获取圆锥区域内的所有实体
     *
     * @param Pplayer 检测源实体（用于排除自身）
     * @param coneTip 圆锥顶点坐标
     * @param coneDirection 圆锥方向向量
     * @param maxDistance 圆锥最大距离
     * @param maxRadius 在最大距离处的圆锥半径
     * @return 圆锥区域内的实体列表
     */
    private static List<LivingEntity> getEntitiesInCone(LivingEntity Pplayer, Vec3 coneTip, Vec3 coneDirection,float maxDistance, float maxRadius) {
        Level level = Pplayer.level();

        // 创建球形检测区域，包含整个圆锥体积
        AABB searchArea = new AABB(
                coneTip.x - maxDistance, coneTip.y - maxDistance, coneTip.z - maxDistance,
                coneTip.x + maxDistance, coneTip.y + maxDistance, coneTip.z + maxDistance
        );

        List<LivingEntity> entitiesInCone = new ArrayList<>();

        // 获取球形区域内的所有活着的生物实体，排除玩家自身和旁观者
        List<Entity> nearbyEntities = level.getEntities(Pplayer, searchArea,
                entity -> entity instanceof LivingEntity &&
                        entity.isAlive() &&
                        !entity.isSpectator() &&
                        entity != Pplayer); // 排除玩家自己

        // 对每个实体进行圆锥区域判断
        for (Entity entity : nearbyEntities) {
            if (isInCone(coneTip, coneDirection, entity.position(), maxDistance, maxRadius)) {
                entitiesInCone.add((LivingEntity) entity);
            }
        }

        return entitiesInCone;
    }

    /**
     * 判断点是否在圆锥区域内
     *
     * @param coneTip 圆锥顶点坐标
     * @param coneDirection 圆锥方向向量（已归一化）
     * @param point 要检测的点坐标
     * @param maxDistance 圆锥最大距离
     * @param maxRadius 在最大距离处的圆锥半径
     * @return 如果点在圆锥内返回true，否则返回false
     */
    private static boolean isInCone(Vec3 coneTip, Vec3 coneDirection, Vec3 point, float maxDistance, float maxRadius) {
        // 计算从圆锥顶点到检测点的向量
        Vec3 toPoint = point.subtract(coneTip);
        double distanceToPoint = toPoint.length();

        // 距离检查：点必须在有效距离范围内
        if (distanceToPoint > maxDistance || distanceToPoint < 0.5) {
            return false; // 距离太远或太近（避免锁定自身）
        }

        // 计算点到圆锥轴线的垂直距离（即该距离处的实际半径）
        Vec3 pointDirection = toPoint.normalize();
        double dotProduct = pointDirection.dot(coneDirection);

        // 使用点积计算角度（更高效的方法）
        double angleRad = Math.acos(Math.min(1.0, Math.max(-1.0, dotProduct)));

        // 计算在该距离处允许的最大角度
        double allowedAngle = Math.atan2(maxRadius, maxDistance);

        // 判断点的角度是否在允许范围内
        return angleRad <= allowedAngle;
    }

    /**
     * 视线遮挡检查 - 判断两点之间是否有视觉遮挡
     *
     * @param level 世界对象
     * @param start 起点坐标
     * @param end 终点坐标
     * @return 如果没有视觉遮挡返回true，否则返回false
     */
    private static boolean hasClearLineOfSight(Level level, Vec3 start, Vec3 end) {
        // 检测起点到终点之间的视觉遮挡物
        BlockHitResult blockHit = level.clip(new ClipContext(
                start, end,
                ClipContext.Block.VISUAL, // 检测视觉遮挡的方块
                ClipContext.Fluid.NONE,
                null
        ));

        // 如果没有命中任何方块，说明视线畅通
        return blockHit.getType() == HitResult.Type.MISS;
    }


    /**
     * 获取圆锥区域内最接近光标方向的实体
     * 这个方法会返回圆锥范围内，与玩家视线方向夹角最小的实体
     *
     * @param player 进行检测的玩家实体
     * @param distance 检测的最大距离（圆锥高度）
     * @param coneRadius 在最大距离处的圆锥底面半径
     * @return 最接近光标方向的实体，如果没有符合条件的实体则返回null
     */
    public static LivingEntity getClosestEntityToCursor(LivingEntity player, float distance, float coneRadius) {
        Level level = player.level();

        // 只在服务端执行
        if (level.isClientSide) {
            return null;
        }

        Vec3 startPos = player.getEyePosition(); // 圆锥顶点：玩家眼睛位置
        Vec3 lookVec = player.getLookAngle().normalize(); // 圆锥方向：玩家视线方向

        // 获取圆锥范围内的所有实体
        List<LivingEntity> entitiesInCone = getEntitiesInCone(player, startPos, lookVec, distance, coneRadius);

        // 如果没有实体，直接返回null
        if (entitiesInCone.isEmpty()) {
            return null;
        }

        // 如果只有一个实体，直接返回
        if (entitiesInCone.size() == 1) {
            DeBug.Logger.log("CETC: " + entitiesInCone.get(0).getName().getString());
            return entitiesInCone.get(0);
        }

        // 寻找最接近视线方向的实体
        LivingEntity closestEntity = null;
        double smallestAngle = Double.MAX_VALUE; // 初始化最小角度为最大值

        for (LivingEntity entity : entitiesInCone) {
            // 计算从玩家到实体的向量
            Vec3 toEntity = entity.getEyePosition().subtract(startPos).normalize();

            // 计算实体方向与视线方向的点积（余弦值）
            double dotProduct = toEntity.dot(lookVec);

            // 将点积转换为角度（弧度），点积越大角度越小
            double angle = Math.acos(Math.min(1.0, Math.max(-1.0, dotProduct)));

            // 如果这个实体角度更小，更新最接近的实体
            if (angle < smallestAngle) {
                smallestAngle = angle;
                closestEntity = entity;
            }
        }
        DeBug.Logger.log("CETC: " + closestEntity.getName().getString());
        return closestEntity;
    }

    /**
     * 获取圆锥区域内最接近光标方向的实体（带权重评分）
     *
     * @param player 进行检测的玩家实体
     * @param distance 检测的最大距离（圆锥高度）
     * @param coneRadius 在最大距离处的圆锥底面半径
     * @param angleWeight 角度偏差的权重（推荐0.7-0.8）
     * @param distanceWeight 距离因素的权重（推荐0.2-0.3）
     * @return 权重最高的实体，如果没有符合条件的实体则返回null
     */
    public static LivingEntity getPrioritizedEntityInCone(LivingEntity player, float distance, float coneRadius, double angleWeight, double distanceWeight) {
        Level level = player.level();

        // 只在服务端执行
        if (level.isClientSide) {
            return null;
        }

        Vec3 startPos = player.getEyePosition();
        Vec3 lookVec = player.getLookAngle().normalize();

        // 获取圆锥范围内的所有实体
        List<LivingEntity> entitiesInCone = getEntitiesInCone(player, startPos, lookVec, distance, coneRadius);

        // 如果没有实体，直接返回null
        if (entitiesInCone.isEmpty()) {
            return null;
        }

        // 如果只有一个实体，直接返回
        if (entitiesInCone.size() == 1) {
            return entitiesInCone.get(0);
        }

        // 计算每个实体的权重，选择权重最高的
        LivingEntity bestEntity = null;
        double bestScore = Double.MAX_VALUE; // 权重越高越好

        for (LivingEntity entity : entitiesInCone) {
            // 计算角度偏差分数
            Vec3 toEntity = entity.getEyePosition().subtract(startPos).normalize();
            double dotProduct = toEntity.dot(lookVec);
            double angleScore = 1.0 - dotProduct; // 角度偏差（0表示完全对准）

            // 计算距离分数（标准化到0-1范围）
            double distanceToEntity = startPos.distanceTo(entity.position());
            double normalizedDistance = distanceToEntity / distance;

            // 计算权重
            double totalScore = (angleScore * angleWeight) + (normalizedDistance * distanceWeight);

            // 如果这个实体权重更高，更新最佳实体
            if (totalScore < bestScore) {
                bestScore = totalScore;
                bestEntity = entity;
            }
        }

        return bestEntity;
    }

}
