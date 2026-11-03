package org.mylove.tprt.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.SpectralArrowRenderer;
import net.minecraft.client.renderer.entity.TippableArrowRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.EntityRenderersEvent;
import org.mylove.tprt.common.SoulArrowVisuals;

import java.util.HashMap;
import java.util.Map;

/** Invoked only with Cataclysm loaded. Forge exposes dispatcher.renderers via its access transformer. */
public final class SoulArrowRendererCompat {
    private SoulArrowRendererCompat() {}

    public static void addLayers(EntityRenderersEvent.AddLayers event) {
        var dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        Map<EntityType<?>, EntityRenderer<?>> renderers = new HashMap<>(dispatcher.renderers);
        renderers.replaceAll((type, renderer) -> wrapKnownVanillaRenderer(event.getContext(), renderer));
        dispatcher.renderers = renderers;
    }

    @SuppressWarnings("unchecked")
    private static EntityRenderer<?> wrapKnownVanillaRenderer(EntityRendererProvider.Context context, EntityRenderer<?> renderer) {
        // Deliberately exact classes: unknown subclasses may have custom final packed-light or shadow behavior
        // that cannot be transparently delegated. Never alter non-arrow renderers or guess third-party contracts.
        if (renderer.getClass() == TippableArrowRenderer.class || renderer.getClass() == SpectralArrowRenderer.class) {
            return new MarkedArrowRenderer(context, (EntityRenderer<AbstractArrow>) renderer);
        }
        return renderer;
    }

    private static final class MarkedArrowRenderer extends EntityRenderer<AbstractArrow> {
        private final EntityRenderer<AbstractArrow> original;
        private final GhostArrowRenderer<AbstractArrow> ghost;

        private MarkedArrowRenderer(EntityRendererProvider.Context context, EntityRenderer<AbstractArrow> original) {
            super(context);
            this.original = original;
            this.ghost = new GhostArrowRenderer<>(context);
            // Both supported vanilla renderers inherit default block/sky lighting and zero shadow radius.
        }

        @Override
        public void render(AbstractArrow arrow, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
            if (SoulArrowVisuals.isMarked(arrow)) ghost.render(arrow, yaw, partialTick, pose, buffers, light);
            else original.render(arrow, yaw, partialTick, pose, buffers, light);
        }

        @Override
        public boolean shouldRender(AbstractArrow arrow, Frustum frustum, double x, double y, double z) {
            return original.shouldRender(arrow, frustum, x, y, z);
        }

        @Override
        public Vec3 getRenderOffset(AbstractArrow arrow, float partialTick) {
            return original.getRenderOffset(arrow, partialTick);
        }

        @Override
        public ResourceLocation getTextureLocation(AbstractArrow arrow) {
            return SoulArrowVisuals.isMarked(arrow) ? ghost.getTextureLocation(arrow) : original.getTextureLocation(arrow);
        }
    }
}
