package org.mylove.tprt.Modifiers;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeDamageModifierHook;
import slimeknights.tconstruct.library.modifiers.impl.NoLevelsModifier;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;


public class Magic_sublimation extends NoLevelsModifier implements MeleeDamageModifierHook {

    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        super.registerHooks(hookBuilder);
        hookBuilder.addHook(this, ModifierHooks.MELEE_DAMAGE);
    }

    @Override
    public  float getMeleeDamage(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float baseDamage, float damage) {
        LivingEntity attacker = context.getPlayerAttacker();
        LivingEntity target = context.getLivingTarget();
        AttributeInstance X = null;
        if (attacker != null) {
            X = attacker.getAttribute(AttributeRegistry.SPELL_POWER.get());
        }
        if(attacker!=null && target != null){
            if (X!= null){X.getValue();}
            if (X != null) {
                float y = (float) Math.sqrt(X.getValue());
                return damage* y;
            }
        }
        return damage;
    }
}
