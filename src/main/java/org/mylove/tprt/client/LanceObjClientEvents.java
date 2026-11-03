package org.mylove.tprt.client;

import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.mylove.tprt.Tprt;

/** One material-aware OBJ display path, independent of optional GeckoLib installation. */
@Mod.EventBusSubscriber(modid = Tprt.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class LanceObjClientEvents {
    private static final ResourceLocation DISPLAY = new ResourceLocation(Tprt.MODID, "item/tool/lance/lance");
    private static volatile BakedModel displayModel;
    private LanceObjClientEvents() {}
    public static BakedModel displayModel() { return displayModel; }

    @SubscribeEvent
    public static void registerLoaders(ModelEvent.RegisterGeometryLoaders event) {
        event.register("lance_obj", LanceObjGeometry.LOADER);
    }

    @SubscribeEvent
    public static void registerAdditional(ModelEvent.RegisterAdditional event) { event.register(DISPLAY); }

    @SubscribeEvent
    public static void selectDisplayModel(ModelEvent.ModifyBakingResult event) {
        displayModel = event.getModels().get(DISPLAY);
    }
}
