package org.mylove.tprt.common;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.mylove.tprt.Modifiers.melee.BlazeforgedTraitData;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

/** Minimal actual-fire bridge and persistent, target-following arrow auras. No projectile mixins. */
@Mod.EventBusSubscriber(modid = "tprt", bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class BlazeforgedFireEvents {
    private static final slimeknights.tconstruct.library.modifiers.ModifierId MELEE = new slimeknights.tconstruct.library.modifiers.ModifierId("tprt", "kindled_rebirth");
    private static final slimeknights.tconstruct.library.modifiers.ModifierId RANGED = new slimeknights.tconstruct.library.modifiers.ModifierId("tprt", "ranged_kindled_rebirth");
    private static final slimeknights.tconstruct.library.modifiers.ModifierId IGNITION = new slimeknights.tconstruct.library.modifiers.ModifierId("tprt", "flame_ignition");
    private static final String SHOT = "tprt:flame_shot", AURAS = "tprt:flame_auras", PULSES = "tprt:flame_pulses";
    private static final java.lang.reflect.Field LAST_HURT =
            net.minecraftforge.fml.util.ObfuscationReflectionHelper.findField(LivingEntity.class, "f_20898_");
    private BlazeforgedFireEvents() {}

    public static void snapshot(IToolStackView tool, LivingEntity shooter, Projectile projectile, AbstractArrow arrow) {
        if (shooter.level().isClientSide || arrow == null || tool.isBroken()
                || projectile.getPersistentData().contains(SHOT, Tag.TAG_COMPOUND)) return;
        // Additional-arrow callbacks must match a genuine held ranged tool, in either hand.
        if (!matchesHeldRanged(tool, shooter.getMainHandItem())
                && !matchesHeldRanged(tool, shooter.getOffhandItem())) return;
        boolean ignition = tool.getModifierLevel(IGNITION) > 0;
        // The current launch ignites first; all same-tick multishot arrows share that snapshot.
        if (ignition) shooter.setSecondsOnFire(5);
        CompoundTag group = shooter.getPersistentData().getCompound("tprt:flame_launch");
        long now = shooter.level().getGameTime();
        if (group.getLong("Tick") != now || !group.hasUUID("Owner")) {
            group = new CompoundTag();
            group.putLong("Tick", now);
            group.putUUID("Owner", shooter.getUUID());
            group.putBoolean("Burning", shooter.isOnFire());
            shooter.getPersistentData().put("tprt:flame_launch", group);
        }
        CompoundTag payload = new CompoundTag();
        payload.putUUID("Owner", shooter.getUUID());
        payload.putBoolean("Burning", group.getBoolean("Burning"));
        payload.putBoolean("Aura", ignition);
        float bonus = tool.getModifierLevel(RANGED) > 0
                ? BlazeforgedTraitData.damageBonus(tool, now) : 0;
        payload.putFloat("Bonus", bonus);
        projectile.getPersistentData().put(SHOT, payload);
        // ForgeData and modified baseDamage travel together in sandstorm clones; never apply twice.
        arrow.setBaseDamage(arrow.getBaseDamage() * (1 + bonus));
    }

    private static boolean matchesHeldRanged(IToolStackView tool, net.minecraft.world.item.ItemStack held) {
        if (!held.is(TinkerTags.Items.MODIFIABLE) || !held.is(TinkerTags.Items.RANGED)) return false;
        ToolStack active = ToolStack.from(held);
        return !active.isBroken() && active.getPersistentData().equals(tool.getPersistentData())
                && active.getModifierList().equals(tool.getModifierList())
                && (active.getModifierLevel(IGNITION) > 0 || active.getModifierLevel(RANGED) > 0);
    }

    private static void addHeldFirePoint(net.minecraft.world.item.ItemStack stack, long now) {
        if (!stack.is(TinkerTags.Items.MODIFIABLE)) return;
        ToolStack tool = ToolStack.from(stack);
        boolean eligible = stack.is(TinkerTags.Items.RANGED) ? tool.getModifierLevel(RANGED) > 0
                : tool.getModifierLevel(MELEE) > 0;
        if (!tool.isBroken() && eligible) BlazeforgedTraitData.addPoint(tool, now);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void actualDamage(LivingDamageEvent event) {
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide || event.getAmount() <= 0) return;
        if (event.getSource().is(DamageTypeTags.IS_FIRE)) {
            var main = target.getMainHandItem();
            var off = target.getOffhandItem();
            long now = target.level().getGameTime();
            addHeldFirePoint(main, now);
            // Identity, not NBT equality: two separate tools retain independent point data.
            if (off != main) addHeldFirePoint(off, now);
        }
        if (!(event.getSource().getDirectEntity() instanceof AbstractArrow arrow)) return;
        CompoundTag payload = arrow.getPersistentData().getCompound(SHOT);
        if (!payload.getBoolean("Aura") || !payload.getBoolean("Burning") || !payload.hasUUID("Owner")
                || arrow.getOwner() == null || !arrow.getOwner().getUUID().equals(payload.getUUID("Owner"))) return;
        if (!(arrow.getOwner() instanceof LivingEntity owner) || !enemy(owner, target)) return;
        target.setSecondsOnFire(5);
        CompoundTag all = target.getPersistentData().getCompound(AURAS);
        String key = owner.getUUID().toString();
        CompoundTag aura = new CompoundTag();
        aura.putUUID("Owner", owner.getUUID());
        aura.putLong("Until", target.level().getGameTime() + 100);
        // Actual post-armor arrow damage already contains the launch bonus: do not multiply again.
        aura.putFloat("Damage", event.getAmount());
        all.put(key, aura);
        target.getPersistentData().put(AURAS, all);
    }

    @SubscribeEvent
    public static void tick(LivingEvent.LivingTickEvent event) {
        LivingEntity center = event.getEntity();
        if (!(center.level() instanceof ServerLevel level) || !center.isAlive()
                || !center.getPersistentData().contains(AURAS, Tag.TAG_COMPOUND)) return;
        CompoundTag all = center.getPersistentData().getCompound(AURAS);
        long now = level.getGameTime();
        for (String key : java.util.List.copyOf(all.getAllKeys())) {
            CompoundTag aura = all.getCompound(key);
            if (now >= aura.getLong("Until") || !aura.hasUUID("Owner")) { all.remove(key); continue; }
            if (!(level.getEntity(aura.getUUID("Owner")) instanceof LivingEntity owner) || !owner.isAlive()) continue;
            if (now % 10 == 0) circle(center);
            if (now % 20 != 0) continue;
            for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class,
                    center.getBoundingBox().inflate(4), entity -> entity.distanceToSqr(center) <= 16 && enemy(owner, entity))) {
                CompoundTag pulses = victim.getPersistentData().getCompound(PULSES);
                // One pulse per shooter and target per second, even overlapping cloned-arrow auras.
                if (pulses.contains(key) && now - pulses.getLong(key) < 20) continue;
                pulses.putLong(key, now);
                victim.getPersistentData().put(PULSES, pulses);
                pulse(owner, victim, aura.getFloat("Damage"));
            }
        }
        if (all.isEmpty()) center.getPersistentData().remove(AURAS);
    }

    public static boolean enemy(LivingEntity owner, LivingEntity target) {
        return target != owner && target.isAlive() && !owner.isAlliedTo(target) && !target.isAlliedTo(owner)
                && !(target instanceof TamableAnimal pet && (pet.isOwnedBy(owner)
                || (pet.getOwner() != null && owner.isAlliedTo(pet.getOwner())))) && owner.canAttack(target);
    }

    public static void pulse(LivingEntity owner, LivingEntity target, float damage) {
        if (damage <= 0) return;
        var source = BlazeforgedTraitData.fireFrom(owner);
        if (target.isInvulnerableTo(source)) return;
        int oldFrames = target.invulnerableTime;
        // Scoped bypass: restore both vanilla hurt-window fields even when damage throws.
        float oldLastHurt;
        try { oldLastHurt = LAST_HURT.getFloat(target); }
        catch (IllegalAccessException exception) { throw new IllegalStateException("Cannot read lastHurt", exception); }
        try { target.invulnerableTime = 0; target.hurt(source, damage); }
        finally {
            target.invulnerableTime = oldFrames;
            try { LAST_HURT.setFloat(target, oldLastHurt); }
            catch (IllegalAccessException exception) { throw new IllegalStateException("Cannot restore lastHurt", exception); }
        }
    }

    public static void circle(LivingEntity center) {
        if (!(center.level() instanceof ServerLevel level)) return;
        for (int i = 0; i < 24; i++) {
            double angle = i * Math.PI * 2 / 24;
            level.sendParticles(ParticleTypes.FLAME, center.getX() + Math.cos(angle) * 4,
                    center.getY() + 0.12, center.getZ() + Math.sin(angle) * 4, 1, 0, 0, 0, 0);
        }
    }
}
