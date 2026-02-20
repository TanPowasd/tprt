package org.mylove.tprt.Modifiers;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.interaction.GeneralInteractionModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.InventoryTickModifierHook;
import slimeknights.tconstruct.library.modifiers.impl.NoLevelsModifier;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.helper.ModifierUtil;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

public class Automation extends NoLevelsModifier implements InventoryTickModifierHook {

    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        super.registerHooks(hookBuilder);
        hookBuilder.addHook(this, ModifierHooks.INVENTORY_TICK);
    }
    public void onInventoryTick(IToolStackView iToolStackView, ModifierEntry modifier, Level world, LivingEntity entity, int in, boolean boo, boolean bo1, ItemStack stack) {
        if (!world.isClientSide&&entity.isUsingItem()&&entity.getUseItem()==stack){
            int drawtime = ModifierUtil.getPersistentInt(stack, GeneralInteractionModifierHook.KEY_DRAWTIME, -1);
            if ((stack.getUseDuration() + (stack.getUseDuration()<2?1:0) - entity.getUseItemRemainingTicks()) / (float)drawtime>=1){
                entity.releaseUsingItem();
            }
        }
    }
}
