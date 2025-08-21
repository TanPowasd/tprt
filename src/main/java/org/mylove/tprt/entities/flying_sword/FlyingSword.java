package org.mylove.tprt.entities.flying_sword;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import org.mylove.tprt.registries.ModEntities;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

public class FlyingSword extends Entity implements GeoEntity {
    private int age;
    private int health = 5;
    public int value;
    private int count = 1;
    private static final EntityDataAccessor<Integer> COUNTER =
            SynchedEntityData.defineId(FlyingSword.class, EntityDataSerializers.INT);


    protected static final RawAnimation FLY_ANIM = RawAnimation.begin().thenLoop("move.fly");
    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    public FlyingSword(EntityType<FlyingSword> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }
    public FlyingSword(Level pLevel){
        this(ModEntities.FLYING_SWORD.get(), pLevel);
        // set all things
    }


    public boolean isAttackable() {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(COUNTER, 0);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag pCompound) {
        this.health = pCompound.getShort("Health");
        this.age = pCompound.getShort("Age");
        this.value = pCompound.getShort("Value");
        this.count = Math.max(pCompound.getInt("Count"), 1);

    }

    @Override
    protected void addAdditionalSaveData(CompoundTag pCompound) {
        pCompound.putShort("Health", (short)this.health);
        pCompound.putShort("Age", (short)this.age);
        pCompound.putShort("Value", (short)this.value);
        pCompound.putInt("Count", this.count);
    }

    @Override
    public void registerControllers(final AnimatableManager.ControllerRegistrar controllers) {
//        controllers.add(new AnimationController<>(this, "Flying", 5, this::flyAnimController));
    }

//    protected <E extends FlyingSword> PlayState flyAnimController(final AnimationState<E> event) {
//        if (event.isMoving())
//            return event.setAndContinue(FLY_ANIM);
//
//        return PlayState.STOP;
//    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }

}
