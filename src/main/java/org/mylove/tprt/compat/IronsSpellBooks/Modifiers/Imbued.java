package org.mylove.tprt.compat.IronsSpellBooks.Modifiers;

import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.armor.EquipmentChangeModifierHook;
import slimeknights.tconstruct.library.modifiers.impl.SingleLevelModifier;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.context.EquipmentChangeContext;
import slimeknights.tconstruct.library.tools.item.armor.ModifiableArmorItem;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

public class Imbued extends SingleLevelModifier implements EquipmentChangeModifierHook {

    @Override
    protected void registerHooks(ModuleHookMap.@NotNull Builder hookBuilder) {
        super.registerHooks(hookBuilder);
        hookBuilder.addHook(this, ModifierHooks.EQUIPMENT_CHANGE);
    }

    @Override
    public void onEquip(@NotNull IToolStackView tool, @NotNull ModifierEntry modifier, EquipmentChangeContext context) {
        ItemStack item = context.getReplacement();
        if (!ISpellContainer.isSpellContainer(item)) {
            var container = ISpellContainer.create(modifier.getLevel(), true, item.getItem() instanceof ModifiableArmorItem);
            ISpellContainer.set(item, container);
        }
        EquipmentChangeModifierHook.super.onEquip(tool, modifier, context);
    }
}
