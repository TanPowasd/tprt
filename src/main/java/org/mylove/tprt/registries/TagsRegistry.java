package org.mylove.tprt.registries;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public class TagsRegistry {
    public static class ItemsTag {
        public static final TagKey<Item> anchor_sword = local("modifiable/melee/anchor_sword");
        public static final TagKey<Item> magic_blade = local("modifiable/melee/magic_blade");
        public static final TagKey<Item> sickle = local("modifiable/melee/sickle");

        private static TagKey<Item> local(String name) {
            return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("tprt", name));
        }

        private static TagKey<Item> common(String name) {
            return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("forge", name));
        }
    }
}

