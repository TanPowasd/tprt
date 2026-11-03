package org.mylove.tprt.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import org.mylove.tprt.client.SoulArrowVisualClient;

import java.util.UUID;
import java.util.function.Supplier;

/** Entity id plus UUID prevents applying a delayed visual packet to a reused entity id. */
public record SoulArrowVisualPacket(int entityId, UUID uuid) {
    public SoulArrowVisualPacket(AbstractArrow arrow) {
        this(arrow.getId(), arrow.getUUID());
    }

    public SoulArrowVisualPacket(FriendlyByteBuf buffer) {
        this(buffer.readVarInt(), buffer.readUUID());
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeVarInt(entityId);
        buffer.writeUUID(uuid);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> SoulArrowVisualClient.accept(this));
        return true;
    }
}
