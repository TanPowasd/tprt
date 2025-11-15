package org.mylove.tprt.hooks.Curios;

import java.util.Collection;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import top.theillusivec4.curios.api.SlotContext;

public interface CurioInventoryTickModifierHook {
    default void onCurioTick(IToolStackView curio, ModifierEntry entry, SlotContext context, LivingEntity entity, ItemStack stack) {
    }

    public static record AllMerger(Collection<org.mylove.tprt.hooks.Curios.CurioInventoryTickModifierHook> modules) implements org.mylove.tprt.hooks.Curios.CurioInventoryTickModifierHook {
        public void onCurioTick(IToolStackView curio, ModifierEntry entry, SlotContext context, LivingEntity entity, ItemStack stack) {
            for(org.mylove.tprt.hooks.Curios.CurioInventoryTickModifierHook module : this.modules) {
                module.onCurioTick(curio, entry, context, entity, stack);
            }

        }
    }
}
