package org.mylove.tprt.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.mylove.tprt.common.chtholly.ChthollySyncData;

import java.util.function.Supplier;

/**
 * 服务端 -> 客户端同步「挚爱」各词条运行态快照的轻量网络包（仅在数据变化时按节流发送）。
 */
public class ChthollyHudSyncPacket {

    private final ChthollySyncData data;

    public ChthollyHudSyncPacket(ChthollySyncData data) {
        this.data = data;
    }

    public ChthollyHudSyncPacket(FriendlyByteBuf buf) {
        int weLevel = buf.readVarInt();
        int wePhase = buf.readByte();
        int wePoolTenths = buf.readVarInt();
        int weBerserkSec = buf.readVarInt();
        int weCdSec = buf.readVarInt();

        boolean hgActive = buf.readBoolean();
        int hgShieldTenths = buf.readVarInt();
        int hgPeakTenths = buf.readVarInt();
        int hgFinaleSec = buf.readVarInt();
        int hgCdSec = buf.readVarInt();

        boolean elActive = buf.readBoolean();
        int elActiveTargets = buf.readVarInt();
        int elMaxStoredTenths = buf.readVarInt();
        int elRemainSec = buf.readVarInt();

        int ssHoverCount = buf.readByte();
        int ssHoverSec = buf.readVarInt();
        int ssWaymarkCount = buf.readByte();
        int ssWaymarkSec = buf.readVarInt();
        String ssLinkedSword = buf.readUtf(64);

        this.data = new ChthollySyncData(
                weLevel, wePhase, wePoolTenths, weBerserkSec, weCdSec,
                hgActive, hgShieldTenths, hgPeakTenths, hgFinaleSec, hgCdSec,
                elActive, elActiveTargets, elMaxStoredTenths, elRemainSec,
                ssHoverCount, ssHoverSec, ssWaymarkCount, ssWaymarkSec, ssLinkedSword
        );
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(data.weLevel());
        buf.writeByte(data.wePhase());
        buf.writeVarInt(data.wePoolTenths());
        buf.writeVarInt(data.weBerserkSec());
        buf.writeVarInt(data.weCdSec());

        buf.writeBoolean(data.hgActive());
        buf.writeVarInt(data.hgShieldTenths());
        buf.writeVarInt(data.hgPeakTenths());
        buf.writeVarInt(data.hgFinaleSec());
        buf.writeVarInt(data.hgCdSec());

        buf.writeBoolean(data.elActive());
        buf.writeVarInt(data.elActiveTargets());
        buf.writeVarInt(data.elMaxStoredTenths());
        buf.writeVarInt(data.elRemainSec());

        buf.writeByte(data.ssHoverCount());
        buf.writeVarInt(data.ssHoverSec());
        buf.writeByte(data.ssWaymarkCount());
        buf.writeVarInt(data.ssWaymarkSec());
        String sword = data.ssLinkedSword();
        if (sword.length() > 64) {
            sword = sword.substring(0, 64);
        }
        buf.writeUtf(sword, 64);
    }

    public static void handle(ChthollyHudSyncPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> ChthollySyncData.setClient(msg.data));
        ctx.setPacketHandled(true);
    }
}

