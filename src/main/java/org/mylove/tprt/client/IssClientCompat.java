package org.mylove.tprt.client;

import net.minecraftforge.client.event.EntityRenderersEvent;
import org.mylove.tprt.compat.IronsSpellBooks.IssCompat;
import org.mylove.tprt.compat.IronsSpellBooks.entity.EchoCasterRenderer;

/**
 * 铁魔法联动客户端注册。
 *
 * 与 CataclysmClientCompat 同理：只会在确认铁魔法已加载时被调用，
 * 避免未安装铁魔法时在 ClientModEvents 的校验阶段加载 EchoCasterRenderer。
 */
public final class IssClientCompat {

    private IssClientCompat() {
    }

    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(IssCompat.ECHO_CASTER.get(), EchoCasterRenderer::new);
    }
}
