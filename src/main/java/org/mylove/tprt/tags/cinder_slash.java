package org.mylove.tprt.tags;

import net.minecraft.client.particle.Particle;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.mylove.tprt.ModUsed.DeBug;
import org.mylove.tprt.registries.ModDamageSource;
import org.mylove.tprt.utilities.Vector0;
import slimeknights.mantle.client.TooltipKey;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.build.ConditionalStatModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeDamageModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeHitModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.display.TooltipModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.ranged.ProjectileHitModifierHook;
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
    //溅射倍率
    private static double getDamageMuti(int tagLevel){
        return .4 + tagLevel * .2;
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
        Vec3 att, tar, vecT, vecApply;
        double speedMuti, boxX, boxY, boxZ;
        int tagLevel = modifier.getLevel();

        att = attacker.position();
        tar = target.position();
        vecT = att.vectorTo(tar).normalize();
        speedMuti = tagLevel * 2;

        /// 大致思路是在击中地点创建一个大盒子，检测盒中实体是不是在攻击者->目标的锥形范围内，判定一次
        boxX = 7 + tagLevel * 4;
        boxY = 7 + tagLevel * 4;
        boxZ = 7 + tagLevel * 4;
        AABB range = AABB.ofSize(tar, boxX, boxY, boxZ);
        List<? extends Entity> entitiesList = level.getEntities(attacker, range,
                entity -> entity instanceof LivingEntity && entity.isPickable() && entity.isAlive());

        // DeBug.Logger.log("普通攻击触发, damage:"+damageDealt);
        for (var e : entitiesList) {
            // 若在锥形范围里，则主目标A->溅射目标B的向量 和攻击者P->主目标A的向量 满足一个条件(cosθ<=角度)
            Vec3 t2 = tar.vectorTo(e.position()).normalize();
            if(Vector0.getTheta(vecT, t2) > Math.PI / 3) continue;

            // 这里能获取到实体, 但交互为何---
            // 破案了, 假人会把伤害吞掉
            // 暂时没找到方法让粒子锥形发射, 就先写成被命中目标散发粒子好了
            e.playSound(SoundEvents.FIRE_EXTINGUISH, .4f, 1);
            for (int i = 0; i < 20; i++) {
                ((ServerLevel) level).sendParticles(
                    ParticleTypes.FLAME,
                    e.getX(),
                    e.getY() + 1,
                    e.getZ(),
                    1,
                    random.nextGaussian(),
                    random.nextGaussian(),
                    random.nextGaussian(),
                    .1);
            }
            // DeBug.Logger.log(e.getStringUUID());
            if(e == target) continue;
            e.hurt(level.damageSources().explosion(attacker, attacker), (float) (rawDamage * getDamageMuti(tagLevel)));
        }

        return knockback;
//        if(!(attacker instanceof Player)) return knockback;
        // 释放扇形范围灰烬粒子
//        int degree = 120;
//        for(int i=0; i<degree; i++){
//            float rad = (float) ((i - (double) degree /2) / Math.PI);
//            vecApply = vecT.yRot(rad).scale(speedMuti);
//
//            // todo 让粒子以扇形发射
//            ((ServerLevel) level).sendParticles(
//                    ParticleTypes.FLAME,
//                    tar.x,
//                    tar.y + 1,
//                    tar.z,
//                    1,
//                    vecApply.x,
//                    0,
//                    vecApply.z,
//                    1);
//
//        }
    }

    @Override
    public void addTooltip(IToolStackView tool, ModifierEntry modifier, @Nullable Player player, List<Component> tooltip, TooltipKey key, TooltipFlag tooltipFlag) {
        ToolType type = ToolType.from(tool.getItem(), TYPES);
        if (type != null) {
            int level = modifier.getLevel();
            double bonus = getDamageMuti(level);
            if (bonus > 0) {
                // todo: 加个lang
                TooltipModifierHook.addPercentBoost(this, TooltipModifierHook.statName(this, ToolStats.ATTACK_DAMAGE), bonus, tooltip);
            }
        }
    }
}
