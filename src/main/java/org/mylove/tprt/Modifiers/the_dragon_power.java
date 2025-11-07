package org.mylove.tprt.Modifiers;

import net.minecraft.world.entity.LivingEntity;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeDamageModifierHook;
import slimeknights.tconstruct.library.modifiers.impl.NoLevelsModifier;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import java.util.UUID;

public class the_dragon_power extends NoLevelsModifier implements MeleeDamageModifierHook {
    UUID uuid = UUID.fromString("d6ab3741-d1ad-4e3e-1145-f37f1aac9cf5");
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        super.registerHooks(hookBuilder);
        hookBuilder.addHook(this, ModifierHooks.MELEE_DAMAGE);
    }
    @Override
    public float getMeleeDamage(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float baseDamage, float damage) {
        LivingEntity entity=context.getLivingTarget();
        LivingEntity attacker=context.getAttacker();
        double bl=1.0f;
        if(entity!=null && attacker!=null){
            double MaxA_H=attacker.getMaxHealth();
            //double bl=1;
            if(MaxA_H<=400.0f){
                bl+=((int)MaxA_H/10)*0.1f;
            }
            else{
                bl=4;
                MaxA_H-=400;
                int powL= (int) (MaxA_H/10);
                bl=bl*Math.pow(1.1,powL);
            }
        }
        return (float) (damage*bl);
    }
}
