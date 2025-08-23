package org.mylove.tprt.client;

import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.mylove.tprt.Tprt;
import org.mylove.tprt.client.flying_sword.FlyingSwordModelVanilla;
import org.mylove.tprt.client.flying_sword.FlyingSwordRendererVanilla;
import org.mylove.tprt.registries.ModEntities;

@Mod.EventBusSubscriber(modid = Tprt.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientModEvents {
    //gecko
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        // EntityRenderers.register(ModEntities.FLYING_SWORD.get(), FlyingSwordRenderer::new);
//        event.enqueueWork(()->{
//            EntityRenderers.register(ModEntities.FLYING_SWORD.get(), FlyingSwordRendererVanilla::new);
//        });
    }

    //vanilla
    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.FLYING_SWORD.get(), FlyingSwordRendererVanilla::new);
    }

    @SubscribeEvent
    public static void onRegisterLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(FlyingSwordModelVanilla.LAYER_LOCATION, FlyingSwordModelVanilla::createLayer);
    }
}