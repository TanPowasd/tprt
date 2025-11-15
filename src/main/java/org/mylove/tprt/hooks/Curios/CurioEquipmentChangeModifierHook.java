package org.mylove.tprt.hooks.Curios;

import java.util.Collection;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import top.theillusivec4.curios.api.SlotContext;

public interface CurioEquipmentChangeModifierHook {
    default void onCurioEquip(IToolStackView curio, ModifierEntry entry, SlotContext context, LivingEntity entity, ItemStack prevStack, ItemStack stack) {
    }

    default void onCurioUnequip(IToolStackView curio, ModifierEntry entry, SlotContext context, LivingEntity entity, ItemStack newStack, ItemStack stack) {
    }

    public static record AllMerger(Collection<org.mylove.tprt.hooks.Curios.CurioEquipmentChangeModifierHook> modules) implements org.mylove.tprt.hooks.Curios.CurioEquipmentChangeModifierHook {
        public void onCurioEquip(IToolStackView curio, ModifierEntry entry, SlotContext context, LivingEntity entity, ItemStack prevStack, ItemStack stack) {
            for(org.mylove.tprt.hooks.Curios.CurioEquipmentChangeModifierHook module : this.modules) {
                module.onCurioEquip(curio, entry, context, entity, prevStack, stack);
            }

        }

        public void onCurioUnequip(IToolStackView curio, ModifierEntry entry, SlotContext context, LivingEntity entity, ItemStack newStack, ItemStack stack) {
            for(org.mylove.tprt.hooks.Curios.CurioEquipmentChangeModifierHook module : this.modules) {
                module.onCurioUnequip(curio, entry, context, entity, newStack, stack);
            }

        }
    }
}
