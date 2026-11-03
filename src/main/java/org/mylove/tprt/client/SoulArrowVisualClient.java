package org.mylove.tprt.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.mylove.tprt.Tprt;
import org.mylove.tprt.common.SoulArrowVisuals;
import org.mylove.tprt.network.SoulArrowVisualPacket;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = Tprt.MODID, value = Dist.CLIENT)
public final class SoulArrowVisualClient {
    private record Pending(SoulArrowVisualPacket packet, int ticksLeft) {}
    private static final Map<UUID, Pending> PENDING = new HashMap<>();
    private static ClientLevel pendingLevel;

    private SoulArrowVisualClient() {}

    public static void accept(SoulArrowVisualPacket packet) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;
        if (pendingLevel != level) {
            PENDING.clear();
            pendingLevel = level;
        }
        if (!apply(level, packet)) {
            // Normally StartTracking follows spawn, but tolerate packet/spawn ordering for 5 seconds.
            if (PENDING.size() < 4096) PENDING.put(packet.uuid(), new Pending(packet, 100));
        }
    }

    private static boolean apply(ClientLevel level, SoulArrowVisualPacket packet) {
        if (level.getEntity(packet.entityId()) instanceof AbstractArrow arrow
                && arrow.getUUID().equals(packet.uuid())) {
            SoulArrowVisuals.mark(arrow);
            return true;
        }
        return false;
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        ClientLevel level = Minecraft.getInstance().level;
        if (level != pendingLevel) {
            PENDING.clear();
            pendingLevel = level;
        }
        if (level == null) return;
        if (org.mylove.tprt.utils.ModListUtil.CALoaded && !Minecraft.getInstance().isPaused()) {
            SoulArrowTrail.tick(level);
        }
        PENDING.entrySet().removeIf(entry -> apply(level, entry.getValue().packet())
                || entry.getValue().ticksLeft() <= 1);
        PENDING.replaceAll((id, pending) -> new Pending(pending.packet(), pending.ticksLeft() - 1));
    }
}
