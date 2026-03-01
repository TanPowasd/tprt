package org.mylove.tprt.compat.Cataclysm.Modifiers;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeHitModifierHook;
import slimeknights.tconstruct.library.modifiers.impl.NoLevelsModifier;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import com.github.L_Ender.cataclysm.init.ModEffect;

import java.util.UUID;

public class wrath_of_flames extends NoLevelsModifier implements MeleeHitModifierHook {
    UUID uuid=UUID.fromString("d2ab3741-d1ad-4e3e-1145-f37f1aac9cf1");

    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        super.registerHooks(hookBuilder);
        hookBuilder.addHook(this, ModifierHooks.MELEE_HIT);
    }
    @Override
    public int getPriority() {
        return 51;
    }
    @Override
    public void afterMeleeHit(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float damageDealt)
    {
        LivingEntity target = context.getLivingTarget();
        LivingEntity attacker = context.getAttacker();
        if (target != null && target.hasEffect(ModEffect.EFFECTBLAZING_BRAND.get()) && attacker instanceof Player player) {
            target.invulnerableTime = 0;
            MobEffect effect = ModEffect.EFFECTBLAZING_BRAND.get();
            MobEffectInstance instance = target.getEffect(effect);
            int bufnum= 0;
            if (instance != null) {
                bufnum = instance.getAmplifier()+1;
            }
            System.out.println(damageDealt);
            if (context.getLivingTarget() != null) {
                context.getLivingTarget().hurt(context.getLivingTarget().damageSources().onFire(), damageDealt*bufnum*0.1f);
            }
        }
    }
}
