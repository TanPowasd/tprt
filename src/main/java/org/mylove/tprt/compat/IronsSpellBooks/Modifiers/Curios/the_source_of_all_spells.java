package org.mylove.tprt.compat.IronsSpellBooks.Modifiers.Curios;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import org.jetbrains.annotations.NotNull;
import org.mylove.tprt.common.modifier.modifierModule.CuriosModifier;
import org.mylove.tprt.registries.ModifierIds;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.modules.build.ModifierRequirementsModule;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import top.theillusivec4.curios.api.SlotContext;

import java.util.UUID;
import java.util.function.BiConsumer;

public class the_source_of_all_spells extends CuriosModifier {

    private static final UUID ID = UUID.fromString("7977b8d0-fde6-44fd-b953-7551c25c7d1c");

    protected void registerHooks(ModuleHookMap.@NotNull Builder hookBuilder) {
        super.registerHooks(hookBuilder);
        hookBuilder.addModule(ModifierRequirementsModule.builder().requireModifier(ModifierIds.netherite_spell_book, 1).modifierKey(ModifierIds.the_source_of_all_spells).build());
        hookBuilder.addModule(ModifierRequirementsModule.builder().requireModifier(ModifierIds.gold_spell_book, 1).modifierKey(ModifierIds.the_source_of_all_spells).build());
        hookBuilder.addModule(ModifierRequirementsModule.builder().requireModifier(ModifierIds.evoker_spell_book, 1).modifierKey(ModifierIds.the_source_of_all_spells).build());
        hookBuilder.addModule(ModifierRequirementsModule.builder().requireModifier(ModifierIds.blaze_spell_book, 1).modifierKey(ModifierIds.the_source_of_all_spells).build());
        hookBuilder.addModule(ModifierRequirementsModule.builder().requireModifier(ModifierIds.necronomicon_spell_book, 1).modifierKey(ModifierIds.the_source_of_all_spells).build());
        hookBuilder.addModule(ModifierRequirementsModule.builder().requireModifier(ModifierIds.dragonskin_spell_book, 1).modifierKey(ModifierIds.the_source_of_all_spells).build());
        hookBuilder.addModule(ModifierRequirementsModule.builder().requireModifier(ModifierIds.villager_spell_book, 1).modifierKey(ModifierIds.the_source_of_all_spells).build());
        hookBuilder.addModule(ModifierRequirementsModule.builder().requireModifier(ModifierIds.druidic_spell_book, 1).modifierKey(ModifierIds.the_source_of_all_spells).build());
        hookBuilder.addModule(ModifierRequirementsModule.builder().requireModifier(ModifierIds.cursed_doll_spell_book, 1).modifierKey(ModifierIds.the_source_of_all_spells).build());
        hookBuilder.addModule(ModifierRequirementsModule.builder().requireModifier(ModifierIds.ice_spell_book, 1).modifierKey(ModifierIds.the_source_of_all_spells).build());
    }

    public boolean isNoLevels() {
        return true;
    }

    public void modifyCurioAttribute(IToolStackView curio, ModifierEntry entry, SlotContext context, UUID uuid, BiConsumer<Attribute, AttributeModifier> consumer) {
        consumer.accept(AttributeRegistry.MAX_MANA.get(), new AttributeModifier(ID, "the_source_of_all_spells", 1500 * (float)entry.getLevel(), AttributeModifier.Operation.ADDITION));
        consumer.accept(AttributeRegistry.MANA_REGEN.get(), new AttributeModifier(ID, "the_source_of_all_spells", 300 * (float)entry.getLevel(), AttributeModifier.Operation.ADDITION));
        consumer.accept(AttributeRegistry.SPELL_POWER.get(), new AttributeModifier(ID, "the_source_of_all_spells", 0.6 * (float)entry.getLevel(), AttributeModifier.Operation.MULTIPLY_TOTAL));
    }
}
