package org.mylove.tprt.common.item;

import slimeknights.tconstruct.library.tools.definition.ToolDefinition;
import slimeknights.tconstruct.library.tools.item.ModifiableItem;

/** Tinkers tool shell. Charging is supplied by its intrinsic lance_charge modifier. */
public class LanceItem extends ModifiableItem {
    public LanceItem(Properties properties, ToolDefinition definition) {
        super(properties, definition);
    }

    @Override
    public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
        consumer.accept(org.mylove.tprt.client.LanceClientExtensions.INSTANCE);
    }
}
