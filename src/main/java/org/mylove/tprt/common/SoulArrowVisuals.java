package org.mylove.tprt.common;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.mylove.tprt.Tprt;
import org.mylove.tprt.network.PacketHandler;
import org.mylove.tprt.network.SoulArrowVisualPacket;

/** Cosmetic only: never changes arrow type, physics, damage, pickup or hit handling. */
@Mod.EventBusSubscriber(modid = Tprt.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SoulArrowVisuals {
    private static final String KEY = "tprt:soul_arrow_visual";

    private SoulArrowVisuals() {}

    public static boolean isMarked(AbstractArrow arrow) {
        return arrow.getPersistentData().getBoolean(KEY);
    }

    public static void mark(AbstractArrow arrow) {
        if (isMarked(arrow)) return;
        arrow.getPersistentData().putBoolean(KEY, true);
        if (!arrow.level().isClientSide) {
            PacketHandler.sentToTrackingEntity(arrow, new SoulArrowVisualPacket(arrow));
        }
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof AbstractArrow arrow && isMarked(arrow)
                && event.getEntity() instanceof ServerPlayer player) {
            PacketHandler.sendToPlayer(new SoulArrowVisualPacket(arrow), player);
        }
    }
}
