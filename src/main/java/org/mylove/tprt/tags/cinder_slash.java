package org.mylove.tprt.tags;

import net.minecraft.client.particle.Particle;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
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

import java.util.List;

/// 灰烬斩击-合金巨兽材料词条
public class cinder_slash extends Modifier implements MeleeHitModifierHook, TooltipModifierHook {
    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        hookBuilder.addHook(this, ModifierHooks.MELEE_HIT, ModifierHooks.TOOLTIP);
    }

    @Override
    public void afterMeleeHit(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float damageDealt) {
        LivingEntity attacker = context.getAttacker();
        LivingEntity target = context.getLivingTarget();
        if(target == null) return;
        Level level = attacker.level();
        Vec3 att, tar, vecT, vecApply;
        double speedMuti, boxX, boxY, boxZ;
        int tagLevel = modifier.getLevel();

        att = attacker.position();
        tar = target.position();
        vecT = att.vectorTo(tar).normalize();
        speedMuti = tagLevel * 2;

        // todo: 暂时先用轴对齐的, 之后参考下@better combat实现
        boxX = 3 + tagLevel * 2;
        boxY = 3;
        boxZ = 3 + tagLevel * 2;
        AABB range = AABB.ofSize(tar, boxX, boxY, boxZ);
        List<? extends Entity> entitiesList = level.getEntities(attacker, range, entity -> entity.isPickable() && entity.isAlive());
        DeBug.Logger.log("普通攻击触发");
        for (var e : entitiesList) {
            // EntityHitResult entityHitResult = new EntityHitResult(e, tar);
//            e.hurt(new DamageSource(s, attacker), damageDealt);

            // 这里能获取到实体, 但交互为何---
            e.hurt(level.damageSources().lava(), damageDealt);
            DeBug.Logger.log(e.getStringUUID());
        }

        // todo 换个动静
        target.playSound(SoundEvents.BLAZE_BURN);
        if(!(attacker instanceof Player)) return;
        // 释放扇形范围灰烬粒子
        int degree = 120;
        for(int i=0; i<degree; i++){
            float rad = (float) ((i - (double) degree /2) / Math.PI);
            vecApply = vecT.yRot(rad).scale(speedMuti);

            // todo 让粒子以扇形发射
            ((ServerLevel)level).sendParticles(
                    ParticleTypes.FLAME,
                    tar.x,
                    tar.y+1,
                    tar.z,
                    1,
                    vecApply.x,
                    0,
                    vecApply.z,
                    1);

        }
    }

    @Override
    public void addTooltip(IToolStackView tool, ModifierEntry modifier, @Nullable Player player, List<Component> tooltip, TooltipKey tooltipKey, TooltipFlag tooltipFlag) {

    }
}
