package org.mylove.tprt.compat.IronsSpellBooks.Modifiers;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import org.jetbrains.annotations.NotNull;
import org.mylove.tprt.registries.TagsRegistry;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.build.ToolStatsModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeHitModifierHook;
import slimeknights.tconstruct.library.modifiers.impl.NoLevelsModifier;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.nbt.IToolContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.stat.ModifierStatsBuilder;
import slimeknights.tconstruct.library.tools.stat.ToolStats;

public class enchanting extends NoLevelsModifier implements MeleeHitModifierHook, ToolStatsModifierHook {
    @Override
    protected void registerHooks(ModuleHookMap.@NotNull Builder hookBuilder) {
        super.registerHooks(hookBuilder);
        hookBuilder.addHook(this, ModifierHooks.MELEE_HIT, ModifierHooks.TOOL_STATS);
    }

    @Override
    public void addToolStats(IToolContext context, ModifierEntry modifier, ModifierStatsBuilder builder) {
        if (context.hasTag(TagsRegistry.ItemsTag.magic_blade)) {
            int level = modifier.getLevel();
            ToolStats.ATTACK_DAMAGE.multiply(builder, 1.0 + 0.25 * level);
        }
    }

    @Override
    public void afterMeleeHit(IToolStackView tool, @NotNull ModifierEntry modifier, @NotNull ToolAttackContext context, float damageDealt) {
        LivingEntity attacker = context.getPlayerAttacker();
        LivingEntity target = context.getLivingTarget();
        AttributeInstance X = null;
        if (attacker != null) {
            X = attacker.getAttribute(AttributeRegistry.SPELL_POWER.get());
        }
        if (attacker != null && target != null) {
            if (X != null) {
                X.getValue();
            }
            if (context.getLivingTarget() != null) {
                context.getLivingTarget().invulnerableTime = 0;
                if (X != null) {
                    double y = Math.sqrt(X.getValue());
                    context.getLivingTarget().hurt(context.getLivingTarget().damageSources().magic(), (float) ((5+damageDealt) * 0.20f * y));
                }
                context.getLivingTarget().setLastHurtByMob(context.getAttacker());
            }
        }

    }
}

