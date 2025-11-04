package org.mylove.tprt.Modifiers;

import io.redspace.ironsspellbooks.damage.DamageSources;
import net.minecraft.world.entity.LivingEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Debug;
import org.jetbrains.annotations.NotNull;
import org.mylove.tprt.ModUsed.DeBug;
import org.mylove.tprt.registries.SpellsRegistry;
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

public class fire_addition extends NoLevelsModifier implements MeleeHitModifierHook, ToolStatsModifierHook {
    private static final Logger log = LogManager.getLogger(fire_addition.class);

    @Override
    protected void registerHooks(ModuleHookMap.@NotNull Builder hookBuilder) {
        super.registerHooks(hookBuilder);
        hookBuilder.addHook(this, ModifierHooks.MELEE_HIT, ModifierHooks.TOOL_STATS);
    }

    @Override
    public void addToolStats(IToolContext context, ModifierEntry modifier, ModifierStatsBuilder builder) {
        if(context.hasTag(TagsRegistry.ItemsTag.magic_blade))
        {
            int level = modifier.getLevel();
            ToolStats.ATTACK_DAMAGE.multiply(builder, 1.0 + 0.5 * level);
        }
    }

    public void afterMeleeHit(IToolStackView tool, @NotNull ModifierEntry modifier, @NotNull ToolAttackContext context, float damageDealt) {
        if (tool.hasTag(TagsRegistry.ItemsTag.magic_blade) && !context.isExtraAttack()){
            LivingEntity attacker = context.getAttacker();
            LivingEntity target = context.getLivingTarget();
            if (target != null)
            {
                int level = modifier.getLevel();
                target.invulnerableTime = 0; // 清空无敌帧后施加下一次伤害
                DamageSources.applyDamage(target, SpellsRegistry.fire_addition.get().getSpellPower(level, attacker), SpellsRegistry.fire_addition.get().getDamageSource(attacker));
                target.setRemainingFireTicks(100 + level * 60);
            }
        }
    }
}

