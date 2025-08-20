package org.mylove.tprt.tags.modifier;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.mylove.tprt.entities.flying_sword.FlyingSword;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.armor.EquipmentChangeModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.InventoryTickModifierHook;
import slimeknights.tconstruct.library.modifiers.impl.SingleLevelModifier;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.context.EquipmentChangeContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

public class flying_sword_tag extends SingleLevelModifier implements EquipmentChangeModifierHook, InventoryTickModifierHook {
    // UUID uuid =

    private void generateFlyingSword(IToolStackView tool, EquipmentChangeContext context){
        LivingEntity player = context.getEntity();
        player.sendSystemMessage(Component.literal("generateFlyingSword!"+"\n"));

        FlyingSword flyingSword = new FlyingSword(context.getLevel());
        flyingSword.setPos(player.getX(), player.getY(), player.getZ());
        context.getLevel().addFreshEntity(flyingSword);
    }

    private void degenerateFlyingSword(){}

    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        hookBuilder.addHook(this, ModifierHooks.EQUIPMENT_CHANGE, ModifierHooks.INVENTORY_TICK);
    }

    @Override
    public void onEquip(IToolStackView tool, ModifierEntry modifier, EquipmentChangeContext context) {
//        degenerateFlyingSword(tool, context);
    }

    @Override
    public void onUnequip(IToolStackView tool, ModifierEntry modifier, EquipmentChangeContext context) {
        generateFlyingSword(tool, context);
    }

    @Override
    public void onInventoryTick(IToolStackView tool, ModifierEntry modifier, Level world, LivingEntity holder, int itemSlot, boolean isSelected, boolean isCorrectSlot, ItemStack stack) {

    }
}
