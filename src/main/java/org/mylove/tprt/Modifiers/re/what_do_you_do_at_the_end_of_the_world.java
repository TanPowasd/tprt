package org.mylove.tprt.Modifiers.re;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.TraceableEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.entity.PartEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mylove.tprt.Tprt;
import org.mylove.tprt.registries.ModDamageSource;
import org.mylove.tprt.registries.ModifierIds;
import slimeknights.mantle.client.TooltipKey;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.display.TooltipModifierHook;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.item.IModifiable;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/*
  ============================================================
  末日时在做什么？有没有空？可以来拯救吗？
  (what_do_you_do_at_the_end_of_the_world) —— 挚爱 · 有等级护甲核心词条 (Modifier)

  等级体系 (统计全身护甲槽 + 副手盾牌总等级 L, L = 1 即拥有原版完整基准强度, 每多 1 级进一步突破上限!):
    - 单件单部件: Lv.1 (原版完整基准) | 单件纯挚爱: Lv.2 | 四件套主甲片: Lv.4 | 四件套纯挚爱满配: Lv.8

  【第一问】末日时在做什么？ (常驻):
    - 基础常驻全伤害减免: 15% + 3% × (L - 1) (Lv.1 为 15%, Lv.4 为 24%, Lv.8 为 36%)
    - 已损生命最大额外减伤: 25% + 4% × (L - 1) (Lv.1 为 +25%, Lv.4 为 +37%, Lv.8 为 +53%; 残血合计最高 89%)
    - 单次最终伤害上限 (防一击秒杀, 含跨模组直接 setHealth 强扣拦截):
      max(20%, 50% - 4% × (L - 1)) 最大生命值 (Lv.1 为 50%, Lv.4 为 38%, Lv.8 压低至 22%!)

  【第二问】有没有空？ (跌入锚点血线触发):
    - 锚点触发与回溯保底血线: min(75%, 50% + 3% × (L - 1)) 最大生命值 (Lv.1 为 50%, Lv.4 为 59%, Lv.8 为 71%)
    - 首次跌至该血线时种下「记忆锚点」(记录血线、安全陆地坐标与维度、饱食度、全身装备与 Curios 饰品耐久)
    - 处于该血线以下时, 受到伤害的 min(80%, 50% + 4% × (L - 1)) 截留进「延迟伤害池」(Lv.1 为 50%, Lv.4 为 62%, Lv.8 高达 78%)
    - 延迟伤害池每秒流逝比例: max(8%, 20% - 1.5% × (L - 1)) (Lv.1 为 20%/s, Lv.4 为 15.5%/s, Lv.8 缓释至 9.5%/s)

  【第三问】可以来拯救吗？ (受到致命伤触发):
    - 拦截致死并锁 1 血, 溢出致死量记入延迟伤害池, 进入「绯红狂暴」:
        * 狂暴持续时间: 6.0 + 0.5 × (L - 1) 秒 (Lv.1 为 6.0s, Lv.4 为 7.5s, Lv.8 为 9.5s)
        * 全类型造成伤害加成: 35% + 5% × (L - 1) (Lv.1 为 +35%, Lv.4 为 +50%, Lv.8 为 +70%)
        * 全伤害源吸血比例 (溢出直接抵消延迟池): 35% + 5% × (L - 1) (Lv.1 为 35%, Lv.4 为 50%, Lv.8 为 70%)
        * 攻速加成 40% + 5% × (L - 1) / 移速加成 20% + 3% × (L - 1)
    - 狂暴结束结算剩余延迟池:
        * 活得了 (当前生命 > 延迟池且未坠入虚空): 扣除延迟池原地存活, 冷却 max(12s, 30s - 2s × (L - 1)) (Lv.1 为 30s, Lv.4 为 24s, Lv.8 为 16s)
        * 活不了 (当前生命 ≤ 延迟池或深陷虚空): 引爆锚点【时光回溯】, 冷却 max(24s, 60s - 4s × (L - 1)) (Lv.1 为 60s, Lv.4 为 48s, Lv.8 为 32s)

  【等级阶梯质变机制 (单件双部件 Lv.2 与四件套 Lv.4 即刻质变)】:
    - [Lv.2+ · 救赎余温]: 狂暴结束成功原地自救时, 净化自身全部负面状态, 并将危机余波凝聚为 (20% + 4% × L) 最大生命值的金心护盾 (Lv.2 为 28%, Lv.4 为 36%, Lv.8 为 52%)
    - [Lv.4+ · 因果逆流]: 触发【时光回溯】时, 在回溯起点与锚点终点同时引爆 6 格「终末因果震爆」,
      将本次清空的延迟伤害池 (80% + 10% × L) (+ 30% 最大生命值) 转化为跨模组纯粹真伤奉还给周围敌对目标 (Lv.4 奉还 120% 延迟池, Lv.8 高达 160%)!

  数值均为顶部常量, 可自由调整
  ============================================================
 */
public class what_do_you_do_at_the_end_of_the_world extends Modifier implements TooltipModifierHook {

    // ===== Lv.1 基准常量 (单件 1 级即享原版完整强度) =====
    public static final float BASE_DAMAGE_REDUCTION = 0.15f;       // Lv.1 基础常驻全伤害减免 (15%)
    public static final float MISSING_HP_EXTRA_REDUCTION = 0.25f;  // Lv.1 残血最大额外减伤 (+25%, 空血合计 40%)
    public static final float SINGLE_HIT_CAP_RATIO = 0.50f;        // Lv.1 单次最终伤害上限 (最大生命值的 50%)
    public static final float SECOND_QUESTION_HP_RATIO = 0.50f;    // Lv.1 进入第二问的血量阈值 (50% 最大生命值)
    public static final float DEFER_RATIO = 0.50f;                 // Lv.1 第二问截留进延迟池的伤害比例 (50%)
    public static final int DRAIN_INTERVAL_TICKS = 20;             // 延迟伤害池结算间隔 (20 tick = 1 秒)
    public static final float DRAIN_RATIO_PER_STEP = 0.20f;        // Lv.1 每秒流逝当前延迟池的比例 (20%)
    public static final float MIN_DRAIN_PER_STEP = 1.0f;           // 每秒最低流逝点数 (1.0 点)
    public static final int ANCHOR_EXPIRE_TICKS = 200;             // 回到锚点血线以上且脱战多久后释放旧锚点 (10 秒)
    public static final int BERSERK_DURATION_TICKS = 120;          // Lv.1 绯红狂暴持续时间 (120 tick = 6 秒)
    public static final float BERSERK_DAMAGE_BONUS = 0.35f;        // Lv.1 狂暴期间全类型造成伤害加成 (+35%)
    public static final float BERSERK_ATTACK_SPEED_BONUS = 0.40f;  // Lv.1 狂暴期间攻击速度加成 (+40%)
    public static final float BERSERK_MOVE_SPEED_BONUS = 0.20f;    // Lv.1 狂暴期间移动速度加成 (+20%)
    public static final float BERSERK_LIFESTEAL_RATIO = 0.35f;     // Lv.1 狂暴期间全伤害吸血比例 (35%)
    public static final int SURVIVE_COOLDOWN_TICKS = 600;          // Lv.1 狂暴结束成功自救的冷却 (600 tick = 30 秒)
    public static final int REWIND_COOLDOWN_TICKS = 1200;          // Lv.1 狂暴结束触发回溯的冷却 (1200 tick = 60 秒)
    public static final int REWIND_INVULNERABLE_TICKS = 40;        // 回溯落地短暂无敌帧 (40 tick = 2 秒)

