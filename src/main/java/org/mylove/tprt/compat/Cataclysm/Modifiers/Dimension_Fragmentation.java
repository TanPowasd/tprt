package org.mylove.tprt.compat.Cataclysm.Modifiers;

import com.github.L_Ender.cataclysm.init.ModEffect;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeHitModifierHook;
import slimeknights.tconstruct.library.modifiers.impl.NoLevelsModifier;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import java.util.UUID;

public class Dimension_Fragmentation extends NoLevelsModifier implements MeleeHitModifierHook {

    UUID uuid=UUID.fromString("e3ae56e6-d26a-4fa1-9e54-85275487ac14");

    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        hookBuilder.addHook(this, ModifierHooks.MELEE_HIT);
    }

    @Override
    public void afterMeleeHit(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float damageDealt) {
        if (!(context.getTarget() instanceof LivingEntity target)) return;
        LivingEntity attacker=context.getAttacker();
        if(attacker instanceof Player && target.hasEffect(ModEffect.EFFECTABYSSAL_FEAR.get())){
            AttributeInstance attribute = target.getAttribute(Attributes.MAX_HEALTH);
            if (attribute != null) {
                double basevalue = 0;
                AttributeModifier modifier1 = attribute.getModifier(uuid);
                if(modifier1 != null) {
                    basevalue = modifier1.getAmount();
                }
                attribute.removeModifier(uuid);
                attribute.addPermanentModifier(new AttributeModifier(uuid,"Dimension_Fragmentation",basevalue - 0.03, AttributeModifier.Operation.MULTIPLY_TOTAL));
            }
        }
    }
}
