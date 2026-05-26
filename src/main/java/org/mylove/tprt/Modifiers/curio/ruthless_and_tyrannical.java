package org.mylove.tprt.Modifiers.curio;

import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.mylove.tprt.common.modifier.modifierModule.CuriosModifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import top.theillusivec4.curios.api.SlotContext;

import java.util.UUID;
import java.util.function.BiConsumer;

public class ruthless_and_tyrannical extends CuriosModifier {

    private static final UUID ID = UUID.fromString("b0419b9c-98b1-42a6-9545-d2c35e2c225c");

    public void modifyCurioAttribute(IToolStackView curio, ModifierEntry entry, SlotContext context, UUID uuid, BiConsumer<Attribute, AttributeModifier> consumer) {
        consumer.accept(Attributes.ARMOR, new AttributeModifier(ID, "ruthless_and_tyrannical", -0.2 * (float)entry.getLevel(), AttributeModifier.Operation.MULTIPLY_TOTAL));
        consumer.accept(Attributes.ATTACK_DAMAGE, new AttributeModifier(ID, "ruthless_and_tyrannical", 0.2 * (float)entry.getLevel(), AttributeModifier.Operation.MULTIPLY_BASE));
        consumer.accept(Attributes.ATTACK_SPEED, new AttributeModifier(ID, "ruthless_and_tyrannical", 0.15 * (float)entry.getLevel(), AttributeModifier.Operation.MULTIPLY_BASE));
    }
}
