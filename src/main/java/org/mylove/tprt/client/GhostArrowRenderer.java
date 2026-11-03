package org.mylove.tprt.client;

import com.github.L_Ender.cataclysm.client.render.CMRenderTypes;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.projectile.AbstractArrow;

/** Cataclysm phantom-arrow geometry adapted to ANY AbstractArrow; no phantom-entity cast or proxy entity.
 * Only load/register this class when Cataclysm is present. No fade: that would imply phantom lifetime behavior.
 */
public class GhostArrowRenderer<T extends AbstractArrow> extends EntityRenderer<T> {
    private static final ResourceLocation TEXTURE = new ResourceLocation("cataclysm", "textures/entity/maledictus/phantom_arrow.png");
    private static final RenderType GHOST = CMRenderTypes.getGhost(TEXTURE);

    public GhostArrowRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(T arrow) {
        return TEXTURE;
    }

    @Override
    public void render(T arrow, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partialTick, arrow.yRotO, arrow.getYRot()) - 90.0F));
        pose.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(partialTick, arrow.xRotO, arrow.getXRot())));
        float shake = arrow.shakeTime - partialTick;
        if (shake > 0) pose.mulPose(Axis.ZP.rotationDegrees(-Mth.sin(shake * 3.0F) * shake));
        pose.mulPose(Axis.XP.rotationDegrees(45.0F));
        pose.scale(0.075F, 0.075F, 0.075F);
        pose.translate(-4.0F, 0.0F, 0.0F);
        VertexConsumer vertex = buffers.getBuffer(GHOST);
        vertex(pose, vertex, -7, -2, -2, 0, 0.15625F, -1, 0, 0, light);
        vertex(pose, vertex, -7, -2, 2, 0.15625F, 0.15625F, -1, 0, 0, light);
        vertex(pose, vertex, -7, 2, 2, 0.15625F, 0.3125F, -1, 0, 0, light);
        vertex(pose, vertex, -7, 2, -2, 0, 0.3125F, -1, 0, 0, light);
        vertex(pose, vertex, -7, 2, -2, 0, 0.15625F, 1, 0, 0, light);
        vertex(pose, vertex, -7, 2, 2, 0.15625F, 0.15625F, 1, 0, 0, light);
        vertex(pose, vertex, -7, -2, 2, 0.15625F, 0.3125F, 1, 0, 0, light);
        vertex(pose, vertex, -7, -2, -2, 0, 0.3125F, 1, 0, 0, light);
        for (int i = 0; i < 4; i++) {
            pose.mulPose(Axis.XP.rotationDegrees(90.0F));
            vertex(pose, vertex, -8, -2, 0, 0, 0, 0, 0, 1, light);
            vertex(pose, vertex, 8, -2, 0, 0.65F, 0, 0, 0, 1, light);
            vertex(pose, vertex, 8, 2, 0, 0.65F, 0.15625F, 0, 0, 1, light);
            vertex(pose, vertex, -8, 2, 0, 0, 0.15625F, 0, 0, 1, light);
        }
        pose.popPose();
        super.render(arrow, yaw, partialTick, pose, buffers, light);
    }

    private static void vertex(PoseStack pose, VertexConsumer vertex, int x, int y, int z,
                               float u, float v, int nx, int ny, int nz, int light) {
        vertex.vertex(pose.last().pose(), x, y, z).color(255, 255, 255, 255).uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light)
                .normal(pose.last().normal(), nx, ny, nz).endVertex();
    }
}
