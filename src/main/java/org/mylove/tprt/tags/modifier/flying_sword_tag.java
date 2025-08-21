package org.mylove.tprt.tags.modifier;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.mylove.tprt.entities.flying_sword.FlyingSword;
import org.mylove.tprt.utilities.Math0;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.armor.EquipmentChangeModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.build.ModifierRemovalHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.InventoryTickModifierHook;
import slimeknights.tconstruct.library.modifiers.impl.SingleLevelModifier;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.context.EquipmentChangeContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import javax.annotation.Nullable;
import java.util.UUID;

// 此类为所有强化所共有，不要在这里加特指某个工具的数据
public class flying_sword_tag extends SingleLevelModifier implements
        EquipmentChangeModifierHook, InventoryTickModifierHook, ModifierRemovalHook {
    private FlyingSword flyingSwordEntity;
    private UUID swordUUID;

    // 生成飞剑，一个工具对应的飞剑应当是唯一的
    private FlyingSword generateFlyingSword(IToolStackView tool, Level level, Player player, int itemSlot, ItemStack stack, @Nullable UUID uuid){
        FlyingSword flyingSword = new FlyingSword(tool, level, player, itemSlot, stack);
//        flyingSword.setPos(player.getX(), player.getY(), player.getZ());
        level.addFreshEntity(flyingSword);

        return flyingSword;
    }

    private void degenerateFlyingSword(){

    }

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
//        generateFlyingSword(tool, context);
    }

    @Override
    public void onInventoryTick(IToolStackView tool, ModifierEntry modifier, Level level, LivingEntity holder, int itemSlot, boolean isSelected, boolean isCorrectSlot, ItemStack stack) {
        if(!(holder instanceof Player player)) return;
        if(player.tickCount % 20 != 1) return;
        // todo: 处理位于副手的情况 08/21
        if(!Math0.isBetweenAnd(itemSlot, 0, 8)) return;

        if(!level.isClientSide){
            if(flyingSwordEntity == null){
                this.flyingSwordEntity = generateFlyingSword(tool, level, player, itemSlot, stack, swordUUID);
            }
        } else {
            // holder.sendSystemMessage(Component.literal(itemSlot+"  "+player.tickCount));
        }
    }

    @Override
    public @org.jetbrains.annotations.Nullable Component onRemoved(IToolStackView tool, Modifier modifier) {
        return null;
    }
}
