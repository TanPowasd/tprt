package org.mylove.tprt.compat.Cataclysm.Modifiers;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeHitModifierHook;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import java.util.UUID;

public class  burning_flames extends Modifier implements MeleeHitModifierHook {
    UUID uuid = UUID.fromString("d2ab3741-d1ad-4e3e-1145-f37f1aac9cf1");
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        super.registerHooks(hookBuilder);
        hookBuilder.addHook(this, ModifierHooks.MELEE_HIT);
    }

    @Override
    public void afterMeleeHit(@NotNull IToolStackView tool, @NotNull ModifierEntry modifier, ToolAttackContext context, float damageDealt) {
        if (!(context.getTarget() instanceof LivingEntity target)) {return;
        }
        LivingEntity attacker=context.getAttacker();
        if(attacker instanceof Player){
            AttributeInstance attribute1 = target.getAttribute(Attributes.ARMOR);
            AttributeInstance attribute2 = target.getAttribute(Attributes.ARMOR_TOUGHNESS);
            target.setSecondsOnFire(200 * modifier.getLevel());
            if (attribute1 != null && attribute2 != null) {
                double basevalue1 = 0;
                double basevalue2 = 0;
                AttributeModifier modifier1 = attribute1.getModifier(uuid);
                AttributeModifier modifier2 = attribute2.getModifier(uuid);
                if(modifier1 != null && modifier2 != null) {
                    basevalue1 = modifier1.getAmount();
                    basevalue2 = modifier2.getAmount();
                }
                attribute1.removeModifier(uuid);
                attribute1.addPermanentModifier(new AttributeModifier(uuid,"burning_flames",basevalue1 - 0.05 * modifier.getLevel(), AttributeModifier.Operation.MULTIPLY_TOTAL));
                attribute2.removeModifier(uuid);
                attribute2.addPermanentModifier(new AttributeModifier(uuid,"burning_flames",basevalue2 - 0.05 * modifier.getLevel(), AttributeModifier.Operation.MULTIPLY_TOTAL));
            }
        }
    }
}
