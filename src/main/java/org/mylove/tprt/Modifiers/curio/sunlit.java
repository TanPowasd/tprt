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

public class sunlit extends CuriosModifier {

    private static final UUID ID = UUID.fromString("0d197bbf-6dcf-4b7f-8d34-d68b8a8eba8f");

    public void modifyCurioAttribute(IToolStackView curio, ModifierEntry entry, SlotContext context, UUID uuid, BiConsumer<Attribute, AttributeModifier> consumer) {
        consumer.accept(TinkerAttributes.CRITICAL_DAMAGE.get(), new AttributeModifier(ID, "sunlit", 0.2 * (float)entry.getLevel(), AttributeModifier.Operation.ADDITION));
    }
}