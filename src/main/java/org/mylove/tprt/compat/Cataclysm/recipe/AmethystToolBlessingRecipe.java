package org.mylove.tprt.compat.Cataclysm.recipe;

import com.github.L_Ender.cataclysm.crafting.AltarOfAmethystRecipe;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import org.mylove.tprt.compat.Cataclysm.CataclysmCompat;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

/** Dynamic, NBT-preserving blessing in Cataclysm's existing amethyst recipe type. */
public final class AmethystToolBlessingRecipe extends AltarOfAmethystRecipe {
    public static final int BLESSING_TIME = 120;
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Set<String> REPORTED_REJECTIONS = ConcurrentHashMap.newKeySet();

    private static ItemStack reject(ItemStack input, String reason) {
        // One message per item/reason per launch, never one message per altar tick.
        String key = BuiltInRegistries.ITEM.getKey(input.getItem()) + ": " + reason;
        if (REPORTED_REJECTIONS.add(key)) {
            LOGGER.info("Amethyst tool blessing rejected {}", key);
        }
        return ItemStack.EMPTY;
    }
    private static final TagKey<Item> TINKER_CHARM = TagKey.create(Registries.ITEM,
            new ResourceLocation("curios", "tinker_charm"));

    public AmethystToolBlessingRecipe(ResourceLocation id) {
        // Static output is intentionally absent: this recipe always needs the actual input NBT.
        super(id, Ingredient.of(TinkerTags.Items.MODIFIABLE), ItemStack.EMPTY, BLESSING_TIME);
    }

    /** Shared by recipe matching and the informational JEI page. */
    public static boolean isEligibleItem(ItemStack stack) {
        return !stack.isEmpty()
                && !stack.is(TinkerTags.Items.AMMO)
                && !stack.is(TinkerTags.Items.BOOKS)
                && !stack.is(TinkerTags.Items.GUIDEBOOKS)
                && !stack.is(TINKER_CHARM)
                && (stack.is(TinkerTags.Items.ARMOR) || stack.is(TinkerTags.Items.SHIELDS)
                || stack.is(TinkerTags.Items.MELEE) || stack.is(TinkerTags.Items.HARVEST)
                || stack.is(TinkerTags.Items.BOWS) || stack.is(TinkerTags.Items.RANGED));
    }

    /** All validation and modifier hooks run against a private NBT copy. */
    public ItemStack bless(ItemStack input) {
        if (!isEligibleItem(input)) {
            return reject(input, "item is outside the eligible tool/armor tags");
        }
        if (input.getCount() != 1) {
            return reject(input, "stack count is not one");
        }
        if (!ToolStack.isInitialized(input)) {
            return reject(input, "missing initialized tic_stats compound");
        }
        ToolStack tool = ToolStack.copyFrom(input);
        if (tool.getModifierLevel(CataclysmCompat.AMETHYST_PROTECTION.getId()) > 0) {
            return reject(input, "already blessed");
        }
        Component validation = tool.tryValidate();
        if (validation != null) {
            return reject(input, "input validation: " + validation.getString());
        }
        // Direct modifier addition deliberately consumes no upgrade or ability slots.
        tool.addModifier(CataclysmCompat.AMETHYST_PROTECTION.getId(), 1);
        validation = tool.tryValidate();
        if (validation != null) {
            return reject(input, "blessed tool validation: " + validation.getString());
        }
        return tool.copyStack(input, 1);
    }

    @Override
    public boolean matches(Container container, Level level) {
        return container.getContainerSize() > 0 && !bless(container.getItem(0)).isEmpty();
    }

    @Override
    public ItemStack assemble(Container container, RegistryAccess registries) {
        return container.getContainerSize() == 0 ? ItemStack.EMPTY : bless(container.getItem(0));
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return CataclysmCompat.AMETHYST_TOOL_BLESSING.get();
    }

    public static final class Serializer implements RecipeSerializer<AmethystToolBlessingRecipe> {
        @Override
        public AmethystToolBlessingRecipe fromJson(ResourceLocation id, JsonObject json) {
            if (json.has("time") && json.get("time").getAsInt() != BLESSING_TIME) {
                throw new JsonSyntaxException("Amethyst tool blessing time must be 120 ticks");
            }
            return new AmethystToolBlessingRecipe(id);
        }

        @Override
        public AmethystToolBlessingRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            return new AmethystToolBlessingRecipe(id);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, AmethystToolBlessingRecipe recipe) {
            // Eligibility, modifier, and duration are fixed by the serializer.
        }
    }
}
