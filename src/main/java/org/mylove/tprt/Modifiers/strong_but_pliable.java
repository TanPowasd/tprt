package org.mylove.tprt.Modifiers;

import org.jetbrains.annotations.NotNull;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.build.ToolStatsModifierHook;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.nbt.IToolContext;
import slimeknights.tconstruct.library.tools.stat.ModifierStatsBuilder;
import slimeknights.tconstruct.library.tools.stat.ToolStats;

public class strong_but_pliable extends Modifier implements ToolStatsModifierHook {

    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        super.registerHooks(hookBuilder);
        hookBuilder.addHook(this, ModifierHooks.TOOL_STATS);
    }
    public void addToolStats(@NotNull IToolContext context, ModifierEntry modifier, ModifierStatsBuilder builder) {
        int x = modifier.getLevel();
        double X= Math.sqrt(x);
        float y= builder.getStat(ToolStats.DURABILITY);
        double LogY=Math.log10(y);
        double z=LogY*Math.pow(y,1.2)/(6*y+1000);
        ToolStats.ATTACK_DAMAGE.add(builder, 0.6*z * X);
        ToolStats.PROJECTILE_DAMAGE.add(builder, 0.75*z * X);
        ToolStats.ARMOR.add(builder, 1.25*z * X);
        ToolStats.DURABILITY.add(builder,500);
    }
}
