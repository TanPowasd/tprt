package org.mylove.tprt.entity.tec;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.PushReaction;
import org.jetbrains.annotations.NotNull;

public class TecEntity extends Entity {
    protected static final EntityDataAccessor<Integer> DATA_REMAIN_TICK = SynchedEntityData.defineId(TecEntity.class, EntityDataSerializers.INT);

    public TecEntity(EntityType<?> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    public final boolean noPhysics = true;
    public final boolean invulnerable = true;
    public boolean isAttackable() {
        return false;
    }
    public boolean mayInteract(Level pLevel, BlockPos pPos) {
        return false;
    }
    public boolean skipAttackInteraction(Entity pEntity) {
        return true;
    }
    public boolean isIgnoringBlockTriggers() {
        return true;
    }
    public boolean canChangeDimensions() {
        return false;
    }
    public @NotNull PushReaction getPistonPushReaction() {
        return PushReaction.IGNORE;
    }
    public boolean isInvulnerable() {
        return true;
    }
    public boolean ignoreExplosion() {
        return true;
    }
    public boolean fireImmune() {
        return true;
    }
//    public boolean isSilent() {
//        return true;
//    }


    @Override
    public void tick() {
        int life = getDataRemainTick();
        if (life <= 0) discard0();
        else {
            setDataRemainTick(life - 1);
        }
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(DATA_REMAIN_TICK, 0);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag pCompound) {}

    @Override
    protected void addAdditionalSaveData(CompoundTag pCompound) {}

    public int getDataRemainTick() {
        return entityData.get(DATA_REMAIN_TICK);
    }

    public void setDataRemainTick(int i) {
        entityData.set(DATA_REMAIN_TICK, i);
    }

    protected void discard0() {
        if (!level().isClientSide) {
            kill();
        }
    }
}
