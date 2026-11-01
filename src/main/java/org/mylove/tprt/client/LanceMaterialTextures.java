package org.mylove.tprt.client;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import slimeknights.tconstruct.library.client.materials.MaterialRenderInfo;
import slimeknights.tconstruct.library.client.materials.MaterialRenderInfoLoader;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

/** Direct-PNG counterpart to the Tinkers part texture naming convention. */
public final class LanceMaterialTextures {
    private LanceMaterialTextures() {}
    public record Selection(ResourceLocation texture, int tint) {}

    public static Selection select(ItemStack stack, String part, int index) {
        String path = "textures/item/tool/lance/large/" + part + "/" + part;
        ResourceLocation base = new ResourceLocation("tprt", path + ".png");
        if (stack == null || !ToolStack.isInitialized(stack)) return new Selection(base, 0xFFFFFF);
        var materials = ToolStack.from(stack).getMaterials();
        if (index >= materials.size()) return new Selection(base, 0xFFFFFF);
        var variant = materials.get(index).getVariant();
        var info = MaterialRenderInfoLoader.INSTANCE.getRenderInfo(variant);
        // Generated textures are already colored. Only the uncolored base is tinted.
        if (info.isPresent()) {
            var render = info.get();
            if (render.texture() != null) {
                ResourceLocation candidate = new ResourceLocation("tprt", path + "_"
                        + MaterialRenderInfo.getSuffix(render.texture()) + ".png");
                if (Minecraft.getInstance().getResourceManager().getResource(candidate).isPresent())
                    return new Selection(candidate, 0xFFFFFF);
            }
            for (String suffix : render.fallbacks()) {
                ResourceLocation candidate = new ResourceLocation("tprt", path + "_" + suffix + ".png");
                if (Minecraft.getInstance().getResourceManager().getResource(candidate).isPresent())
                    return new Selection(candidate, render.vertexColor() & 0xFFFFFF);
            }
            return new Selection(base, render.vertexColor() & 0xFFFFFF);
        }
        return new Selection(base, 0xFFFFFF);
    }
}
