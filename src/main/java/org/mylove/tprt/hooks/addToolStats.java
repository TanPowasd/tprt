package org.mylove.tprt.hooks;

import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.nbt.IToolContext;
import slimeknights.tconstruct.library.tools.stat.ModifierStatsBuilder;

public interface addToolStats {
    void addToolStats(IToolContext context, ModifierEntry modifier, ModifierStatsBuilder builder);
}
