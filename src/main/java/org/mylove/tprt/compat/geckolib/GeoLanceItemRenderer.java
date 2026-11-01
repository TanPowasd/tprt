package org.mylove.tprt.compat.geckolib;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;
import slimeknights.tconstruct.library.client.materials.MaterialRenderInfoLoader;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import software.bernie.geckolib.core.object.Color;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

import java.util.List;

/** Four aligned Gecko models, each tinted from the corresponding Tinkers part. */
public final class GeoLanceItemRenderer extends BlockEntityWithoutLevelRenderer {
    public static final GeoLanceItemRenderer INSTANCE = new GeoLanceItemRenderer();

    private final List<PartRenderer> parts;

    private GeoLanceItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
        parts = List.of(new PartRenderer("tip", 0), new PartRenderer("shaft", 1),
                new PartRenderer("guard", 2), new PartRenderer("grip", 3));
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose,
                             MultiBufferSource buffer, int light, int overlay) {
        if (!(stack.getItem() instanceof GeoLanceItem)) return;
        // GUI rendering stays entirely in the native Tinkers model path.
        if (context == ItemDisplayContext.GUI) return;

        // GeoItemRenderer adds (0.5, 0.51, 0.5) to cancel ItemRenderer's
        // (-0.5, -0.5, -0.5). Remove its extra 0.01-block Y offset.
        // Geometry uses 1/16-block units: NEVER pass model units straight to translate.
        boolean centered = context == ItemDisplayContext.FIXED
                || context == ItemDisplayContext.GROUND || context == ItemDisplayContext.HEAD;
        pose.pushPose();
        pose.translate(0.0D, -0.01D - (centered ? 12.64D / 16.0D : 0.0D), 0.0D);
        for (PartRenderer renderer : parts) {
            pose.pushPose();
            renderer.renderByItem(stack, context, pose, buffer, light, overlay);
            pose.popPose();
        }
        pose.popPose();
    }

    private static final class PartRenderer extends GeoItemRenderer<GeoLanceItem> {
        private final int materialIndex;
        private final String texturePart;

        PartRenderer(String name, int index) {
            super(new PartModel(name));
            materialIndex = index;
            texturePart = new String[]{"head_1", "head_2", "guard", "handle"}[index];
        }

        @Override
        public Color getRenderColor(GeoLanceItem item, float partialTick, int packedLight) {
            return Color.ofOpaque(org.mylove.tprt.client.LanceMaterialTextures
                    .select(getCurrentItemStack(), texturePart, materialIndex).tint());
        }

        @Override
        public ResourceLocation getTextureLocation(GeoLanceItem item) {
            return org.mylove.tprt.client.LanceMaterialTextures
                    .select(getCurrentItemStack(), texturePart, materialIndex).texture();
        }
    }

    private static final class PartModel extends GeoModel<GeoLanceItem> {
        private final ResourceLocation model;
        private final ResourceLocation texture;
        private static final ResourceLocation ANIMATION = new ResourceLocation("tprt", "animations/item/lance.animation.json");

        PartModel(String part) {
            model = new ResourceLocation("tprt", "geo/item/lance_" + part + ".geo.json");
            String name = switch (part) { case "tip" -> "head_1"; case "shaft" -> "head_2"; case "grip" -> "handle"; default -> "guard"; };
            texture = new ResourceLocation("tprt", "textures/item/tool/lance/large/" + name + "/" + name + ".png");
        }

        @Override public ResourceLocation getModelResource(GeoLanceItem item) { return model; }
        @Override public ResourceLocation getTextureResource(GeoLanceItem item) { return texture; }
        @Override public ResourceLocation getAnimationResource(GeoLanceItem item) { return ANIMATION; }
    }
}
