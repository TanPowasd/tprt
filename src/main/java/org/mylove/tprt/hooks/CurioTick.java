package org.mylove.tprt.hooks;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import top.theillusivec4.curios.api.SlotContext;

public interface CurioTick extends CurioInventoryTickModifierHook {
    void CurioTick(IToolStackView curio, ModifierEntry entry, SlotContext context, LivingEntity entity, int level, ItemStack stack);
}
