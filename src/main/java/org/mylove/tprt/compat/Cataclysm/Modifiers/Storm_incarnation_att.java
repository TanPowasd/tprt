package org.mylove.tprt.compat.Cataclysm.Modifiers;

import com.github.L_Ender.cataclysm.entity.effect.Lightning_Area_Effect_Entity;
import com.github.L_Ender.cataclysm.entity.effect.Lightning_Storm_Entity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeDamageModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeHitModifierHook;
import slimeknights.tconstruct.library.modifiers.impl.NoLevelsModifier;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 风暴化身 (攻击):
 * 前半段 —— 命中伤害提升 (目标在水/雨中 1.8 倍, 否则 1.35 倍);
 * 后半段 —— 若攻击目标处于 5 级潮湿, 则在落点掀起一片雷暴 (照掣雷巨锤 brontes 那套):
 *            落点一圈 16 个闪电风暴 + 落点中心留下一段时间的静电场;
 *            落雷与静电场的伤害均为玩家攻击力的 50%, caster/owner 都传玩家, 所以伤害来源是玩家;
 *            雷暴带 10 秒内置冷却。
 */
public class Storm_incarnation_att extends NoLevelsModifier implements MeleeDamageModifierHook, MeleeHitModifierHook {

    /** 目标潮湿需要达到 5 级 (amplifier 4) */
    public static final int REQUIRED_WET_AMPLIFIER = 4;
    /** 雷暴内置冷却: 10 秒 = 200 tick */
    public static final int STORM_COOLDOWN_TICKS = 200;
    /** 落雷 / 静电场 的伤害倍率: 玩家攻击力的 150% */
    public static final float STORM_DAMAGE_RATIO = 1.5F;
    /** 闪电风暴数量, 沿落点周围一圈排布 */
    public static final int STORM_COUNT = 16;
    public static final double STORM_RING_RADIUS = 3.0D;
    public static final float STORM_SIZE = 2.5F;
    /** 落雷延迟, 与掣雷巨锤 / 斯库拉一致 */
    public static final int STORM_DELAY = -9;
    /** 落点中心的静电场: 半径 / 起效延迟 / 持续时间 */
    public static final float FIELD_RADIUS = 4.0F;
    public static final int FIELD_WAIT_TIME = 20;
    public static final int FIELD_DURATION = 100;
    /** 每位玩家的雷暴冷却: 玩家UUID -> 上次召唤雷暴的 gameTime */
    private static final Map<UUID, Long> STORM_COOLDOWNS = new HashMap<>();
    /** 灾变的潮湿效果 */
    private static final ResourceLocation WETNESS = ResourceLocation.fromNamespaceAndPath("cataclysm", "wetness");

    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        super.registerHooks(hookBuilder);
        hookBuilder.addHook(this, ModifierHooks.MELEE_DAMAGE, ModifierHooks.MELEE_HIT);
    }

    @Override
    public float getMeleeDamage(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float baseDamage, float damage) {
        LivingEntity entity = context.getLivingTarget();
        LivingEntity attacker = context.getPlayerAttacker();
        if (attacker != null && entity != null) {
            if (entity.isInWaterOrRain()) {
                return (float) (baseDamage * 1.8);
            } else {
                return (float) (baseDamage * 1.35);
            }
        }
        return baseDamage;
    }

    @Override
    public void afterMeleeHit(IToolStackView tool, @NotNull ModifierEntry modifier, @NotNull ToolAttackContext context, float damageDealt) {
        Player attacker = context.getPlayerAttacker();
        LivingEntity target = context.getLivingTarget();
        if (attacker == null || target == null || target == attacker || attacker.level().isClientSide) {
            return;
        }
        MobEffect wetness = ForgeRegistries.MOB_EFFECTS.getValue(WETNESS);
        if (wetness == null) {
            return;
        }
        // 后半段: 目标 5 级潮湿才触发雷暴
        MobEffectInstance wet = target.getEffect(wetness);
        if (wet == null || wet.getAmplifier() < REQUIRED_WET_AMPLIFIER) {
            return;
        }
        long gameTime = attacker.level().getGameTime();
        Long lastStorm = STORM_COOLDOWNS.get(attacker.getUUID());
        if (lastStorm != null && gameTime >= lastStorm && gameTime - lastStorm < STORM_COOLDOWN_TICKS) {
            return;
        }
        float damage = (float) attacker.getAttributeValue(Attributes.ATTACK_DAMAGE) * STORM_DAMAGE_RATIO;
        if (damage <= 0.0F) {
            return;
        }
        STORM_COOLDOWNS.put(attacker.getUUID(), gameTime);
        summonStormBarrage(attacker, (ServerLevel) attacker.level(), target, damage);
    }

    /** 落点周围掀起一片雷暴: 一圈 16 个闪电风暴, 中心再留下一段时间的静电场 */
    private static void summonStormBarrage(Player player, ServerLevel level, LivingEntity target, float damage) {
        for (int i = 0; i < STORM_COUNT; i++) {
            double angle = Math.PI * 2.0D / STORM_COUNT * i;
            Lightning_Storm_Entity storm = new Lightning_Storm_Entity(level,
                    target.getX() + Math.cos(angle) * STORM_RING_RADIUS,
                    target.getY(),
                    target.getZ() + Math.sin(angle) * STORM_RING_RADIUS,
                    0.0F, STORM_DELAY, damage, 0.0F, player, STORM_SIZE);
            level.addFreshEntity(storm);
        }
        // 静电场: 会跳过 owner, 所以不会伤到玩家自己
        Lightning_Area_Effect_Entity field = new Lightning_Area_Effect_Entity(level,
                target.getX(), target.getY(), target.getZ());
        field.setRadius(FIELD_RADIUS);
        field.setOwner(player);
        field.setDamage(damage);
        field.setWaitTime(FIELD_WAIT_TIME);
        field.setDuration(FIELD_DURATION);
        field.setRadiusPerTick(0.0F);
        level.addFreshEntity(field);
    }
}