package org.mylove.tprt.compat.IronsSpellBooks.Modifiers.Curios;

import io.redspace.ironsspellbooks.api.item.curios.AffinityData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import org.mylove.tprt.common.modifier.modifierModule.CuriosModifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import top.theillusivec4.curios.api.SlotContext;

import java.util.UUID;
import java.util.function.BiConsumer;

public class necronomicon_spell_book extends CuriosModifier {

    private static final UUID ID = UUID.fromString("012e40a2-f237-4a62-8cf8-88c9952d620a");

    public boolean isNoLevels() {
        return true;
    }

    public void modifyCurioAttribute(IToolStackView curio, ModifierEntry entry, SlotContext context, UUID uuid, BiConsumer<Attribute, AttributeModifier> consumer) {
        consumer.accept(AttributeRegistry.MAX_MANA.get(), new AttributeModifier(ID, "necronomicon_spell_book", 200 * (float)entry.getLevel(), AttributeModifier.Operation.ADDITION));
        consumer.accept(AttributeRegistry.BLOOD_SPELL_POWER.get(), new AttributeModifier(ID, "necronomicon_spell_book", 0.1 * (float)entry.getLevel(), AttributeModifier.Operation.ADDITION));
    }

    public void onCurioTick(IToolStackView curio, ModifierEntry entry, SlotContext context, LivingEntity entity, ItemStack stack) {
        AffinityData.setAffinityData(stack, SpellRegistry.RAISE_DEAD_SPELL.get(), 2);
    }
}
