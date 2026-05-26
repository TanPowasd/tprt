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

public class magician extends CuriosModifier {

    private static final UUID ID = UUID.fromString("2fd15ef4-a170-4cd1-87a7-89f48b9f54ad");

    public void modifyCurioAttribute(IToolStackView curio, ModifierEntry entry, SlotContext context, UUID uuid, BiConsumer<Attribute, AttributeModifier> consumer) {
        consumer.accept(AttributeRegistry.CAST_TIME_REDUCTION.get(), new AttributeModifier(ID, "magician", 0.1 * (float)entry.getLevel(), AttributeModifier.Operation.MULTIPLY_TOTAL));
        consumer.accept(AttributeRegistry.COOLDOWN_REDUCTION.get(), new AttributeModifier(ID, "magician", 0.1 * (float)entry.getLevel(), AttributeModifier.Operation.MULTIPLY_TOTAL));
    }
}
