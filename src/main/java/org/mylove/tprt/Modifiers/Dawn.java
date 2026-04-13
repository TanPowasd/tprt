package org.mylove.tprt.Modifiers;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mylove.tprt.hooks.Arrowmodifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.build.ToolStatsModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.ranged.ProjectileHitModifierHook;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.nbt.*;
import slimeknights.tconstruct.library.tools.stat.ModifierStatsBuilder;

import static com.ssakura49.sakuratinker.common.tinkering.modifiers.special.PolishModifier.POLISH_STAT;

public class Dawn extends Arrowmodifier implements ToolStatsModifierHook, ProjectileHitModifierHook{

    @Override
    protected void registerHooks(ModuleHookMap.@NotNull Builder hookBuilder) {
        super.registerHooks(hookBuilder);
        hookBuilder.addHook(this, ModifierHooks.PROJECTILE_HIT);
        hookBuilder.addHook(this, ModifierHooks.TOOL_STATS);
    }

    @Override
    public void addToolStats(IToolContext iToolContext, ModifierEntry modifier, ModifierStatsBuilder builder) {
        POLISH_STAT.multiply(builder, 0.25*modifier.getLevel());
    }

    @Override
    public void arrowhurt(ModifierNBT modifiers, ModDataNBT persistentData, int level, Projectile projectile, EntityHitResult hit, AbstractArrow arrow, LivingEntity attacker, LivingEntity target) {
        if (target != null && attacker instanceof Player) {
        }
    }

    @Override
    public boolean onProjectileHitEntity(ModifierNBT modifiers, ModDataNBT persistentData, ModifierEntry modifier, Projectile projectile, EntityHitResult hit, @Nullable LivingEntity attacker, @Nullable LivingEntity target, boolean notBlocked) {
        if (target != null && attacker instanceof Player) {
        }
        return notBlocked;
    }

    @Override
    public void onProjectileShoot(IToolStackView tool, ModifierEntry modifier, @Nullable LivingEntity shooter, ItemStack ammo, Projectile projectile, @Nullable AbstractArrow arrow, ModDataNBT persistentData, boolean primary) {

    }
}
