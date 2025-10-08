package org.mylove.tprt.tags;

import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.damage.DamageSources;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;
import org.mylove.tprt.hooks.MeleeDamagePercentModifierHook;
import org.mylove.tprt.registries.ModifierHooksRegistry;
import org.mylove.tprt.registries.SpellsRegistry;
import org.mylove.tprt.registries.TagsRegistry;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeHitModifierHook;
import slimeknights.tconstruct.library.modifiers.impl.NoLevelsModifier;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

public class fire_addition extends NoLevelsModifier implements MeleeHitModifierHook, MeleeDamagePercentModifierHook {
    protected void registerHooks(ModuleHookMap.@NotNull Builder hookBuilder) {
        super.registerHooks(hookBuilder);
        hookBuilder.addHook(this, ModifierHooks.MELEE_HIT, ModifierHooksRegistry.MELEE_DAMAGE_PERCENT);
    }

    public void getMeleeDamageModifier(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float baseDamage, float damage, MeleeDamagePercentModifierHook.DamageModifier damagemodifier) {
        if (tool.hasTag(TagsRegistry.ItemsTag.magic_blade) && !context.isExtraAttack()) {
            int level = modifier.getLevel();
            damagemodifier.addMultiply(1.0F + 0.5F * (float)level);
        }
    }

    public void afterMeleeHit(IToolStackView tool, @NotNull ModifierEntry modifier, @NotNull ToolAttackContext context, float damageDealt) {
        if (tool.hasTag(TagsRegistry.ItemsTag.magic_blade) && !context.isExtraAttack()){
            LivingEntity attacker = context.getAttacker();
            LivingEntity target = context.getLivingTarget();
            if (target != null) {
                int level = modifier.getLevel();
                DamageSources.applyDamage(target, ((AbstractSpell) SpellsRegistry.fire_addition.get()).getSpellPower(level, attacker), ((AbstractSpell)SpellsRegistry.fire_addition.get()).getDamageSource(attacker));
                target.setRemainingFireTicks(100 + level * 60);
            }
        }

    }
}

