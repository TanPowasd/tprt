package org.mylove.tprt.common.modifier;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.mylove.tprt.Tprt;
import org.mylove.tprt.registries.ModifierRegistry;
import slimeknights.tconstruct.library.modifiers.impl.NoLevelsModifier;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/** Charge timing belongs to LanceChargeModifier, never to inventory/client ticks. */
@Mod.EventBusSubscriber(modid = Tprt.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ThunderousMomentumModifier extends NoLevelsModifier {
    private static final UUID SPEED_ID = UUID.fromString("9c3fa084-e767-49bc-935a-56f0645d765b");
    public static final int RAMP_TICKS = 40;
    public static final double MAX_BONUS = 0.5D;
    // Weak keys avoid retaining disconnected players; values are cleared whenever the mount changes.
    private static final Map<Player, LivingEntity> BOOSTED_MOUNTS =
            Collections.synchronizedMap(new WeakHashMap<>());

    static void update(Player player, long elapsed) {
        double amount = MAX_BONUS * Math.min(RAMP_TICKS, Math.max(0L, elapsed)) / RAMP_TICKS;
        apply(player, amount);
        LivingEntity mount = resolveMount(player);
        LivingEntity previous = BOOSTED_MOUNTS.get(player);
        if (previous != null && previous != mount) {
            apply(previous, 0.0D);
            BOOSTED_MOUNTS.remove(player);
        }
        if (mount != null) {
            apply(mount, amount);
            BOOSTED_MOUNTS.put(player, mount);
        }
    }

    static void clear(Player player) {
        apply(player, 0.0D);
        LivingEntity mount = BOOSTED_MOUNTS.remove(player);
        if (mount != null) apply(mount, 0.0D);
    }

    /** The ridden entity that actually moves; boats and minecarts have no speed attribute. */
    private static LivingEntity resolveMount(Player player) {
        Entity root = player.getRootVehicle();
        return root != player && root instanceof LivingEntity living ? living : null;
    }

    private static void apply(LivingEntity entity, double amount) {
        var attribute = entity.getAttribute(Attributes.MOVEMENT_SPEED);
        if (attribute == null) return;
        var current = attribute.getModifier(SPEED_ID);
        if (current != null && current.getAmount() == amount) return;
        attribute.removeModifier(SPEED_ID);
        if (amount > 0.0D) attribute.addTransientModifier(new AttributeModifier(
                SPEED_ID, "tprt.thunderous_momentum", amount, AttributeModifier.Operation.MULTIPLY_TOTAL));
    }

    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && !event.player.level().isClientSide)
            ModifierRegistry.LANCE_CHARGE.get().validateCharge(event.player);
    }

    private static void reset(Player player) {
        if (!player.level().isClientSide) ModifierRegistry.LANCE_CHARGE.get().clearCharge(player);
    }

    @SubscribeEvent public static void death(LivingDeathEvent event) {
        if (event.getEntity() instanceof Player player) reset(player);
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) { reset(event.getEntity()); }
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event) { reset(event.getEntity()); }
    @SubscribeEvent public static void clone(PlayerEvent.Clone event) {
        reset(event.getOriginal());
        reset(event.getEntity());
    }
}
