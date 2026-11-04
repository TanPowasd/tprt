package org.mylove.tprt.compat.Cataclysm.entity;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

/** Sandstorm flight, retaining the vanilla/TC arrow collision and payload pipeline. */
public class TprtCursedSandstormArrow extends TprtPhantomArrow {
    public static final int MAX_LIFETIME = 300;
    private static final int FADE_TICKS = 10;
    private static final EntityDataAccessor<Integer> AGE = SynchedEntityData.defineId(TprtCursedSandstormArrow.class, EntityDataSerializers.INT);
    // -1 means flying; 0..10 is the authoritative, synchronized fade age.
    private static final EntityDataAccessor<Integer> FADE = SynchedEntityData.defineId(TprtCursedSandstormArrow.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> POWER_X = SynchedEntityData.defineId(TprtCursedSandstormArrow.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> POWER_Y = SynchedEntityData.defineId(TprtCursedSandstormArrow.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> POWER_Z = SynchedEntityData.defineId(TprtCursedSandstormArrow.class, EntityDataSerializers.FLOAT);
    private Vec3 power = Vec3.ZERO;
    private Vec3 fadePosition;
    private UUID targetId;
    private boolean initialized;
    private double launchSpeed;

    public TprtCursedSandstormArrow(EntityType<? extends TprtCursedSandstormArrow> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(AGE, 0);
        entityData.define(FADE, -1);
        entityData.define(POWER_X, 0.0F);
        entityData.define(POWER_Y, 0.0F);
        entityData.define(POWER_Z, 0.0F);
    }

    /** May be called after launch motion/owner are assigned, before spawning. Never rescans. */
    public void initializeFlight() {
        if (initialized || level().isClientSide) return;
        initialized = true;
        // Side shots inherit the original damage-reference speed through saved StormLaunchSpeed.
        if (launchSpeed <= 0.0) launchSpeed = getDeltaMovement().length();
        Vec3 direction = getDeltaMovement().normalize();
        setDeltaMovement(direction.scale(0.2));
        power = direction.scale(0.1);
        syncPower();
        Entity owner = getOwner();
        if (!(owner instanceof LivingEntity living)) return;
        Vec3 start = living.getEyePosition();
        Vec3 end = start.add(living.getLookAngle().scale(40.0));
        end = level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, living)).getLocation();
        double closest = start.distanceToSqr(end);
        for (LivingEntity candidate : level().getEntitiesOfClass(LivingEntity.class,
                living.getBoundingBox().expandTowards(end.subtract(start)).inflate(2.0),
                e -> e != living && e.isAlive() && !e.isSpectator() && !living.isAlliedTo(e) && !e.isAlliedTo(living))) {
            // Native-sized aim assistance: the crosshair need not intersect the exact hitbox.
            Vec3 intersection = candidate.getBoundingBox().inflate(1.0).clip(start, end).orElse(null);
            Vec3 aim = candidate.getBoundingBox().getCenter();
            boolean visible = level().clip(new ClipContext(start, aim, ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE, living)).getType() == net.minecraft.world.phys.HitResult.Type.MISS;
            if (visible && intersection != null && start.distanceToSqr(intersection) < closest) {
                closest = start.distanceToSqr(intersection);
                targetId = candidate.getUUID();
            }
        }
    }

    @Override
    public void tick() {
        setNoGravity(true);
        if (entityData.get(FADE) >= 0) {
            // No AbstractArrow collision/pickup/TC tick hooks during fade: this is inert visual state.
            setDeltaMovement(Vec3.ZERO);
            baseTick();
            if (!level().isClientSide) {
                int fade = entityData.get(FADE) + 1;
                entityData.set(FADE, fade);
                if (fade >= FADE_TICKS) discard();
            }
            return;
        }
        initializeFlight();
        if (!level().isClientSide) {
            entityData.set(AGE, entityData.get(AGE) + 1);
            if (entityData.get(AGE) >= MAX_LIFETIME - FADE_TICKS) {
                beginFade();
                return;
            }
            if (entityData.get(AGE) > 15 && targetId != null && level() instanceof ServerLevel server) {
                Entity target = server.getEntity(targetId);
                Entity owner = getOwner();
                if (target instanceof LivingEntity living && living.isAlive() && !living.isSpectator()
                        && (owner == null || (!owner.isAlliedTo(living) && !living.isAlliedTo(owner)))) {
                    Vec3 delta = new Vec3(living.getX(), living.getY() + living.getBbHeight() * 0.3, living.getZ()).subtract(position());
                    double distanceSquared = Math.max(delta.lengthSqr(), 1.0E-6);
                    power = new Vec3((float) Mth.clamp(power.x + delta.x * 3.0 / distanceSquared, -0.075, 0.075),
                            (float) Mth.clamp(power.y + delta.y * 3.0 / distanceSquared, -0.075, 0.075),
                            (float) Mth.clamp(power.z + delta.z * 3.0 / distanceSquared, -0.075, 0.075));
                }
            }
        }
        if (level().isClientSide) {
            power = new Vec3(entityData.get(POWER_X), entityData.get(POWER_Y), entityData.get(POWER_Z));
        } else {
            entityData.set(POWER_X, (float) power.x);
            entityData.set(POWER_Y, (float) power.y);
            entityData.set(POWER_Z, (float) power.z);
        }
        limitFlightSpeed(); // also bound velocities supplied by external steering before movement
        super.tick();
        if (isRemoved()) return;
        if (entityData.get(FADE) >= 0) {
            if (fadePosition != null) setPos(fadePosition);
            setDeltaMovement(Vec3.ZERO);
            return;
        }
        if (inGround) {
            if (!level().isClientSide) beginFade();
            return;
        }
        // Transform the actual POST-hit velocity, never restore pre-hit velocity. AbstractArrow
        // applied water/air drag already; undo only that drag, then apply storm acceleration/inertia.
        double vanillaDrag = isInWater() ? getWaterInertia() : (double) 0.99F;
        if (vanillaDrag > 0.0) setDeltaMovement(getDeltaMovement().scale(1.0 / vanillaDrag).add(power).scale(0.9));
        limitFlightSpeed();
    }

    private void limitFlightSpeed() {
        Vec3 motion = getDeltaMovement();
        if (motion.lengthSqr() > 0.9 * 0.9) setDeltaMovement(motion.normalize().scale(0.9));
    }

    private void syncPower() {
        entityData.set(POWER_X, (float) power.x);
        entityData.set(POWER_Y, (float) power.y);
        entityData.set(POWER_Z, (float) power.z);
    }

    private void beginFade() {
        if (isRemoved() || entityData.get(FADE) >= 0) return;
        entityData.set(FADE, 0);
        fadePosition = position();
        setDeltaMovement(Vec3.ZERO);
    }

    @Override
    public void playerTouch(net.minecraft.world.entity.player.Player player) {
        if (entityData.get(FADE) < 0) super.playerTouch(player);
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        return entityData.get(FADE) < 0 && super.canHitEntity(entity);
    }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        if (entityData.get(FADE) >= 0) return;
        // Forge/TC impact listeners have already run. Compensate only inside the damage
        // path, keeping their baseDamage changes; bound the ratio to avoid stall explosions.
        double speed = getDeltaMovement().length();
        double factor = launchSpeed > 1.0E-6 && Double.isFinite(speed)
                ? Mth.clamp(launchSpeed / Math.max(speed, 1.0E-6), 1.0 / 16.0, 16.0) : 1.0;
        setBaseDamage(getBaseDamage() * factor);
        try {
            super.onHitEntity(hit);
        } finally {
            setBaseDamage(getBaseDamage() / factor);
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult hit) {
        if (entityData.get(FADE) >= 0) return;
        super.onHitBlock(hit);
        if (!level().isClientSide && !isRemoved()) {
            // Anchor just outside the actual impact, not the tick's pre-impact position.
            Vec3 backoff = getDeltaMovement().normalize().scale(0.05);
            setPos(hit.getLocation().subtract(backoff));
            beginFade();
        }
    }

    @Override
    protected void doPostHurtEffects(LivingEntity target) {
        super.doPostHurtEffects(target);
        // Verified against Cataclysm ModEffect's DeferredRegister using javap (7849424 JAR).
        MobEffect curse = ForgeRegistries.MOB_EFFECTS.getValue(new ResourceLocation("cataclysm", "curse_of_desert"));
        if (curse != null) target.addEffect(new MobEffectInstance(curse, 100, 1), getEffectSource());
    }

    public float getVisualAge(float partialTick) {
        return entityData.get(AGE) + partialTick;
    }

    /** Negative while flying; otherwise synchronized elapsed despawn animation ticks. */
    public float getFadeAge(float partialTick) {
        int fade = entityData.get(FADE);
        return fade < 0 ? -1.0F : Mth.clamp(fade + partialTick, 0.0F, FADE_TICKS);
    }

    public float getVisualScale(float partialTick) {
        int fade = entityData.get(FADE);
        return fade >= 0 ? Mth.clamp(1.0F - (fade + partialTick) / FADE_TICKS, 0.0F, 1.0F)
                : Mth.clamp((entityData.get(AGE) + partialTick) / 5.0F, 0.0F, 1.0F);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("StormAge", entityData.get(AGE));
        tag.putInt("StormFade", entityData.get(FADE));
        tag.putBoolean("StormInitialized", initialized);
        tag.putDouble("StormLaunchSpeed", launchSpeed);
        tag.putDouble("StormPowerX", power.x);
        tag.putDouble("StormPowerY", power.y);
        tag.putDouble("StormPowerZ", power.z);
        if (targetId != null) tag.putUUID("StormTarget", targetId);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(AGE, Mth.clamp(tag.getInt("StormAge"), 0, MAX_LIFETIME));
        entityData.set(FADE, tag.contains("StormFade") ? Mth.clamp(tag.getInt("StormFade"), -1, FADE_TICKS) : -1);
        initialized = tag.getBoolean("StormInitialized");
        launchSpeed = tag.getDouble("StormLaunchSpeed");
        if (!Double.isFinite(launchSpeed) || launchSpeed < 0) launchSpeed = 0;
        double x = tag.getDouble("StormPowerX"), y = tag.getDouble("StormPowerY"), z = tag.getDouble("StormPowerZ");
        power = Double.isFinite(x) && Double.isFinite(y) && Double.isFinite(z) ? new Vec3(x, y, z) : Vec3.ZERO;
        syncPower();
        targetId = tag.hasUUID("StormTarget") ? tag.getUUID("StormTarget") : null;
        if (entityData.get(FADE) >= 0) {
            fadePosition = position();
            setDeltaMovement(Vec3.ZERO);
        }
    }
}
