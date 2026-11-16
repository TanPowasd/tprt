package org.mylove.tprt.compat.Cataclysm.jei;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;
import slimeknights.tconstruct.common.TinkerTags;

/** Information pages for the NBT-preserving dynamic recipe, not fixed input/output recipes. */
@JeiPlugin
public final class AmethystBlessingJeiPlugin implements IModPlugin {
    @Override
    public ResourceLocation getPluginUid() {
        return new ResourceLocation("tprt", "amethyst_tool_blessing");
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        if (!ModList.get().isLoaded("cataclysm")) {
            return;
        }

        // Reuse JEI's materialized tool examples instead of constructing invalid blank tools.
        List<ItemStack> examples = new ArrayList<>();
        Set<Item> seen = new HashSet<>();
        for (ItemStack stack : registration.getIngredientManager().getAllIngredients(VanillaTypes.ITEM_STACK)) {
            if (isEligibleTool(stack) && seen.add(stack.getItem())) {
                examples.add(stack.copy());
            }
        }
        Item altar = ForgeRegistries.ITEMS.getValue(new ResourceLocation("cataclysm", "altar_of_amethyst"));
        if (altar != null && altar != net.minecraft.world.item.Items.AIR) {
            examples.add(new ItemStack(altar));
        }
        if (!examples.isEmpty()) {
            registration.addItemStackInfo(examples,
                    Component.translatable("jei.tprt.amethyst_tool_blessing.info"));
        }
    }

    private static boolean isEligibleTool(ItemStack stack) {
        return !stack.isEmpty()
                && !stack.is(TinkerTags.Items.AMMO)
                && !stack.is(TinkerTags.Items.BOOKS)
                && !stack.is(TinkerTags.Items.GUIDEBOOKS)
                && !stack.is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM,
                        new ResourceLocation("curios", "tinker_charm")))
                && (stack.is(TinkerTags.Items.ARMOR) || stack.is(TinkerTags.Items.SHIELDS)
                    || stack.is(TinkerTags.Items.MELEE) || stack.is(TinkerTags.Items.HARVEST)
                    || stack.is(TinkerTags.Items.BOWS) || stack.is(TinkerTags.Items.RANGED));
    }
}
