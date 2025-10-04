package org.mylove.tprt.tags;

import org.mylove.tprt.hooks.TprtHooks;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.build.ToolStatsModifierHook;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.nbt.IToolContext;
import slimeknights.tconstruct.library.tools.stat.ModifierStatsBuilder;
import slimeknights.tconstruct.library.tools.stat.ToolStats;

public class _TestModifier extends Modifier implements ToolStatsModifierHook {
    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        hookBuilder.addHook(this, ModifierHooks.TOOL_STATS);
    }

    @Override
    public void addToolStats(IToolContext context, ModifierEntry modifier, ModifierStatsBuilder builder) {
        /// {@link com.ssakura49.sakuratinker.common.tinkering.modifiers.armor.DragonSoulModifier}
        int level = modifier.getLevel();
        ToolStats.DURABILITY.add(builder, 500f * level);
        ToolStats.ARMOR.add(builder, (double) 3.0f * level);
        ToolStats.ARMOR_TOUGHNESS.add(builder, (double) 2.0f * level);
    }
}
