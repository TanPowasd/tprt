package org.mylove.tprt.entity.tec;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.mylove.tprt.registries.ModEntities;
import org.mylove.tprt.tags.cinder_slash;

/// 因服务端不能定向发出粒子, 而匠魂的攻击钩子又只执行在服务端, 我们引入经典的“技术性实体”
/// 09/17 写一个带客户端level的钩子是更好的选择
public class CinderParticle extends TecEntity {
    protected static final EntityDataAccessor<Float> DATA_SPEED = SynchedEntityData.defineId(CinderParticle.class, EntityDataSerializers.FLOAT);
    protected static final EntityDataAccessor<Vector3f> DATA_FIRE_VEC3_TARGET = SynchedEntityData.defineId(CinderParticle.class, EntityDataSerializers.VECTOR3);
    protected static final EntityDataAccessor<Vector3f> DATA_FIRE_VEC3_VERTICAL = SynchedEntityData.defineId(CinderParticle.class, EntityDataSerializers.VECTOR3);

    public static final double splashDegree = cinder_slash.splashDegree;

    private ParticleProvider particleProvider;

    public CinderParticle(EntityType<?> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }
    public CinderParticle(Level level, int tickRemain, Vec3 target, Vec3 vertical, float speed) {
        this(ModEntities.CINDER_SLASH_PARTICLE.get(), level);

        setDataRemainTick(tickRemain);
        setDataSpeed(speed);
        setDataFireVec3Target(target);
        setDataFireVec3Vertical(vertical);
    }

    @Override
    public void tick() {
        System.out.println("CinderParticle" + tickCount + "   client?:" + level().isClientSide);
        //particleProvider.runPerTick(level(), tickCount, entityData.get(DATA_REMAIN_TICK)); //方法同步不到客户端
        doCinderParticle();
        super.tick();
    }

    private void doCinderParticle() {
        float speed = getDataSpeed();
        Vec3 target = getDataFireVec3Target();
        Vec3 ver = getDataFireVec3Vertical();

        int degree = (int) (splashDegree * 180 / Math.PI); //45
        for(int i=0; i<degree * 2; i++){
            float rad = (float) ((i - degree) * Math.PI / 180);
            //Vec3 vecApply = vecT.yRot(rad).scale(speed);
            Vec3 vecApply = target.add(ver.scale((double) (i - degree) / (degree * 2))).normalize().scale(speed);

            level().addParticle(
                    ParticleTypes.FLAME,
                    position().x,
                    position().y,
                    position().z,
                    vecApply.x,
                    vecApply.y,
                    vecApply.z
            );

        }
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(DATA_SPEED, 0f);
        entityData.define(DATA_FIRE_VEC3_TARGET, Vec3.ZERO.toVector3f());
        entityData.define(DATA_FIRE_VEC3_VERTICAL, Vec3.ZERO.toVector3f());
    }

    public Float getDataSpeed() {
        return entityData.get(DATA_SPEED);
    }

    public Vec3 getDataFireVec3Target() {
        return new Vec3(entityData.get(DATA_FIRE_VEC3_TARGET));
    }

    public Vec3 getDataFireVec3Vertical() {
        return new Vec3(entityData.get(DATA_FIRE_VEC3_VERTICAL));
    }

    public void setDataSpeed(float f) {
        entityData.set(DATA_SPEED, f);
    }

    public void setDataFireVec3Target(Vec3 v3) {
        entityData.set(DATA_FIRE_VEC3_TARGET, v3.toVector3f());
    }

    public void setDataFireVec3Vertical(Vec3 v3) {
        entityData.set(DATA_FIRE_VEC3_VERTICAL, v3.toVector3f());
    }
}
