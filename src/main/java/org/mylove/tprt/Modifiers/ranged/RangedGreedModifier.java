package org.mylove.tprt.Modifiers.ranged;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.ranged.ProjectileLaunchModifierHook;
import slimeknights.tconstruct.library.modifiers.impl.NoLevelsModifier;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/** Independent player state; register this modifier as ranged_greed. Non-arrows are unsupported. */
public class RangedGreedModifier extends NoLevelsModifier implements ProjectileLaunchModifierHook {
    private static final String STATE = "tprt:ranged_greed", PAYLOAD = "tprt:ranged_greed_shot";
    private static final int MAX_SHOTS = 256;
    private static final long SHOT_LIFETIME = 12000; // ten minutes; older arrows fail closed
    private static boolean eventsRegistered;
    private static final Map<LivingEntity, Group> GROUPS = new WeakHashMap<>();
    private static final Map<LivingEntity, List<Pending>> PENDING = new WeakHashMap<>();

    public RangedGreedModifier() {
        registerEvents();
    }

    public static synchronized void registerEvents() {
        if (eventsRegistered) return;
        eventsRegistered = true;
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false,
                net.minecraftforge.event.entity.living.LivingEvent.LivingTickEvent.class,
                RangedGreedModifier::displayTick);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST, false, LivingHurtEvent.class,
                RangedGreedModifier::beforeDamage);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, false, LivingDamageEvent.class,
                RangedGreedModifier::afterDamage);
    }

    @Override
    protected void registerHooks(ModuleHookMap.Builder builder) {
        builder.addHook(this, ModifierHooks.PROJECTILE_LAUNCH);
    }

    /** TC bow/crossbow iterate sides BEFORE the primary (index count/2), with one shared ammo object. */
    @Override
    public void onProjectileLaunch(IToolStackView tool, ModifierEntry modifier, LivingEntity shooter,
                                   ItemStack ammo, Projectile projectile, AbstractArrow arrow,
                                   ModDataNBT data, boolean primary) {
        if (shooter.level().isClientSide || !(shooter instanceof Player)) return;
        if (projectile.getPersistentData().contains(PAYLOAD, Tag.TAG_COMPOUND)) return;
        int count = ammo.getCount();
        long now = shooter.level().getGameTime();
        Group group = GROUPS.get(shooter);
        boolean continuing = group != null && group.ammo == ammo && group.tool == tool
                && group.tick == now && group.index < group.count && group.count == count;
        int index = continuing ? group.index : 0;
        // Refuse ambiguous helpers instead of attaching them to an unrelated preceding shot.
        if (count < 1 || primary != (index == count / 2)) {
            GROUPS.remove(shooter);
            unsupported(projectile);
            return;
        }
        if (!continuing) {
            group = new Group(ammo, tool, count, now, newShot(shooter, now));
            GROUPS.put(shooter, group);
        }
        group.index++;
        stamp(projectile, arrow, group.payload);
        if (group.index == group.count) GROUPS.remove(shooter);
    }

    /** Legacy false-only helpers have no group boundary. Only inherited payloads are safe there. */
    @Override
    public void onProjectileLaunch(IToolStackView tool, ModifierEntry modifier, LivingEntity shooter,
                                   Projectile projectile, AbstractArrow arrow, ModDataNBT data, boolean primary) {
        if (shooter.level().isClientSide || !(shooter instanceof Player)
                || projectile.getPersistentData().contains(PAYLOAD, Tag.TAG_COMPOUND)) return;
        if (!primary) { unsupported(projectile); return; }
        stamp(projectile, arrow, newShot(shooter, shooter.level().getGameTime()));
    }

    private static void unsupported(Projectile projectile) {
        projectile.getPersistentData().putBoolean("tprt:ranged_greed_unsupported", true);
    }

    private static void stamp(Projectile projectile, AbstractArrow arrow, CompoundTag snapshot) {
        CompoundTag payload = snapshot.copy();
        payload.putBoolean("Supported", arrow != null);
        projectile.getPersistentData().put(PAYLOAD, payload);
        if (arrow == null) { unsupported(projectile); return; }
        // Replacement and sandstorm clone paths copy ForgeData AND already-modified baseDamage.
        // The payload is also the once-only application marker; never replay on copied arrows.
        arrow.setBaseDamage(arrow.getBaseDamage() * payload.getDouble("Multiplier"));
    }

    private static void displayTick(net.minecraftforge.event.entity.living.LivingEvent.LivingTickEvent event) {
        LivingEntity owner = event.getEntity();
        if (owner.level().isClientSide || !(owner instanceof Player)
                || !owner.getPersistentData().contains(STATE, Tag.TAG_COMPOUND)) return;
        CompoundTag state = state(owner);
        long now = owner.level().getGameTime();
        int stacks = decay(state, now);
        var effect = org.mylove.tprt.registries.ModBuffRegistry.RANGED_GREED.get();
        var current = owner.getEffect(effect);
        if (stacks == 0) {
            if (current != null) owner.removeEffect(effect);
            return;
        }
        int duration = (int) Math.max(2, Math.min(201,
                state.getLong("NextDecay") - now + (stacks - 1) * 20 + 1));
        if (current == null || current.getAmplifier() != stacks - 1
                || Math.abs(current.getDuration() - duration) > 2) {
            owner.removeEffect(effect);
            owner.addEffect(new net.minecraft.world.effect.MobEffectInstance(effect, duration, stacks - 1));
        }
    }

    private static CompoundTag state(LivingEntity owner) {
        CompoundTag root = owner.getPersistentData();
        if (!root.contains(STATE, Tag.TAG_COMPOUND)) root.put(STATE, new CompoundTag());
        return root.getCompound(STATE);
    }

    private static int decay(CompoundTag state, long now) {
        int stacks = Math.max(0, Math.min(6, state.getInt("Stacks")));
        long next = state.getLong("NextDecay");
        if (stacks > 0 && (next <= 0 || next > now + 80)) stacks = 0;
        if (stacks > 0 && now >= next) {
            long drops = 1 + (now - next) / 20;
            stacks = (int) Math.max(0, stacks - drops);
            state.putLong("NextDecay", next + drops * 20);
        }
        state.putInt("Stacks", stacks);
        return stacks;
    }

    private static ListTag prune(CompoundTag state, long now) {
        ListTag shots = state.getList("Shots", Tag.TAG_COMPOUND);
        for (int i = shots.size() - 1; i >= 0; i--) {
            long expires = shots.getCompound(i).getLong("Expires");
            if (expires <= now || expires > now + SHOT_LIFETIME) shots.remove(i);
        }
        while (shots.size() > MAX_SHOTS) shots.remove(0);
        state.put("Shots", shots);
        return shots;
    }

    private static CompoundTag newShot(LivingEntity shooter, long now) {
        CompoundTag state = state(shooter);
        int stacks = decay(state, now);
        boolean full = shooter.getHealth() >= shooter.getMaxHealth();
        CompoundTag payload = new CompoundTag();
        payload.putUUID("Shot", UUID.randomUUID());
        payload.putUUID("Owner", shooter.getUUID());
        payload.putBoolean("OwnerFull", full);
        payload.putDouble("Multiplier", 1.0 + (full ? 0.20 : 0) + stacks * 0.05);
        ListTag shots = prune(state, now);
        while (shots.size() >= MAX_SHOTS) shots.remove(0);
        CompoundTag record = new CompoundTag();
        record.putUUID("Shot", payload.getUUID("Shot"));
        record.putLong("Expires", now + SHOT_LIFETIME);
        shots.add(record);
        return payload;
    }

    private static CompoundTag payload(DamageSource source) {
        if (!(source.getDirectEntity() instanceof AbstractArrow arrow)) return null;
        CompoundTag tag = arrow.getPersistentData().getCompound(PAYLOAD);
        return tag.getBoolean("Supported") && tag.hasUUID("Shot") && tag.hasUUID("Owner") ? tag : null;
    }

    private static void beforeDamage(LivingHurtEvent event) {
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide || event.isCanceled() || payload(event.getSource()) == null) return;
        long now = target.level().getGameTime();
        List<Pending> pending = PENDING.computeIfAbsent(target, key -> new ArrayList<>());
        pending.removeIf(hit -> hit.tick != now);
        while (pending.size() >= 16) pending.remove(0);
        pending.add(new Pending(event.getSource(), now, target.getHealth() >= target.getMaxHealth()));
    }

    private static void afterDamage(LivingDamageEvent event) {
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide || event.isCanceled() || !(event.getAmount() > 0)) return;
        CompoundTag payload = payload(event.getSource());
        if (payload == null || !(event.getSource().getEntity() instanceof Player owner)
                || !owner.getUUID().equals(payload.getUUID("Owner"))) return;
        long now = target.level().getGameTime();
        List<Pending> pending = PENDING.get(target);
        if (pending == null) return;
        Pending captured = null;
        // Identity, not merely projectile/tick equality: nested damage uses its own source object.
        for (int i = pending.size() - 1; i >= 0; i--) {
            Pending candidate = pending.get(i);
            if (candidate.source == event.getSource() && candidate.tick == now) {
                captured = candidate;
                pending.remove(i);
                break;
            }
        }
        if (captured == null) return;
        CompoundTag state = state(owner);
        ListTag shots = prune(state, now);
        for (int i = 0; i < shots.size(); i++) {
            CompoundTag shot = shots.getCompound(i);
            if (shot.hasUUID("Shot") && shot.getUUID("Shot").equals(payload.getUUID("Shot"))) {
                if (shot.getBoolean("Consumed")) return;
                shot.putBoolean("Consumed", true);
                int stacks = decay(state, now);
                int gain = 1 + ((payload.getBoolean("OwnerFull") || captured.full) ? 1 : 0);
                state.putInt("Stacks", Math.min(6, stacks + gain));
                state.putLong("NextDecay", now + 80);
                return;
            }
        }
        // An expired/evicted shot may still deal its snapshotted damage, but can never grant stacks.
    }

    private static final class Group {
        final ItemStack ammo;
        final IToolStackView tool;
        final int count;
        final long tick;
        final CompoundTag payload;
        int index;
        Group(ItemStack ammo, IToolStackView tool, int count, long tick, CompoundTag payload) {
            this.ammo = ammo; this.tool = tool; this.count = count; this.tick = tick; this.payload = payload;
        }
    }

    private record Pending(DamageSource source, long tick, boolean full) {}
}
