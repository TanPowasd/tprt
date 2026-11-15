package org.mylove.tprt.Modifiers.ranged;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import org.mylove.tprt.common.BlazeforgedFireEvents;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.ranged.ProjectileLaunchModifierHook;
import slimeknights.tconstruct.library.modifiers.impl.NoLevelsModifier;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;

/** 引火: launch-time snapshot, including additional TC arrows. */
public class flame_ignition extends NoLevelsModifier implements ProjectileLaunchModifierHook {
    @Override protected void registerHooks(ModuleHookMap.Builder builder) {
        super.registerHooks(builder);
        builder.addHook(this, ModifierHooks.PROJECTILE_LAUNCH);
    }
    @Override public void onProjectileLaunch(IToolStackView tool, ModifierEntry modifier, LivingEntity shooter,
            Projectile projectile, AbstractArrow arrow, ModDataNBT data, boolean primary) {
        BlazeforgedFireEvents.snapshot(tool, shooter, projectile, arrow);
    }
}
