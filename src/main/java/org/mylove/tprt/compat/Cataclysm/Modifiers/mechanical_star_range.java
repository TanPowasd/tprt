package org.mylove.tprt.compat.Cataclysm.Modifiers;

import com.github.L_Ender.cataclysm.entity.projectile.Wither_Homing_Missile_Entity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.EventPriority;
import slimeknights.mantle.client.TooltipKey;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.modifiers.hook.display.TooltipModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.InventoryTickModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.ranged.ProjectileLaunchModifierHook;
import slimeknights.tconstruct.library.modifiers.impl.NoLevelsModifier;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 机械之星 (远程): 凋零炮击。
 *
 * 通过匠魂发射钩子生效, 弓与弩都支持。
 * - 常态射击: 额外发射 (1 + 多重射击等级) 枚追踪凋零飞弹
 * - 潜行射击: 改为死亡激光 (自绘粒子光束, 实时跟随玩家视线)
 * - 红石充能: 每 0.5 秒 4 红石 -> +10 充能 (上限 160)
 */
public class mechanical_star_range extends NoLevelsModifier
        implements ProjectileLaunchModifierHook, InventoryTickModifierHook, TooltipModifierHook {

    public static final ModifierId ID = new ModifierId("tprt", "mechanical_star_range");
    public static final ModifierId MULTISHOT = new ModifierId("tconstruct", "multishot");

    /** 充能 */
    public static final ResourceLocation MECH_CHARGE = new ResourceLocation("tprt", "mech_charge");
    public static final int MAX_CHARGE = 160;
    public static final int CHARGE_PER_REDSTONE = 10;
    public static final int REDSTONE_PER_CHARGE = 4;
    /** 红石块充能量 */
    public static final int CHARGE_PER_BLOCK = 24;

    /** 消耗 */
    public static final int MISSILE_COST = 2;
    public static final int LASER_COST = 8;

    /** 系数 */
    public static final float MISSILE_RATIO = 3F;
    public static final float LASER_RATIO = 12F;
    public static final float LASER_PER_MULTISHOT = 0.25F;

    /** 激光 */
    public static final double LASER_RANGE = 30.0D;
    public static final int LASER_DURATION = 60;
    public static final int LASER_WINDUP = 20;
    public static final int LASER_COOLDOWN = 80;
    public static final long LASER_HIT_INTERVAL = 5L;
    public static final int LASER_HITS = 8;

    /** 索敌 */
    public static final double TARGET_RANGE = 16.0D;
    /** 多枚飞弹之间的发射间隔 (tick), 避免爆炸互相推开 */
    public static final int MISSILE_STAGGER = 3;
    /** 每多少点护甲值(含韧性) 额外 +1 枚飞弹 */
    public static final double ARMOR_PER_MISSILE = 24.0D;
    /** 护甲提供的飞弹数量上限 */
    public static final int MAX_ARMOR_MISSILES = 7;

    private static final Map<UUID, Long> LASER_CD = new HashMap<>();

    /** 弹体命中效果 */
    private record HitFx(int wither, long expire) {
    }

    private static final Map<UUID, HitFx> SHOTS = new HashMap<>();

    /** 待发射的追踪飞弹 */
    private record PendingMissile(Player owner, LivingEntity target, float damage, long fireAt) {
    }

    private static final List<PendingMissile> PENDING_MISSILES = new ArrayList<>();

    /** 进行中的激光 (自绘光束) */
    private record LaserBeam(net.minecraft.world.entity.Entity beam, LivingEntity caster, long startAt, long expire, float damagePerHit) {
    }

    private static final List<LaserBeam> LASER_BEAMS = new ArrayList<>();

    static {
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false,
                net.minecraftforge.event.entity.living.LivingHurtEvent.class, mechanical_star_range::onLivingHurt);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST, false,
                net.minecraftforge.event.entity.living.LivingAttackEvent.class, mechanical_star_range::onLivingAttack);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false,
                net.minecraftforge.event.TickEvent.LevelTickEvent.class, mechanical_star_range::onLevelTick);
    }

    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        super.registerHooks(hookBuilder);
        hookBuilder.addHook(this, ModifierHooks.PROJECTILE_LAUNCH, ModifierHooks.INVENTORY_TICK, ModifierHooks.TOOLTIP);
    }

    @Override
    public void onProjectileLaunch(IToolStackView tool, ModifierEntry modifier, LivingEntity shooter,
                                   Projectile projectile, AbstractArrow arrow, ModDataNBT persistentData, boolean primary) {
        if (!primary || !(shooter instanceof Player player) || player.level().isClientSide) {
            return;
        }
        ToolStack toolStack = tool instanceof ToolStack stack ? stack : ToolStack.from(shooter.getMainHandItem());
        int multishot = toolStack.getModifierLevel(MULTISHOT);
        // 伤害以"满蓄力"为基准: 弹射伤害 x 满蓄力初速度 (3.0 x 初速度属性)
        float base = (float) arrow.getBaseDamage();
        float velocityStat = slimeknights.tconstruct.library.modifiers.hook.build.ConditionalStatModifierHook
                .getModifiedStat(toolStack, player, slimeknights.tconstruct.library.tools.stat.ToolStats.VELOCITY);
        if (velocityStat <= 0.0F) {
            velocityStat = 1.0F;
        }
        float speed = 3.0F * velocityStat;
        float shotDamage = Math.max(1.0F, base * speed);

        // 潜行 -> 死亡激光
        if (player.isShiftKeyDown()) {
            long now = player.level().getGameTime();
            Long last = LASER_CD.get(player.getUUID());
            if (last != null && now >= last && now - last < LASER_COOLDOWN) {
                return;
            }
            if (!consumeCharge(toolStack, LASER_COST)) {
                return;
            }
            LASER_CD.put(player.getUUID(), now);
            float laserDamage = shotDamage * LASER_RATIO * (1.0F + LASER_PER_MULTISHOT * multishot);
            Vec3 eye = player.getEyePosition();
            player.level().playSound(null, eye.x, eye.y, eye.z,
                    com.github.L_Ender.cataclysm.init.ModSounds.HARBINGER_DEATHLASER_PREPARE.get(),
                    net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);
            var beam = new org.mylove.tprt.compat.Cataclysm.entity.TprtDeathLaser(
                    org.mylove.tprt.compat.Cataclysm.CataclysmCompat.TPRT_DEATH_LASER.get(),
                    player.level(), player, eye.x, eye.y, eye.z, player.getYRot(), player.getXRot(),
                    LASER_DURATION, 0.0F, 0.0F);
            beam.setPos(eye.x, eye.y, eye.z);
            player.level().addFreshEntity(beam);
            LASER_BEAMS.add(new LaserBeam(beam, player, now + LASER_WINDUP, now + LASER_DURATION,
                    laserDamage / LASER_HITS));
            arrow.discard();
            return;
        }

        // 常态: 追踪飞弹
        int armorMissiles = armorMissiles(player);
        int want = 1 + multishot + armorMissiles;
        List<LivingEntity> targets = findTargets(player, want);
        if (targets.isEmpty()) {
            return;
        }
        if (!consumeCharge(toolStack, MISSILE_COST * targets.size())) {
            return;
        }
        // 护甲带来的额外飞弹按比例分摊总伤害, 避免变成纯增伤
        float damage = shotDamage * MISSILE_RATIO / (1.0F + 0.5F * armorMissiles);
        long expire = player.level().getGameTime() + 200L;
        for (int i = 0; i < targets.size(); i++) {
            PENDING_MISSILES.add(new PendingMissile(player, targets.get(i), damage,
                    player.level().getGameTime() + (long) i * MISSILE_STAGGER));
        }
    }

    /** 护甲值与韧性换算的额外飞弹数 */
    private static int armorMissiles(Player player) {
        double armor = player.getArmorValue()
                + 2 * player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR_TOUGHNESS);
        int bonus = (int) Math.floor(armor / ARMOR_PER_MISSILE);
        return Math.max(0, Math.min(MAX_ARMOR_MISSILES, bonus));
    }

    /** 准心指向的实体 */
    private static LivingEntity crosshairTarget(Player player) {
        Vec3 eye = player.getEyePosition();
        Vec3 dir = player.getLookAngle().normalize();
        Vec3 end = eye.add(dir.scale(TARGET_RANGE));
        var hit = net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(
                player, eye, end, player.getBoundingBox().expandTowards(end.subtract(eye)).inflate(1.0D),
                e -> e instanceof LivingEntity le && le != player && le.isAlive() && le.canBeSeenAsEnemy(),
                eye.distanceToSqr(end));
        return hit != null && hit.getEntity() instanceof LivingEntity le ? le : null;
    }

    /** 每个目标最多锁定 4 枚飞弹 */
    public static final int MAX_MISSILES_PER_TARGET = 4;

    /** 优先准心目标, 其余按距离补足; 目标不足时允许最多 2 枚锁定同一目标 */
    private static List<LivingEntity> findTargets(Player player, int max) {
        List<LivingEntity> unique = new ArrayList<>();
        LivingEntity aimed = crosshairTarget(player);
        if (aimed != null) {
            unique.add(aimed);
        }
        AABB box = player.getBoundingBox().inflate(TARGET_RANGE);
        List<LivingEntity> near = new ArrayList<>(player.level().getEntitiesOfClass(LivingEntity.class, box,
                e -> e != player && e.isAlive() && e.canBeSeenAsEnemy()));
        near.sort(Comparator.comparingDouble(e -> e.distanceToSqr(player)));
        for (LivingEntity e : near) {
            if (!unique.contains(e)) {
                unique.add(e);
            }
        }
        List<LivingEntity> list = new ArrayList<>();
        for (int round = 0; round < MAX_MISSILES_PER_TARGET && list.size() < max; round++) {
            for (LivingEntity e : unique) {
                if (list.size() >= max) {
                    break;
                }
                list.add(e);
            }
        }
        return list;
    }

    private static boolean consumeCharge(IToolStackView tool, int cost) {
        var data = tool.getPersistentData();
        int charge = data.getInt(MECH_CHARGE);
        if (charge < cost) {
            return false;
        }
        data.putInt(MECH_CHARGE, charge - cost);
        return true;
    }

    @Override
    public void onInventoryTick(IToolStackView tool, ModifierEntry modifier, Level level, LivingEntity entity,
                                int itemSlot, boolean isSelected, boolean isCorrectSlot, ItemStack stack) {
        if (level.isClientSide || !(entity instanceof Player player)) {
            return;
        }
        if (level.getGameTime() % 10L != 0L) {
            return;
        }
        var data = tool.getPersistentData();
        int charge = data.getInt(MECH_CHARGE);
        // 优先红石块; 只在不会溢出时消耗
        if (charge <= MAX_CHARGE - CHARGE_PER_BLOCK && consumeRedstoneBlock(player)) {
            data.putInt(MECH_CHARGE, charge + CHARGE_PER_BLOCK);
        } else if (charge <= MAX_CHARGE - CHARGE_PER_REDSTONE && consumeRedstone(player, REDSTONE_PER_CHARGE)) {
            data.putInt(MECH_CHARGE, charge + CHARGE_PER_REDSTONE);
        }
        if (level.getGameTime() % 100L == 0L) {
            long now = level.getGameTime();
            SHOTS.values().removeIf(fx -> now > fx.expire());
        }
    }

    /** 消耗 1 个红石块 */
    private static boolean consumeRedstoneBlock(Player player) {
        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            var stack = inv.getItem(i);
            if (!stack.isEmpty() && stack.is(net.minecraft.world.item.Items.REDSTONE_BLOCK)) {
                stack.shrink(1);
                return true;
            }
        }
        return false;
    }

    private static boolean consumeRedstone(Player player, int count) {
        int need = count;
        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize() && need > 0; i++) {
            var stack = inv.getItem(i);
            if (!stack.isEmpty() && stack.is(net.minecraft.world.item.Items.REDSTONE)) {
                int take = Math.min(need, stack.getCount());
                stack.shrink(take);
                need -= take;
            }
        }
        return need == 0;
    }

    /** 激光: 每 tick 绘制光束, 每 5 tick 结算伤害 */
    private static void onLevelTick(net.minecraftforge.event.TickEvent.LevelTickEvent event) {
        if (event.phase != net.minecraftforge.event.TickEvent.Phase.END || event.level.isClientSide) {
            return;
        }
        if (!(event.level instanceof ServerLevel level)) {
            return;
        }
        long now = level.getGameTime();
        if (!PENDING_MISSILES.isEmpty()) {
            firePendingMissiles(level, now);
        }
        if (LASER_BEAMS.isEmpty()) {
            return;
        }
        List<LaserBeam> finished = new ArrayList<>();
        for (LaserBeam beam : LASER_BEAMS) {
            if (now > beam.expire() || !beam.caster().isAlive()) {
                finished.add(beam);
            }
        }
        for (LaserBeam beam : finished) {
            beam.beam().discard();
            LASER_BEAMS.remove(beam);
        }

        for (LaserBeam beam : LASER_BEAMS) {
            LivingEntity caster = beam.caster();
            Vec3 eye = caster.getEyePosition();
            Vec3 end = beamEnd(level, caster, eye);
            if (now < beam.startAt()) {
                Vec3 look = caster.getLookAngle();
                level.sendParticles(ParticleTypes.END_ROD,
                        eye.x + look.x * 0.9D, eye.y + look.y * 0.9D, eye.z + look.z * 0.9D,
                        2, 0.08D, 0.08D, 0.08D, 0.0D);
            } else {
                // 光束实体跟随眼睛 (朝向由玩家视线决定, 定身后视觉自然对齐)
                beam.beam().setPos(eye.x, eye.y, eye.z);
                if (now == beam.startAt()) {
                    level.playSound(null, eye.x, eye.y, eye.z,
                            org.mylove.tprt.registries.ModSounds.LASER_LOOP.get(),
                            net.minecraft.sounds.SoundSource.PLAYERS, 0.8F, 1.0F);
                }
            }
        }

        if (now % LASER_HIT_INTERVAL != 0L) {
            return;
        }
        for (LaserBeam beam : LASER_BEAMS) {
            if (now < beam.startAt()) {
                continue;
            }
            LivingEntity caster = beam.caster();
            Vec3 start = caster.getEyePosition();
            Vec3 end = beamEnd(level, caster, start);
            AABB box = new AABB(start, end).inflate(1.5D);
            for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, box,
                    e -> e.isAlive() && e != caster)) {
                if (distanceToSegment(victim.getBoundingBox().getCenter(), start, end)
                        > victim.getBbWidth() * 0.5D + 1.2D) {
                    continue;
                }
                victim.invulnerableTime = 0;
                victim.hurt(level.damageSources().indirectMagic(caster, caster), beam.damagePerHit());
                victim.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.WITHER, 60, 1));
            }
        }
    }

    /** 按队列错开发射飞弹 */
    private static void firePendingMissiles(ServerLevel level, long now) {
        var it = PENDING_MISSILES.iterator();
        while (it.hasNext()) {
            PendingMissile pending = it.next();
            if (now < pending.fireAt()) {
                continue;
            }
            it.remove();
            Player owner = pending.owner();
            if (!owner.isAlive() || owner.level() != level) {
                continue;
            }
            LivingEntity target = pending.target();
            if (target == null || !target.isAlive()) {
                List<LivingEntity> alt = findTargets(owner, 1);
                if (alt.isEmpty()) {
                    continue;
                }
                target = alt.get(0);
            }
            var missile = new Wither_Homing_Missile_Entity(owner, owner.getLookAngle(), level,
                    pending.damage(), target);
            missile.setPos(owner.getEyePosition());
            level.addFreshEntity(missile);
            SHOTS.put(missile.getUUID(), new HitFx(1, now + 200L));
        }
    }

    /** 光束终点 (遇方块截断) */
    private static Vec3 beamEnd(Level level, LivingEntity caster, Vec3 eye) {
        Vec3 target = eye.add(caster.getLookAngle().normalize().scale(LASER_RANGE));
        BlockHitResult hit = level.clip(new ClipContext(eye, target,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster));
        return hit.getType() == HitResult.Type.MISS ? target : hit.getLocation();
    }

    /** 沿光束撒粒子: 用灾变的闪电粒子做光束主体 */
    private static void drawBeam(ServerLevel level, Vec3 start, Vec3 end) {
        Vec3 delta = end.subtract(start);
        double length = delta.length();
        if (length < 1.0E-4D) {
            return;
        }
        int points = (int) (length / 0.5D);
        Vec3 step = delta.scale(1.0D / length * 0.5D);
        for (int i = 0; i <= points; i++) {
            Vec3 p = start.add(step.scale(i));
            level.sendParticles(new com.github.L_Ender.cataclysm.client.particle.Options.LightningParticleOptions(255, 30, 30),
                    p.x, p.y, p.z, 1, 0.03D, 0.03D, 0.03D, 0.0D);
            if ((i % 3) == 0) {
                level.sendParticles(ParticleTypes.END_ROD, p.x, p.y, p.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            }
        }
    }

    private static double distanceToSegment(Vec3 point, Vec3 start, Vec3 end) {
        Vec3 ab = end.subtract(start);
        double lengthSqr = ab.lengthSqr();
        if (lengthSqr <= 1.0E-6D) {
            return point.distanceTo(start);
        }
        double t = Math.max(0.0D, Math.min(1.0D, point.subtract(start).dot(ab) / lengthSqr));
        return point.distanceTo(start.add(ab.scale(t)));
    }

    /** 飞弹命中前清无敌帧, 避免多发飞弹互相吞伤害 */
    private static void onLivingAttack(net.minecraftforge.event.entity.living.LivingAttackEvent event) {
        Entity direct = event.getSource().getDirectEntity();
        if (direct == null || !SHOTS.containsKey(direct.getUUID())) {
            return;
        }
        event.getEntity().invulnerableTime = 0;
    }

    /** 弹体命中: 附加凋零 */
    private static void onLivingHurt(net.minecraftforge.event.entity.living.LivingHurtEvent event) {
        Entity direct = event.getSource().getDirectEntity();
        if (direct == null || event.getEntity().level().isClientSide) {
            return;
        }
        HitFx fx = SHOTS.remove(direct.getUUID());
        if (fx == null) {
            return;
        }
        if (direct instanceof Projectile projectile && event.getEntity() == projectile.getOwner()) {
            return;
        }
        if (fx.wither() > 0) {
            event.getEntity().addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.WITHER, 60, fx.wither() - 1));
        }
    }

    @Override
    public void addTooltip(IToolStackView tool, ModifierEntry modifier, @Nullable Player player,
                           List<Component> list, TooltipKey tooltipKey, TooltipFlag tooltipFlag) {
        int charge = tool.getPersistentData().getInt(MECH_CHARGE);
        list.add(Component.translatable("modifier.tprt.mechanical_star_range.tooltip", charge, MAX_CHARGE)
                .withStyle(net.minecraft.ChatFormatting.DARK_RED));
    }
}
