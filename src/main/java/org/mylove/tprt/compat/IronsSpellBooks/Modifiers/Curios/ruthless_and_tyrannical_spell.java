package org.mylove.tprt.compat.IronsSpellBooks.Modifiers.Curios;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.mylove.tprt.common.modifier.modifierModule.CuriosModifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import top.theillusivec4.curios.api.SlotContext;

import java.util.UUID;
import java.util.function.BiConsumer;

public class ruthless_and_tyrannical_spell extends CuriosModifier {

    private static final UUID ID = UUID.fromString("e2e9259a-7b99-4fb2-88bc-1c970ee245fb");

    public void modifyCurioAttribute(IToolStackView curio, ModifierEntry entry, SlotContext context, UUID uuid, BiConsumer<Attribute, AttributeModifier> consumer) {
        consumer.accept(AttributeRegistry.SPELL_POWER.get(), new AttributeModifier(ID, "ruthless_and_tyrannical_spell", 0.25 * (float)entry.getLevel(), AttributeModifier.Operation.MULTIPLY_TOTAL));
        consumer.accept(AttributeRegistry.SPELL_RESIST.get(), new AttributeModifier(ID, "ruthless_and_tyrannical_spell", -0.25 * (float)entry.getLevel(), AttributeModifier.Operation.MULTIPLY_TOTAL));
        consumer.accept(Attributes.ARMOR, new AttributeModifier(ID, "ruthless_and_tyrannical_spell", -0.25 * (float)entry.getLevel(), AttributeModifier.Operation.MULTIPLY_TOTAL));
    }
}
