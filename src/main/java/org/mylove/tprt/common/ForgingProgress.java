package org.mylove.tprt.common;

import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Shared progress metadata; never changes an item's custom name. */
public final class ForgingProgress {
    public static final String KEY = "tprt:forging";
    public static final int TOTAL = 8;
    public static final TagKey<Item> STEEL = ItemTags.create(new ResourceLocation("forge", "ingots/steel"));

    private ForgingProgress() {}

    public static boolean hasProgress(ItemStack stack) {
        return stack.is(STEEL) && stack.hasTag() && stack.getTag().contains(KEY, Tag.TAG_INT);
    }

    public static int getProgress(ItemStack stack) {
        return stack.hasTag() ? Math.max(0, Math.min(TOTAL, stack.getTag().getInt(KEY))) : 0;
    }
}
