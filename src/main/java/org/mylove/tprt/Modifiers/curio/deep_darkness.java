package org.mylove.tprt.Modifiers.curio;

import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import org.mylove.tprt.common.modifier.modifierModule.CuriosModifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.shared.TinkerAttributes;
import top.theillusivec4.curios.api.SlotContext;

import java.util.UUID;
import java.util.function.BiConsumer;

public class deep_darkness extends CuriosModifier {

    private static final UUID ID = UUID.fromString("994c5807-1fcc-43ac-8b27-6175678a6f30");

    public void modifyCurioAttribute(IToolStackView curio, ModifierEntry entry, SlotContext context, UUID uuid, BiConsumer<Attribute, AttributeModifier> consumer) {
        consumer.accept(TinkerAttributes.CROUCH_DAMAGE_MULTIPLIER.get(), new AttributeModifier(ID, "deep_darkness", 0.3 * (float)entry.getLevel(), AttributeModifier.Operation.ADDITION));
    }
}
