package org.mylove.tprt.client;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.mylove.tprt.Tprt;
import org.mylove.tprt.common.ForgingProgress;

@Mod.EventBusSubscriber(modid = Tprt.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ForgingTooltip {
    private ForgingTooltip() {}

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (ForgingProgress.hasProgress(stack)) {
            event.getToolTip().add(Component.translatable("tooltip.tprt.forging_progress",
                    ForgingProgress.getProgress(stack), ForgingProgress.TOTAL).withStyle(ChatFormatting.GOLD));
        }
    }
}
