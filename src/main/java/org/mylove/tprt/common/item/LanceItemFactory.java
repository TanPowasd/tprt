package org.mylove.tprt.common.item;

import net.minecraft.world.item.Item;
import net.minecraftforge.fml.ModList;
import slimeknights.tconstruct.library.tools.definition.ToolDefinition;

/** Keep optional GeckoLib bytecode out of the always-loaded item registration class. */
public final class LanceItemFactory {
    private LanceItemFactory() {}

    public static LanceItem create(Item.Properties properties, ToolDefinition definition) {
        if (ModList.get().isLoaded("geckolib")) {
            return org.mylove.tprt.compat.geckolib.GeoLanceItemFactory.create(properties, definition);
        }
        return new LanceItem(properties, definition);
    }
}
