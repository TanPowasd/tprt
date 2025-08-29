package org.mylove.tprt.ModSpells.thunder_att;

import io.redspace.ironsspellbooks.entity.spells.AbstractMagicProjectile;
import io.redspace.ironsspellbooks.registries.EntityRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.Optional;


public class thunder_att_MobCreate extends AbstractMagicProjectile {
    //未注册
    private static final EntityDataAccessor<Float> DATA_Z_ROT= SynchedEntityData.defineId(thunder_att_MobCreate.class,EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float>DATA_SCALE = SynchedEntityData.defineId(thunder_att_MobCreate.class,EntityDataSerializers.FLOAT);
    public thunder_att_MobCreate(EntityType<? extends Projectile> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.setNoGravity(true);
    }
    public thunder_att_MobCreate(Level levelIn, LivingEntity shooter){
        super(EntityRegistry.LIGHTNING_LANCE_PROJECTILE.get(),levelIn);
        setOwner(shooter);

    }
    public void setZRot(float zRot){
        if(!level().isClientSide()){
            entityData.set(DATA_Z_ROT,zRot);
        }
    }
    public void setScale(float scale){
        if(!level().isClientSide()){
            entityData.set(DATA_SCALE,scale);
        }
    }
    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_Z_ROT, 0.0F);
        this.entityData.define(DATA_SCALE, 1.0F);
       // super.defineSynchedData(pSynchedEntityData);
    }
    float getZRot(){
        return this.entityData.get(DATA_Z_ROT);
    }
    float getScale(){
        return this.entityData.get(DATA_SCALE);
    }
    @Override
    protected void addAdditionalSaveData(CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        pCompound.putFloat("zRot",getZRot());
        if(getScale()!=1.0F){
            pCompound.putFloat("scale",getScale());
        }
    }
    @Override
    protected void readAdditionalSaveData(CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        setZRot(pCompound.getFloat("zRot"));
        if(pCompound.contains("scale")){
            setScale(pCompound.getFloat("scale"));
        }
    }
    @Override
    protected void onHitEntity(EntityHitResult pResult) {
        super.onHitEntity(pResult);
        applyDamage( pResult.getEntity(),getDamage(),pResult.getEntity().damageSources().magic());
    }

    public boolean applyDamage(Entity pTarget, float pDamage, DamageSource pSource) {
        if(pTarget instanceof LivingEntity livingEntity){
            var flag=livingEntity.hurt(pSource,pDamage);
            return flag;
        }else{
            return pTarget.hurt(pSource,pDamage);
        }
    }

    @Override
    protected void onHit(HitResult pResult) {
        super.onHit(pResult);
        discard();
    }

    private static int soundTimestamp;

    @Override
    public void trailParticles() {
        RandomSource random =RandomSource.createThreadSafe();
    }

    @Override
    public void impactParticles(double pX, double pY, double pZ) {

    }
    @Override
    public float getSpeed(){
        return 2.5F;
    }
    @Override
    public Optional<SoundEvent> getImpactSound() {
        return Optional.empty();
    }
}
