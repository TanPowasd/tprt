package org.mylove.tprt.ModUsed;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SpellMagic {

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
}