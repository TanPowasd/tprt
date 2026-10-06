package org.mylove.tprt.compat.Cataclysm.Modifiers;

import com.github.L_Ender.cataclysm.entity.effect.Wave_Entity;
import com.github.L_Ender.cataclysm.init.ModSounds;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.registries.ForgeRegistries;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeHitModifierHook;
import slimeknights.tconstruct.library.modifiers.impl.NoLevelsModifier;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 潮涌 (原 eye_of_storm):
 * 近战命中给目标叠加潮湿 (灾变的 cataclysm:wetness, 1 级起, 最多 5 级, 持续 10 秒);
 * 命中时 50% 概率掀起水浪冲击目标 (表现照霆浪锚戟的蓄力攻击, 伤害为玩家攻击力的 50%);
 * 命中前目标若已潮湿, 则必定触发, 且水浪伤害提升为玩家攻击力的 100%。
 * 水浪本身带 0.5 秒内置冷却, 免得高攻速武器疯狂刷实体。
 */
public class Tidal_surge extends NoLevelsModifier implements MeleeHitModifierHook {
    /** 潮湿持续: 10 秒 = 200 tick */
    public static final int WET_DURATION = 200;
    /** 潮湿最多 5 级 -> amplifier 4 */
    public static final int WET_MAX_AMPLIFIER = 4;
    /** 平时: 50% 概率, 50% 攻击力 */
    public static final float WAVE_CHANCE = 0.5F;
    public static final float WAVE_DAMAGE_RATIO = 0.5F;
    /** 目标已潮湿: 必定触发, 100% 攻击力 */
    public static final float WET_WAVE_CHANCE = 1.0F;
    public static final float WET_WAVE_DAMAGE_RATIO = 1.0F;
    /** 水浪存活时间, 与霆浪锚戟蓄力攻击一致 */
    public static final int WAVE_MAX_TICKS = 60;
    /** 水浪触发内置冷却: 0.5 秒 = 10 tick */
    public static final int WAVE_COOLDOWN_TICKS = 10;
    /** 每个玩家的水浪冷却: 玩家UUID -> 上次生成水浪的 gameTime */
    private static final Map<UUID, Long> WAVE_COOLDOWNS = new HashMap<>();
    /** 灾变的潮湿效果 */
    private static final ResourceLocation WETNESS = ResourceLocation.fromNamespaceAndPath("cataclysm", "wetness");
    /** 潮涌水浪的标记, 用来只认自己召唤的水浪 */
    public static final String WAVE_TAG = "tprt_tidal_surge_wave";
    /** 水浪自带的击退原本约 1.5 的速度, 这里只保留这么一点点 */
    public static final double WAVE_KNOCKBACK_KEEP = 0.05D;
    /** 水浪命中后待修正速度的目标: UUID -> 修正后的速度 */
    private static final Map<UUID, Vec3> PENDING_KNOCKBACK_FIX = new HashMap<>();

    static {
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, LivingDamageEvent.class, Tidal_surge::onLivingDamage);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, TickEvent.LevelTickEvent.class, Tidal_surge::onLevelTick);
    }

    /** 水浪打中目标: 记下命中前的速度 (此刻水浪自己的推挤与原版受击击退都还没结算) */
    private static void onLivingDamage(LivingDamageEvent event) {
        if (event.getEntity().level().isClientSide) {
            return;
        }
        if (!(event.getSource().getDirectEntity() instanceof Wave_Entity wave) || !wave.getPersistentData().getBoolean(WAVE_TAG)) {
            return;
        }
        LivingEntity target = event.getEntity();
        // 水浪的推挤方向就是它的朝向, 这里只保留一点点
        float yaw = wave.getYRot() * ((float) Math.PI / 180.0F);
        Vec3 nudge = new Vec3(-Mth.sin(yaw), 0.0D, Mth.cos(yaw)).scale(WAVE_KNOCKBACK_KEEP);
        PENDING_KNOCKBACK_FIX.put(target.getUUID(), target.getDeltaMovement().add(nudge));
    }

    /** 这一 tick 的实体都跑完之后, 把水浪那一下大幅击退换成很小的推劲 */
    private static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.level.isClientSide() || PENDING_KNOCKBACK_FIX.isEmpty()) {
            return;
        }
        if (event.level instanceof ServerLevel level) {
            for (Map.Entry<UUID, Vec3> entry : PENDING_KNOCKBACK_FIX.entrySet()) {
                if (level.getEntity(entry.getKey()) instanceof LivingEntity target) {
                    target.setDeltaMovement(entry.getValue());
                    target.hasImpulse = true;
                }
            }
        }
        PENDING_KNOCKBACK_FIX.clear();
    }

    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        hookBuilder.addHook(this, ModifierHooks.MELEE_HIT);
    }

    @Override
    public void afterMeleeHit(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float damageDealt) {
        LivingEntity target = context.getLivingTarget();
        Player player = context.getPlayerAttacker();
        if (target == null || player == null || target == player || player.level().isClientSide) {
            return;
        }
        MobEffect wetness = ForgeRegistries.MOB_EFFECTS.getValue(WETNESS);
        if (wetness == null) {
            return;
        }
        // 先看命中前有没有潮湿: 它同时决定叠加层数、触发概率与倍率
        MobEffectInstance current = target.getEffect(wetness);
        boolean alreadyWet = current != null;
        int amplifier = alreadyWet ? Math.min(current.getAmplifier() + 1, WET_MAX_AMPLIFIER) : 0;
        target.addEffect(new MobEffectInstance(wetness, WET_DURATION, amplifier));

        if (player.getRandom().nextFloat() >= (alreadyWet ? WET_WAVE_CHANCE : WAVE_CHANCE)) {
            return;
        }
        // 内置冷却: 冷却期间不再生成水浪 (潮湿照常叠加)
        long gameTime = player.level().getGameTime();
        Long lastWave = WAVE_COOLDOWNS.get(player.getUUID());
        if (lastWave != null && gameTime >= lastWave && gameTime - lastWave < WAVE_COOLDOWN_TICKS) {
            return;
        }
        float attackPower = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        float damage = attackPower * (alreadyWet ? WET_WAVE_DAMAGE_RATIO : WAVE_DAMAGE_RATIO);
        if (damage > 0.0F) {
            WAVE_COOLDOWNS.put(player.getUUID(), gameTime);
            summonWave(player, (ServerLevel) player.level(), target, damage);
        }
    }

    /** 掀起水浪冲击目标: 实体、状态、朝向与音效都照霆浪锚戟的蓄力攻击来 */
    private static void summonWave(Player player, ServerLevel level, LivingEntity target, float damage) {
        double dx = target.getX() - player.getX();
        double dz = target.getZ() - player.getZ();
        double length = Math.sqrt(dx * dx + dz * dz);
        if (length < 1.0E-4D) {
            double radians = Math.toRadians(player.getYRot());
            dx = -Math.sin(radians);
            dz = Math.cos(radians);
        } else {
            dx /= length;
            dz /= length;
        }
        Wave_Entity wave = new Wave_Entity(level, player, WAVE_MAX_TICKS, damage);
        wave.setPos(target.getX(), target.getY(), target.getZ());
        wave.setState(1);
        wave.setYRot((float) (-Mth.atan2(dx, dz) * (180.0D / Math.PI)));
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.HEAVY_SMASH.get(), SoundSource.PLAYERS, 0.6F, 1.0F);
        wave.getPersistentData().putBoolean(WAVE_TAG, true);
        level.addFreshEntity(wave);
    }
}
