package org.mylove.tprt.Modifiers;

import io.redspace.ironsspellbooks.damage.SpellDamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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

import java.util.UUID;

public class fire_addition extends NoLevelsModifier implements MeleeHitModifierHook, ToolStatsModifierHook {
    private static final Logger log = LogManager.getLogger(fire_addition.class);
    UUID uuid=UUID.fromString("d2ab9741-d1ad-4e3e-afa2-f37f1aac9cf2");
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
    @Override
    public void afterMeleeHit(IToolStackView tool, @NotNull ModifierEntry modifier, @NotNull ToolAttackContext context, float damageDealt) {
        DeBug.Logger.log("fire_addition afterMeleeHit------0 TRUE");

        if (tool.hasTag(TagsRegistry.ItemsTag.magic_blade) && !context.isExtraAttack()) {
            DeBug.Logger.log("fire_addition afterMeleeHit------1 TRUE");

            LivingEntity attacker = context.getAttacker();
            LivingEntity target = context.getLivingTarget();

            if (target != null && !target.level().isClientSide) {
                DeBug.Logger.log("fire_addition afterMeleeHit------2 TRUE");

                int level = modifier.getLevel();
                target.invulnerableTime = 0;
                // 使用更安全的方式处理额外伤害
                applyFireDamage(attacker, target, level);
            }
        }
    }

    private void applyFireDamage(LivingEntity attacker, LivingEntity target, int level) {
        // 检查目标是否已经死亡或处于无效状态
        if (!target.isAlive() || target.isRemoved()) {
            return;
        }

        try {
            // 应用火焰伤害
            float spellPower = SpellsRegistry.fire_addition.get().getSpellPower(level, attacker);
            SpellDamageSource damageSource = SpellsRegistry.fire_addition.get().getDamageSource(attacker);

            // 使用更安全的方式应用伤害
            if (target.isAlive()) {
                target.hurt(damageSource, spellPower);
                target.setRemainingFireTicks(100 + level * 60);
            }
        } catch (Exception e) {
            log.error("Error applying fire damage in fire_addition modifier", e);
        }
    }
}

