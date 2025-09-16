package org.mylove.tprt.tags;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.mylove.tprt.entity.tec.CinderParticle;
import org.mylove.tprt.utilities.Vector0;
import slimeknights.mantle.client.TooltipKey;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeHitModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.display.TooltipModifierHook;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.stat.ToolStats;
import slimeknights.tconstruct.tools.stats.ToolType;

import java.util.List;

/// 灰烬斩击-合金巨兽材料词条
public class cinder_slash extends Modifier implements MeleeHitModifierHook, TooltipModifierHook {
    public static final ToolType[] TYPES = {ToolType.MELEE, ToolType.RANGED};
    public static final RandomSource random = RandomSource.create();
    public static final double splashDegree = Math.PI / 4;
    //溅射倍率
    private static double getDamageMuti(int tagLevel){
        return .4 + tagLevel * .2;
    }
    private static int getRangeMuti(int tagLevel){
        return 7 + tagLevel * 4;
    }

    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        hookBuilder.addHook(this, ModifierHooks.MELEE_HIT, ModifierHooks.TOOLTIP);
    }

    @Override
    public float beforeMeleeHit(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float rawDamage, float baseKnockback, float knockback) {
        LivingEntity attacker = context.getAttacker();
        LivingEntity target = context.getLivingTarget();
        if(target == null) return knockback;
        Level level = attacker.level();
        Vec3 att, tar, vecT;
        double speedMuti, boxX, boxY, boxZ;
        int tagLevel = modifier.getLevel();

        att = attacker.position();
        tar = target.position();
        vecT = att.vectorTo(tar).normalize();
        speedMuti = tagLevel * 2;

        /// 大致思路是在击中地点创建一个大盒子，检测盒中实体是不是在攻击者->目标的锥形范围内，判定一次
        /// 09/16 魂樱中有个连锁闪电, 跟这个思路一样
        boxX = getRangeMuti(tagLevel);
        boxY = getRangeMuti(tagLevel);
        boxZ = getRangeMuti(tagLevel);
        AABB range = AABB.ofSize(tar, boxX, boxY, boxZ);
        List<? extends Entity> entitiesList = level.getEntities(attacker, range,
                entity -> entity instanceof LivingEntity && entity.isPickable() && entity.isAlive());

        for (var captured : entitiesList) {
            // 若在锥形范围里，则主目标A->溅射目标B的向量 和攻击者P->主目标A的向量 满足一个条件(cosθ<=角度)
            Vec3 t2 = tar.vectorTo(captured.position()).normalize();
            if(Vector0.getTheta(vecT, t2) > splashDegree) continue;

            // 这里能获取到实体, 但交互为何---
            // 破案了, 假人会把伤害吞掉
            // 暂时没找到方法让粒子锥形发射, 就先写成被命中目标散发粒子好了
            captured.playSound(SoundEvents.FIRE_EXTINGUISH, .4f, 1);
            for (int i = 0; i < 4; i++) {
                ((ServerLevel) level).sendParticles(
                    ParticleTypes.EXPLOSION,
                    captured.getX(),
                    captured.getY() + 1,
                    captured.getZ(),
                    1,
                    random.nextGaussian(),
                    random.nextGaussian(),
                    random.nextGaussian(),
                    .2);
            }
            if(captured == target) continue;
            captured.hurt(level.damageSources().explosion(attacker, attacker), (float) (rawDamage * getDamageMuti(tagLevel)));
        }

        CinderParticle particleProvider = new CinderParticle(attacker.level(), 2, (pLevel, tick, remain) -> {
            int degree = (int) (splashDegree * 180 / Math.PI);
            for(int i=0; i<degree * 2; i++){
                float rad = (float) ((i - degree) / Math.PI);
                Vec3 vecApply = vecT.yRot(rad).scale(speedMuti);

                pLevel.addParticle(
                        ParticleTypes.FLAME,
                        tar.x,
                        tar.y + attacker.getEyeHeight(),
                        tar.z,
                        vecApply.x,
                        vecApply.y,
                        vecApply.z
                        );

            }
        });
        // todo: 服务端的particleProvider没同步到客户端
        particleProvider.setPos(tar);
        //level.addFreshEntity(particleProvider);

        return knockback;
    }

    @Override
    public void addTooltip(IToolStackView tool, ModifierEntry modifier, @Nullable Player player, List<Component> tooltip, TooltipKey key, TooltipFlag tooltipFlag) {
        ToolType type = ToolType.from(tool.getItem(), TYPES);
        if (type != null) {
            int level = modifier.getLevel();
            double bonus1 = getDamageMuti(level);
            double bonus2 = (double) getRangeMuti(level) / 2;
            if (bonus1 > 0) {
                TooltipModifierHook.addPercentBoost(this, TooltipModifierHook.statName(this, ToolStats.ATTACK_DAMAGE), bonus1, tooltip);
            }
            if (bonus2 > 0) {
                TooltipModifierHook.addFlatBoost(this, TooltipModifierHook.statName(this, ToolStats.USE_ITEM_SPEED), bonus2, tooltip);
            }
        }
    }
}
