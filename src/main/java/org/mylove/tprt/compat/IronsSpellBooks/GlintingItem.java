package org.mylove.tprt.compat.IronsSpellBooks;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** 带附魔光效的物品 (源质合金锭) */
public class GlintingItem extends Item {
    public GlintingItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}