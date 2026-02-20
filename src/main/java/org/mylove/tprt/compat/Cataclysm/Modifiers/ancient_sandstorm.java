package org.mylove.tprt.compat.Cataclysm.Modifiers;

import org.jetbrains.annotations.NotNull;
import org.mylove.tprt.registries.ModifierIds;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.build.ToolStatsModifierHook;
import slimeknights.tconstruct.library.modifiers.impl.NoLevelsModifier;
import slimeknights.tconstruct.library.modifiers.modules.build.ModifierRequirementsModule;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.nbt.IToolContext;
import slimeknights.tconstruct.library.tools.stat.ModifierStatsBuilder;
import slimeknights.tconstruct.library.tools.stat.ToolStats;

public class ancient_sandstorm extends NoLevelsModifier implements ToolStatsModifierHook {

    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        super.registerHooks(hookBuilder);
        hookBuilder.addHook(this, ModifierHooks.TOOL_STATS);

        hookBuilder.addModule(ModifierRequirementsModule.builder().requireModifier(ModifierIds.STRONG_BUT_PLIABLE, 1).modifierKey(ModifierIds.ANCIENT_SANDSTORM).build());
    }

    @Override
    public void addToolStats(IToolContext context, @NotNull ModifierEntry modifier, @NotNull ModifierStatsBuilder builder) {
        float x= builder.getStat(ToolStats.DURABILITY);
        double X=Math.log10(x);
        double y=X * x/(6 * x +30000);

        ToolStats.ATTACK_DAMAGE.multiply(builder, 1 + 0.8 * y );
        ToolStats.PROJECTILE_DAMAGE.multiply(builder, 1 + 1.0 * y );
        ToolStats.ARMOR.multiply(builder, 1 + 1.5 * y );
        ToolStats.DURABILITY.add(builder,1500);
    }
}

