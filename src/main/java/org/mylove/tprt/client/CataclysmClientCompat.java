package org.mylove.tprt.client;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraftforge.client.event.EntityRenderersEvent;
import org.mylove.tprt.compat.Cataclysm.CataclysmCompat;

/**
 * 灾变联动客户端注册。
 *
 * 单独放在这个类里，只有确认灾变已加载时才会被调用 (进而被类加载)；
 * 否则 ClientModEvents 的字节码校验会去加载灾变的渲染器类，导致 NoClassDefFoundError。
 */
public final class CataclysmClientCompat {

    private CataclysmClientCompat() {
    }

    @SuppressWarnings("unchecked")
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(CataclysmCompat.TPRT_DEATH_LASER.get(),
                ctx -> (EntityRenderer) new com.github.L_Ender.cataclysm.client.render.entity
                        .Death_Laser_beam_Renderer(ctx));
    }
}
