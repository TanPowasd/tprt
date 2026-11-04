package org.mylove.tprt.compat.Cataclysm.Modifiers;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.ranged.ProjectileLaunchModifierHook;
import slimeknights.tconstruct.library.modifiers.impl.NoLevelsModifier;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;

/** A replacement request only; the shared join handler resolves sandstorm over phantom after all launch hooks. */
public class CursedSandstormModifier extends NoLevelsModifier implements ProjectileLaunchModifierHook {
    public static final String REPLACE = "tprt:replace_with_cursed_sandstorm";

    @Override
    protected void registerHooks(ModuleHookMap.Builder builder) {
        builder.addHook(this, ModifierHooks.PROJECTILE_LAUNCH);
    }

    @Override
    public void onProjectileLaunch(IToolStackView tool, ModifierEntry modifier, LivingEntity shooter,
                                   Projectile projectile, AbstractArrow arrow, ModDataNBT data, boolean primary) {
        if (arrow != null && !arrow.level().isClientSide) {
            arrow.getPersistentData().putBoolean(REPLACE, true);
        }
    }
}
