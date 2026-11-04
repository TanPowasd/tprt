package org.mylove.tprt.client;

import com.github.L_Ender.cataclysm.client.render.CMRenderTypes;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.mylove.tprt.compat.Cataclysm.entity.TprtCursedSandstormArrow;

/** Client-only native sandstorm geometry, independent of Citadel/Lionfish model API changes. */
public final class CursedSandstormArrowRenderer extends EntityRenderer<TprtCursedSandstormArrow> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation("cataclysm", "textures/entity/cursed_sandstorm.png");
    private final ModelPart root;
    private final ModelPart[] storms;
    private final ModelPart[] shells;
    private static final float END = 0.3333F;
    private static final float[] SPAWN_PEAK = {0.125F, 0.1667F, 0.2083F, 0.25F};
    private static final float[] DESPAWN_PEAK = {0.2083F, 0.1667F, 0.125F, 0.0833F};

    public CursedSandstormArrowRenderer(EntityRendererProvider.Context context) {
        super(context);
        // Cursed_Sandstorm_Model constructor bytecode: identical UVs, boxes and nested pivots.
        MeshDefinition mesh = new MeshDefinition();
        var rootDefinition = mesh.getRoot().addOrReplaceChild("root", CubeListBuilder.create(),
                PartPose.offset(0.0F, 24.0F, 0.0F));
        var storm = rootDefinition.addOrReplaceChild("storm", CubeListBuilder.create(), PartPose.offset(0, -4, 0));
        var storm2 = storm.addOrReplaceChild("storm2", CubeListBuilder.create(), PartPose.offset(0, -9, 0));
        var storm3 = storm2.addOrReplaceChild("storm3", CubeListBuilder.create(), PartPose.offset(0, -9, 0));
        var storm4 = storm3.addOrReplaceChild("storm4", CubeListBuilder.create(), PartPose.offset(0, -9, 0));
        // Lionfish scaleChildren defaults false: scale shells, never their nested child pivots.
        storm.addOrReplaceChild("shell", CubeListBuilder.create().texOffs(65, 72).addBox(-4, -4, -4, 8, 8, 8), PartPose.ZERO);
        storm2.addOrReplaceChild("shell", CubeListBuilder.create().texOffs(0, 72).addBox(-8, -4, -8, 16, 8, 16), PartPose.ZERO);
        storm3.addOrReplaceChild("shell", CubeListBuilder.create().texOffs(0, 39).addBox(-12, -4, -12, 24, 8, 24), PartPose.ZERO);
        storm4.addOrReplaceChild("shell", CubeListBuilder.create().texOffs(0, 0).addBox(-15, -4, -15, 30, 8, 30), PartPose.ZERO);
        root = LayerDefinition.create(mesh, 128, 128).bakeRoot().getChild("root");
        ModelPart first = root.getChild("storm");
        ModelPart second = first.getChild("storm2");
        ModelPart third = second.getChild("storm3");
        storms = new ModelPart[]{first, second, third, third.getChild("storm4")};
        shells = new ModelPart[]{storms[0].getChild("shell"), storms[1].getChild("shell"),
                storms[2].getChild("shell"), storms[3].getChild("shell")};
    }

    @Override
    public ResourceLocation getTextureLocation(TprtCursedSandstormArrow arrow) {
        return TEXTURE;
    }

    private void animate(float age) {
        root.getAllParts().forEach(ModelPart::resetPose);
        for (ModelPart shell : shells) shell.xScale = shell.yScale = shell.zScale = 1.0F;
        // Verified Lionfish 3.0 chainFlap: cos(age * speed + offset * PI / (2 * count) * i) * degree.
        float phaseOffset = (float) (-2.0D * Math.PI / (2 * storms.length));
        for (int i = 0; i < storms.length; i++) {
            storms[i].zRot = Mth.cos(age * 0.25F + phaseOffset * i) * 0.1F;
        }
        // Native setupAnim's nested local Y rotations, in radians (world rates 1, .5, .3, .6).
        storms[0].yRot = age;
        storms[1].yRot = -storms[0].yRot + age * 0.5F;
        storms[2].yRot = -storms[0].yRot - storms[1].yRot + age * 0.3F;
        storms[3].yRot = -storms[0].yRot - storms[1].yRot - storms[2].yRot + age * 0.6F;
    }

    /** Scalar equivalent of Lionfish AdvancedKeyframeAnimations' clamped Catmull-Rom sampler. */
    private static float sample(float seconds, float[] times, float... values) {
        int from = 0;
        while (from + 1 < times.length && seconds > times[from + 1]) from++;
        int to = Math.min(from + 1, times.length - 1);
        float t = from == to ? 0 : Mth.clamp((seconds - times[from]) / (times[to] - times[from]), 0, 1);
        float p0 = values[Math.max(0, from - 1)], p1 = values[from];
        float p2 = values[to], p3 = values[Math.min(values.length - 1, to + 1)];
        return 0.5F * (2 * p1 + (p2 - p0) * t
                + (2 * p0 - 5 * p1 + 4 * p2 - p3) * t * t
                + (-p0 + 3 * p1 - 3 * p2 + p3) * t * t * t);
    }

    private void keyframes(float ticks, boolean despawn) {
        float seconds = Math.max(0, ticks) / 20.0F;
        float spin = sample(seconds, new float[]{0, END}, despawn ? 360 : 0, despawn ? 0 : 360) * Mth.DEG_TO_RAD;
        for (int i = 0; i < storms.length; i++) {
            storms[i].yRot += spin;
            // posVec negates authored Y; all other authored position components are zero.
            storms[i].y -= sample(seconds, new float[]{0, despawn ? DESPAWN_PEAK[i] : SPAWN_PEAK[i], END}, 0, 6, 0);
            float scale;
            float yScale;
            if (!despawn) {
                if (i == 0) scale = sample(seconds, new float[]{0, 0.0833F}, 0, 1);
                else if (i == 1) scale = sample(seconds, new float[]{0, 0.0833F, 0.1667F}, 1, 0, 1);
                else if (i == 2) scale = sample(seconds, new float[]{0, 0.1667F, 0.25F}, 1, 0, 1);
                else scale = sample(seconds, new float[]{0, 0.25F, END}, 1, 0, 1);
            } else {
                if (i == 0) scale = sample(seconds, new float[]{0.25F, END}, 1, 0);
                else if (i == 1) scale = sample(seconds, new float[]{0.1667F, 0.25F, END}, 1, 0, 1);
                else if (i == 2) scale = sample(seconds, new float[]{0.0833F, 0.1667F, END}, 1, 0, 1);
                else scale = sample(seconds, new float[]{0, 0.0833F, END}, 1, 0, 1);
            }
            yScale = i == 2 ? 1 : scale;
            shells[i].xScale = shells[i].zScale = scale;
            shells[i].yScale = yScale;
        }
    }

    @Override
    public void render(TprtCursedSandstormArrow arrow, float yaw, float partialTick,
                       PoseStack pose, MultiBufferSource buffers, int light) {
        pose.pushPose();
        // Native renderer transform: the vortex stays upright, independent of arrow pitch.
        pose.scale(-0.5F, -0.5F, 0.5F);
        pose.translate(0.0F, -1.5F, 0.0F);
        float age = arrow.getVisualAge(partialTick);
        animate(age);
        float fadeAge = arrow.getFadeAge(partialTick);
        if (fadeAge >= 0) keyframes(fadeAge, true);
        else if (age <= END * 20) keyframes(age, false);
        root.render(pose, buffers.getBuffer(CMRenderTypes.getGhost(TEXTURE)),
                light, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        pose.popPose();
        super.render(arrow, yaw, partialTick, pose, buffers, light);
    }
}
