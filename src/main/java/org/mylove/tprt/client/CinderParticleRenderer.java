package org.mylove.tprt.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.mylove.tprt.Tprt;
import org.mylove.tprt.entity.tec.CinderParticle;


/**
 * <h3>references:</h3>
 * <ul>
 *     <li>{@link net.minecraft.client.renderer.entity.ItemFrameRenderer} {@link net.minecraft.client.renderer.entity.FoxRenderer} 物品渲染参考</li>
 *     <li><a href="https://learnopengl.com/Getting-started/Transformations">@OpenGL</a> 矩阵变换</li>
 *     <li><a href="https://github.com/Krasjet/quaternion">@Github</a> 四元数</li>
 * </ul>
 * <p>"It is advised to first do scaling operations, then rotations and lastly translations when combining matrices otherwise they may (negatively) affect each other."<p/>
 */
@OnlyIn(Dist.CLIENT)
public class CinderParticleRenderer extends EntityRenderer<CinderParticle> {
    private final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Tprt.MODID, "");
    public CinderParticleRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager);
    }

    @Override
    public void render(CinderParticle entity, float entityYaw, float partialTicks,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(CinderParticle entity) {
        return TEXTURE;
    }
}
