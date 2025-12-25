package org.mylove.tprt.registries.item;

import io.netty.util.Attribute;
import org.mylove.tprt.compat.IronsSpellBooks.IssCompat;
import slimeknights.tconstruct.library.tools.definition.ToolDefinition;

public class TPRTToolDefinitions {
    public static final ToolDefinition ANCHOR_SWORD;
    public static final ToolDefinition MAGIC_BLADE;

    static {
        ANCHOR_SWORD = ToolDefinition.create(ModItems.anchor_sword);
        MAGIC_BLADE = ToolDefinition.create(ModItems.magic_blade);
    }
}

