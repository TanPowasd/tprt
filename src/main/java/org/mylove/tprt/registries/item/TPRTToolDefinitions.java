package org.mylove.tprt.registries.item;

import slimeknights.tconstruct.library.tools.definition.ToolDefinition;

public class TPRTToolDefinitions {
    public static final ToolDefinition ANCHOR_SWORD;
    public static final ToolDefinition MAGIC_BLADE;

    static {
        ANCHOR_SWORD = ToolDefinition.create(ItemsRegistry.anchor_sword);
        MAGIC_BLADE = ToolDefinition.create(ItemsRegistry.magic_blade);
    }
}

