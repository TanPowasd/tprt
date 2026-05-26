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

public class ice_spell_book extends CuriosModifier {

    private static final UUID ID = UUID.fromString("6d564807-a7c1-4f8d-8105-62cf69921bda");

    public boolean isNoLevels() {
        return true;
    }

    public void modifyCurioAttribute(IToolStackView curio, ModifierEntry entry, SlotContext context, UUID uuid, BiConsumer<Attribute, AttributeModifier> consumer) {
        consumer.accept(AttributeRegistry.MAX_MANA.get(), new AttributeModifier(ID, "ice_spell_book", 200 * (float)entry.getLevel(), AttributeModifier.Operation.ADDITION));
        consumer.accept(AttributeRegistry.ICE_SPELL_POWER.get(), new AttributeModifier(ID, "ice_spell_book", 0.1 * (float)entry.getLevel(), AttributeModifier.Operation.ADDITION));
    }
}
