package org.mylove.tprt.compat.geckolib;

import net.minecraft.world.item.Item;
import org.mylove.tprt.common.item.LanceItem;
import slimeknights.tconstruct.library.tools.definition.ToolDefinition;

/** Loaded only when the geckolib mod is present. */
public final class GeoLanceItemFactory {
    private GeoLanceItemFactory() {}

    public static LanceItem create(Item.Properties properties, ToolDefinition definition) {
        return new GeoLanceItem(properties, definition);
    }
}
