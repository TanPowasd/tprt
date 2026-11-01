package org.mylove.tprt.client;

import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.mylove.tprt.Tprt;
import org.mylove.tprt.registries.item.ItemsRegistry;

/**
 * Client-only static rendering support; deliberately has no GeckoLib references.
 *
 * <p>The native tool model bakes material colours; Tinkers' own colour handler
 * supplies modifier tints without multiplying the material colour a second time.
 */
@Mod.EventBusSubscriber(modid = Tprt.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class LanceClientEvents {
    private LanceClientEvents() {}

    @SubscribeEvent
    public static void registerColors(net.minecraftforge.client.event.RegisterColorHandlersEvent.Item event) {
        event.register(slimeknights.tconstruct.library.client.model.tools.ToolModel.COLOR_HANDLER,
                ItemsRegistry.LANCE.get());
    }

    @SubscribeEvent
    public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ItemProperties.register(ItemsRegistry.LANCE.get(),
                    new ResourceLocation(Tprt.MODID, "lance_charging"),
                    (stack, level, entity, seed) -> {
                        if (entity == null || !entity.isUsingItem() || entity.getUseItem() != stack) return 0.0F;
                        int elapsed = stack.getUseDuration() - entity.getUseItemRemainingTicks();
                        return Math.min(1.0F, Math.max(0.001F, elapsed / 8.0F));
                    });
            // Drives the tconstruct:broken model override that swaps in the
            // damaged head sprite once durability runs out.
            slimeknights.tconstruct.library.client.model.TinkerItemProperties
                    .registerBrokenProperty(ItemsRegistry.LANCE.get());
        });
    }
}
