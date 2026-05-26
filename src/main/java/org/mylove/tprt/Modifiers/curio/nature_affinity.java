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

public class nature_affinity extends CuriosModifier {

    private static final UUID ID = UUID.fromString("97f636d7-04c7-4d52-ada1-03f960ca430c");

    public void modifyCurioAttribute(IToolStackView curio, ModifierEntry entry, SlotContext context, UUID uuid, BiConsumer<Attribute, AttributeModifier> consumer) {
        consumer.accept(Attributes.MAX_HEALTH, new AttributeModifier(ID, "nature_affinity", 3 * (float)entry.getLevel(), AttributeModifier.Operation.ADDITION));
    }
}
