package org.mylove.tprt.tags.modifier;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.mylove.tprt.utilities.DeBug;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.interaction.UsingToolModifierHook;
import slimeknights.tconstruct.library.modifiers.impl.SingleLevelModifier;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

/** 测试椭圆路径 */
public class test_ellipse extends SingleLevelModifier implements UsingToolModifierHook {
    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        hookBuilder.addHook(this, ModifierHooks.TOOL_USING);
    }

    @Override
    public void onUsingTick(IToolStackView tool, ModifierEntry modifier, LivingEntity entity, int useDuration, int timeLeft, ModifierEntry activeModifier) {
        if(!(entity instanceof Player player)) return;
        if(player.tickCount % 20 != 0) return;
        Level level = player.level();

        if (level.isClientSide){

            Vec3 target = player.position().add(player.getLookAngle().scale(10));
            Vec3 delta = player.position().vectorTo(target);
            double distance = delta.length();
            DeBug.Console(player, player.position().toString());
            DeBug.Console(player, target.toString());
            DeBug.Console(player, delta.toString() + "   " + distance);

            float pitch = player.getViewXRot(1);
            float yaw = player.getViewYRot(1);
            DeBug.Console(player, "pitch"+pitch);
            DeBug.Console(player, "yaw"+yaw);

            double a, b, x, z, tmp, z2;

            a = distance / 2;
            b = 2*a/3;

            int all = 360;
            for (int i=0; i<=all; i++){
                z = i * distance / all;
                tmp = b*b - b*b * (z-a)*(z-a) / (a*a);
                x = Math.sqrt(Math.abs(tmp));

                Vec3 posEllipse = new Vec3(x,0,z).add(player.position());
                level.addParticle(ParticleTypes.CRIT, posEllipse.x, posEllipse.y, posEllipse.z, 0, 0,0);

                Vec3 posEllipse2 = new Vec3(x,0,z).yRot((float) Math.toRadians(90)).add(player.position());
                level.addParticle(ParticleTypes.ENCHANTED_HIT, posEllipse2.x, posEllipse2.y, posEllipse2.z, 0, 0,0);

                Vec3 posEllipse3 = new Vec3(x,0,z).zRot((float) Math.toRadians(90)).add(player.position());
                level.addParticle(ParticleTypes.FALLING_HONEY, posEllipse3.x, posEllipse3.y, posEllipse3.z, 0, 0,0);

                // straight
                z2 = i * distance / all;
                Vec3 fi2 = new Vec3(0, 0, z2).xRot((float) Math.toRadians(-45)).add(player.position());
                level.addParticle(ParticleTypes.ELECTRIC_SPARK, fi2.x, fi2.y, fi2.z, 0, 0,0);

                Vec3 fi3 = new Vec3(0, 0, z2).xRot((float) Math.toRadians(-pitch)).yRot((float) Math.toRadians(-yaw)).add(player.position());
                level.addParticle(ParticleTypes.ELECTRIC_SPARK, fi3.x, fi3.y, fi3.z, 0, 0,0);
            }
        }
    }
}
