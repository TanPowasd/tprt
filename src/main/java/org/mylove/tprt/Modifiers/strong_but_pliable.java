package org.mylove.tprt.Modifiers;

import com.ssakura49.sakuratinker.generic.BaseModifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.nbt.IToolContext;
import slimeknights.tconstruct.library.tools.stat.ModifierStatsBuilder;
import slimeknights.tconstruct.library.tools.stat.ToolStats;

public class strong_but_pliable extends  BaseModifier {
    public void addToolStats(IToolContext context, ModifierEntry modifier, ModifierStatsBuilder builder) {
        int x = modifier.getLevel();
        double X= Math.sqrt(x);
        float y= builder.getStat(ToolStats.DURABILITY);
        double LogY=Math.log10(y);
        double z=LogY*Math.pow(y,1.2)/(6*y+1000);
        ToolStats.ATTACK_DAMAGE.add(builder, z * X);
        ToolStats.PROJECTILE_DAMAGE.add(builder, 0.75*z * X);
        ToolStats.ARMOR.add(builder, 1.25*z * X);
    }
}
