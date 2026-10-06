package org.mylove.tprt.registries.item;

import slimeknights.tconstruct.library.tools.definition.ToolDefinition;

public class TPRTToolDefinitions {
    public static final ToolDefinition ANCHOR_SWORD;
    //注法者的工具定义已移到 IssCompat.MAGIC_BLADE (只在装了铁魔法时创建)

    static {
        ANCHOR_SWORD = ToolDefinition.create(ItemsRegistry.anchor_sword);
    }
}

