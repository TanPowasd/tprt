package org.mylove.tprt.Modifiers;

import com.ssakura49.sakuratinker.generic.BaseModifier;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.mylove.tprt.registries.ModifierIds;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.modules.build.ModifierRequirementsModule;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.nbt.IToolContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.stat.ModifierStatsBuilder;
import slimeknights.tconstruct.library.tools.stat.ToolStats;

public class ancient_sandstorm extends BaseModifier {

    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        hookBuilder.addHook(this, ModifierHooks.GENERAL_INTERACT);
        hookBuilder.addModule(ModifierRequirementsModule.builder().requireModifier(ModifierIds.STRONG_BUT_PLIABLE, 1).modifierKey(ModifierIds.ANCIENT_SANDSTORM).build());
    }

    @Override
    public Component validate(IToolStackView Tool, @NotNull ModifierEntry entry){
            if (
                    entry.getLevel() == 1 &&
                    (Tool.getModifierLevel(ModifierIds.STRONG_BUT_PLIABLE) > 1)
            ){
                return Component.translatable("recipe.tprt.modifier.ancient_sandstorm_requirement");
            }
            return null;
    }

    public void addToolStats(IToolContext context, @NotNull ModifierEntry modifier, @NotNull ModifierStatsBuilder builder) {
        int x = modifier.getLevel();
        double X= Math.sqrt(x);
        float y= builder.getStat(ToolStats.DURABILITY);
        double LogY=Math.log10(y);
        double z=LogY*y/(6*y+30000);
        float Att=builder.getStat(ToolStats.ATTACK_DAMAGE);
        float Pro=builder.getStat(ToolStats.PROJECTILE_DAMAGE);
        float Arm=builder.getStat(ToolStats.ARMOR);
        ToolStats.ATTACK_DAMAGE.add(builder, 0.75*Att * z * X);
        ToolStats.PROJECTILE_DAMAGE.add(builder, 0.75 * Pro * z * X);
        ToolStats.ARMOR.add(builder, 0.75 * Arm * z * X);
    }
}

