package org.mylove.tprt.compat.IronsSpellBooks.entity;

import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.entity.mobs.IMagicSummon;
import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMob;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 奥法回响的临时施法者: 由玩家身上的法术书生成, 代替玩家重放记录的法术。
 * 独立实体有自己的施法状态, 因此多个法术不会互相覆盖, 玩家也不会被锁定动作。
 */
public class EchoCaster extends AbstractSpellCastingMob implements IMagicSummon {

    public static final int MAX_LIFE = 400;
    public static final int CAST_INTERVAL = 10;

    private static final EntityDataAccessor<Optional<UUID>> OWNER_UUID =
            SynchedEntityData.defineId(EchoCaster.class, EntityDataSerializers.OPTIONAL_UUID);

    public record Queued(AbstractSpell spell, int level) {
    }

    private final List<Queued> queue = new ArrayList<>();
    private int life;
    private int timer;
    /** 生成后的初始延迟, 用于让多个施法者错开出手 */
    private int delayTicks;
    /** 队列已空后等待施法结束的计时 */
    private int idle;
    private LivingEntity cachedSummoner;

    public EchoCaster(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setInvisible(true);
        this.setNoGravity(true);
        this.noPhysics = true;   // 彻底不参与碰撞, 避免挤压玩家
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(OWNER_UUID, Optional.empty());
    }

    /** 实体属性 (必须在 EntityAttributeCreationEvent 里注册, 否则构造时 NPE) */
    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0D)
                .add(Attributes.ATTACK_DAMAGE, 1.0D)
                .add(Attributes.ATTACK_SPEED, 4.0D)
                .add(Attributes.ARMOR, 0.0D)
                .add(Attributes.ARMOR_TOUGHNESS, 0.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    public void setDelay(int ticks) {
        this.delayTicks = Math.max(0, ticks);
    }

    public void setQueue(List<Queued> spells, LivingEntity caster) {
        this.queue.addAll(spells);
        this.setSummoner(caster);
    }

    /** 复制施法者的属性, 避免实体用法术强度 0 施法 */
    public void copyAttributesFrom(LivingEntity source) {
        for (var attribute : net.minecraftforge.registries.ForgeRegistries.ATTRIBUTES.getValues()) {
            var src = source.getAttribute(attribute);
            var dst = this.getAttribute(attribute);
            if (src == null || dst == null) {
                continue;
            }
            dst.setBaseValue(src.getBaseValue());
            for (var modifier : src.getModifiers()) {
                dst.removeModifier(modifier.getId());
                dst.addTransientModifier(modifier);
            }
        }
    }

    public void tryCastSpell(AbstractSpell spell, int spellLevel) {
        this.initiateCastSpell(spell, spellLevel);
        // 关键: 把施法状态同步给客户端, 否则客户端看不到法术
        this.getMagicData().getSyncedData().doSync();
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            return;
        }
        this.life++;
        if (this.life > MAX_LIFE) {
            this.discard();
            return;
        }
        if (!this.queue.isEmpty()) {
            this.idle = 0;
            if (this.delayTicks > 0) {
                this.delayTicks--;
                return;
            }
            if (++this.timer >= CAST_INTERVAL) {
                this.timer = 0;
                Queued next = this.queue.remove(0);
                this.tryCastSpell(next.spell(), next.level());
            }
            return;
        }
        // 队列放完: 等当前施法(尤其是引导型)结束再消失
        if (this.isCasting()) {
            this.idle = 0;
            return;
        }
        if (++this.idle > 40) {
            this.discard();
        }
    }
    public void setSummoner(@Nullable LivingEntity owner) {
        if (owner != null) {
            this.entityData.set(OWNER_UUID, Optional.of(owner.getUUID()));
            this.cachedSummoner = owner;
        }
    }

    public LivingEntity getSummoner() {
        UUID uuid = this.entityData.get(OWNER_UUID).orElse(null);
        return io.redspace.ironsspellbooks.util.OwnerHelper.getAndCacheOwner(level(), cachedSummoner, uuid);
    }

    @Nullable
    public UUID getSummonerUUID() {
        return this.entityData.get(OWNER_UUID).orElse(null);
    }

    @Override
    public void onUnSummon() {
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        this.entityData.get(OWNER_UUID).ifPresent(uuid ->
                io.redspace.ironsspellbooks.util.OwnerHelper.serializeOwner(tag, uuid));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        UUID uuid = io.redspace.ironsspellbooks.util.OwnerHelper.deserializeOwner(tag);
        if (uuid != null) {
            this.entityData.set(OWNER_UUID, Optional.of(uuid));
        }
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public boolean canBeHitByProjectile() {
        return false;
    }
    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isInvisible() {
        return true;
    }

    @Override
    public boolean shouldShowName() {
        return false;
    }
    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public float getHealth() {
        return this.getMaxHealth();
    }

    /**
     * 让召唤者(以及其盟友)始终被视为友方。
     * 否则实体施放的范围法术会把玩家自己也打进去。
     */
    @Override
    public boolean isAlliedTo(net.minecraft.world.entity.Entity entity) {
        if (entity == this) {
            return true;
        }
        LivingEntity summoner = this.getSummoner();
        if (summoner != null) {
            if (entity == summoner) {
                return true;
            }
            if (entity instanceof LivingEntity living && living.isAlliedTo(summoner)) {
                return true;
            }
        }
        return super.isAlliedTo(entity);
    }
    @Override
    public boolean isNoGravity() {
        return true;
    }
}