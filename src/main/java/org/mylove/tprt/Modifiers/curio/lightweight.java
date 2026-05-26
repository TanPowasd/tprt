package org.mylove.tprt.Modifiers.curio;

import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.mylove.tprt.common.modifier.modifierModule.CuriosModifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.shared.TinkerAttributes;
import top.theillusivec4.curios.api.SlotContext;

import java.util.UUID;
import java.util.function.BiConsumer;

public class lightweight extends CuriosModifier {

    private static final UUID ID = UUID.fromString("3b723bb6-fc85-4ed5-ba8c-81697bd0e705");

    public void modifyCurioAttribute(IToolStackView curio, ModifierEntry entry, SlotContext context, UUID uuid, BiConsumer<Attribute, AttributeModifier> consumer) {
        consumer.accept(TinkerAttributes.JUMP_BOOST.get(), new AttributeModifier(ID, "lightweight", 0.75 * (float)entry.getLevel(), AttributeModifier.Operation.ADDITION));
        consumer.accept(Attributes.MOVEMENT_SPEED, new AttributeModifier(ID, "lightweight", 0.15 * (float)entry.getLevel(), AttributeModifier.Operation.MULTIPLY_TOTAL));
    }
}