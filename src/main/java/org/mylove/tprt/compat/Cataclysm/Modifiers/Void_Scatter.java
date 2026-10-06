package org.mylove.tprt.compat.Cataclysm.Modifiers;

import com.github.L_Ender.cataclysm.entity.projectile.Void_Shard_Entity;
import com.github.L_Ender.cataclysm.init.ModItems;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import org.jetbrains.annotations.Nullable;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.entity.ProjectileWithPower;
import slimeknights.tconstruct.library.modifiers.hook.ranged.ProjectileHitModifierHook;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;
import slimeknights.tconstruct.library.tools.nbt.ModifierNBT;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 虚空散射 (void_scatter)
 * <p>
 * 做成灾变「虚空散射箭」({@code Void_Scatter_Arrow_Entity}) 的效果: 箭矢命中实体或方块时当场碎裂,
 * 从命中点朝四周炸出 17 片虚空碎片 ({@link Void_Shard_Entity}), 并伴随玻璃碎裂音效与物品碎屑粒子。
 * <p>
 * 与灾变本体的对齐点 (照抄其 {@code onHit} / {@code getShootVectors} / {@code mulPoseVector}):
 * <ul>
 *     <li>碎片方向是 17 个黄金角斐波那契球分布点, 额外 +Y 再乘 0.5, 然后按命中面的法线做坐标置换
 *         (命中生物时按 {@link Direction#UP});</li>
 *     <li>碎片初速 = 方向 × 0.35, 生成位置在命中点沿该方向偏移一点 (Y 再 +0.25);</li>
 *     <li>命中生物时把该生物设为碎片的 ignoreEntity, 免得碎片立刻回头打同一个目标;</li>
 *     <li>音效 {@code GLASS_BREAK} (音量 1.1 / 音调 0.8), 以及 8 个虚空散射箭的物品碎屑粒子。</li>
 * </ul>
 * <b>唯一的差别</b>: 灾变里碎片伤害是写死的 1.5, 这里改成 <b>箭矢伤害的 25%</b>
 * (箭矢伤害用匠魂自己的 {@link ProjectileWithPower#getDamage(Projectile)}, 它等价于原版箭矢伤害公式)。
 */
public class Void_Scatter extends Modifier implements ProjectileHitModifierHook {

    /* ===================== 数值 ===================== */

    /** 碎裂产生的虚空碎片数量 (与灾变一致) */
    private static final int SHARD_COUNT = 17;
    /** 碎片初速缩放 (与灾变一致) */
    private static final double SHARD_SPEED_SCALE = 0.35D;
    /** 碎片伤害 = 箭矢伤害 × 25% */
    private static final float SHARD_DAMAGE_RATIO = 0.15F;
    /** 碎裂特效的粒子数量 (与灾变一致) */
    private static final int SHATTER_PARTICLES = 8;
    /** 碎片伤害记录的最长保留时间 (tick), 只作兜底清理 */
    private static final long SHARD_RECORD_LIFETIME = 200L;
    /** 碎裂后是否让投掷物消失: 灾变本体命中后会 discard, 这里默认保留, 免得影响箭矢回收/穿透 */
    private static final boolean CONSUME_PROJECTILE = true;

    /** 我们生成的虚空碎片 → 伤害改写值 */
    private static final Map<UUID, ShardDamage> SHARDS = new HashMap<>();

    /** 碎片伤害记录 */
    private record ShardDamage(float damage, long expireAt) {}

    static {
        // 只在类首次加载时注册一次; 显式给出事件类型, 不依赖 lambda 的泛型推导。
        // 灾变里碎片的伤害写死在 Void_Shard_Entity#onHitEntity (1.5), 没有 setter, 只能在这里拦。
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, LivingDamageEvent.class, Void_Scatter::onLivingDamage);
    }

    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        super.registerHooks(hookBuilder);
        hookBuilder.addHook(this, ModifierHooks.PROJECTILE_HIT);
    }

    /* ============================================================
     *  命中: 碎裂
     * ============================================================ */

    @Override
    public boolean onProjectileHitEntity(ModifierNBT modifiers, ModDataNBT persistentData, ModifierEntry modifier, Projectile projectile, EntityHitResult hit, @Nullable LivingEntity attacker, @Nullable LivingEntity target, boolean notBlocked) {
        // 与灾变本体一致: 命中生物时用 UP 作为碎裂朝向
        shatter(projectile, attacker, hit.getLocation(), Direction.UP, target);
        // 返回 false = 不取消这次命中, 箭矢照常结算伤害/插在方块上
        return false;
    }

    @Override
    public boolean onProjectileHitsBlock(ModifierNBT modifiers, ModDataNBT persistentData, ModifierEntry modifier, Projectile projectile, BlockHitResult hit, @Nullable LivingEntity owner) {
        // 与灾变本体一致: 命中方块时按命中面的法线确定碎裂朝向
        shatter(projectile, owner, hit.getLocation(), hit.getDirection(), null);
        return false;
    }

    /** 在命中点碎裂出一圈虚空碎片 */
    private static void shatter(Projectile projectile, @Nullable LivingEntity owner, Vec3 pos, Direction direction, @Nullable Entity ignore) {
        Level level = projectile.level();
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        // 碎片伤害 = 箭矢伤害 × 25%
        float projectileDamage = ProjectileWithPower.getDamage(projectile);
        if (projectileDamage <= 0.0F && owner != null) {
            // 极少数匠魂没有覆盖的投掷物: 用发射者攻击力兜底, 免得碎片变成 0 伤害
            projectileDamage = (float) owner.getAttributeValue(Attributes.ATTACK_DAMAGE);
        }
        float shardDamage = projectileDamage * SHARD_DAMAGE_RATIO + 1.5f;

        long now = serverLevel.getGameTime();
        SHARDS.entrySet().removeIf(entry -> entry.getValue().expireAt() < now);

        // 朝四周炸出碎片
        for (Vec3 vector : getShootVectors(level.getRandom())) {
            Vec3 velocity = mulPoseVector(vector.scale(SHARD_SPEED_SCALE), direction);
            Void_Shard_Entity shard = new Void_Shard_Entity(level, owner,
                    pos.x + velocity.x, pos.y + velocity.y + 0.25D, pos.z + velocity.z,
                    velocity, ignore);
            if (level.addFreshEntity(shard)) {
                SHARDS.put(shard.getUUID(), new ShardDamage(shardDamage, now + SHARD_RECORD_LIFETIME));
            }
        }

        // 碎裂表现: 玻璃碎裂音效 + 虚空散射箭的物品碎屑
        level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.GLASS_BREAK, SoundSource.NEUTRAL, 1.1F, 0.8F);
        ItemParticleOption debris = new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(ModItems.VOID_SCATTER_ARROW.get()));
        RandomSource random = level.getRandom();
        for (int i = 0; i < SHATTER_PARTICLES; i++) {
            // count = 0 时偏移量被客户端当作初速度使用
            serverLevel.sendParticles(debris, pos.x, pos.y, pos.z, 0,
                    random.nextGaussian() * 0.1D, random.nextDouble() * 0.15D, random.nextGaussian() * 0.1D, 0.0D);
        }

        if (CONSUME_PROJECTILE) {
            projectile.discard();
        }
    }

    /**
     * 与灾变 {@code Void_Scatter_Arrow_Entity#getShootVectors} 一致的碎裂方向:
     * 黄金角斐波那契球上的 {@link #SHARD_COUNT} 个点, 第一颗额外 +Y, 最后整体乘 0.5。
     */
    private static List<Vec3> getShootVectors(RandomSource random) {
        List<Vec3> vectors = new ArrayList<>(SHARD_COUNT);
        float golden = (1.0F + Mth.sqrt(5.0F)) / 2.0F;
        double cap = 0.8D;
        for (int i = 1; i <= SHARD_COUNT; i++) {
            float t = (float) i / (float) SHARD_COUNT;
            float theta = (float) Math.acos(1.0D - cap * (double) t);
            float phi = (float) (Math.PI * 2.0D * (random.nextFloat() + golden * i));
            double sinTheta = Math.sin(theta);
            Vec3 vector = new Vec3(sinTheta * Math.cos(phi), sinTheta * Math.sin(phi), Math.cos(theta));
            if (i == 1) {
                vector = vector.add(0.0D, 1.0D, 0.0D);
            }
            vectors.add(vector.scale(0.5D));
        }
        return vectors;
    }

    /** 与灾变 {@code mulPoseVector} 一致: 把以 +X 为轴的碎片分布按命中面法线做坐标置换 */
    private static Vec3 mulPoseVector(Vec3 vec, Direction direction) {
        return switch (direction) {
            case UP -> vec;
            case DOWN -> vec.multiply(1.0D, -1.0D, 1.0D);
            case NORTH -> new Vec3(vec.z, vec.x, -vec.y);
            case SOUTH -> new Vec3(vec.z, vec.x, vec.y);
            case WEST -> new Vec3(-vec.y, vec.z, vec.x);
            case EAST -> new Vec3(vec.y, vec.z, vec.x);
        };
    }

    /* ============================================================
     *  碎片伤害改写
     * ============================================================ */

    /** 把我们生成的虚空碎片的伤害改成“箭矢伤害的 25%” */
    private static void onLivingDamage(LivingDamageEvent event) {
        Entity direct = event.getSource().getDirectEntity();
        if (direct == null) {
            return;
        }
        ShardDamage override = SHARDS.get(direct.getUUID());
        if (override != null) {
            // LivingDamageEvent 拿到的是护甲/抗性结算完的最终伤害, 直接覆盖即可
            event.setAmount(override.damage());
        }
    }
}
