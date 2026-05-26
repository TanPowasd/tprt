package org.mylove.tprt.compat.Cloudertinker.Modifiers.Curios;

import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.mylove.tprt.common.modifier.modifierModule.CuriosModifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import top.theillusivec4.curios.api.SlotContext;

import java.util.UUID;
import java.util.function.BiConsumer;

public class Fragile extends CuriosModifier {

    private static final UUID ID = UUID.fromString("d2a6c49f-151f-46b1-81b5-e8bfa2fcb47a");

    public void modifyCurioAttribute(IToolStackView curio, ModifierEntry entry, SlotContext context, UUID uuid, BiConsumer<Attribute, AttributeModifier> consumer) {
        consumer.accept(Attributes.MAX_HEALTH, new AttributeModifier(ID, "fragile", -0.35 * (float)entry.getLevel(), AttributeModifier.Operation.MULTIPLY_TOTAL));
        consumer.accept(Attributes.ATTACK_DAMAGE, new AttributeModifier(ID, "fragile", 0.25 * (float)entry.getLevel(), AttributeModifier.Operation.MULTIPLY_TOTAL));
    }
}
