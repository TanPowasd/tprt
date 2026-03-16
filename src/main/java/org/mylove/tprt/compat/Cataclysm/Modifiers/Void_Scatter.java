package org.mylove.tprt.compat.Cataclysm.Modifiers;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import org.jetbrains.annotations.Nullable;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.ranged.ProjectileHitModifierHook;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;
import slimeknights.tconstruct.library.tools.nbt.ModifierNBT;

import java.util.List;

public class Void_Scatter extends Modifier implements ProjectileHitModifierHook {

    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        super.registerHooks(hookBuilder);
        hookBuilder.addHook(this, ModifierHooks.PROJECTILE_HIT);
    }

    @Override
    public boolean onProjectileHitEntity(ModifierNBT modifiers, ModDataNBT persistentData, ModifierEntry modifier, Projectile projectile, EntityHitResult hit, @Nullable LivingEntity attacker, @Nullable LivingEntity target, boolean notBlocked) {
        Level world = projectile.level();
        int x = modifier.getLevel();
        AABB area = null;
        if (target != null) {
            area = new AABB(target.getX() - 1.5 * x, target.getY() - 1.5 * x, target.getZ() - 1.5 * x,
                    target.getX() + 1.5 * x, target.getY() + 1.5 * x, target.getZ() + 1.5 * x);
        }
        List<LivingEntity> nearbyEntities = null;
        if (area != null) {
            nearbyEntities = world.getEntitiesOfClass(LivingEntity.class, area);
        }
        DamageSource damageSource = null;
        if (attacker != null) {
            damageSource = attacker.damageSources().magic();
        }
        if (target != null) {
            if (attacker != null) {
                target.hurt(attacker.damageSources().playerAttack((Player) attacker), 6 * x * x);
                        target.invulnerableTime = 10 ;
            }
        }
        float damage = (float) (6.5 + 4 * x * x);
        if (nearbyEntities != null) {
            for (LivingEntity entity : nearbyEntities) {
                if (damageSource != null) {
                    entity.hurt(damageSource, damage);
                }
            }
        }
        return notBlocked;
    }

    @Override
    public boolean onProjectileHitsBlock(ModifierNBT modifiers, ModDataNBT persistentData, ModifierEntry modifier, Projectile projectile, BlockHitResult hit, @Nullable LivingEntity owner) {
        Level world = projectile.level();
        int x = modifier.getLevel();
        AABB area = new AABB(hit.getLocation().x - 1.5 * x, hit.getLocation().y - 1.5 * x, hit.getLocation().z - 1.5 * x,
                hit.getLocation().x + 1.5 * x, hit.getLocation().y + 1.5 * x, hit.getLocation().z + 1.5 * x);
        List<LivingEntity> nearbyEntities = world.getEntitiesOfClass(LivingEntity.class, area);
        DamageSource damageSource = null;
        if (owner != null) {
            damageSource = owner.damageSources().magic();
        }
        float damage = (float) (6.5 + 4 * x * x);
        for (LivingEntity entity : nearbyEntities) {
            if (damageSource != null) {
                entity.hurt(damageSource, damage);
            }
        }
        return false;
    }
}
