package org.mylove.tprt.compat.IronsSpellBooks.thunder_att;
/*
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
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class Render extends EntityRenderer<thunder_att_MobCreate> {

    protected Render(EntityRendererProvider.Context pContext) {
        super(pContext);
    }
    @Override
    public ResourceLocation getTextureLocation(thunder_att_MobCreate pEntity) {
        return null;
    }

    private static final ResourceLocation TEXTURE = new ResourceLocation("textures/entity/thunder_MobCreate/needl_5.png");
    @Override
    public void render(thunder_att_MobCreate pEntity, float pEntityYaw, float pPartialTicks, PoseStack pPoseStack, MultiBufferSource pBuffer, int pPackedLight){
        pPoseStack.pushPose();//传入的矩阵
        PoseStack.Pose pose=pPoseStack.last();
        Vec3 motion=pEntity.getDeltaMovement();
        float xRot=-((float) (Mth.atan2(motion.horizontalDistance(),motion.y)*(double)(180f/(float) Math.PI))-90.0f);
        float yRot=-((float) (Mth.atan2(motion.z,motion.x)*(double) (180f/(float) Math.PI))+90f);
        pPoseStack.mulPose(Axis.YP.rotationDegrees(yRot));
        pPoseStack.mulPose(Axis.XP.rotationDegrees(xRot));
        pPoseStack.mulPose(Axis.ZP.rotationDegrees(pEntity.getZRot()+(pEntity.tickCount+pPartialTicks)*40f));

        float width=2.5f;
        float scale=pEntity.getScale();
        pPoseStack.mulPose(Axis.XP.rotationDegrees(45));
        pPoseStack.scale(scale,scale,scale);
        drawSlash(pose,pEntity,pBuffer,pPackedLight,width);
        pPoseStack.popPose();
        super.render(pEntity, pEntityYaw, pPartialTicks, pPoseStack, pBuffer, pPackedLight);

    }
    private void drawSlash(PoseStack.Pose pose,thunder_att_MobCreate entity,MultiBufferSource bufferSource,int light,float width){
        Matrix4f poseMatrix=pose.pose();
        Matrix3f normalMatrix=pose.normal();
        VertexConsumer consumer=bufferSource.getBuffer(RenderType.entityCutoutNoCull(getTextureLocation(entity)));
        float halfWidth=width*0.5f;
        consumer.vertex(poseMatrix,0,-halfWidth,-halfWidth).color(51,255,255,255).uv(0f,1f).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(pose.normal(),0f,1f,0f).endVertex();
        consumer.vertex(poseMatrix,0,halfWidth,-halfWidth).color(51,255,255,255).uv(1f,1f).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(pose.normal(),0f,1f,0f).endVertex();
        consumer.vertex(poseMatrix,0,halfWidth,halfWidth).color(51,255,255,255).uv(1f,0f).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(pose.normal(),0f,1f,0f).endVertex();
        consumer.vertex(poseMatrix,0,-halfWidth,halfWidth).color(51,255,255,255).uv(0f,0f).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(pose.normal(),0f,1f,0f).endVertex();
    }


}*/
