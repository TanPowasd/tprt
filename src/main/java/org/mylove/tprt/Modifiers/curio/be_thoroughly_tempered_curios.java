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

public class be_thoroughly_tempered_curios extends CuriosModifier {

    private static final UUID ID = UUID.fromString("9ab0c5ce-a4ec-49e0-b85f-ae80e0ae63a4");

    public void modifyCurioAttribute(IToolStackView curio, ModifierEntry entry, SlotContext context, UUID uuid, BiConsumer<Attribute, AttributeModifier> consumer) {
        consumer.accept(Attributes.MAX_HEALTH, new AttributeModifier(ID, "be_thoroughly_tempered_curios", 0.06 * (float)entry.getLevel(), AttributeModifier.Operation.MULTIPLY_TOTAL));
        consumer.accept(Attributes.ATTACK_DAMAGE, new AttributeModifier(ID, "be_thoroughly_tempered_curios", 0.06 * (float)entry.getLevel(), AttributeModifier.Operation.MULTIPLY_TOTAL));
    }
}
