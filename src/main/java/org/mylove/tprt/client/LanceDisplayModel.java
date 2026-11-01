package org.mylove.tprt.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.state.BlockState;
import java.util.List;

/** Keeps the resolved native tool visible to tint and lighting code before transforms. */
public final class LanceDisplayModel implements BakedModel {
    private final BakedModel nativeModel;
    private final BakedModel displayModel;

    public LanceDisplayModel(BakedModel nativeModel, BakedModel displayModel) {
        this.nativeModel = nativeModel;
        this.displayModel = displayModel;
    }

    @Override public BakedModel applyTransform(ItemDisplayContext context, PoseStack pose, boolean leftHand) {
        return (context == ItemDisplayContext.GUI ? nativeModel : displayModel).applyTransform(context, pose, leftHand);
    }
    @Override public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource random) {
        return nativeModel.getQuads(state, side, random);
    }
    @Override public boolean useAmbientOcclusion() { return nativeModel.useAmbientOcclusion(); }
    @Override public boolean isGui3d() { return nativeModel.isGui3d(); }
    @Override public boolean usesBlockLight() { return nativeModel.usesBlockLight(); }
    @Override public boolean isCustomRenderer() { return nativeModel.isCustomRenderer(); }
    @Override public TextureAtlasSprite getParticleIcon() { return nativeModel.getParticleIcon(); }
    @Override public ItemOverrides getOverrides() { return nativeModel.getOverrides(); }
}
