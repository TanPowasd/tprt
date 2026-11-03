package org.mylove.tprt.common.item;

import net.minecraft.world.item.Item;
import slimeknights.tconstruct.library.tools.definition.ToolDefinition;

/** Lance gameplay is independent of its native material-aware OBJ display model. */
public final class LanceItemFactory {
    private LanceItemFactory() {}

    public static LanceItem create(Item.Properties properties, ToolDefinition definition) {
        return new LanceItem(properties, definition);
    }
}
