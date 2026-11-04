package org.mylove.tprt.compat.Cataclysm.Modifiers;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import org.mylove.tprt.compat.Cataclysm.CataclysmCompat;
import org.mylove.tprt.compat.Cataclysm.entity.TprtPhantomArrow;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.interaction.InventoryTickModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.ranged.ProjectileHitModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.ranged.ProjectileLaunchModifierHook;
import slimeknights.tconstruct.library.modifiers.impl.NoLevelsModifier;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.capability.EntityModifierCapability;
import slimeknights.tconstruct.library.tools.capability.PersistentDataCapability;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;
import slimeknights.tconstruct.library.tools.nbt.ModifierNBT;

/** Register only through the Cataclysm integration. No injected launch or damage methods. */
public class CursedSpiritArrowModifier extends NoLevelsModifier implements ProjectileLaunchModifierHook,
        ProjectileHitModifierHook, InventoryTickModifierHook {
    private static final String REPLACE = "tprt:replace_with_phantom_arrow";

    public static void registerEvents() {
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, false, EntityJoinLevelEvent.class,
                CursedSpiritArrowModifier::replaceArrow);
    }

    @Override
    protected void registerHooks(ModuleHookMap.Builder builder) {
        builder.addHook(this, ModifierHooks.PROJECTILE_LAUNCH, ModifierHooks.PROJECTILE_HIT, ModifierHooks.INVENTORY_TICK);
    }

    @Override
    public void onProjectileLaunch(IToolStackView tool, ModifierEntry modifier, LivingEntity shooter,
                                   Projectile projectile, AbstractArrow arrow, ModDataNBT data, boolean primary) {
        if (arrow != null && !arrow.level().isClientSide) {
            arrow.setPierceLevel((byte) Math.min(127, Math.max(0, arrow.getPierceLevel()) + 1));
            arrow.getPersistentData().putBoolean(REPLACE, true);
            Soul_puncture.trackArrow(shooter, arrow);
        }
    }

    private static void replaceArrow(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide || !(event.getEntity() instanceof AbstractArrow original)
                || original instanceof TprtPhantomArrow
                || (!original.getPersistentData().getBoolean(REPLACE)
                    && !original.getPersistentData().getBoolean(CursedSandstormModifier.REPLACE))) {
            return;
        }
        // This event occurs after ALL launch hooks. Never run them again: multishot, damage,
        // ammunition and durability have already been processed by the original launcher.
        CompoundTag tag = original.saveWithoutId(new CompoundTag());
        // Only copy TC capabilities explicitly. Arbitrary third-party providers may be entity-specific.
        tag.remove("ForgeCaps");
        tag.getCompound("ForgeData").remove(REPLACE);
        // Resolve once after every launch hook: independent of modifier iteration order.
        boolean sandstorm = original.getPersistentData().getBoolean(CursedSandstormModifier.REPLACE);
        tag.getCompound("ForgeData").remove(CursedSandstormModifier.REPLACE);
        if (sandstorm) tag.getCompound("ForgeData").remove("tprt:soul_arrow_visual");
        TprtPhantomArrow replacement = sandstorm
                ? CataclysmCompat.TPRT_CURSED_SANDSTORM.get().create(event.getLevel())
                : CataclysmCompat.TPRT_PHANTOM_ARROW.get().create(event.getLevel());
        if (replacement == null) {
            return;
        }
        replacement.load(tag);
        replacement.setOwner(original.getOwner());
        replacement.setDeltaMovement(original.getDeltaMovement()); // Entity.load clamps high velocities
        replacement.setKnockback(original.getKnockback()); // not saved by AbstractArrow
        replacement.setNoGravity(original.isNoGravity());
        replacement.setNoPhysics(original.isNoPhysics());
        replacement.copySpectralEffect(original, tag);
        replacement.getCapability(EntityModifierCapability.CAPABILITY).ifPresent(cap ->
                cap.setModifiers(EntityModifierCapability.getOrEmpty(original)));
        PersistentDataCapability.getOrWarn(replacement).copyFrom(PersistentDataCapability.getOrWarn(original).getCopy());
        if (replacement instanceof org.mylove.tprt.compat.Cataclysm.entity.TprtCursedSandstormArrow storm) {
            storm.initializeFlight();
        }
        if (event.getLevel().addFreshEntity(replacement)) {
            Soul_puncture.transferTracking(original, replacement);
            if (sandstorm) {
                spawnSandstormSideArrow(replacement, -15.0F);
                spawnSandstormSideArrow(replacement, 15.0F);
            }
            event.setCanceled(true);
            original.discard();
        }
        // If another mod denies the new spawn, retain the original arrow instead of eating ammunition.
    }

    /** Extra projectiles reuse the completed launch payload, without replaying launch hooks. */
    private static void spawnSandstormSideArrow(TprtPhantomArrow center, float yawOffset) {
        TprtPhantomArrow side = CataclysmCompat.TPRT_CURSED_SANDSTORM.get().create(center.level());
        if (side == null) return;
        CompoundTag tag = center.saveWithoutId(new CompoundTag());
        tag.remove("UUID");
        tag.remove("ForgeCaps");
        tag.putBoolean("StormInitialized", false); // recompute acceleration for this side's rotated launch
        tag.remove("StormTarget");
        tag.getCompound("ForgeData").remove(REPLACE);
        tag.getCompound("ForgeData").remove(CursedSandstormModifier.REPLACE);
        side.load(tag);
        side.setOwner(center.getOwner());
        // Vec3.yRot uses the opposite sign to entity yaw.
        side.setDeltaMovement(center.getDeltaMovement().yRot(-yawOffset * ((float) Math.PI / 180.0F)));
        side.setYRot(center.getYRot() + yawOffset);
        side.yRotO = side.getYRot();
        side.setKnockback(center.getKnockback());
        side.setNoGravity(center.isNoGravity());
        side.setNoPhysics(center.isNoPhysics());
        // These are free secondary projectiles, not additional recoverable ammunition.
        side.pickup = AbstractArrow.Pickup.DISALLOWED;
        side.getCapability(EntityModifierCapability.CAPABILITY).ifPresent(cap ->
                cap.setModifiers(EntityModifierCapability.getOrEmpty(center)));
        PersistentDataCapability.getOrWarn(side).copyFrom(PersistentDataCapability.getOrWarn(center).getCopy());
        ((org.mylove.tprt.compat.Cataclysm.entity.TprtCursedSandstormArrow) side).initializeFlight();
        if (center.level().addFreshEntity(side) && center.getOwner() instanceof LivingEntity shooter
                && (EntityModifierCapability.getOrEmpty(center).getLevel(CataclysmCompat.Soul_puncture.getId()) > 0
                    || EntityModifierCapability.getOrEmpty(center).getLevel(CataclysmCompat.CURSED_SPIRIT_ARROW.getId()) > 0)) {
            Soul_puncture.trackArrow(shooter, side);
        }
        // A denied side spawn must never cancel or remove the already spawned center arrow.
    }

    @Override
    public void onInventoryTick(IToolStackView tool, ModifierEntry modifier, Level level, LivingEntity holder,
                                int slot, boolean selected, boolean correctSlot, ItemStack stack) {
        Soul_puncture.tickTracked(level, holder);
    }

    @Override
    public boolean onProjectileHitEntity(ModifierNBT modifiers, ModDataNBT data, ModifierEntry modifier,
                                          Projectile projectile, EntityHitResult hit, LivingEntity attacker, LivingEntity target) {
        // Soul_puncture has its own hit hook. Do not register the same impact twice when both are present.
        if (modifiers.getLevel(CataclysmCompat.Soul_puncture.getId()) == 0 && target != null) {
            return Soul_puncture.recordHit(projectile, target);
        }
        return false;
    }
}
