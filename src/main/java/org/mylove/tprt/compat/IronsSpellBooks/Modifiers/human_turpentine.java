package org.mylove.tprt.compat.IronsSpellBooks.Modifiers;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import org.jetbrains.annotations.NotNull;
import org.mylove.tprt.registries.TagsRegistry;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.build.ToolStatsModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeDamageModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeHitModifierHook;
import slimeknights.tconstruct.library.modifiers.impl.NoLevelsModifier;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.nbt.IToolContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.stat.ModifierStatsBuilder;
import slimeknights.tconstruct.library.tools.stat.ToolStats;

import static io.redspace.ironsspellbooks.damage.ISSDamageTypes.ENDER_MAGIC;

public class human_turpentine extends NoLevelsModifier implements MeleeHitModifierHook, MeleeDamageModifierHook, ToolStatsModifierHook {
    protected void registerHooks(ModuleHookMap.@NotNull Builder hookBuilder) {
        super.registerHooks(hookBuilder);
        hookBuilder.addHook(this, ModifierHooks.MELEE_DAMAGE);
        hookBuilder.addHook(this, ModifierHooks.MELEE_HIT);
        hookBuilder.addHook(this, ModifierHooks.TOOL_STATS);
    }

    @Override
    public void addToolStats(IToolContext context, ModifierEntry modifier, ModifierStatsBuilder builder) {
        if(context.hasTag(TagsRegistry.ItemsTag.sickle))
        {
            ToolStats.ATTACK_DAMAGE.multiply(builder, 2.50);
            ToolStats.ATTACK_SPEED.multiply(builder, 1.75);
        }
    }

    @Override
    public float getMeleeDamage(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float baseDamage, float damage) {
        return 1.25f*damage;
    }

    @Override
    public void afterMeleeHit(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float damageDealt){
        LivingEntity target= context.getLivingTarget();
        LivingEntity attacker=context.getPlayerAttacker();
        AttributeInstance X = null;
        AttributeInstance Y = null;
        if (attacker != null) {
            X = attacker.getAttribute(AttributeRegistry.SPELL_POWER.get());
            Y = attacker.getAttribute(AttributeRegistry.ENDER_SPELL_POWER.get());
        }
        if (attacker != null && target != null) {
            if (X != null && Y != null) {
                X.getValue();
                Y.getValue();
            }
            if (context.getLivingTarget() != null) {
                context.getLivingTarget().invulnerableTime = 0;
                if (X != null && Y != null) {
                    double x = Math.sqrt(X.getValue());
                    double y = Math.sqrt(Y.getValue());
                    {
                        {;
                        context.getLivingTarget().hurt(new DamageSource((Holder<DamageType>) ENDER_MAGIC), (float) (damageDealt * 0.10f * x * y));
                        }
                    }
                }
                context.getLivingTarget().setLastHurtByMob(context.getAttacker());
            }
        }
    }
}
