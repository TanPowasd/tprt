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

public class source_alloy_spell_cloth extends CuriosModifier {

    public boolean isNoLevels() {
        return true;
    }

    private static final UUID ID = UUID.fromString("5d74b1db-43e9-4b67-b142-67355777b78a");

    public void modifyCurioAttribute(IToolStackView curio, ModifierEntry entry, SlotContext context, UUID uuid, BiConsumer<Attribute, AttributeModifier> consumer) {
        consumer.accept(AttributeRegistry.SPELL_POWER.get(), new AttributeModifier(ID, "source_alloy_spell_cloth", 0.5, AttributeModifier.Operation.MULTIPLY_TOTAL));
    }
}

