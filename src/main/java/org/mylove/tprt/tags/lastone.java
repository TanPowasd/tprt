package org.mylove.tprt.tags;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeDamageModifierHook;
import slimeknights.tconstruct.library.modifiers.impl.NoLevelsModifier;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import java.util.UUID;

public class lastone extends NoLevelsModifier implements MeleeDamageModifierHook {
    UUID uuid=UUID.fromString("d2ab3741-d1ad-4e3e-afa2-f37f1aac9cf1");
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        super.registerHooks(hookBuilder);
        hookBuilder.addHook(this, ModifierHooks.MELEE_DAMAGE);
    }

    public float getMeleeDamage(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float baseDamage, float damage) {
        LivingEntity entity = context.getLivingTarget();
        if(entity != null) {
            AttributeInstance instance = entity.getAttribute(Attributes.MAX_HEALTH);
            if (instance != null) {
                double basevalue = 0;
                AttributeModifier temp = instance.getModifier(uuid);
                if(temp != null) {
                    basevalue = temp.getAmount();
                }
                instance.removeModifier(uuid);
                instance.addPermanentModifier(new AttributeModifier(uuid,"reducemaxhp",basevalue - damage, AttributeModifier.Operation.ADDITION));
            }
        }
        return damage;
    }
}
