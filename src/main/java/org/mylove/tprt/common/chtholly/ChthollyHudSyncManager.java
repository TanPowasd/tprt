package org.mylove.tprt.common.chtholly;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.mylove.tprt.Modifiers.re.elegy_of_world_end;
import org.mylove.tprt.Modifiers.re.even_as_the_sun_sets;
import org.mylove.tprt.Modifiers.re.happiest_girl_in_the_world;
import org.mylove.tprt.Modifiers.re.seniorious;
import org.mylove.tprt.Modifiers.re.what_do_you_do_at_the_end_of_the_world;
import org.mylove.tprt.Tprt;
import org.mylove.tprt.network.ChthollyHudSyncPacket;
import org.mylove.tprt.network.PacketHandler;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 「挚爱」(chtholly) 服务端统一状态调度与增量同步管理器:
 * <ul>
 *   <li>驱动无弓状态下残余「守望之矢 / 不归路标」的到期折返结算；</li>
 *   <li>每 4 ticks 汇总一次玩家身上各词条快照，仅在快照相较上一次实际发生变化时发送单个轻量 S2C 包，
 *       空闲或数值未变动时零发包、零 ItemStack NBT 污染；</li>
 *   <li>在服务器关闭时统一清理各词条静态运行态缓存，防止单人世界连续切换时的跨存档残留。</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = Tprt.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ChthollyHudSyncManager {

    private static final int SYNC_INTERVAL_TICKS = 4;
    private static final Map<UUID, ChthollySyncData> LAST_SENT = new ConcurrentHashMap<>();

    private ChthollyHudSyncManager() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) {
            return;
        }
        if (!(event.player instanceof ServerPlayer serverPlayer)
                || !(serverPlayer.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        // 确保即使把弓收起或仅使用挚爱箭矢射击，「守望之矢」与「不归路标」依然能按时到期折返
        if (serverPlayer.isAlive()) {
            even_as_the_sun_sets.tickArcherStateIfPresent(serverLevel, serverPlayer);
        }

        UUID uuid = serverPlayer.getUUID();
        boolean firstSync = !LAST_SENT.containsKey(uuid);
        if (!firstSync && serverPlayer.tickCount % SYNC_INTERVAL_TICKS != 0) {
            return;
        }

        ChthollySyncData snapshot = captureSnapshot(serverPlayer, serverLevel.getGameTime());
        ChthollySyncData prev = LAST_SENT.get(uuid);
        if (!snapshot.equals(prev)) {
            LAST_SENT.put(uuid, snapshot);
            PacketHandler.sendToPlayer(new ChthollyHudSyncPacket(snapshot), serverPlayer);
        }
    }

    private static ChthollySyncData captureSnapshot(ServerPlayer player, long gameTime) {
        var we = what_do_you_do_at_the_end_of_the_world.getHudSnapshot(player);
        var hg = happiest_girl_in_the_world.getHudSnapshot(player);
        var el = elegy_of_world_end.getHudSnapshot(player);
        var ss = even_as_the_sun_sets.getHudSnapshot(player, gameTime);

        return new ChthollySyncData(
                we.totalLevel(),
                we.phase(),
                we.poolTenths(),
                we.berserkSec(),
                we.cdSec(),
                hg.equipped(),
                hg.shieldTenths(),
                hg.peakTenths(),
                hg.finaleSec(),
                hg.cdSec(),
                el.equipped(),
                el.activeTargets(),
                el.maxStoredTenths(),
                el.remainSec(),
                ss.hoverCount(),
                ss.hoverSec(),
                ss.waymarkCount(),
                ss.waymarkSec(),
                ss.linkedSword()
        );
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        invalidatePlayer(event.getEntity());
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        invalidatePlayer(event.getEntity());
    }

    @SubscribeEvent
    public static void onPlayerDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
        invalidatePlayer(event.getEntity());
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        invalidatePlayer(event.getEntity());
    }

    private static void invalidatePlayer(Player player) {
        if (player != null) {
            LAST_SENT.remove(player.getUUID());
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        LAST_SENT.clear();
        seniorious.clearServerState();
        elegy_of_world_end.clearServerState();
        even_as_the_sun_sets.clearServerState();
        happiest_girl_in_the_world.clearServerState();
        what_do_you_do_at_the_end_of_the_world.clearServerState();
        ChthollySyncData.clearClient();
    }
}

