package org.mylove.tprt.client.flying_sword;

import net.minecraft.resources.ResourceLocation;
import org.mylove.tprt.Tprt;
import org.mylove.tprt.entities.flying_sword.FlyingSword;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;

public class FlyingSwordModel extends GeoModel<FlyingSword> {
    private final ResourceLocation model = new ResourceLocation(Tprt.MODID, "geo/flying_sword.geo.json");
    private final ResourceLocation texture = new ResourceLocation(Tprt.MODID, "textures/entity/flying_sword.png");
    private final ResourceLocation animations = new ResourceLocation(Tprt.MODID, "animations/flying_sword.animation.json");

    @Override
    public ResourceLocation getModelResource(FlyingSword flyingSword) {
        return this.model;
    }

    @Override
    public ResourceLocation getTextureResource(FlyingSword flyingSword) {
        return this.texture;
    }

    @Override
    public ResourceLocation getAnimationResource(FlyingSword flyingSword) {
        return this.animations;
    }

    @Override
    public void setCustomAnimations(FlyingSword animatable, long instanceId, AnimationState<FlyingSword> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);
    }
}
