package org.mylove.tprt.compat.Cataclysm.Modifiers;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import org.mylove.tprt.registries.ModBuffRegistry;
import org.mylove.tprt.registries.ModifierIds;
import org.mylove.tprt.utils.ModifierLEVEL;
import slimeknights.tconstruct.library.modifiers.impl.NoLevelsModifier;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 风暴化身 (护甲):
 * 1) 最终伤害减免: 受到的最终伤害 -2 (在水/雨中 -3) —— 走 LivingDamageEvent, 也就是最后结算那一步;
 * 2) 累积受伤: 记录实际吃到的伤害 (15 秒没有新增就清零), 累积达到自身最大生命值的 50% 时
 *    获得"次数盾": 可以取消接下来 5 次受到的攻击 —— 走 LivingAttackEvent 直接取消,
 *    所以那一下既没有伤害, 也不会占用无敌帧、不产生击退;
 *    触发冷却 5 秒; 剩余次数用一个隐形效果同步给客户端, 在生命值 UI 上方提示。
 */
public class Storm_incarnation_arm extends NoLevelsModifier {

    /** 最终伤害减免: 平时 2 点 */
    public static final float FINAL_DAMAGE_REDUCTION = 2.0F;
    /** 在水/雨中改为 3 点 */
    public static final float WET_FINAL_DAMAGE_REDUCTION = 3.0F;
    /** 累积受伤达到最大生命值的 50% 就获得次数盾 */
    public static final float SHIELD_THRESHOLD_RATIO = 0.5F;
    /** 累积记录 15 秒没有变化就清零: 15 秒 = 300 tick */
    public static final int RECORD_RESET_TICKS = 300;
    /** 次数盾: 免疫 5 次攻击 */
    public static final int SHIELD_CHARGES = 5;
    /** 次数盾触发冷却: 5 秒 = 100 tick */
    public static final int SHIELD_COOLDOWN_TICKS = 100;

    /** 每位穿戴者的受伤累计: UUID -> (累计值, 最后一次变化的 gameTime) */
    private static final Map<UUID, DamageRecord> RECORDS = new HashMap<>();
    /** 次数盾剩余次数 (服务端权威) */
    private static final Map<UUID, Integer> SHIELD_COUNTS = new HashMap<>();
    /** 上次触发次数盾的时间 (冷却用) */
    private static final Map<UUID, Long> SHIELD_COOLDOWNS = new HashMap<>();

    static {
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, LivingAttackEvent.class, Storm_incarnation_arm::onLivingAttack);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, LivingDamageEvent.class, Storm_incarnation_arm::onLivingDamage);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, TickEvent.PlayerTickEvent.class, Storm_incarnation_arm::onPlayerTick);
    }

    /** 次数盾: 还有次数就把这次攻击整个取消 (最先结算的一步) */
    private static void onLivingAttack(LivingAttackEvent event) {
        LivingEntity living = event.getEntity();
        if (living.level().isClientSide) {
            return;
        }
        Integer count = SHIELD_COUNTS.get(living.getUUID());
        if (count == null || count <= 0) {
            return;
        }
        if (ModifierLEVEL.getTotalArmorModifierlevel(living, ModifierIds.Storm_incarnation_arm) <= 0) {
            return;
        }
        event.setCanceled(true);
        int left = count - 1;
        if (left <= 0) {
            SHIELD_COUNTS.remove(living.getUUID());
            living.removeEffect(ModBuffRegistry.ATTACK_SHIELD.get());
        } else {
            SHIELD_COUNTS.put(living.getUUID(), left);
            showShield(living, left);
        }
    }

    /** 最终结算: 先减伤, 再记账, 达到阈值就发次数盾 */
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

        // 2) 累计受伤, 达到最大生命值 50% 就发次数盾
        long gameTime = living.level().getGameTime();
        UUID id = living.getUUID();
        DamageRecord record = RECORDS.get(id);
        if (record == null || gameTime < record.lastChange() || gameTime - record.lastChange() > RECORD_RESET_TICKS) {
            record = new DamageRecord(0.0F, gameTime);
        }
        float total = record.total() + taken;
        if (total < living.getMaxHealth() * SHIELD_THRESHOLD_RATIO) {
            RECORDS.put(id, new DamageRecord(total, gameTime));
            return;
        }
        // 触发冷却中就先攒着, 冷却一过下次结算再发
        Long lastTrigger = SHIELD_COOLDOWNS.get(id);
        if (lastTrigger != null && gameTime >= lastTrigger && gameTime - lastTrigger < SHIELD_COOLDOWN_TICKS) {
            RECORDS.put(id, new DamageRecord(total, gameTime));
            return;
        }
        RECORDS.remove(id);
        SHIELD_COOLDOWNS.put(id, gameTime);
        SHIELD_COUNTS.put(id, SHIELD_CHARGES);
        showShield(living, SHIELD_CHARGES);
    }

    /** 每 tick: 清理过期的累计记录; 拿掉护甲后收回次数盾 */
    private static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) {
            return;
        }
        Player player = event.player;
        UUID id = player.getUUID();
        DamageRecord record = RECORDS.get(id);
        if (record != null && player.level().getGameTime() - record.lastChange() > RECORD_RESET_TICKS) {
            RECORDS.remove(id);
        }
        if (ModifierLEVEL.getTotalArmorModifierlevel(player, ModifierIds.Storm_incarnation_arm) <= 0) {
            if (SHIELD_COUNTS.remove(id) != null) {
                player.removeEffect(ModBuffRegistry.ATTACK_SHIELD.get());
            }
            SHIELD_COOLDOWNS.remove(id);
        }
    }

    /** 用隐形效果把剩余次数同步给客户端 (无粒子、不进状态栏), 客户端据此在生命值上方提示 */
    private static void showShield(LivingEntity living, int count) {
        MobEffectInstance current = living.getEffect(ModBuffRegistry.ATTACK_SHIELD.get());
        if (current != null && current.getAmplifier() == count - 1) {
            return;
        }
        living.addEffect(new MobEffectInstance(ModBuffRegistry.ATTACK_SHIELD.get(),
                MobEffectInstance.INFINITE_DURATION, count - 1, false, false, false));
    }

    /** 受伤累计记录 */
    private record DamageRecord(float total, long lastChange) {
    }
}