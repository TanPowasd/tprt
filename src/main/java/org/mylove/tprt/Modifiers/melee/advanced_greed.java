package org.mylove.tprt.Modifiers.melee;

import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import org.mylove.tprt.registries.ModBuffRegistry;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeDamageModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeHitModifierHook;
import slimeknights.tconstruct.library.modifiers.impl.NoLevelsModifier;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

public class advanced_greed extends NoLevelsModifier implements MeleeDamageModifierHook, MeleeHitModifierHook {
    // Weak contexts also release failed attacks, for which afterMeleeHit is never called.
    private final Map<ToolAttackContext, Boolean> fullHealthHits = new WeakHashMap<>();

    @Override
    protected void registerHooks(ModuleHookMap.Builder builder) {
        super.registerHooks(builder);
        builder.addHook(this, ModifierHooks.MELEE_HIT, ModifierHooks.MELEE_DAMAGE);
    }

    @Override
    public int getPriority() { return 99; }

    private static boolean full(LivingEntity entity) {
        return entity.getHealth() >= entity.getMaxHealth();
    }

    @Override
    public float getMeleeDamage(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context,
                                float baseDamage, float damage) {
        LivingEntity target = context.getLivingTarget();
        if (target == null) return damage;
        LivingEntity attacker = context.getAttacker();
        boolean attackerFull = full(attacker);
        if (!attacker.level().isClientSide) {
            fullHealthHits.put(context, attackerFull || full(target));
        }
        MobEffectInstance buff = attacker.getEffect(ModBuffRegistry.MYL_GIRIG_PRO.get());
        int stacks = buff == null ? 0 : Math.min(6, buff.getAmplifier() + 1);
        return damage * (1.0F + (attackerFull ? 0.20F : 0.0F) + stacks * 0.05F);
    }

    @Override
    public void afterMeleeHit(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float damageDealt) {
        Boolean bonus = fullHealthHits.remove(context);
        LivingEntity attacker = context.getAttacker();
        if (attacker.level().isClientSide || context.getLivingTarget() == null || damageDealt <= 0) return;
        var effect = ModBuffRegistry.MYL_GIRIG_PRO.get();
        MobEffectInstance previous = attacker.getEffect(effect);
        int stacks = previous == null ? 0 : Math.min(6, previous.getAmplifier() + 1);
        stacks = Math.min(6, stacks + (Boolean.TRUE.equals(bonus) ? 2 : 1));
        // Replace, rather than merge: merging can retain old hidden amplifier instances.
        attacker.removeEffect(effect);
        attacker.addEffect(new MobEffectInstance(effect, 80, stacks - 1));
    }
}
