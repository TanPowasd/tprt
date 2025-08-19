package org.mylove.tprt.Entities;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import org.mylove.tprt.registries.ModEntities;

public class FlyingSword extends Entity {

    public FlyingSword(EntityType<?> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }
    public FlyingSword(Level pLevel){
        this(ModEntities.FLYING_SWORD.get(), pLevel);
        // set all things
    }

    @Override
    public void tick() {
        super.tick();
    }

    @Override
    protected void defineSynchedData() {

    }

    @Override
    protected void readAdditionalSaveData(CompoundTag pCompound) {

    }

    @Override
    protected void addAdditionalSaveData(CompoundTag pCompound) {

    }
}
