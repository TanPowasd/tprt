package org.mylove.tprt.Modifiers.ranged;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import org.mylove.tprt.common.BlazeforgedFireEvents;
import org.mylove.tprt.Modifiers.melee.kindled_rebirth;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.ranged.ProjectileLaunchModifierHook;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;

/** 浴火重生: shares point display/expiry, but deliberately registers no melee damage hook. */
public class ranged_kindled_rebirth extends kindled_rebirth implements ProjectileLaunchModifierHook {
    @Override protected String tooltipKey() { return "modifier.tprt.ranged_kindled_rebirth.tooltip"; }
    @Override protected void registerHooks(ModuleHookMap.Builder builder) {
        builder.addHook(this, ModifierHooks.PROJECTILE_LAUNCH, ModifierHooks.INVENTORY_TICK, ModifierHooks.TOOLTIP);
    }
    @Override public void onProjectileLaunch(IToolStackView tool, ModifierEntry modifier, LivingEntity shooter,
            Projectile projectile, AbstractArrow arrow, ModDataNBT data, boolean primary) {
        BlazeforgedFireEvents.snapshot(tool, shooter, projectile, arrow);
    }
}
