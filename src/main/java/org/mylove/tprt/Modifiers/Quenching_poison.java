package org.mylove.tprt.Modifiers;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.EntityHitResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeHitModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.ranged.ProjectileHitModifierHook;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;
import slimeknights.tconstruct.library.tools.nbt.ModifierNBT;

public class Quenching_poison extends Modifier implements MeleeHitModifierHook, ProjectileHitModifierHook {

    protected void registerHooks(ModuleHookMap.@NotNull Builder hookBuilder) {
        super.registerHooks(hookBuilder);
        hookBuilder.addHook(this, ModifierHooks.MELEE_HIT);
        hookBuilder.addHook(this, ModifierHooks.PROJECTILE_HIT);
    }

    @Override
    public void afterMeleeHit(@NotNull IToolStackView tool, @NotNull ModifierEntry entry, ToolAttackContext context, float damageDealt) {
        if (!(context.getTarget() instanceof LivingEntity target)) return;
        MobEffect effect = MobEffects.POISON;
        MobEffectInstance instance = target.getEffect(effect);
        if (instance != null) {
            int level = entry.getLevel();
            int x = instance.getAmplifier();
            int X = x + 1;
            int y = instance.getDuration();
            int Y = y+100*(level+1);
            boolean A = instance.isAmbient();
            boolean B = instance.isVisible();
            boolean C = instance.showIcon();
            if (X > 4) {
                X = 4;
            }
            MobEffectInstance newEffect = new MobEffectInstance(
                    effect,
                    Y,
                    X,
                    A,
                    B,
                    C);
            target.addEffect(newEffect);
        } else {
            target.addEffect(new MobEffectInstance(MobEffects.POISON, 200));
        }
    }
    @Override
    public boolean onProjectileHitEntity(ModifierNBT modifiers, ModDataNBT persistentData, ModifierEntry modifier, Projectile projectile, EntityHitResult hit, @Nullable LivingEntity attacker, @Nullable LivingEntity target) {
        MobEffect effect = MobEffects.POISON;
        MobEffectInstance instance = null;
        if (target != null) {
            instance = target.getEffect(effect);
        }
        if (instance != null) {
            int level = modifier.getLevel();
            int x = instance.getAmplifier();
            int X = x + 2;
            int y = instance.getDuration();
            int Y = y+300*(level+1);
            boolean A = instance.isAmbient();
            boolean B = instance.isVisible();
            boolean C = instance.showIcon();
            if (X > 4) {
                X = 4;
            }
            MobEffectInstance newEffect = new MobEffectInstance(
                    effect,
                    Y,
                    X,
                    A,
                    B,
                    C);
            target.addEffect(newEffect);
        } else {
            if (target != null) {
                target.addEffect(new MobEffectInstance(MobEffects.POISON, 200));
            }
        }
        return false;
    }
}
