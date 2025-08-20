package org.mylove.tprt.client.flying_sword;

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

//    @Override
//    @NotNull
//    public ResourceLocation getTextureLocation(FlyingSword flyingSword) {
//        return new ResourceLocation(Tprt.MODID, "textures/entity/flying_sword.png");
//    }

//    override

}
