package org.mylove.tprt.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.mylove.tprt.Tprt;
import org.mylove.tprt.entity.tec.CinderParticle;
import org.mylove.tprt.registries.ModEntities;

@Mod.EventBusSubscriber(modid = Tprt.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientModEvents {
    //gecko
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {

    }

    //vanilla
    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.CINDER_SLASH_PARTICLE.get(), CinderParticleRenderer::new);
        if (org.mylove.tprt.utils.ModListUtil.CALoaded) {
            event.registerEntityRenderer(org.mylove.tprt.compat.Cataclysm.CataclysmCompat.TPRT_DEATH_LASER.get(),
                    ctx -> (net.minecraft.client.renderer.entity.EntityRenderer)
                            new com.github.L_Ender.cataclysm.client.render.entity.Death_Laser_beam_Renderer(ctx));
        }
        // 奥法回响的临时施法者 (仅铁魔法加载时注册)
        if (org.mylove.tprt.utils.ModListUtil.ISSLoaded) {
            event.registerEntityRenderer(org.mylove.tprt.compat.IronsSpellBooks.IssCompat.ECHO_CASTER.get(),
                    org.mylove.tprt.compat.IronsSpellBooks.entity.EchoCasterRenderer::new);
        }
    }

    @SubscribeEvent
    public static void onRegisterLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {

    }
    /** 次数盾: 画在生命值 UI 上方 */
    @SubscribeEvent
    public static void onRegisterOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAbove(VanillaGuiOverlay.PLAYER_HEALTH.id(), "attack_shield", Attack_shield_hud.OVERLAY);
    }
}