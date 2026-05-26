package org.mylove.tprt.compat.IronsSpellBooks.Modifiers.Curios;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import org.mylove.tprt.common.modifier.modifierModule.CuriosModifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import top.theillusivec4.curios.api.SlotContext;

import java.util.UUID;
import java.util.function.BiConsumer;

public class elemental_mastery extends CuriosModifier {

    private static final UUID ID = UUID.fromString("56f6b4bf-6061-4b82-8c85-84d3f6de6b2b");

    public void modifyCurioAttribute(IToolStackView curio, ModifierEntry entry, SlotContext context, UUID uuid, BiConsumer<Attribute, AttributeModifier> consumer) {
        consumer.accept(AttributeRegistry.ICE_SPELL_POWER.get(), new AttributeModifier(ID, "elemental_mastery", 0.1 * (float)entry.getLevel(), AttributeModifier.Operation.MULTIPLY_TOTAL));
        consumer.accept(AttributeRegistry.FIRE_SPELL_POWER.get(), new AttributeModifier(ID, "elemental_mastery", 0.1 * (float)entry.getLevel(), AttributeModifier.Operation.MULTIPLY_TOTAL));
        consumer.accept(AttributeRegistry.LIGHTNING_SPELL_POWER.get(), new AttributeModifier(ID, "elemental_mastery", 0.1 * (float)entry.getLevel(), AttributeModifier.Operation.MULTIPLY_TOTAL));
        consumer.accept(AttributeRegistry.NATURE_SPELL_POWER.get(), new AttributeModifier(ID, "elemental_mastery", 0.1 * (float)entry.getLevel(), AttributeModifier.Operation.MULTIPLY_TOTAL));
        consumer.accept(AttributeRegistry.HOLY_SPELL_POWER.get(), new AttributeModifier(ID, "elemental_mastery", 0.1 * (float)entry.getLevel(), AttributeModifier.Operation.MULTIPLY_TOTAL));
    }
}
