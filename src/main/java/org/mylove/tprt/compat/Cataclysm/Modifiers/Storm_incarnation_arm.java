package org.mylove.tprt.compat.Cataclysm.Modifiers;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import org.mylove.tprt.registries.ModifierIds;
import org.mylove.tprt.utils.ModifierLEVEL;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.interaction.InventoryTickModifierHook;
import slimeknights.tconstruct.library.modifiers.impl.NoLevelsModifier;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 风暴化身 (护甲):
 * 1) 最终伤害减免: 受到的最终伤害 -2 (在水/雨中 -3) —— 走 LivingDamageEvent, 即最后结算那一步;
 * 2) 受伤累计: 记录实际吃到的伤害, 累计达到最大生命值的 50% 时, 在自身周围释放
 *    斯库拉转阶段那样的击退力场 (照她的 Stormknockback: 把附近生物沿背离自身的方向推开);
 *    记录若 15 秒内没有新增受伤则清零。
 */
public class Storm_incarnation_arm extends NoLevelsModifier implements InventoryTickModifierHook {

    /** 最终伤害减免: 平时 2 点 */
    public static final float FINAL_DAMAGE_REDUCTION = 2.0F;
    /** 在水/雨中改为 3 点 */
    public static final float WET_FINAL_DAMAGE_REDUCTION = 3.0F;
    /** 累计受伤达到最大生命值的 50% 就张开力场 */
    public static final float FIELD_THRESHOLD_RATIO = 0.5F;
    /** 累计记录 15 秒没有变化就清零: 15 秒 = 300 tick */
    public static final int RECORD_RESET_TICKS = 300;
    /** 击退力场的推力与半径: 照斯库拉转阶段的 Stormknockback (她用 0.5~0.7 力度 / 5.5 格半径) */
    public static final float FIELD_PUSH = 1.0F;
    public static final double FIELD_RADIUS = 5.5D;
    /** 水花扩散: 从中心扩散到力场边缘要用多少 tick */
    public static final int FIELD_EXPAND_TICKS = 20;
    /** 扩散环上一圈撒多少个点 */
    public static final int FIELD_PARTICLE_COUNT = 48;

    /** 每位穿戴者的受伤累计: UUID -> (累计值, 最后一次变化的 gameTime) */
    private static final Map<UUID, DamageRecord> RECORDS = new HashMap<>();
    /** 正在播放的力场扩散动画: UUID -> 起始 gameTime */
    private static final Map<UUID, Long> FIELD_ANIMATIONS = new HashMap<>();

    static {
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, LivingDamageEvent.class, Storm_incarnation_arm::onLivingDamage);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, TickEvent.LevelTickEvent.class, Storm_incarnation_arm::onLevelTick);
    }

    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        super.registerHooks(hookBuilder);
        hookBuilder.addHook(this, ModifierHooks.INVENTORY_TICK);
    }

    /** 最终结算: 先减伤, 再记账 */
    private static void onLivingDamage(LivingDamageEvent event) {
        LivingEntity living = event.getEntity();
        if (living.level().isClientSide) {
            return;
        }
        if (ModifierLEVEL.getTotalArmorModifierlevel(living, ModifierIds.Storm_incarnation_arm) <= 0) {
            return;
        }
        float amount = event.getAmount();
        if (amount <= 0.0F) {
            return;
        }
        // 1) 最终伤害减免 (护甲/药水都算完之后再扣)
        float reduction = living.isInWaterOrRain() ? WET_FINAL_DAMAGE_REDUCTION : FINAL_DAMAGE_REDUCTION;
        float taken = Math.max(amount - reduction, 0.0F);
        event.setAmount(taken);

        // 2) 累计受伤, 达到最大生命值 50% 就张开力场
        long gameTime = living.level().getGameTime();
        UUID id = living.getUUID();
        DamageRecord record = RECORDS.get(id);
        if (record == null || gameTime < record.lastChange() || gameTime - record.lastChange() > RECORD_RESET_TICKS) {
            record = new DamageRecord(0.0F, gameTime);
        }
        float total = record.total() + taken;
        if (total >= living.getMaxHealth() * FIELD_THRESHOLD_RATIO) {
            RECORDS.remove(id);
            openField(living, (ServerLevel) living.level());
        } else {
            RECORDS.put(id, new DamageRecord(total, gameTime));
        }
    }

    /** 在自身周围释放击退力场: 力度与半径照斯库拉转阶段的 Stormknockback */
    private static void openField(LivingEntity owner, ServerLevel level) {
        List<Entity> nearby = level.getEntities(owner, owner.getBoundingBox().inflate(FIELD_RADIUS),
                entity -> entity instanceof LivingEntity && !entity.isAlliedTo(owner));
        for (Entity entity : nearby) {
            double dx = entity.getX() - owner.getX();
            double dz = entity.getZ() - owner.getZ();
            double distanceSq = Math.max(dx * dx + dz * dz, 0.001D);
            double power = entity.isShiftKeyDown() ? FIELD_PUSH / 3.0D : FIELD_PUSH;
            entity.push(dx / distanceSq * power, 0.0D, dz / distanceSq * power);
        }
        FIELD_ANIMATIONS.put(owner.getUUID(), level.getGameTime());
    }

    /** 水花从中心向外扩散: 每 tick 在递增的半径上撒一圈, 并用云粒子体现排斥 */
    private static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.level.isClientSide() || FIELD_ANIMATIONS.isEmpty()) {
            return;
        }
        if (!(event.level instanceof ServerLevel level)) {
            return;
        }
        long gameTime = level.getGameTime();
        Iterator<Map.Entry<UUID, Long>> iterator = FIELD_ANIMATIONS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Long> entry = iterator.next();
            long elapsed = gameTime - entry.getValue();
            if (elapsed > FIELD_EXPAND_TICKS || !(level.getEntity(entry.getKey()) instanceof LivingEntity owner) || !owner.isAlive()) {
                iterator.remove();
                continue;
            }
            double radius = FIELD_RADIUS * (elapsed + 1.0D) / (FIELD_EXPAND_TICKS + 1.0D);
            spawnFieldRing(level, owner, radius);
        }
    }

    /** 在指定半径上撒一圈水花 + 云粒子 (云粒子体现排斥) */
    private static void spawnFieldRing(ServerLevel level, LivingEntity owner, double radius) {
        double y = owner.getY() + 0.25D;
        for (int i = 0; i < FIELD_PARTICLE_COUNT; i++) {
            double angle = Math.PI * 2.0D / FIELD_PARTICLE_COUNT * i;
            double x = owner.getX() + Math.cos(angle) * radius;
            double z = owner.getZ() + Math.sin(angle) * radius;
            level.sendParticles(ParticleTypes.SPLASH, x, y, z, 2, 0.1D, 0.12D, 0.1D, 0.02D);
            level.sendParticles(ParticleTypes.CLOUD, x, y + 0.2D, z, 1, 0.08D, 0.06D, 0.08D, 0.02D);
        }
    }
    @Override
    public void onInventoryTick(IToolStackView tool, ModifierEntry modifier, Level world, LivingEntity entity, int itemSlot, boolean isSelected, boolean isCorrectSlot, ItemStack stack) {
        if (world.isClientSide() || !isCorrectSlot) {
            return;
        }
        // 15 秒没有新增受伤就把记录清零
        DamageRecord record = RECORDS.get(entity.getUUID());
        if (record != null && world.getGameTime() - record.lastChange() > RECORD_RESET_TICKS) {
            RECORDS.remove(entity.getUUID());
        }
    }

    /** 受伤累计记录 */
    private record DamageRecord(float total, long lastChange) {
    }
}