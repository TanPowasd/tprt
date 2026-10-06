package org.mylove.tprt.compat.Cataclysm.Modifiers;

import com.github.L_Ender.cataclysm.entity.effect.Sandstorm_Entity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import org.mylove.tprt.Tprt;
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
 * 远古沙暴 (ancient_sandstorm)
 * <p>
 * 近战命中后, 在使用者两侧各召唤一道沙暴 (Cataclysm 的 {@link Sandstorm_Entity})。<br>
 * 1) 整套召唤拥有 <b>2 秒冷却</b> (记在工具持久数据里, 按游戏刻计算, 冷却中再命中不会召唤);<br>
 * 2) 沙暴造成的伤害改为 <b>玩家攻击力 × 75%</b>。
 * <p>
 * 关于伤害: Cataclysm 里 {@code Sandstorm_Entity} 的伤害是写死的 7 点 (私有 {@code damage} 方法,
 * 没有公开 setter, 也没有配置项), 所以这里在 Forge 的 {@link LivingDamageEvent} 上拦一道:
 * 只对我们召唤出来的那两道沙暴 (按实体 UUID 记录, 召唤时快照玩家攻击力), 把最终伤害改写成
 * 玩家攻击力 × 75%。其余来源的沙暴 (例如 Boss 自己放的) 不受影响。
 */
public class ancient_sandstorm extends NoLevelsModifier implements MeleeHitModifierHook {

    /* ===================== 数值 ===================== */

    /** 冷却: 2 秒 */
    private static final int COOLDOWN = 40;
    /** 沙暴存活时间 (tick), 对应构造参数 lifespan */
    private static final int LIFESPAN = 200;
    /** 沙暴伤害倍率: 玩家攻击力 × 75% */
    private static final double DAMAGE_PERCENT = 0.75D;
    /** 两侧沙暴距离使用者的距离 (格) */
    private static final double SUMMON_DISTANCE = 6.0D;

    /* ===================== 持久数据 ===================== */

    /** 冷却记账: 记录“可以再次使用的时间点” (游戏刻) */
    private static final ResourceLocation READY_AT = ResourceLocation.fromNamespaceAndPath(Tprt.MODID, "ancient_sandstorm_ready_at");
    private static final String READY_AT_TICK = "ready_at";

    /** 我们召唤出来的沙暴 → 伤害改写值 */
    private static final Map<UUID, SummonedStorm> SUMMONED = new HashMap<>();

    /** 召唤记录: 改写后的伤害 + 记录过期时间 (游戏刻, 用于清理) */
    private record SummonedStorm(float damage, long expireAt) {}

    static {
        // 只在类首次加载时注册一次; 词条被实例化时必然已经加载过, 一定早于任何一次沙暴伤害。
        // 这里显式给出事件类型, 不依赖 lambda 的泛型推导。
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, LivingDamageEvent.class, ancient_sandstorm::onLivingDamage);
    }

    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        super.registerHooks(hookBuilder);
        hookBuilder.addHook(this, ModifierHooks.MELEE_HIT);
    }

    @Override
    public void afterMeleeHit(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float damageDealt) {
        LivingEntity attacker = context.getAttacker();
        Level level = context.getLevel();
        if (level.isClientSide() || !(context.getTarget() instanceof LivingEntity) || !(attacker instanceof ServerPlayer player)) {
            return;
        }

        // 2 秒冷却: 冷却中再命中不会召唤
        long now = level.getGameTime();
        if (now < tool.getPersistentData().getCompound(READY_AT).getLong(READY_AT_TICK)) {
            return;
        }

        // 伤害在召唤瞬间快照: 沙暴造成的伤害 = 玩家攻击力 × 75%
        float damage = (float) (player.getAttributeValue(Attributes.ATTACK_DAMAGE) * DAMAGE_PERCENT);
        for (int i = 0; i < 2; i++) {
            float angle = i * Mth.PI;
            double sx = player.getX() + (Mth.cos(angle) * SUMMON_DISTANCE);
            double sy = player.getY();
            double sz = player.getZ() + (Mth.sin(angle) * SUMMON_DISTANCE);
            Sandstorm_Entity sandstorm = new Sandstorm_Entity(level, sx, sy, sz, LIFESPAN, angle, player);
            if (level.addFreshEntity(sandstorm)) {
                trackSummoned(sandstorm, damage, now);
            }
        }

        CompoundTag readyTag = new CompoundTag();
        readyTag.putLong(READY_AT_TICK, now + COOLDOWN);
        tool.getPersistentData().put(READY_AT, readyTag);
    }

    /** 记下这道沙暴的伤害改写值 (顺手清掉过期记录, 避免长期堆积) */
    private static void trackSummoned(Sandstorm_Entity sandstorm, float damage, long now) {
        SUMMONED.entrySet().removeIf(entry -> entry.getValue().expireAt() < now);
        SUMMONED.put(sandstorm.getUUID(), new SummonedStorm(damage, now + LIFESPAN + 40L));
    }

    /** 把我们召唤的沙暴造成的伤害改成 玩家攻击力 × 75% */
    private static void onLivingDamage(LivingDamageEvent event) {
        Entity direct = event.getSource().getDirectEntity();
        if (direct == null) {
            return;
        }
        SummonedStorm override = SUMMONED.get(direct.getUUID());
        if (override != null) {
            // LivingDamageEvent 拿到的是护甲/抗性结算完的最终伤害, 直接覆盖即可
            event.setAmount(override.damage());
        }
    }
}
