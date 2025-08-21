package org.mylove.tprt.client.flying_sword;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.mylove.tprt.Tprt;
import org.mylove.tprt.entities.flying_sword.FlyingSword;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class FlyingSwordRenderer extends GeoEntityRenderer<FlyingSword> {
    public FlyingSwordRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new FlyingSwordModel());
    }

    @Override
    public ResourceLocation getTextureLocation(FlyingSword flyingSword) {
        return new ResourceLocation(Tprt.MODID, "textures/entity/flying_sword.png");
    }

    @Override
    public void render(FlyingSword entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }
}
