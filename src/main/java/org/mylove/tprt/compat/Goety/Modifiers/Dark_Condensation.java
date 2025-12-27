package org.mylove.tprt.compat.Goety.Modifiers;

import com.ssakura49.sakuratinker.library.tinkering.tools.STToolStats;
import org.jetbrains.annotations.NotNull;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.build.ToolStatsModifierHook;
import slimeknights.tconstruct.library.modifiers.impl.NoLevelsModifier;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.nbt.IToolContext;
import slimeknights.tconstruct.library.tools.stat.ModifierStatsBuilder;
import slimeknights.tconstruct.library.tools.stat.ToolStats;

public class Dark_Condensation extends NoLevelsModifier implements ToolStatsModifierHook {

    protected void registerHooks(ModuleHookMap.@NotNull Builder hookBuilder) {
        super.registerHooks(hookBuilder);
        hookBuilder.addHook(this, ModifierHooks.TOOL_STATS);
    }

    public void addToolStats(@NotNull IToolContext context, @NotNull ModifierEntry modifier, @NotNull ModifierStatsBuilder builder) {
        float x = builder.getStat(ToolStats.ATTACK_DAMAGE);
        double xx = x - 5;
        if (xx > 0) {
            float X = (float) Math.log10(x+5);
            STToolStats.SOUL_INCREASE.add(builder, 0.2 * X);
            STToolStats.SOUL_INCREASE.multiply(builder, X * X);
            ToolStats.ATTACK_SPEED.multiply(builder, -X);
        }
    }
}
