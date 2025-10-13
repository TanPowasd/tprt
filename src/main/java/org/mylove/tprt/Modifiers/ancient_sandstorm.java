package org.mylove.tprt.Modifiers;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;
import org.mylove.tprt.registries.ModifierIds;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.build.ToolStatsModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.GeneralInteractionModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.InteractionSource;
import slimeknights.tconstruct.library.modifiers.impl.NoLevelsModifier;
import slimeknights.tconstruct.library.modifiers.modules.build.ModifierRequirementsModule;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.nbt.IToolContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.stat.ModifierStatsBuilder;
import slimeknights.tconstruct.library.tools.stat.ToolStats;

public class ancient_sandstorm extends NoLevelsModifier implements ToolStatsModifierHook, GeneralInteractionModifierHook {

    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        super.registerHooks(hookBuilder);
        hookBuilder.addHook(this, ModifierHooks.TOOL_STATS);

        hookBuilder.addHook(this, ModifierHooks.GENERAL_INTERACT);
        hookBuilder.addModule(ModifierRequirementsModule.builder().requireModifier(ModifierIds.STRONG_BUT_PLIABLE, 1).modifierKey(ModifierIds.ANCIENT_SANDSTORM).build());
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
        ToolStats.PROJECTILE_DAMAGE.add(builder, 1.0 * Pro * z * X);
        ToolStats.ARMOR.add(builder, 1.25 * Arm * z * X);
        ToolStats.DURABILITY.add(builder,1000);
    }

    @Override
    public InteractionResult onToolUse(IToolStackView tool, ModifierEntry modifier, Player player, InteractionHand hand, InteractionSource source) {
        return null;
    }
}

