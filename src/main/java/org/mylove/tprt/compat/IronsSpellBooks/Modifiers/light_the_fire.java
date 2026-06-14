package org.mylove.tprt.compat.IronsSpellBooks.Modifiers;

import io.redspace.ironsspellbooks.registries.MobEffectRegistry;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.EntityHitResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeHitModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.ranged.ProjectileHitModifierHook;
import slimeknights.tconstruct.library.modifiers.impl.NoLevelsModifier;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;
import slimeknights.tconstruct.library.tools.nbt.ModifierNBT;

public class light_the_fire extends NoLevelsModifier implements MeleeHitModifierHook, ProjectileHitModifierHook {

    protected void registerHooks(ModuleHookMap.@NotNull Builder hookBuilder) {
        super.registerHooks(hookBuilder);
        hookBuilder.addHook(this, ModifierHooks.MELEE_HIT,ModifierHooks.PROJECTILE_HIT);
    }

    @Override
    public void afterMeleeHit(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float damageDealt) {
        LivingEntity target = context.getLivingTarget();
        LivingEntity player = context.getAttacker();
        if (player instanceof Player && target instanceof LivingEntity){
            MobEffect effect = MobEffectRegistry.IMMOLATE.get();
            MobEffectInstance instance = target.getEffect(effect);
            if (instance != null) {
                int x = instance.getAmplifier();
                int X = x + 1;
                target.addEffect(new MobEffectInstance(effect, 200, X));
            }else {
                target.addEffect(new MobEffectInstance(effect, 200, 0));
            }
        }
    }

    @Override
    public boolean onProjectileHitEntity(ModifierNBT modifiers, ModDataNBT persistentData, ModifierEntry modifier, Projectile projectile, EntityHitResult hit, @Nullable LivingEntity attacker, @Nullable LivingEntity target, boolean notBlocked) {
        if (attacker instanceof Player && target instanceof LivingEntity){
            MobEffect effect = MobEffectRegistry.IMMOLATE.get();
            MobEffectInstance instance = target.getEffect(effect);
            if (instance != null) {
                int x = instance.getAmplifier();
                int X = x + 1;
                target.addEffect(new MobEffectInstance(effect, 200, X));
            }else {
                target.addEffect(new MobEffectInstance(effect, 200, 2));
            }
        }
        return false ;
    }
}
