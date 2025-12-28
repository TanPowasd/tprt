package org.mylove.tprt.Modifiers;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeDamageModifierHook;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import java.util.Random;
import java.util.UUID;

public class sacred_sacrifice extends Modifier implements MeleeDamageModifierHook {
    UUID uuid=UUID.fromString("d2ab3741-d1ad-4e3e-afa2-f37f1aa11451");
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        super.registerHooks(hookBuilder);
        hookBuilder.addHook(this, ModifierHooks.MELEE_DAMAGE);
    }
    @Override
    public float getMeleeDamage(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float baseDamage, float damage) {
        LivingEntity entity = context.getLivingTarget();
        if(entity != null) {
           LivingEntity myself= context.getAttacker();
           if(myself!=null) {
               Random rand=new Random();
               myself.setHealth(myself.getHealth() - modifier.getLevel() * rand.nextInt(5)+1);
               myself.addEffect(new MobEffectInstance(MobEffects.REGENERATION,400,4));
           }
        }
        return damage+(entity.getMaxHealth()-entity.getHealth())*0.3f*modifier.getLevel();
    }
}