    // ===== 每多 1 级 (L - 1) 的额外成长步伐与阶梯质变阈值 =====
    public static final int MILESTONE_SURVIVE_SHIELD_LEVEL = 2;    // 解锁「救赎余温」(自救化盾) 所需总等级 (Lv.2, 单件双部件即可解锁!)
    public static final float SURVIVE_SHIELD_BASE_RATIO = 0.20f;   // 救赎余温基础护盾比例 (20% + 4% × L)
    public static final float SURVIVE_SHIELD_PER_LEVEL = 0.04f;    // 每级额外提升护盾比例 (+4%)

    public static final int MILESTONE_REWIND_BURST_LEVEL = 4;      // 解锁「因果逆流」(回溯奉还延迟池真伤) 所需总等级 (Lv.4, 四件套或两件双部件解锁!)
    public static final double REWIND_BURST_RADIUS = 6.0D;         // 因果逆流真伤震爆半径 (6 格)
    public static final float REWIND_BURST_POOL_BASE = 0.80f;      // 因果逆流奉还延迟池基础倍率 (80% + 10% × L: Lv.4=120%, Lv.8=160%)
    public static final float REWIND_BURST_POOL_PER_LEVEL = 0.10f; // 每级额外提升奉还倍率 (+10%)
    public static final float REWIND_BURST_MAX_HP_RATIO = 0.30f;   // 因果逆流附加自身最大生命值真伤比例 (30%)

    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD
    };

    // 玩家持久化冷却 Key (防重登刷冷却)
    private static final String PLAYER_NBT_COOLDOWN = "tprt_world_end_cd";

    // 护甲 Tooltip 同步 Key (按秒节流写入, 避免每 tick 刷新物品栈)
    private static final ResourceLocation SYNC_PHASE = ResourceLocation.fromNamespaceAndPath(Tprt.MODID, "we_phase");
    private static final ResourceLocation SYNC_POOL_TENTHS = ResourceLocation.fromNamespaceAndPath(Tprt.MODID, "we_pool_tenths");
    private static final ResourceLocation SYNC_BERSERK_SEC = ResourceLocation.fromNamespaceAndPath(Tprt.MODID, "we_berserk_sec");
    private static final ResourceLocation SYNC_CD_SEC = ResourceLocation.fromNamespaceAndPath(Tprt.MODID, "we_cd_sec");
    private static final ResourceLocation SYNC_TOTAL_LEVEL = ResourceLocation.fromNamespaceAndPath(Tprt.MODID, "we_total_lvl");

    // 狂暴临时属性修饰符 UUID
    private static final UUID BERSERK_ATK_SPEED_UUID = UUID.fromString("c47a91e2-6d18-4f3b-9a21-8e5c3f1d70a1");
    private static final UUID BERSERK_MOVE_SPEED_UUID = UUID.fromString("c47a91e2-6d18-4f3b-9a21-8e5c3f1d70a2");

    /** 服务端玩家实时状态表 */
    private static final Map<UUID, PlayerState> STATES = new HashMap<>();
    /** 冷却期延迟池致死结算重入保护 */
    private static final Set<UUID> SETTLING_LETHAL = new HashSet<>();

    static {
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, LivingHurtEvent.class, what_do_you_do_at_the_end_of_the_world::onLivingHurt);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, false, LivingDamageEvent.class, what_do_you_do_at_the_end_of_the_world::onLivingDamage);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST, false, LivingDeathEvent.class, what_do_you_do_at_the_end_of_the_world::onLivingDeath);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, false, LivingHealEvent.class, what_do_you_do_at_the_end_of_the_world::onLivingHeal);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, TickEvent.PlayerTickEvent.class, what_do_you_do_at_the_end_of_the_world::onPlayerTick);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, PlayerEvent.Clone.class, what_do_you_do_at_the_end_of_the_world::onPlayerClone);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, PlayerEvent.PlayerLoggedOutEvent.class, what_do_you_do_at_the_end_of_the_world::onPlayerLogout);
    }

    @Override
    protected void registerHooks(ModuleHookMap.@NotNull Builder hookBuilder) {
        super.registerHooks(hookBuilder);
        hookBuilder.addHook(this, ModifierHooks.TOOLTIP);
    }

    // ============================================================
    //  等级数值曲线计算 (以 Lv.1 单件即为原版满强度起点, 每多 1 级进一步突破上限)
    // ============================================================

    /** 第一问: 基础常驻全伤害减免 (Lv.1=15%, Lv.4=24%, Lv.8=36%) */
    public static float getBaseDamageReduction(int level) {
        int extra = Math.max(0, level - 1);
        return Mth.clamp(BASE_DAMAGE_REDUCTION + 0.03f * extra, BASE_DAMAGE_REDUCTION, 0.45f);
    }

    /** 第一问: 已损生命最大额外减伤 (Lv.1=+25%, Lv.4=+37%, Lv.8=+53%) */
    public static float getMissingHpExtraReduction(int level) {
        int extra = Math.max(0, level - 1);
        return Mth.clamp(MISSING_HP_EXTRA_REDUCTION + 0.04f * extra, MISSING_HP_EXTRA_REDUCTION, 0.60f);
    }

    /** 第一问: 单次最终伤害占最大生命上限 (Lv.1=50%, Lv.4=38%, Lv.8=22%) */
    public static float getSingleHitCapRatio(int level) {
        int extra = Math.max(0, level - 1);
        return Mth.clamp(SINGLE_HIT_CAP_RATIO - 0.04f * extra, 0.20f, SINGLE_HIT_CAP_RATIO);
    }

    /** 第二问: 记忆锚点触发与回溯恢复血线比例 (Lv.1=50%, Lv.4=59%, Lv.8=71%) */
    public static float getSecondQuestionHpRatio(int level) {
        int extra = Math.max(0, level - 1);
        return Mth.clamp(SECOND_QUESTION_HP_RATIO + 0.03f * extra, SECOND_QUESTION_HP_RATIO, 0.75f);
    }

    /** 第二问: 伤害截留进延迟池比例 (Lv.1=50%, Lv.4=62%, Lv.8=78%) */
    public static float getDeferRatio(int level) {
        int extra = Math.max(0, level - 1);
        return Mth.clamp(DEFER_RATIO + 0.04f * extra, DEFER_RATIO, 0.80f);
    }

    /** 第二问: 延迟伤害池每秒流逝比例 (Lv.1=20%, Lv.4=15.5%, Lv.8=9.5%) */
    public static float getDrainRatioPerStep(int level) {
        int extra = Math.max(0, level - 1);
        return Mth.clamp(DRAIN_RATIO_PER_STEP - 0.015f * extra, 0.08f, DRAIN_RATIO_PER_STEP);
    }

    /** 第三问: 绯红狂暴持续 tick (Lv.1=120t/6.0s, Lv.4=150t/7.5s, Lv.8=190t/9.5s) */
    public static int getBerserkDurationTicks(int level) {
        int extra = Math.max(0, level - 1);
        return Mth.clamp(BERSERK_DURATION_TICKS + 10 * extra, BERSERK_DURATION_TICKS, 240);
    }

    /** 第三问: 绯红狂暴全伤害加成 (Lv.1=+35%, Lv.4=+50%, Lv.8=+70%) */
    public static float getBerserkDamageBonus(int level) {
        int extra = Math.max(0, level - 1);
        return Mth.clamp(BERSERK_DAMAGE_BONUS + 0.05f * extra, BERSERK_DAMAGE_BONUS, 0.90f);
    }

    /** 第三问: 绯红狂暴全伤害吸血比例 (Lv.1=35%, Lv.4=50%, Lv.8=70%) */
    public static float getBerserkLifestealRatio(int level) {
        int extra = Math.max(0, level - 1);
        return Mth.clamp(BERSERK_LIFESTEAL_RATIO + 0.05f * extra, BERSERK_LIFESTEAL_RATIO, 0.90f);
    }

    /** 第三问: 绯红狂暴攻速加成 (Lv.1=+40%, Lv.4=+55%, Lv.8=+75%) */
    public static float getBerserkAttackSpeedBonus(int level) {
        int extra = Math.max(0, level - 1);
        return Mth.clamp(BERSERK_ATTACK_SPEED_BONUS + 0.05f * extra, BERSERK_ATTACK_SPEED_BONUS, 0.90f);
    }

    /** 第三问: 绯红狂暴移速加成 (Lv.1=+20%, Lv.4=+29%, Lv.8=+41%) */
    public static float getBerserkMoveSpeedBonus(int level) {
        int extra = Math.max(0, level - 1);
        return Mth.clamp(BERSERK_MOVE_SPEED_BONUS + 0.03f * extra, BERSERK_MOVE_SPEED_BONUS, 0.50f);
    }

    /** 第三问: 原地自救冷却 tick (Lv.1=600t/30s, Lv.4=480t/24s, Lv.8=320t/16s) */
    public static int getSurviveCooldownTicks(int level) {
        int extra = Math.max(0, level - 1);
        return Math.max(240, SURVIVE_COOLDOWN_TICKS - 40 * extra);
    }

    /** 第三问: 时光回溯冷却 tick (Lv.1=1200t/60s, Lv.4=960t/48s, Lv.8=640t/32s) */
    public static int getRewindCooldownTicks(int level) {
        int extra = Math.max(0, level - 1);
        return Math.max(480, REWIND_COOLDOWN_TICKS - 80 * extra);
    }

    /** Lv.2+ 救赎余温护盾比例 (20% + 4% × L: Lv.2=28%, Lv.4=36%, Lv.8=52%) */
    public static float getSurviveShieldRatio(int level) {
        return Mth.clamp(SURVIVE_SHIELD_BASE_RATIO + SURVIVE_SHIELD_PER_LEVEL * Math.max(1, level), 0.24f, 0.60f);
    }

    /** Lv.4+ 因果逆流奉还延迟池真伤倍率 (80% + 10% × L: Lv.4=120%, Lv.8=160%) */
    public static float getRewindBurstPoolMult(int level) {
        return Mth.clamp(REWIND_BURST_POOL_BASE + REWIND_BURST_POOL_PER_LEVEL * Math.max(1, level), 1.00f, 2.00f);
    }

    // ============================================================
    //  进攻增益: 狂暴期间全类型造成伤害加成 (兼容近战/远程/铁魔法法术/召唤物)
    // ============================================================
    private static void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity().level().isClientSide || event.getAmount() <= 0.0f) return;
        Player attacker = resolvePlayerAttacker(event.getSource());
        if (attacker == null || attacker == event.getEntity()) return;

        PlayerState state = STATES.get(attacker.getUUID());
        if (state != null && state.berserkTicks > 0) {
            int level = getTotalLevel(attacker);
            if (level > 0) {
                event.setAmount(event.getAmount() * (1.0f + getBerserkDamageBonus(level)));
            }
        }
    }

    // ============================================================
    //  最终伤害层: 1) 狂暴全伤害吸血 (含法术/召唤物)  2) 三问受击结算
    // ============================================================
    private static void onLivingDamage(LivingDamageEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide || event.getAmount() <= 0.0f || !Float.isFinite(event.getAmount())) return;

        // 1) 攻击者处于【绯红狂暴】 -> 全伤害源吸血, 溢出抵消延迟伤害池
        Player attacker = resolvePlayerAttacker(event.getSource());
        if (attacker != null && attacker != entity) {
            PlayerState atkState = STATES.get(attacker.getUUID());
            if (atkState != null && atkState.berserkTicks > 0) {
                int atkLevel = getTotalLevel(attacker);
                if (atkLevel > 0) {
                    applyBerserkLifesteal(attacker, atkState, event.getAmount() * getBerserkLifestealRatio(atkLevel));
                }
            }
        }

        // 2) 受击者穿戴本词条 -> 执行第一问/第二问/第三问
        if (!(entity instanceof Player player)) return;
        if (event.getSource().is(DamageTypes.GENERIC_KILL)) return; // 放行 /kill 指令
        if (SETTLING_LETHAL.contains(player.getUUID())) return;    // 冷却期延迟池致死结算不重复拦截
        int level = getTotalLevel(player);
        if (level <= 0) return;

        PlayerState state = getOrCreateState(player);
        state.lastCombatTick = player.level().getGameTime();

        float maxHealth = Math.max(1.0f, player.getMaxHealth());
        float health = player.getHealth();

        // ----- 第一问: 已损生命动态减伤 + 单次限伤锁 (随等级 L 强化) -----
        float missingRatio = Mth.clamp(1.0f - health / maxHealth, 0.0f, 1.0f);
        float reduction = Mth.clamp(
                getBaseDamageReduction(level) + missingRatio * getMissingHpExtraReduction(level),
                0.0f, 0.90f);
        float amount = event.getAmount() * (1.0f - reduction);
        amount = Math.min(amount, maxHealth * getSingleHitCapRatio(level));

        // ----- 若已处于第三问【绯红狂暴】: 强制锁 1 血, 溢出伤害记入延迟池 -----
        if (state.berserkTicks > 0) {
            float allowed = Math.max(0.0f, health - 1.0f);
            if (amount > allowed) {
                state.delayedPool += (amount - allowed);
                amount = allowed;
            }
            event.setAmount(amount);
            state.lastTrackedHealth = Math.max(1.0f, health - amount);
            syncArmorNbt(player, state, level);
            return;
        }

        // ----- 第二问: 跌入锚点血线种下记忆锚点 + 按等级截留伤害进延迟池 -----
        float anchorHp = maxHealth * getSecondQuestionHpRatio(level);
        float deferRatio = getDeferRatio(level);
        if (health > anchorHp) {
            float aboveAnchor = Math.min(amount, health - anchorHp);
            float belowAnchor = amount - aboveAnchor;
            if (health - aboveAnchor <= anchorHp + 1.0e-4f) {
                if (state.anchor == null && state.cooldownTicks <= 0) {
                    plantAnchor(player, state, anchorHp);
                }
            }
            if (belowAnchor > 0.0f) {
                float deferred = belowAnchor * deferRatio;
                state.delayedPool += deferred;
                amount = aboveAnchor + (belowAnchor - deferred);
            }
        } else {
            if (state.anchor == null && state.cooldownTicks <= 0) {
                plantAnchor(player, state, anchorHp);
            }
            float deferred = amount * deferRatio;
            state.delayedPool += deferred;
            amount -= deferred;
        }

        // ----- 第三问: 即将致死时锁 1 血并开启【绯红狂暴】 -----
        if (amount >= health && state.cooldownTicks <= 0) {
            if (state.anchor == null) {
                plantAnchor(player, state, anchorHp);
            }
            float allowed = Math.max(0.0f, health - 1.0f);
            state.delayedPool += (amount - allowed);
            event.setAmount(allowed);
            if (health < 1.0f) {
                player.setHealth(1.0f);
            }
            state.lastTrackedHealth = 1.0f;
            enterBerserk(player, state, level);
            return;
        }

        event.setAmount(amount);
        state.lastTrackedHealth = Math.max(0.0f, health - amount);
        syncArmorNbt(player, state, level);
    }

    // ============================================================
    //  致死兜底拦截 (优先级 HIGHEST, 天然早于不死新生等 NORMAL 死亡监听)
    // ============================================================
    private static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide) return;
        if (event.getSource().is(DamageTypes.GENERIC_KILL)) return;
        if (SETTLING_LETHAL.contains(player.getUUID())) return;
        int level = getTotalLevel(player);
        if (level <= 0) return;

        PlayerState state = getOrCreateState(player);
        if (state.berserkTicks > 0) {
            // 狂暴期间锁血兜底
            event.setCanceled(true);
            player.setHealth(1.0f);
            state.lastTrackedHealth = 1.0f;
            return;
        }
        if (state.cooldownTicks <= 0) {
            event.setCanceled(true);
            if (state.anchor == null) {
                plantAnchor(player, state, player.getMaxHealth() * getSecondQuestionHpRatio(level));
            }
            player.setHealth(1.0f);
            state.lastTrackedHealth = 1.0f;
            enterBerserk(player, state, level);
        }
    }

    // ============================================================
    //  狂暴期间外部治疗溢出同样可抵消延迟伤害池 (与再生/治疗/护符词条自然叠加)
    // ============================================================
    private static void onLivingHeal(LivingHealEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide) return;
        float heal = event.getAmount();
        if (heal <= 0.0f) return;

        PlayerState state = STATES.get(player.getUUID());
        if (state == null || !isWearing(player)) return;

        float maxHp = player.getMaxHealth();
        state.lastTrackedHealth = Math.min(maxHp, player.getHealth() + heal);

        if (state.berserkTicks <= 0 || state.delayedPool <= 0.0f) return;
        float overflow = (player.getHealth() + heal) - maxHp;
        if (overflow > 0.0f) {
            state.delayedPool = Math.max(0.0f, state.delayedPool - overflow);
        }
    }

    // ============================================================
    //  每 tick 驱动: 跨模组直接扣血拦截 / 安全坐标记录 / 狂暴倒计时 / 延迟池流逝 / 锚点维护
    // ============================================================
    private static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) return;
        Player player = event.player;
        if (!player.isAlive()) return;

        int level = getTotalLevel(player);
        if (level <= 0) {
            PlayerState removed = STATES.remove(player.getUUID());
            if (removed != null) {
                removeBerserkModifiers(player);
            }
            return;
        }

        PlayerState state = getOrCreateState(player);
        long now = player.level().getGameTime();
        float maxHp = Math.max(1.0f, player.getMaxHealth());
        float anchorHp = maxHp * getSecondQuestionHpRatio(level);
        float deferRatio = getDeferRatio(level);

        // 0) 跨模组直接 setHealth 强制扣血/百分比斩杀拦截 (防其他模组绕过 LivingDamageEvent 直接改血秒杀)
        float currentHp = player.getHealth();
        if (state.lastTrackedHealth > 0.0f && currentHp < state.lastTrackedHealth - 0.05f) {
            float rawDrop = state.lastTrackedHealth - currentHp;
            state.lastCombatTick = now;

            // 套用第一问动态减伤与单次限伤锁
            float missingRatio = Mth.clamp(1.0f - state.lastTrackedHealth / maxHp, 0.0f, 1.0f);
            float reduction = Mth.clamp(
                    getBaseDamageReduction(level) + missingRatio * getMissingHpExtraReduction(level),
                    0.0f, 0.90f);
            float allowedDrop = Math.min(rawDrop * (1.0f - reduction), maxHp * getSingleHitCapRatio(level));

            if (state.berserkTicks > 0) {
                // 狂暴期间强制锁 1 血, 溢出转入延迟池
                float maxCanLose = Math.max(0.0f, state.lastTrackedHealth - 1.0f);
                if (allowedDrop > maxCanLose) {
                    state.delayedPool += (allowedDrop - maxCanLose);
                    allowedDrop = maxCanLose;
                }
                currentHp = Math.max(1.0f, state.lastTrackedHealth - allowedDrop);
                player.setHealth(currentHp);
            } else {
                // 第二问: 跌破锚点血线部分按等级截留入延迟池
                float startHp = state.lastTrackedHealth;
                if (startHp > anchorHp) {
                    float aboveAnchor = Math.min(allowedDrop, startHp - anchorHp);
                    float belowAnchor = allowedDrop - aboveAnchor;
                    if (startHp - aboveAnchor <= anchorHp + 1.0e-4f && state.anchor == null && state.cooldownTicks <= 0) {
                        plantAnchor(player, state, anchorHp);
                    }
                    if (belowAnchor > 0.0f) {
                        float deferred = belowAnchor * deferRatio;
                        state.delayedPool += deferred;
                        allowedDrop = aboveAnchor + (belowAnchor - deferred);
                    }
                } else {
                    if (state.anchor == null && state.cooldownTicks <= 0) {
                        plantAnchor(player, state, anchorHp);
                    }
                    float deferred = allowedDrop * deferRatio;
                    state.delayedPool += deferred;
                    allowedDrop -= deferred;
                }

                float targetHp = startHp - allowedDrop;
                if (targetHp <= 1.0f && state.cooldownTicks <= 0) {
                    if (state.anchor == null) {
                        plantAnchor(player, state, anchorHp);
                    }
                    state.delayedPool += Math.max(0.0f, 1.0f - targetHp);
                    currentHp = 1.0f;
                    player.setHealth(1.0f);
                    enterBerserk(player, state, level);
                } else if (targetHp > currentHp + 0.05f) {
                    currentHp = Math.min(maxHp, targetHp);
                    player.setHealth(currentHp);
                }
            }
        }

        // 1) 记录最近一次安全陆地坐标 (防击飞坠虚空时把锚点种在半空)
        if (player.tickCount % 10 == 0 && player.onGround() && !player.isInLava() && !player.isInPowderSnow) {
            state.safeDimension = player.level().dimension();
            state.safePos = player.position();
            state.safeYRot = player.getYRot();
            state.safeXRot = player.getXRot();
        }

        // 2) 冷却递减并持久化
        if (state.cooldownTicks > 0) {
            state.cooldownTicks--;
            if (state.cooldownTicks == 0 || player.tickCount % 20 == 0) {
                saveCooldown(player, state.cooldownTicks);
            }
        }

        // 3) 第三问【绯红狂暴】倒计时与结束结算
        if (state.berserkTicks > 0) {
            if (player.getHealth() < 1.0f) {
                player.setHealth(1.0f);
            }
            state.berserkTicks--;
            if (state.berserkTicks <= 0) {
                finishBerserk(player, state, level);
            }
            state.lastTrackedHealth = player.getHealth();
            syncArmorNbt(player, state, level);
            return;
        }

        // 4) 第二问血线检测
        if (player.getHealth() <= anchorHp && state.anchor == null && state.cooldownTicks <= 0) {
            plantAnchor(player, state, anchorHp);
        }

        // 5) 延迟伤害池按秒流逝 (非致死走 setHealth, 不污染受击/次数盾/抛光)
        if (state.delayedPool > 0.0f && player.tickCount % DRAIN_INTERVAL_TICKS == 0) {
            float drainRatio = getDrainRatioPerStep(level);
            float drain = Math.min(state.delayedPool, Math.max(MIN_DRAIN_PER_STEP, state.delayedPool * drainRatio));
            state.delayedPool = Math.max(0.0f, state.delayedPool - drain);
            if (state.delayedPool < 0.05f) {
                state.delayedPool = 0.0f;
            }

            currentHp = player.getHealth();
            if (currentHp > drain) {
                player.setHealth(currentHp - drain);
                state.lastTrackedHealth = player.getHealth();
            } else if (state.cooldownTicks <= 0) {
                // 延迟池即将致死且第三问就绪 -> 拦截并进入绯红狂暴
                if (state.anchor == null) {
                    plantAnchor(player, state, anchorHp);
                }
                float unabsorbed = drain - Math.max(0.0f, currentHp - 1.0f);
                state.delayedPool += unabsorbed;
                player.setHealth(1.0f);
                state.lastTrackedHealth = 1.0f;
                enterBerserk(player, state, level);
                syncArmorNbt(player, state, level);
                return;
            } else {
                // 第三问处于冷却中 -> 走标准致死伤害, 让原版图腾/不死新生等正常接管
                state.delayedPool = 0.0f;
                UUID id = player.getUUID();
                SETTLING_LETHAL.add(id);
                try {
                    DamageSource pure = new DamageSource(player.level().registryAccess()
                            .registryOrThrow(Registries.DAMAGE_TYPE)
                            .getHolderOrThrow(ModDamageSource.TPRT_PURE_DAMAGE_TYPE));
                    player.hurt(pure, currentHp + 10.0f);
                } finally {
                    SETTLING_LETHAL.remove(id);
                }
                state.lastTrackedHealth = player.getHealth();
                syncArmorNbt(player, state, level);
                return;
            }
        }

        // 6) 回到锚点血线以上、延迟池清空且脱战一段时间后, 释放旧锚点以便下一场战斗重新立锚
        if (state.anchor != null && state.delayedPool <= 0.0f && player.getHealth() > anchorHp) {
            if (now - state.lastCombatTick >= ANCHOR_EXPIRE_TICKS) {
                state.anchor = null;
            }
        }

        state.lastTrackedHealth = player.getHealth();
        syncArmorNbt(player, state, level);
    }

    private static void onPlayerClone(PlayerEvent.Clone event) {
        UUID id = event.getEntity().getUUID();
        STATES.remove(id);
        SETTLING_LETHAL.remove(id);
        if (event.isWasDeath()) {
            saveCooldown(event.getEntity(), 0);
        } else {
            int cd = loadCooldown(event.getOriginal());
            saveCooldown(event.getEntity(), cd);
        }
    }

    private static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        Player player = event.getEntity();
        PlayerState state = STATES.remove(player.getUUID());
        SETTLING_LETHAL.remove(player.getUUID());
        if (state != null) {
            removeBerserkModifiers(player);
            saveCooldown(player, state.cooldownTicks);
        }
    }

    // ============================================================
    //  Tooltip 显示 (实时展示当前阶段、全身总等级 L 及对应三问动态数值)
    // ============================================================
    @Override
    public void addTooltip(@NotNull IToolStackView tool, @NotNull ModifierEntry modifier, @Nullable Player player,
                           @NotNull List<Component> tooltip, @NotNull TooltipKey tooltipKey, @NotNull TooltipFlag tooltipFlag) {
        if (!tool.hasTag(TinkerTags.Items.ARMOR)) return;

        ModDataNBT data = tool.getPersistentData();
        int phase = data.getInt(SYNC_PHASE);
        int poolTenths = data.getInt(SYNC_POOL_TENTHS);
        int berserkSec = data.getInt(SYNC_BERSERK_SEC);
        int cdSec = data.getInt(SYNC_CD_SEC);

        int effectiveLevel = player != null ? Math.max(modifier.getLevel(), getTotalLevel(player))
                : Math.max(modifier.getLevel(), data.getInt(SYNC_TOTAL_LEVEL));
        effectiveLevel = Math.max(1, effectiveLevel);

        if (phase == 3) {
            tooltip.add(Component.translatable("tooltip.tprt.world_end.phase_3", berserkSec));
        } else if (phase == 2) {
            tooltip.add(Component.translatable("tooltip.tprt.world_end.phase_2"));
        } else {
            tooltip.add(Component.translatable("tooltip.tprt.world_end.phase_1"));
        }

        int capPct = Math.round(getSingleHitCapRatio(effectiveLevel) * 100.0f);
        int deferPct = Math.round(getDeferRatio(effectiveLevel) * 100.0f);
        tooltip.add(Component.translatable("tooltip.tprt.world_end.level_summary",
                effectiveLevel, capPct, deferPct));

        if (poolTenths > 0) {
            tooltip.add(Component.translatable("tooltip.tprt.world_end.delayed_pool",
                    String.format("%.1f", poolTenths / 10.0f)));
        }

        if (cdSec > 0) {
            tooltip.add(Component.translatable("tooltip.tprt.world_end.cooldown", cdSec));
        } else {
            tooltip.add(Component.translatable("tooltip.tprt.world_end.ready"));
        }

        if (tooltipKey == TooltipKey.SHIFT) {
            float minDr = getBaseDamageReduction(effectiveLevel) * 100.0f;
            float maxDr = Math.min(90.0f, (getBaseDamageReduction(effectiveLevel) + getMissingHpExtraReduction(effectiveLevel)) * 100.0f);
            float berserkSecTotal = getBerserkDurationTicks(effectiveLevel) / 20.0f;
            int berserkBonusPct = Math.round(getBerserkDamageBonus(effectiveLevel) * 100.0f);
            int surviveCdSec = getSurviveCooldownTicks(effectiveLevel) / 20;
            int rewindCdSec = getRewindCooldownTicks(effectiveLevel) / 20;
            tooltip.add(Component.translatable("tooltip.tprt.world_end.detail_q12",
                    String.format("%.1f", minDr), String.format("%.1f", maxDr), capPct, deferPct));
            tooltip.add(Component.translatable("tooltip.tprt.world_end.detail_q3",
                    String.format("%.1f", berserkSecTotal), berserkBonusPct, surviveCdSec, rewindCdSec));
            if (effectiveLevel >= MILESTONE_SURVIVE_SHIELD_LEVEL) {
                tooltip.add(Component.translatable("tooltip.tprt.world_end.milestone_2",
                        Math.round(getSurviveShieldRatio(effectiveLevel) * 100.0f)));
            }
            if (effectiveLevel >= MILESTONE_REWIND_BURST_LEVEL) {
                tooltip.add(Component.translatable("tooltip.tprt.world_end.milestone_4",
                        Math.round(getRewindBurstPoolMult(effectiveLevel) * 100.0f)));
            }
        }
    }

    // ============================================================
    //  核心阶段动作: 立锚 / 狂暴 / 吸血 / 结算与回溯
    // ============================================================

    /** 种下第二问「记忆锚点」 (涵盖护甲、主副手与 Curios 饰品耐久快照) */
    private static void plantAnchor(Player player, PlayerState state, float anchorHealth) {
        ResourceKey<Level> dim = player.level().dimension();
        Vec3 pos = player.position();
        float yRot = player.getYRot();
        float xRot = player.getXRot();

        // 若当前在空中或岩浆中, 优先锚定最近一次安全陆地坐标
        if ((!player.onGround() || player.isInLava()) && state.safePos != null && dim.equals(state.safeDimension)) {
            pos = state.safePos;
            yRot = state.safeYRot;
            xRot = state.safeXRot;
        }

        Map<ItemStack, Integer> itemDamages = new IdentityHashMap<>();
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            recordStackDamage(player.getItemBySlot(slot), itemDamages);
        }
        CuriosApi.getCuriosInventory(player).resolve().ifPresent(inv -> {
            for (var entry : inv.getCurios().entrySet()) {
                var stacks = entry.getValue().getStacks();
                for (int i = 0; i < stacks.getSlots(); i++) {
                    recordStackDamage(stacks.getStackInSlot(i), itemDamages);
                }
            }
        });

        FoodData food = player.getFoodData();
        state.anchor = new AnchorSnapshot(
                dim, pos, yRot, xRot,
                Math.max(1.0f, anchorHealth),
                food.getFoodLevel(), food.getSaturationLevel(),
                itemDamages
        );

        Level level = player.level();
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.9f, 1.2f);
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.END_ROD,
                    pos.x, pos.y + 1.0, pos.z, 12, 0.4, 0.5, 0.4, 0.03);
        }
    }

    private static void recordStackDamage(ItemStack stack, Map<ItemStack, Integer> map) {
        if (stack.isEmpty()) return;
        if (stack.getItem() instanceof IModifiable && ToolStack.isInitialized(stack)) {
            map.put(stack, ToolStack.from(stack).getDamage());
        } else if (stack.isDamageableItem()) {
            map.put(stack, stack.getDamageValue());
        }
    }

    private static void restoreStackDamage(ItemStack stack, Map<ItemStack, Integer> map) {
        if (stack.isEmpty()) return;
        Integer savedDamage = map.get(stack);
        if (savedDamage == null) return;
        if (stack.getItem() instanceof IModifiable && ToolStack.isInitialized(stack)) {
            ToolStack tool = ToolStack.from(stack);
            if (tool.getDamage() > savedDamage) {
                tool.setDamage(savedDamage);
            }
        } else if (stack.isDamageableItem() && stack.getDamageValue() > savedDamage) {
            stack.setDamageValue(savedDamage);
        }
    }

    /** 开启第三问【绯红狂暴】 */
    private static void enterBerserk(Player player, PlayerState state, int level) {
        state.berserkTicks = getBerserkDurationTicks(level);
        applyBerserkModifiers(player, level);

        Level world = player.level();
        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 0.7f, 1.25f);
        if (world instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.FLAME,
                    player.getX(), player.getY() + 1.0, player.getZ(), 24, 0.5, 0.6, 0.5, 0.06);
            serverLevel.sendParticles(ParticleTypes.CHERRY_LEAVES,
                    player.getX(), player.getY() + 1.2, player.getZ(), 16, 0.6, 0.6, 0.6, 0.05);
        }
        player.displayClientMessage(Component.translatable("message.tprt.world_end.berserk"), true);
        syncArmorNbt(player, state, level);
    }

    /** 狂暴吸血: 优先回满生命值, 溢出量直接抵消延迟伤害池 */
    private static void applyBerserkLifesteal(Player player, PlayerState state, float leech) {
        if (leech <= 0.0f) return;
        float missing = Math.max(0.0f, player.getMaxHealth() - player.getHealth());
        float toHeal = Math.min(missing, leech);
        float overflow = leech - toHeal;
        if (toHeal > 0.0f) {
            player.heal(toHeal);
        }
        if (overflow > 0.0f && state.delayedPool > 0.0f) {
            state.delayedPool = Math.max(0.0f, state.delayedPool - overflow);
        }
        state.lastTrackedHealth = player.getHealth();
    }

    /** 狂暴结束结算: 活得了原地续战 (Lv.4+ 触发救赎余温化盾), 活不了触发锚点【时光回溯】(Lv.8+ 触发因果逆流震爆) */
    private static void finishBerserk(Player player, PlayerState state, int level) {
        state.berserkTicks = 0;
        removeBerserkModifiers(player);

        boolean inVoid = player.getY() < player.level().getMinBuildHeight() - 16;
        float health = player.getHealth();

        if (!inVoid && health > state.delayedPool) {
            // 活得了: 扣除剩余延迟池原地继续战斗, 不消耗回溯, 进入较短冷却
            if (state.delayedPool > 0.0f) {
                player.setHealth(health - state.delayedPool);
                state.delayedPool = 0.0f;
            }
            state.anchor = null;
            state.cooldownTicks = getSurviveCooldownTicks(level);

            // [Lv.2+ 阶梯质变 · 救赎余温] 自救成功时净化负面并赋予金心护盾
            if (level >= MILESTONE_SURVIVE_SHIELD_LEVEL) {
                cleanseHarmfulEffects(player);
                float bonusShield = player.getMaxHealth() * getSurviveShieldRatio(level);
                player.setAbsorptionAmount(Math.max(player.getAbsorptionAmount(), bonusShield));
            }

            state.lastTrackedHealth = player.getHealth();
            saveCooldown(player, state.cooldownTicks);

            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.7f, 1.3f);
            player.displayClientMessage(Component.translatable("message.tprt.world_end.survived"), true);
        } else {
            // 活不了: 引爆记忆锚点【时光回溯】
            rewindToAnchor(player, state, level);
        }
    }

    /** 执行锚点【时光回溯】 */
    private static void rewindToAnchor(Player player, PlayerState state, int level) {
        AnchorSnapshot anchor = state.anchor;
        float clearedPool = state.delayedPool;
        state.anchor = null;
        state.delayedPool = 0.0f;
        state.cooldownTicks = getRewindCooldownTicks(level);
        saveCooldown(player, state.cooldownTicks);

        // [Lv.4+ 阶梯质变 · 因果逆流] 在回溯起点先释放一次延迟池反噬真伤
        if (level >= MILESTONE_REWIND_BURST_LEVEL) {
            unleashCausalityBurst(player, clearedPool, level);
        }

        // 1) 清除所有负面状态、火焰与细雪冰冻
        cleanseHarmfulEffects(player);
        player.fallDistance = 0.0f;
        player.setDeltaMovement(Vec3.ZERO);

        float rewindHpRatio = getSecondQuestionHpRatio(level);
        if (anchor != null) {
            // 2) 回溯位置与维度
            if (player instanceof ServerPlayer serverPlayer) {
                ServerLevel targetLevel = serverPlayer.server.getLevel(anchor.dimension);
                if (targetLevel != null && targetLevel != serverPlayer.serverLevel()) {
                    serverPlayer.teleportTo(targetLevel, anchor.pos.x, anchor.pos.y, anchor.pos.z, anchor.yRot, anchor.xRot);
                } else {
                    serverPlayer.teleportTo(anchor.pos.x, anchor.pos.y, anchor.pos.z);
                }
                serverPlayer.fallDistance = 0.0f;
            }

            // 3) 回溯生命值 (至少恢复至当前等级对应的锚点血线)
            float targetHp = Math.min(player.getMaxHealth(),
                    Math.max(anchor.health, player.getMaxHealth() * rewindHpRatio));
            player.setHealth(Math.max(1.0f, targetHp));

            // 4) 回溯饱食度与饱和度 (仅在低于锚点时恢复)
            FoodData food = player.getFoodData();
            if (food.getFoodLevel() < anchor.foodLevel) {
                food.setFoodLevel(anchor.foodLevel);
            }
            if (food.getSaturationLevel() < anchor.saturation) {
                food.setSaturation(anchor.saturation);
            }

            // 5) 回溯全身护甲、主副手与 Curios 饰品耐久 (仅在损耗大于锚点快照时还原)
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                restoreStackDamage(player.getItemBySlot(slot), anchor.itemDamages);
            }
            CuriosApi.getCuriosInventory(player).resolve().ifPresent(inv -> {
                for (var entry : inv.getCurios().entrySet()) {
                    var stacks = entry.getValue().getStacks();
                    for (int i = 0; i < stacks.getSlots(); i++) {
                        restoreStackDamage(stacks.getStackInSlot(i), anchor.itemDamages);
                    }
                }
            });
        } else {
            player.setHealth(Math.max(1.0f, player.getMaxHealth() * rewindHpRatio));
        }

        // [Lv.4+ 阶梯质变 · 因果逆流] 在回溯落点再次绽放因果震爆
        if (level >= MILESTONE_REWIND_BURST_LEVEL) {
            unleashCausalityBurst(player, clearedPool, level);
        }

        state.lastTrackedHealth = player.getHealth();
        player.invulnerableTime = Math.max(player.invulnerableTime, REWIND_INVULNERABLE_TICKS);

        Level world = player.level();
        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.CHORUS_FRUIT_TELEPORT, SoundSource.PLAYERS, 0.9f, 1.15f);
        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0f, 1.1f);
        if (world instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.REVERSE_PORTAL,
                    player.getX(), player.getY() + 1.0, player.getZ(), 32, 0.4, 0.7, 0.4, 0.08);
            serverLevel.sendParticles(ParticleTypes.END_ROD,
                    player.getX(), player.getY() + 1.0, player.getZ(), 16, 0.4, 0.6, 0.4, 0.04);
        }
        player.displayClientMessage(Component.translatable("message.tprt.world_end.rewound"), true);
    }

    private static void cleanseHarmfulEffects(Player player) {
        List<MobEffect> harmful = new ArrayList<>();
        for (MobEffectInstance inst : player.getActiveEffects()) {
            if (inst.getEffect().getCategory() == MobEffectCategory.HARMFUL) {
                harmful.add(inst.getEffect());
            }
        }
        for (MobEffect effect : harmful) {
            player.removeEffect(effect);
        }
        player.clearFire();
        player.setTicksFrozen(0);
    }

    /** Lv.4+ 终末因果震爆: 将回溯清空的延迟伤害池转化为 6 格跨模组纯粹真伤奉还给敌人 */
    private static void unleashCausalityBurst(Player player, float clearedPool, int level) {
        float burstDamage = Math.max(0.0f, clearedPool) * getRewindBurstPoolMult(level)
                + player.getMaxHealth() * REWIND_BURST_MAX_HP_RATIO;
        if (burstDamage <= 0.0f) return;

        DamageSource pure = new DamageSource(player.level().registryAccess()
                .registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(ModDamageSource.TPRT_PURE_DAMAGE_TYPE), player);

        Set<UUID> hitRoots = new HashSet<>();
        for (Entity entity : player.level().getEntities(player, player.getBoundingBox().inflate(REWIND_BURST_RADIUS))) {
            LivingEntity target = resolveLivingTarget(entity);
            if (target == null || target == player || !target.isAlive() || target instanceof ArmorStand) continue;
            if (player.isAlliedTo(target)) continue;
            if (target instanceof OwnableEntity ownable && player.getUUID().equals(ownable.getOwnerUUID())) continue;
            if (!hitRoots.add(target.getUUID())) continue;

            int prevHurtTime = target.hurtTime;
            target.invulnerableTime = 0;
            target.hurtTime = 0;
            target.hurt(pure, burstDamage);
            target.hurtTime = prevHurtTime;
        }
    }

    @Nullable
    private static LivingEntity resolveLivingTarget(Entity entity) {
        if (entity instanceof LivingEntity living) return living;
        if (entity instanceof PartEntity<?> part && part.getParent() instanceof LivingEntity parent) return parent;
        return null;
    }

    // ============================================================
    //  辅助方法与内部状态结构
    // ============================================================

    @Nullable
    private static Player resolvePlayerAttacker(DamageSource source) {
        Entity direct = source.getDirectEntity();
        if (direct instanceof Player p) return p;
        Entity causer = source.getEntity();
        if (causer instanceof Player p) return p;
        if (causer instanceof OwnableEntity ownable && ownable.getOwner() instanceof Player p) return p;
        if (causer instanceof TraceableEntity traceable && traceable.getOwner() instanceof Player p) return p;
        if (direct instanceof OwnableEntity ownable && ownable.getOwner() instanceof Player p) return p;
        if (direct instanceof TraceableEntity traceable && traceable.getOwner() instanceof Player p) return p;
        return null;
    }

    /** 统计玩家全身护甲槽 (及手中护甲/盾牌) 上「末日三问」的总词条等级 */
    public static int getTotalLevel(Player player) {
        int total = 0;
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack stack = player.getItemBySlot(slot);
            if (!stack.isEmpty() && stack.getItem() instanceof IModifiable && ToolStack.isInitialized(stack)) {
                ToolStack tool = ToolStack.from(stack);
                if (!tool.isBroken()) {
                    total += tool.getModifierLevel(ModifierIds.what_do_you_do_at_the_end_of_the_world);
                }
            }
        }
        ItemStack offhand = player.getItemBySlot(EquipmentSlot.OFFHAND);
        if (!offhand.isEmpty() && offhand.getItem() instanceof IModifiable && ToolStack.isInitialized(offhand)) {
            ToolStack tool = ToolStack.from(offhand);
            if (!tool.isBroken() && tool.hasTag(TinkerTags.Items.ARMOR)) {
                total += tool.getModifierLevel(ModifierIds.what_do_you_do_at_the_end_of_the_world);
            }
        }
        return total;
    }

    private static boolean isWearing(Player player) {
        return getTotalLevel(player) > 0;
    }

    private static PlayerState getOrCreateState(Player player) {
        return STATES.computeIfAbsent(player.getUUID(), ignored -> {
            PlayerState s = new PlayerState();
            s.cooldownTicks = loadCooldown(player);
            s.lastTrackedHealth = player.getHealth();
            if (player.onGround() && !player.isInLava()) {
                s.safeDimension = player.level().dimension();
                s.safePos = player.position();
                s.safeYRot = player.getYRot();
                s.safeXRot = player.getXRot();
            }
            return s;
        });
    }

    private static void applyBerserkModifiers(Player player, int level) {
        AttributeInstance atkSpeed = player.getAttribute(Attributes.ATTACK_SPEED);
        if (atkSpeed != null) {
            atkSpeed.removeModifier(BERSERK_ATK_SPEED_UUID);
            atkSpeed.addTransientModifier(new AttributeModifier(
                    BERSERK_ATK_SPEED_UUID, "world_end_berserk_attack_speed",
                    getBerserkAttackSpeedBonus(level), AttributeModifier.Operation.MULTIPLY_TOTAL));
        }
        AttributeInstance moveSpeed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (moveSpeed != null) {
            moveSpeed.removeModifier(BERSERK_MOVE_SPEED_UUID);
            moveSpeed.addTransientModifier(new AttributeModifier(
                    BERSERK_MOVE_SPEED_UUID, "world_end_berserk_move_speed",
                    getBerserkMoveSpeedBonus(level), AttributeModifier.Operation.MULTIPLY_TOTAL));
        }
    }

    private static void removeBerserkModifiers(Player player) {
        AttributeInstance atkSpeed = player.getAttribute(Attributes.ATTACK_SPEED);
        if (atkSpeed != null) {
            atkSpeed.removeModifier(BERSERK_ATK_SPEED_UUID);
        }
        AttributeInstance moveSpeed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (moveSpeed != null) {
            moveSpeed.removeModifier(BERSERK_MOVE_SPEED_UUID);
        }
    }

    /** 仅在展示整数变化时同步至穿戴中的护甲 NBT, 兼顾实时 Tooltip 与零冗余同步开销 */
    private static void syncArmorNbt(Player player, PlayerState state, int totalLevel) {
        int phase = state.berserkTicks > 0 ? 3 : (state.anchor != null ? 2 : 1);
        int poolTenths = Math.round(state.delayedPool * 10.0f);
        int berserkSec = (state.berserkTicks + 19) / 20;
        int cdSec = (state.cooldownTicks + 19) / 20;

        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack stack = player.getItemBySlot(slot);
            if (stack.isEmpty() || !(stack.getItem() instanceof IModifiable) || !ToolStack.isInitialized(stack)) continue;
            ToolStack tool = ToolStack.from(stack);
            if (tool.getModifierLevel(ModifierIds.what_do_you_do_at_the_end_of_the_world) <= 0) continue;

            ModDataNBT data = tool.getPersistentData();
            if (data.getInt(SYNC_PHASE) != phase) data.putInt(SYNC_PHASE, phase);
            if (data.getInt(SYNC_POOL_TENTHS) != poolTenths) data.putInt(SYNC_POOL_TENTHS, poolTenths);
            if (data.getInt(SYNC_BERSERK_SEC) != berserkSec) data.putInt(SYNC_BERSERK_SEC, berserkSec);
            if (data.getInt(SYNC_CD_SEC) != cdSec) data.putInt(SYNC_CD_SEC, cdSec);
            if (data.getInt(SYNC_TOTAL_LEVEL) != totalLevel) data.putInt(SYNC_TOTAL_LEVEL, totalLevel);
        }
    }

    private static void saveCooldown(Player player, int cooldownTicks) {
        CompoundTag data = player.getPersistentData();
        if (cooldownTicks > 0) {
            data.putInt(PLAYER_NBT_COOLDOWN, cooldownTicks);
        } else {
            data.remove(PLAYER_NBT_COOLDOWN);
        }
    }

    private static int loadCooldown(Player player) {
        return Math.max(0, player.getPersistentData().getInt(PLAYER_NBT_COOLDOWN));
    }

    private static final class PlayerState {
        AnchorSnapshot anchor;
        float delayedPool;
        int berserkTicks;
        int cooldownTicks;
        long lastCombatTick;
        float lastTrackedHealth;
        ResourceKey<Level> safeDimension;
        Vec3 safePos;
        float safeYRot;
        float safeXRot;
    }

    private record AnchorSnapshot(
            ResourceKey<Level> dimension,
            Vec3 pos,
            float yRot,
            float xRot,
            float health,
            int foodLevel,
            float saturation,
            Map<ItemStack, Integer> itemDamages
    ) {
    }
}
