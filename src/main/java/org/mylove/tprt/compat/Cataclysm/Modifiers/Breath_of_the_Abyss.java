package org.mylove.tprt.compat.Cataclysm.Modifiers;

import com.github.L_Ender.cataclysm.init.ModEffect;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeHitModifierHook;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import java.util.UUID;

public class Breath_of_the_Abyss extends Modifier implements MeleeHitModifierHook {

    UUID uuid=UUID.fromString("240acc60-3d89-440c-8fd8-7d7e29d6916f");

    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        hookBuilder.addHook(this, ModifierHooks.MELEE_HIT);
    }

    @Override
    public void afterMeleeHit(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float damageDealt) {
        if (!(context.getTarget() instanceof LivingEntity target)) return;
        LivingEntity attacker=context.getAttacker();
        MobEffect effect = ModEffect.EFFECTABYSSAL_FEAR.get();
        if(attacker instanceof Player){
            target.addEffect(new MobEffectInstance(effect,200,0));
            target.invulnerableTime = 10 ;
            target.hurt(target.damageSources().fellOutOfWorld(),7.5f + 2.5f * modifier.getLevel());
        }
    }
}
