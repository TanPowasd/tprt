package org.mylove.tprt.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.RenderTypeGroup;
import net.minecraftforge.client.model.CompositeModel;
import net.minecraftforge.client.model.geometry.*;
import net.minecraftforge.client.model.obj.ObjLoader;
import net.minecraftforge.client.model.obj.ObjModel;
import slimeknights.tconstruct.library.client.materials.MaterialRenderInfo.TintedSprite;
import slimeknights.tconstruct.library.client.model.tools.MaterialModel;
import slimeknights.tconstruct.library.materials.definition.IMaterial;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

import java.util.*;
import java.util.function.Function;

/** Forge OBJ geometry with the same material sprite/fallback semantics as native Tinkers tools. */
public final class LanceObjGeometry implements IUnbakedGeometry<LanceObjGeometry> {
    public static final IGeometryLoader<LanceObjGeometry> LOADER = (json, context) -> new LanceObjGeometry();
    private static final String[] PARTS = {"head_1", "head_2", "guard", "handle"};
    private static final RenderTypeGroup TYPES = new RenderTypeGroup(RenderType.cutout(), RenderType.entityCutoutNoCull(net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS));
    private final ObjModel[] parts = new ObjModel[4];

    private LanceObjGeometry() {
        for (int i = 0; i < parts.length; i++) {
            parts[i] = ObjLoader.INSTANCE.loadModel(new ObjModel.ModelSettings(
                    new ResourceLocation("tprt", "models/item/tool/lance/obj/" + PARTS[i] + ".obj"),
                    false, true, true, false, null));
        }
    }

    @Override
    public BakedModel bake(IGeometryBakingContext owner, ModelBaker baker,
                           Function<Material, TextureAtlasSprite> sprites, ModelState transform,
                           ItemOverrides vanilla, ResourceLocation location) {
        // Each reload creates a fresh handler/cache, so no atlas sprites survive a resource reload.
        MaterialOverrides overrides = new MaterialOverrides(owner, baker, transform, vanilla, location);
        return bakeMaterials(owner, baker, sprites, transform, location,
                Collections.nCopies(4, IMaterial.UNKNOWN_ID), overrides);
    }

    private BakedModel bakeMaterials(IGeometryBakingContext owner, ModelBaker baker,
                                     Function<Material, TextureAtlasSprite> sprites, ModelState transform,
                                     ResourceLocation location, List<MaterialVariantId> materials, ItemOverrides overrides) {
        var builder = CompositeModel.Baked.builder(owner, sprites.apply(owner.getMaterial("particle")), overrides, owner.getTransforms());
        for (int i = 0; i < parts.length; i++) {
            TintedSprite selected = MaterialModel.getMaterialSprite(sprites, owner.getMaterial(PARTS[i]), materials.get(i));
            // Forge owns OBJ normals, winding, V inversion and sprite-space UV baking.
            BakedModel part = parts[i].bake(owner, baker, ignored -> selected.sprite(), transform, ItemOverrides.EMPTY, location);
            List<BakedQuad> quads = new ArrayList<>();
            for (BakedQuad quad : part.getQuads(null, null, RandomSource.create(0))) {
                quads.add(withMaterial(quad, selected));
            }
            builder.addQuads(TYPES, quads);
        }
        return new CenteredModel(builder.build());
    }

    private static BakedQuad withMaterial(BakedQuad quad, TintedSprite material) {
        int[] data = quad.getVertices().clone();
        int stride = data.length / 4;
        int color = material.color(); // ARGB, already white for a precolored material texture.
        for (int vertex = 0; vertex < 4; vertex++) {
            int offset = vertex * stride;
            int abgr = data[offset + 3];
            int r = (abgr & 255) * ((color >>> 16) & 255) / 255;
            int g = ((abgr >>> 8) & 255) * ((color >>> 8) & 255) / 255;
            int b = ((abgr >>> 16) & 255) * (color & 255) / 255;
            int a = ((abgr >>> 24) & 255) * ((color >>> 24) & 255) / 255;
            data[offset + 3] = a << 24 | b << 16 | g << 8 | r;
            if (material.emissivity() > 0) {
                int light = data[offset + 6];
                data[offset + 6] = LightTexture.pack(Math.max(LightTexture.block(light), material.emissivity()),
                        Math.max(LightTexture.sky(light), material.emissivity()));
            }
        }
        // Color is baked once, never multiply native tool tint a second time.
        return new BakedQuad(data, -1, quad.getDirection(), material.sprite(), quad.isShade(), quad.hasAmbientOcclusion());
    }

    private final class MaterialOverrides extends ItemOverrides {
        private final Map<List<MaterialVariantId>, BakedModel> cache = new HashMap<>();
        private final IGeometryBakingContext owner;
        private final ModelBaker baker;
        private final ModelState transform;
        private final ItemOverrides vanilla;
        private final ResourceLocation location;

        private MaterialOverrides(IGeometryBakingContext owner, ModelBaker baker, ModelState transform,
                                  ItemOverrides vanilla, ResourceLocation location) {
            this.owner = owner; this.baker = baker; this.transform = transform;
            this.vanilla = vanilla; this.location = location;
        }

        @Override
        public BakedModel resolve(BakedModel original, ItemStack stack, ClientLevel level, LivingEntity entity, int seed) {
            // Resolve charge predicates with the real actor before choosing material geometry.
            BakedModel pose = vanilla.resolve(original, stack, level, entity, seed);
            if (pose != null && pose != original) {
                BakedModel resolved = pose.getOverrides().resolve(pose, stack, level, entity, seed);
                return resolved == null ? pose : resolved;
            }
            List<MaterialVariantId> key = new ArrayList<>(4);
            var materials = ToolStack.isInitialized(stack) ? ToolStack.from(stack).getMaterials() : null;
            for (int i = 0; i < 4; i++) {
                key.add(materials != null && i < materials.size() ? materials.get(i).getVariant() : IMaterial.UNKNOWN_ID);
            }
            return cache.computeIfAbsent(List.copyOf(key), ids -> bakeMaterials(owner, baker, Material::sprite,
                    transform, location, ids, ItemOverrides.EMPTY));
        }
    }

    /** Matches the old renderer's centered display contexts, without BEWLR/Gecko offsets. */
    private static final class CenteredModel extends net.minecraftforge.client.model.BakedModelWrapper<BakedModel> {
        private CenteredModel(BakedModel model) { super(model); }
        @Override
        public BakedModel applyTransform(ItemDisplayContext context, PoseStack pose, boolean leftHand) {
            originalModel.applyTransform(context, pose, leftHand);
            if (context == ItemDisplayContext.FIXED || context == ItemDisplayContext.GROUND || context == ItemDisplayContext.HEAD) {
                pose.translate(0, -12.64 / 16.0, 0);
            }
            return this;
        }
    }
}
