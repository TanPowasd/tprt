package org.mylove.tprt.compat.Cataclysm.Modifiers;

import com.github.L_Ender.cataclysm.init.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.mylove.tprt.Tprt;
import slimeknights.mantle.client.TooltipKey;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.display.TooltipModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.InventoryTickModifierHook;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;

import java.util.List;

public class Void_Power extends Modifier implements TooltipModifierHook, InventoryTickModifierHook {

    public static final ResourceLocation Void_Power = Tprt.getResource("void_power");

    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        hookBuilder.addHook(this, ModifierHooks.TOOLTIP,ModifierHooks.INVENTORY_TICK);
    }

    @Override
    public void addTooltip(IToolStackView tool, ModifierEntry modifier, @Nullable Player player, List<Component> list, TooltipKey tooltipKey, TooltipFlag tooltipFlag) {
        if (player != null) {
            ModDataNBT tooldata = tool.getPersistentData();
            list.add(Component.translatable("modifier.tprt.void_power.tooltip", tooldata.getInt(Void_Power),modifier.getLevel()* 50 + 50).withStyle(ChatFormatting.DARK_PURPLE));
        }
    }

    @Override
    public void onInventoryTick(IToolStackView tool, ModifierEntry modifier, Level level, LivingEntity livingEntity, int itemSlot, boolean isSelected, boolean isCorrectSlot, ItemStack itemStack) {
        if (level.isClientSide || !(livingEntity instanceof Player player)) return;
        int X = modifier.getLevel();
        if (X <= 0) return;
        if (player.tickCount % 10 != 0) return;
        if (!isSelected && !isCorrectSlot) return;
        ModDataNBT tooldata = tool.getPersistentData();
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack invStack = player.getInventory().getItem(i);
            if (invStack.is(ModItems.VOID_STONE.get()) && tooldata.getInt(Void_Power)<= X * 50 + 40) {
                invStack.shrink(1);
                tooldata.putInt(Void_Power,tooldata.getInt(Void_Power)+10);
                break;
            }
        }
    }
}
