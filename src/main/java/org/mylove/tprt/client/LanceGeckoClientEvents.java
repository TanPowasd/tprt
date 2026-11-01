package org.mylove.tprt.client;

import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import org.mylove.tprt.Tprt;

/** Additional display models; the inventory registry always retains the native tool. */
@Mod.EventBusSubscriber(modid = Tprt.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class LanceGeckoClientEvents {
    private static final ResourceLocation GEO = new ResourceLocation(Tprt.MODID, "item/tool/lance/lance_geo");
    private static final ResourceLocation STATIC = new ResourceLocation(Tprt.MODID, "item/tool/lance/lance");
    private static volatile BakedModel displayModel;

    private LanceGeckoClientEvents() {}

    public static BakedModel displayModel() { return displayModel; }

    @SubscribeEvent
    public static void registerAdditional(ModelEvent.RegisterAdditional event) {
        event.register(STATIC);
        if (ModList.get().isLoaded("geckolib")) event.register(GEO);
    }

    @SubscribeEvent
    public static void selectDisplayModel(ModelEvent.ModifyBakingResult event) {
        // Reset every reload: never keep a stale baked model or replace INVENTORY.
        displayModel = event.getModels().get(STATIC);
        if (ModList.get().isLoaded("geckolib")) {
            BakedModel geo = event.getModels().get(GEO);
            if (geo != null && geo.isCustomRenderer()) displayModel = geo;
            else com.mojang.logging.LogUtils.getLogger().warn("GeckoLib lance model not baked; keeping static display fallback");
        }
    }
}
