package org.mylove.tprt.compat.Cataclysm.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.SpectralArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;

/** Phantom appearance with vanilla/TC arrow damage and impact handling, not Cataclysm's fixed damage. */
public class TprtPhantomArrow extends Arrow {
    private int spectralDuration;

    public TprtPhantomArrow(EntityType<? extends TprtPhantomArrow> type, Level level) {
        super(type, level);
    }

    public void copySpectralEffect(net.minecraft.world.entity.projectile.AbstractArrow original, CompoundTag data) {
        if (original instanceof SpectralArrow) {
            spectralDuration = data.contains("Duration") ? data.getInt("Duration") : 200;
        }
    }

    @Override
    protected void doPostHurtEffects(LivingEntity target) {
        super.doPostHurtEffects(target);
        if (spectralDuration > 0) {
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, spectralDuration, 0), getEffectSource());
        }
    }

    @Override
    protected ItemStack getPickupItem() {
        return spectralDuration > 0 ? new ItemStack(Items.SPECTRAL_ARROW) : super.getPickupItem();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("TprtSpectralDuration", spectralDuration);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        spectralDuration = tag.getInt("TprtSpectralDuration");
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
