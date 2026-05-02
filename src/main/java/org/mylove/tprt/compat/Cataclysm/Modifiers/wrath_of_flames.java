package org.mylove.tprt.compat.Cataclysm.Modifiers;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeDamageModifierHook;
import slimeknights.tconstruct.library.modifiers.impl.NoLevelsModifier;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import java.util.UUID;

public class wrath_of_flames extends NoLevelsModifier implements MeleeDamageModifierHook {
    UUID uuid = UUID.fromString("d2ab3741-d1ad-4e3e-1145-f37f1aac9cf1");

    @Override
    protected void registerHooks(ModuleHookMap.@NotNull Builder hookBuilder) {
        super.registerHooks(hookBuilder);
        hookBuilder.addHook(this, ModifierHooks.MELEE_DAMAGE);
    }

    @Override
    public int getPriority() {
        return 51;
    }

    @Override
    public float getMeleeDamage(IToolStackView iToolStackView, ModifierEntry modifier, ToolAttackContext context, float baseDamage, float damage) {
        LivingEntity target = context.getLivingTarget();
        LivingEntity attacker = context.getAttacker();
        double y = (int) 1.25f;
        if (target != null && attacker instanceof Player) {
            AttributeInstance attribute = target.getAttribute(Attributes.ARMOR);
            if (attribute != null && attribute.getValue() == 0) {
                y = 1.75f;
            }
        }
        return (float) (baseDamage * y);
    }
}
