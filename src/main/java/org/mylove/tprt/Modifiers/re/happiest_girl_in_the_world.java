package org.mylove.tprt.Modifiers.re;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.TraceableEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.entity.PartEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mylove.tprt.Tprt;
import org.mylove.tprt.registries.ModDamageSource;
import org.mylove.tprt.registries.ModifierIds;
import slimeknights.mantle.client.TooltipKey;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.display.TooltipModifierHook;
import slimeknights.tconstruct.library.modifiers.impl.NoLevelsModifier;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.item.IModifiable;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

/*
  ============================================================
  现在的我，不管别人怎么说，都一定是世界上最幸福的女孩
  (happiest_girl_in_the_world) —— 挚爱 (chtholly) · 工匠护符核心词条

  设计原则:
    - 零硬编码点名联动, 纯底层机制自洽, 与「瑟尼欧里斯」「末日三问」自然叠加
    - 全面兼容其他模组机制 (跨模组法术/吐息/激光穿甲伤害识别、直接 setHealth 扣血/治疗拦截、
      非进食通道饱食度恢复、召唤物/法术/远程投射物攻击归因、多部件 Boss 破无敌帧)

  1) 【现在的我】—— 胸针与黄油蛋糕 (增益留温升阶 + 幸福护盾):
     - 美好留温: 获得任何正面药水效果 (BENEFICIAL) 时, 持续时间延长 100%;
       若当前生命值 ≥ 50%, 正面效果等级额外 +1 (最高升至 V 级, 自动过滤内部计数型状态)
     - 治疗增幅与溢疗护盾: 受到任何治疗时治疗量 +30%, 生命回满后的溢出治疗 100% 转化为「幸福护盾」
     - 黄油蛋糕的余温: 进食或通过其他模组机制恢复饥饿值时立即获得 (饥饿值 × 3%) 最大生命值的幸福护盾;
       饱食度 ≥ 16 时每秒自然温养 2% 最大生命值的幸福护盾 (自然温养上限 40%, 总护盾上限 100% 最大生命值)
     - 幸福护盾实时映射为金心 (Absorption), 优先替本体承受常规伤害与模组直接 setHealth 扣血

  2) 【不管别人怎么说】—— 心意不移 (护盾庇护):
     - 拥有幸福护盾或处于「幸福终幕」期间:
       * 免疫受击击退, 拦截并自动净除一切负面药水效果 (HARMFUL)、火焰与冰冻
       * 受到的穿透护甲/魔法/法术/龙息/激光伤害降低 40%

  3) 【都一定是世界上最幸福的女孩】—— 倾尽所有的谢幕 (碎盾爆发):
     - 当累积达到过至少 20% 最大生命值的幸福护盾在战斗中被击碎归零时 (冷却 25 秒),
       开启持续 8 秒的【幸福终幕】:
       * 黄金妖精共鸣: 爆发瞬间对周围 8 格敌人释放星火震击, 并在 8 秒内获得
         +25% 攻击力、+25% 护甲值、+25% 攻击速度
       * 幸福星火追击: 终幕期间每次攻击 (含近战/远程/法术/召唤物) 命中额外造成
         (35% 有效攻击基准 + 50% 本轮峰值护盾) 的纯粹真伤, 并回复该真伤 25% 的生命值

  数值均为顶部常量, 可自由调整
  ============================================================
 */
public class happiest_girl_in_the_world extends NoLevelsModifier implements TooltipModifierHook {

    // ===== 第一幕: 现在的我 (正面药水升阶 + 溢疗/饮食转幸福护盾) =====
    public static final float BENEFICIAL_DURATION_MULTIPLIER = 2.0f; // 正面药水持续时间倍率 (×2.0 = 延长 100%)
    public static final float UPGRADE_EFFECT_MIN_HP_RATIO = 0.50f;   // 正面药水等级 +1 所需生命比例 (50%)
    public static final int MAX_UPGRADED_AMPLIFIER = 4;              // 药水升阶最高等级上限 (amplifier 4 = 等级 V)

    public static final float HEAL_AMPLIFY_BONUS = 0.30f;            // 受到治疗增幅比例 (+30%)
    public static final float OVERFLOW_TO_SHIELD_RATIO = 1.00f;      // 溢出治疗转化为幸福护盾比例 (100%)
    public static final float MAX_SHIELD_RATIO = 1.00f;              // 幸福护盾最大上限 (100% 最大生命值)
    public static final float FOOD_SHIELD_PER_NUTRITION = 0.03f;     // 每点食物饥饿值转化的最大生命护盾比例 (3%)
    public static final float MIN_FOOD_SHIELD_RATIO = 0.06f;         // 单次进食最低护盾获取比例 (6% 最大生命值)
    public static final int WARMTH_MIN_FOOD_LEVEL = 16;              // 自然温养所需最低饱食度 (16)
    public static final float NATURAL_WARMTH_PER_SEC_RATIO = 0.02f;  // 每秒自然温养护盾比例 (2% 最大生命值)
    public static final float NATURAL_WARMTH_CAP_RATIO = 0.40f;      // 自然温养护盾上限 (40% 最大生命值)

    // ===== 第二幕: 不管别人怎么说 (免控净除 + 穿甲/法术减伤) =====
    public static final float BYPASS_ARMOR_REDUCTION = 0.40f;        // 护盾/终幕期间穿透护甲或模组法术伤害减免 (40%)

    // ===== 第三幕: 都一定是世界上最幸福的女孩 (碎盾终幕爆发) =====
    public static final float MIN_SHIELD_FOR_FINALE_RATIO = 0.20f;   // 触发终幕所需本轮最低峰值护盾比例 (20% 最大生命值)
    public static final int FINALE_DURATION_TICKS = 160;             // 幸福终幕持续时间 (160 tick = 8 秒)
    public static final int FINALE_COOLDOWN_TICKS = 500;             // 幸福终幕冷却时间 (500 tick = 25 秒)
    public static final float FINALE_RADIUS = 8.0f;                  // 终幕开启星火震击半径 (8 格)
    public static final float FINALE_ATTACK_BONUS = 0.25f;           // 终幕期间攻击力加成 (+25%)
    public static final float FINALE_ARMOR_BONUS = 0.25f;            // 终幕期间护甲加成 (+25%)
    public static final float FINALE_ATK_SPEED_BONUS = 0.25f;        // 终幕期间攻击速度加成 (+25%)
    public static final float FINALE_ATK_TRUE_DMG_RATIO = 0.35f;     // 终幕星火追击: 攻击基准转化率 (35%)
    public static final float FINALE_SHIELD_TRUE_DMG_RATIO = 0.50f;  // 终幕星火追击: 峰值护盾转化率 (50%)
    public static final float FINALE_LIFESTEAL_RATIO = 0.25f;        // 终幕星火追击: 真伤生命回复比例 (25%)

    // 玩家持久化冷却 Key
    private static final String PLAYER_NBT_COOLDOWN = "tprt_happiest_girl_cd";

    // 护符 Tooltip 同步 Key
    private static final ResourceLocation SYNC_SHIELD_TENTHS = ResourceLocation.fromNamespaceAndPath(Tprt.MODID, "hg_shield_tenths");
    private static final ResourceLocation SYNC_PEAK_TENTHS = ResourceLocation.fromNamespaceAndPath(Tprt.MODID, "hg_peak_tenths");
    private static final ResourceLocation SYNC_FINALE_SEC = ResourceLocation.fromNamespaceAndPath(Tprt.MODID, "hg_finale_sec");
    private static final ResourceLocation SYNC_CD_SEC = ResourceLocation.fromNamespaceAndPath(Tprt.MODID, "hg_cd_sec");

    // 幸福终幕临时属性修饰符 UUID
    private static final UUID FINALE_ATK_UUID = UUID.fromString("d82b47c1-3e94-4a12-b850-7f1a2d9e60b1");
    private static final UUID FINALE_ARMOR_UUID = UUID.fromString("d82b47c1-3e94-4a12-b850-7f1a2d9e60b2");
    private static final UUID FINALE_ATK_SPEED_UUID = UUID.fromString("d82b47c1-3e94-4a12-b850-7f1a2d9e60b3");

    /** 服务端玩家实时状态表 */
    private static final Map<UUID, PlayerState> STATES = new HashMap<>();
    /** 已升阶过的药水实例表 (防同一实例重复翻倍) */
    private static final Set<MobEffectInstance> BOOSTED_EFFECTS = Collections.newSetFromMap(new WeakHashMap<>());
    /** 星火追击重入保护 */
    private static final Set<UUID> TRIGGERING_STARFIRE = new HashSet<>();

    static {
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGH, false, LivingHealEvent.class, happiest_girl_in_the_world::onLivingHeal);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, LivingEntityUseItemEvent.Finish.class, happiest_girl_in_the_world::onItemUseFinish);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, MobEffectEvent.Added.class, happiest_girl_in_the_world::onMobEffectAdded);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST, false, MobEffectEvent.Applicable.class, happiest_girl_in_the_world::onMobEffectApplicable);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST, false, LivingKnockBackEvent.class, happiest_girl_in_the_world::onLivingKnockBack);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, false, LivingHurtEvent.class, happiest_girl_in_the_world::onLivingHurt);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOW, false, LivingDamageEvent.class, happiest_girl_in_the_world::onLivingDamage);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, TickEvent.PlayerTickEvent.class, happiest_girl_in_the_world::onPlayerTick);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, PlayerEvent.Clone.class, happiest_girl_in_the_world::onPlayerClone);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, PlayerEvent.PlayerLoggedOutEvent.class, happiest_girl_in_the_world::onPlayerLogout);
    }

    @Override
    protected void registerHooks(ModuleHookMap.@NotNull Builder hookBuilder) {
        super.registerHooks(hookBuilder);
        hookBuilder.addHook(this, ModifierHooks.TOOLTIP);
    }

    // ============================================================
    //  第一幕: 1) 治疗增幅 (+30%) 与溢出治疗转「幸福护盾」
    // ============================================================
    private static void onLivingHeal(LivingHealEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide) return;
        float amount = event.getAmount();
        if (amount <= 0.0f || !Float.isFinite(amount)) return;
        if (!isWearingCharm(player)) return;

        // 提升 30% 受治疗量 (HIGH 优先级, 天然放大后续「末日三问」LOWEST 的溢疗清池)
        float boosted = amount * (1.0f + HEAL_AMPLIFY_BONUS);
        event.setAmount(boosted);

        float maxHp = Math.max(1.0f, player.getMaxHealth());
        float overflow = (player.getHealth() + boosted) - maxHp;
        PlayerState state = getOrCreateState(player);
        if (overflow > 0.0f) {
            addShield(player, state, overflow * OVERFLOW_TO_SHIELD_RATIO, maxHp * MAX_SHIELD_RATIO);
        }
        state.lastTrackedHealth = Math.min(maxHp, player.getHealth() + boosted);
        syncCharmNbt(player, state);
    }

    // ============================================================
    //  第一幕: 2) 黄油蛋糕的余温 —— 进食立即温养幸福护盾
    // ============================================================
    private static void onItemUseFinish(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide) return;
        ItemStack item = event.getItem();
        if (item.isEmpty() || !item.isEdible()) return;
        if (!isWearingCharm(player)) return;

        FoodProperties food = item.getFoodProperties(player);
        int nutrition = food != null ? Math.max(1, food.getNutrition()) : 2;
        float maxHp = Math.max(1.0f, player.getMaxHealth());
        float ratio = Math.max(MIN_FOOD_SHIELD_RATIO, nutrition * FOOD_SHIELD_PER_NUTRITION);

        PlayerState state = getOrCreateState(player);
        addShield(player, state, maxHp * ratio, maxHp * MAX_SHIELD_RATIO);
        state.lastFoodLevel = player.getFoodData().getFoodLevel();
        syncCharmNbt(player, state);
    }

    // ============================================================
    //  第一幕: 3) 美好留温 —— 正面药水持续时间翻倍 & 半血以上等级 +1
    // ============================================================
    private static void onMobEffectAdded(MobEffectEvent.Added event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide) return;
        MobEffectInstance incoming = event.getEffectInstance();
        if (incoming == null) return;

        MobEffect effect = incoming.getEffect();
        // 若其他模组绕过 Applicable 强行塞入负面效果, 在护盾/终幕期间标记立即净除
        if (effect.getCategory() == MobEffectCategory.HARMFUL) {
            PlayerState state = STATES.get(player.getUUID());
            if (state != null && (state.shield > 0.0f || state.finaleTicks > 0) && isWearingCharm(player)) {
                incoming.update(new MobEffectInstance(effect, 1, 0, false, false, false));
            }
            return;
        }

        if (BOOSTED_EFFECTS.contains(incoming)) return;
        if (effect.getCategory() != MobEffectCategory.BENEFICIAL || effect.isInstantenous()) return;
        if (incoming.isInfiniteDuration() || incoming.getDuration() <= 0) return;
        if (isInternalCounterEffect(effect)) return;
        if (!isWearingCharm(player)) return;

        BOOSTED_EFFECTS.add(incoming);

        int newDuration = Mth.clamp(Math.round(incoming.getDuration() * BENEFICIAL_DURATION_MULTIPLIER),
                incoming.getDuration(), 72000);
        int newAmplifier = incoming.getAmplifier();
        if (player.getHealth() >= player.getMaxHealth() * UPGRADE_EFFECT_MIN_HP_RATIO && newAmplifier < MAX_UPGRADED_AMPLIFIER) {
            newAmplifier = Math.min(MAX_UPGRADED_AMPLIFIER, newAmplifier + 1);
        }

        MobEffectInstance upgraded = new MobEffectInstance(
                effect,
                newDuration,
                newAmplifier,
                incoming.isAmbient(),
                incoming.isVisible(),
                incoming.showIcon()
        );
        BOOSTED_EFFECTS.add(upgraded);
        incoming.update(upgraded);
    }

    // ============================================================
    //  第二幕: 1) 负面药水拦截 & 2) 受击击退免疫
    // ============================================================
    private static void onMobEffectApplicable(MobEffectEvent.Applicable event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide) return;
        MobEffectInstance incoming = event.getEffectInstance();
        if (incoming == null || incoming.getEffect().getCategory() != MobEffectCategory.HARMFUL) return;

        PlayerState state = STATES.get(player.getUUID());
        if (state != null && (state.shield > 0.0f || state.finaleTicks > 0) && isWearingCharm(player)) {
            event.setResult(Event.Result.DENY);
        }
    }

    private static void onLivingKnockBack(LivingKnockBackEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide) return;
        PlayerState state = STATES.get(player.getUUID());
        if (state != null && (state.shield > 0.0f || state.finaleTicks > 0) && isWearingCharm(player)) {
            event.setCanceled(true);
        }
    }

    // ============================================================
    //  伤害前层: 1) 穿甲/法术/吐息/激光伤害减免 40%  2) 暂存金心防原版提前吞盾
    // ============================================================
    private static void onLivingHurt(LivingHurtEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide || event.getAmount() <= 0.0f || !Float.isFinite(event.getAmount())) return;

        if (!(entity instanceof Player player)) return;
        if (!isWearingCharm(player)) return;

        PlayerState state = getOrCreateState(player);
        foldExternalAbsorption(player, state);

        // 护盾或终幕庇护下, 原版穿甲与跨模组魔法/法术/龙息/深渊/激光伤害降低 40%
        if ((state.shield > 0.0f || state.finaleTicks > 0) && isBypassOrModdedSpellDamage(event.getSource())) {
            event.setAmount(event.getAmount() * (1.0f - BYPASS_ARMOR_REDUCTION));
        }

        // 暂存原版 Absorption, 由 onLivingDamage 统一用幸福护盾结算并识别碎盾瞬间
        if (state.shield > 0.0f && player.getAbsorptionAmount() > 0.0f) {
            player.setAbsorptionAmount(0.0f);
        }
    }

    // ============================================================
    //  最终伤害层: 1) 终幕星火追击 (含远程/法术/召唤物)  2) 幸福护盾吸收与碎盾开启【幸福终幕】
    // ============================================================
    private static void onLivingDamage(LivingDamageEvent event) {
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide || event.getAmount() <= 0.0f || !Float.isFinite(event.getAmount())) return;

        // 1) 攻击者处于【幸福终幕】 -> 触发幸福星火真伤追击并回血 (兼容近战/远程/法术/召唤物)
        Player attacker = resolvePlayerAttacker(event.getSource());
        if (attacker != null && attacker != target) {
            if (!TRIGGERING_STARFIRE.contains(attacker.getUUID())) {
                PlayerState atkState = STATES.get(attacker.getUUID());
                if (atkState != null && atkState.finaleTicks > 0 && isWearingCharm(attacker)) {
                    triggerStarfirePursuit(attacker, target, atkState, event.getAmount());
                }
            }
        }

        // 2) 受击者佩戴本护符 -> 幸福护盾吸收伤害 & 检测碎盾绽放【幸福终幕】
        if (!(target instanceof Player player)) return;
        if (!isWearingCharm(player)) return;

        PlayerState state = getOrCreateState(player);
        float amount = event.getAmount();

        if (state.shield > 0.0f) {
            float absorbed = absorbWithShield(player, state, amount);
            amount -= absorbed;
            event.setAmount(amount);
        }

        state.lastTrackedHealth = Math.max(0.0f, player.getHealth() - amount);
        player.setAbsorptionAmount(state.shield);
        syncCharmNbt(player, state);
    }

    // ============================================================
    //  每 tick 驱动: 跨模组直接扣血拦截 / 非进食回饱食度捕获 / 自然温养 / 负面净除 / 终幕倒计时
    // ============================================================
    private static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) return;
        Player player = event.player;
        if (!player.isAlive()) return;

        if (!isWearingCharm(player)) {
            PlayerState removed = STATES.remove(player.getUUID());
            if (removed != null) {
                removeFinaleModifiers(player);
                if (player.getAbsorptionAmount() > 0.0f && removed.shield > 0.0f) {
                    player.setAbsorptionAmount(Math.max(0.0f, player.getAbsorptionAmount() - removed.shield));
                }
            }
            return;
        }

        PlayerState state = getOrCreateState(player);
        foldExternalAbsorption(player, state);

        float currentHp = player.getHealth();
        float maxHp = Math.max(1.0f, player.getMaxHealth());

        // 0) 跨模组直接 setHealth 扣血拦截: 若其他模组绕过 hurt 事件直接扣减玩家生命值, 优先由幸福护盾替本体承受
        if (state.lastTrackedHealth > 0.0f && currentHp < state.lastTrackedHealth - 0.05f && state.shield > 0.0f) {
            // 仅在非「末日三问」每秒延迟池自然流逝时拦截, 或当血量即将跌破半血/致死时兜底保护
            float directDrop = state.lastTrackedHealth - currentHp;
            float absorbed = absorbWithShield(player, state, directDrop);
            if (absorbed > 0.0f) {
                currentHp = Math.min(maxHp, currentHp + absorbed);
                player.setHealth(currentHp);
            }
        }
        state.lastTrackedHealth = player.getHealth();

        // 0.5) 跨模组非进食通道饱食度恢复捕获 (如机械动力注液、背包自动喂食、法术回饱食度等)
        int currentFood = player.getFoodData().getFoodLevel();
        if (state.lastFoodLevel >= 0 && currentFood > state.lastFoodLevel) {
            int gainedFood = currentFood - state.lastFoodLevel;
            float ratio = Math.max(MIN_FOOD_SHIELD_RATIO, gainedFood * FOOD_SHIELD_PER_NUTRITION);
            addShield(player, state, maxHp * ratio, maxHp * MAX_SHIELD_RATIO);
        }
        state.lastFoodLevel = currentFood;

        // 1) 冷却递减并持久化
        if (state.cooldownTicks > 0) {
            state.cooldownTicks--;
            if (state.cooldownTicks == 0 || player.tickCount % 20 == 0) {
                saveCooldown(player, state.cooldownTicks);
            }
        }

        // 2) 【幸福终幕】倒计时与结束处理
        if (state.finaleTicks > 0) {
            state.finaleTicks--;
            if (state.finaleTicks <= 0) {
                finishFinale(player, state);
            }
        }

        // 3) 黄油蛋糕的余温: 饱食度 ≥ 16 时每秒自然温养 2% 最大生命值护盾 (上限 40%)
        if (player.tickCount % 20 == 0 && state.finaleTicks <= 0) {
            if (currentFood >= WARMTH_MIN_FOOD_LEVEL) {
                float naturalCap = maxHp * NATURAL_WARMTH_CAP_RATIO;
                if (state.shield < naturalCap) {
                    addShield(player, state, maxHp * NATURAL_WARMTH_PER_SEC_RATIO, naturalCap);
                }
            }
        }

        // 4) 护盾或终幕存续期间: 自动净除一切存量负面状态、火焰与细雪冰冻
        if (state.shield > 0.0f || state.finaleTicks > 0) {
            cleanseHarmfulEffects(player);
        }

        // 5) 同步金心显示与护符 NBT
        if (Math.abs(player.getAbsorptionAmount() - state.shield) > 0.05f) {
            player.setAbsorptionAmount(state.shield);
        }
        syncCharmNbt(player, state);
    }

    private static void onPlayerClone(PlayerEvent.Clone event) {
        UUID id = event.getEntity().getUUID();
        STATES.remove(id);
        TRIGGERING_STARFIRE.remove(id);
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
        TRIGGERING_STARFIRE.remove(player.getUUID());
        if (state != null) {
            removeFinaleModifiers(player);
            saveCooldown(player, state.cooldownTicks);
        }
    }

    // ============================================================
    //  Tooltip 显示
    // ============================================================
    @Override
    public void addTooltip(@NotNull IToolStackView tool, @NotNull ModifierEntry modifier, @Nullable Player player,
                           @NotNull List<Component> tooltip, @NotNull TooltipKey tooltipKey, @NotNull TooltipFlag tooltipFlag) {
        ModDataNBT data = tool.getPersistentData();
        int shieldTenths = data.getInt(SYNC_SHIELD_TENTHS);
        int peakTenths = data.getInt(SYNC_PEAK_TENTHS);
        int finaleSec = data.getInt(SYNC_FINALE_SEC);
        int cdSec = data.getInt(SYNC_CD_SEC);

        if (finaleSec > 0) {
            tooltip.add(Component.translatable("tooltip.tprt.happiest_girl.state_finale", finaleSec));
        } else if (shieldTenths > 0) {
            tooltip.add(Component.translatable("tooltip.tprt.happiest_girl.state_shielded"));
        } else {
            tooltip.add(Component.translatable("tooltip.tprt.happiest_girl.state_warmth"));
        }

        tooltip.add(Component.translatable("tooltip.tprt.happiest_girl.shield",
                String.format("%.1f", shieldTenths / 10.0f),
                String.format("%.1f", peakTenths / 10.0f)));

        if (cdSec > 0) {
            tooltip.add(Component.translatable("tooltip.tprt.happiest_girl.cooldown", cdSec));
        } else {
            tooltip.add(Component.translatable("tooltip.tprt.happiest_girl.ready"));
        }
    }

    // ============================================================
    //  核心阶段动作: 护盾温养 / 护盾吸收与碎盾判定 / 开启终幕 / 星火追击
    // ============================================================

    private static void addShield(Player player, PlayerState state, float gain, float cap) {
        if (gain <= 0.0f) return;
        float maxAllowed = Math.max(state.shield, cap);
        state.shield = Math.min(maxAllowed, state.shield + gain);
        state.peakShield = Math.max(state.peakShield, state.shield);
        player.setAbsorptionAmount(state.shield);
    }

    /** 扣除幸福护盾并在归零时判定是否开启【幸福终幕】 */
    private static float absorbWithShield(Player player, PlayerState state, float incomingDamage) {
        if (state.shield <= 0.0f || incomingDamage <= 0.0f) return 0.0f;
        float absorbed = Math.min(state.shield, incomingDamage);
        state.shield = Math.max(0.0f, state.shield - absorbed);

        if (state.shield <= 1.0e-4f) {
            state.shield = 0.0f;
            float minPeak = player.getMaxHealth() * MIN_SHIELD_FOR_FINALE_RATIO;
            if (state.peakShield >= minPeak && state.cooldownTicks <= 0 && state.finaleTicks <= 0) {
                enterFinale(player, state);
            } else {
                state.peakShield = 0.0f;
            }
        }
        return absorbed;
    }

    /** 兼容金苹果或模组护盾提供的伤害吸收金心, 自动并入幸福护盾统一管理 */
    private static void foldExternalAbsorption(Player player, PlayerState state) {
        float currentAbs = player.getAbsorptionAmount();
        if (currentAbs > state.shield + 0.05f) {
            float maxCap = Math.max(1.0f, player.getMaxHealth()) * MAX_SHIELD_RATIO;
            state.shield = Math.min(maxCap, currentAbs);
            state.peakShield = Math.max(state.peakShield, state.shield);
        }
    }

    private static void cleanseHarmfulEffects(Player player) {
        List<MobEffect> harmful = null;
        for (MobEffectInstance inst : player.getActiveEffects()) {
            if (inst.getEffect().getCategory() == MobEffectCategory.HARMFUL) {
                if (harmful == null) harmful = new ArrayList<>();
                harmful.add(inst.getEffect());
            }
        }
        if (harmful != null) {
            for (MobEffect effect : harmful) {
                player.removeEffect(effect);
            }
        }
        if (player.isOnFire()) {
            player.clearFire();
        }
        if (player.getTicksFrozen() > 0) {
            player.setTicksFrozen(0);
        }
    }

    /** 开启第三幕【幸福终幕】 */
    private static void enterFinale(Player player, PlayerState state) {
        float minPeak = player.getMaxHealth() * MIN_SHIELD_FOR_FINALE_RATIO;
        state.finaleBonusShield = Math.max(minPeak, state.peakShield);
        state.peakShield = 0.0f;
        state.shield = 0.0f;
        state.finaleTicks = FINALE_DURATION_TICKS;

        applyFinaleModifiers(player);
        cleanseHarmfulEffects(player);

        // 爆发瞬间: 黄金妖精共鸣 8 格星火震击
        Level level = player.level();
        float atk = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        float burstDmg = atk * FINALE_ATK_TRUE_DMG_RATIO + state.finaleBonusShield * FINALE_SHIELD_TRUE_DMG_RATIO;

        List<LivingEntity> targets = level.getEntitiesOfClass(
                LivingEntity.class,
                player.getBoundingBox().inflate(FINALE_RADIUS),
                e -> isValidTarget(player, e) && e.distanceToSqr(player) <= FINALE_RADIUS * FINALE_RADIUS
        );

        UUID id = player.getUUID();
        TRIGGERING_STARFIRE.add(id);
        try {
            for (LivingEntity victim : targets) {
                dealPureTrueDamage(player, victim, burstDmg);
                double dx = victim.getX() - player.getX();
                double dz = victim.getZ() - player.getZ();
                double dist = Math.sqrt(dx * dx + dz * dz);
                if (dist > 1.0e-4) {
                    victim.knockback(0.45f, -dx / dist, -dz / dist);
                }
            }
        } finally {
            TRIGGERING_STARFIRE.remove(id);
        }

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0f, 1.25f);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 0.65f, 1.35f);
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.END_ROD,
                    player.getX(), player.getY() + 1.1, player.getZ(), 28, 0.6, 0.6, 0.6, 0.06);
            serverLevel.sendParticles(ParticleTypes.CHERRY_LEAVES,
                    player.getX(), player.getY() + 1.2, player.getZ(), 20, 0.7, 0.6, 0.7, 0.05);
            serverLevel.sendParticles(ParticleTypes.HEART,
                    player.getX(), player.getY() + 1.5, player.getZ(), 6, 0.4, 0.3, 0.4, 0.02);
        }

        player.displayClientMessage(Component.translatable("message.tprt.happiest_girl.finale_start"), true);
    }

    /** 终幕期间每次攻击命中: 幸福星火真伤追击 (取面板攻击与本击伤害较高者作为基准, 全面兼容远程/法术) + 25% 真伤回血 */
    private static void triggerStarfirePursuit(Player attacker, LivingEntity target, PlayerState state, float hitDamage) {
        if (!target.isAlive()) return;
        float basePower = Math.max((float) attacker.getAttributeValue(Attributes.ATTACK_DAMAGE), hitDamage);
        float bonusDmg = basePower * FINALE_ATK_TRUE_DMG_RATIO + state.finaleBonusShield * FINALE_SHIELD_TRUE_DMG_RATIO;
        if (bonusDmg <= 0.0f) return;

        UUID id = attacker.getUUID();
        TRIGGERING_STARFIRE.add(id);
        try {
            dealPureTrueDamage(attacker, target, bonusDmg);
        } finally {
            TRIGGERING_STARFIRE.remove(id);
        }

        float heal = bonusDmg * FINALE_LIFESTEAL_RATIO;
        if (heal > 0.0f) {
            attacker.heal(heal);
        }

        if (attacker.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.END_ROD,
                    target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
                    6, 0.25, 0.25, 0.25, 0.03);
        }
    }

    private static void finishFinale(Player player, PlayerState state) {
        state.finaleTicks = 0;
        state.finaleBonusShield = 0.0f;
        removeFinaleModifiers(player);
        state.cooldownTicks = FINALE_COOLDOWN_TICKS;
        saveCooldown(player, state.cooldownTicks);
        player.displayClientMessage(Component.translatable("message.tprt.happiest_girl.finale_end"), true);
    }

    private static void dealPureTrueDamage(Player attacker, LivingEntity target, float amount) {
        if (amount <= 0.0f || !Float.isFinite(amount)) return;
        DamageSource source = new DamageSource(
                attacker.level().registryAccess()
                        .registryOrThrow(Registries.DAMAGE_TYPE)
                        .getHolderOrThrow(ModDamageSource.TPRT_PURE_DAMAGE_TYPE),
                attacker
        );
        int prevHurtResistant = target.invulnerableTime;
        resetInvulnerableFrames(target);
        target.hurt(source, amount);
        target.invulnerableTime = prevHurtResistant;
    }

    private static void resetInvulnerableFrames(LivingEntity target) {
        target.invulnerableTime = 0;
        target.hurtTime = 0;
        if (target.isMultipartEntity()) {
            PartEntity<?>[] parts = target.getParts();
            if (parts != null) {
                for (PartEntity<?> part : parts) {
                    if (part != null) {
                        part.invulnerableTime = 0;
                    }
                }
            }
        }
    }

    /** 解析跨模组伤害来源的玩家归属 (支持直接攻击、投射物、铁魔法法术、召唤物/从属实体) */
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

    /** 识别原版及跨模组的穿甲、魔法、法术、吐息、激光、深渊诅咒等伤害类型 */
    private static boolean isBypassOrModdedSpellDamage(DamageSource source) {
        if (source.is(DamageTypeTags.BYPASSES_ARMOR)
                || source.is(DamageTypeTags.BYPASSES_EFFECTS)
                || source.is(DamageTypeTags.BYPASSES_RESISTANCE)
                || source.is(DamageTypeTags.BYPASSES_SHIELD)
                || source.is(DamageTypeTags.WITCH_RESISTANT_TO)
                || source.is(DamageTypeTags.AVOIDS_GUARDIAN_THORNS)) {
            return true;
        }
        String msgId = source.getMsgId().toLowerCase(Locale.ROOT);
        if (msgId.contains("magic") || msgId.contains("spell") || msgId.contains("wither")
                || msgId.contains("dragon") || msgId.contains("laser") || msgId.contains("abyss")
                || msgId.contains("curse") || msgId.contains("sonic") || msgId.contains("bleed")
                || msgId.contains("soul") || msgId.contains("eldritch") || msgId.contains("holy")) {
            return true;
        }
        return source.typeHolder().unwrapKey()
                .map(key -> {
                    String ns = key.location().getNamespace();
                    String path = key.location().getPath();
                    return ns.equals("irons_spellbooks")
                            || path.contains("magic") || path.contains("spell")
                            || path.contains("laser") || path.contains("abyss")
                            || path.contains("breath") || path.contains("pure");
                })
                .orElse(false);
    }

    /** 过滤模组内部用 amplifier 当层数计数器的状态效果, 防止误改其层数逻辑 */
    private static boolean isInternalCounterEffect(MobEffect effect) {
        ResourceLocation id = ForgeRegistries.MOB_EFFECTS.getKey(effect);
        if (id == null) return false;
        String path = id.getPath();
        return path.equals("overlimit_progress")
                || path.equals("overlimit_empowered")
                || path.equals("attack_shield")
                || path.equals("myl_girig_pro")
                || path.equals("ranged_greed");
    }

    private static boolean isValidTarget(Player attacker, LivingEntity candidate) {
        if (candidate == attacker || !candidate.isAlive() || candidate instanceof ArmorStand) return false;
        if (candidate.isAlliedTo(attacker)) return false;
        if (candidate instanceof OwnableEntity ownable && attacker.getUUID().equals(ownable.getOwnerUUID())) {
            return false;
        }
        return true;
    }

    // ============================================================
    //  辅助方法与内部状态结构
    // ============================================================

    private static boolean isWearingCharm(Player player) {
        var inventory = CuriosApi.getCuriosInventory(player).resolve();
        if (inventory.isEmpty()) return false;
        for (var entry : inventory.get().getCurios().entrySet()) {
            var stacks = entry.getValue().getStacks();
            for (int i = 0; i < stacks.getSlots(); i++) {
                ItemStack stack = stacks.getStackInSlot(i);
                if (!stack.isEmpty() && stack.getItem() instanceof IModifiable && ToolStack.isInitialized(stack)) {
                    ToolStack tool = ToolStack.from(stack);
                    if (!tool.isBroken() && tool.getModifierLevel(ModifierIds.happiest_girl_in_the_world) > 0) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static PlayerState getOrCreateState(Player player) {
        return STATES.computeIfAbsent(player.getUUID(), ignored -> {
            PlayerState s = new PlayerState();
            s.cooldownTicks = loadCooldown(player);
            s.lastTrackedHealth = player.getHealth();
            s.lastFoodLevel = player.getFoodData().getFoodLevel();
            return s;
        });
    }

    private static void applyFinaleModifiers(Player player) {
        AttributeInstance atk = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (atk != null && atk.getModifier(FINALE_ATK_UUID) == null) {
            atk.addTransientModifier(new AttributeModifier(
                    FINALE_ATK_UUID, "happiest_girl_finale_attack",
                    FINALE_ATTACK_BONUS, AttributeModifier.Operation.MULTIPLY_TOTAL));
        }
        AttributeInstance armor = player.getAttribute(Attributes.ARMOR);
        if (armor != null && armor.getModifier(FINALE_ARMOR_UUID) == null) {
            armor.addTransientModifier(new AttributeModifier(
                    FINALE_ARMOR_UUID, "happiest_girl_finale_armor",
                    FINALE_ARMOR_BONUS, AttributeModifier.Operation.MULTIPLY_TOTAL));
        }
        AttributeInstance atkSpeed = player.getAttribute(Attributes.ATTACK_SPEED);
        if (atkSpeed != null && atkSpeed.getModifier(FINALE_ATK_SPEED_UUID) == null) {
            atkSpeed.addTransientModifier(new AttributeModifier(
                    FINALE_ATK_SPEED_UUID, "happiest_girl_finale_attack_speed",
                    FINALE_ATK_SPEED_BONUS, AttributeModifier.Operation.MULTIPLY_TOTAL));
        }
    }

    private static void removeFinaleModifiers(Player player) {
        AttributeInstance atk = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (atk != null) atk.removeModifier(FINALE_ATK_UUID);
        AttributeInstance armor = player.getAttribute(Attributes.ARMOR);
        if (armor != null) armor.removeModifier(FINALE_ARMOR_UUID);
        AttributeInstance atkSpeed = player.getAttribute(Attributes.ATTACK_SPEED);
        if (atkSpeed != null) atkSpeed.removeModifier(FINALE_ATK_SPEED_UUID);
    }

    /** 仅在展示整数变化时同步至佩戴中的护符 NBT */
    private static void syncCharmNbt(Player player, PlayerState state) {
        var inventory = CuriosApi.getCuriosInventory(player).resolve();
        if (inventory.isEmpty()) return;

        int shieldTenths = Math.round(state.shield * 10.0f);
        int peakTenths = Math.round(Math.max(state.peakShield, state.finaleBonusShield) * 10.0f);
        int finaleSec = (state.finaleTicks + 19) / 20;
        int cdSec = (state.cooldownTicks + 19) / 20;

        for (var entry : inventory.get().getCurios().entrySet()) {
            var stacks = entry.getValue().getStacks();
            for (int i = 0; i < stacks.getSlots(); i++) {
                ItemStack stack = stacks.getStackInSlot(i);
                if (stack.isEmpty() || !(stack.getItem() instanceof IModifiable) || !ToolStack.isInitialized(stack)) continue;
                ToolStack tool = ToolStack.from(stack);
                if (tool.getModifierLevel(ModifierIds.happiest_girl_in_the_world) <= 0) continue;

                ModDataNBT data = tool.getPersistentData();
                if (data.getInt(SYNC_SHIELD_TENTHS) != shieldTenths) data.putInt(SYNC_SHIELD_TENTHS, shieldTenths);
                if (data.getInt(SYNC_PEAK_TENTHS) != peakTenths) data.putInt(SYNC_PEAK_TENTHS, peakTenths);
                if (data.getInt(SYNC_FINALE_SEC) != finaleSec) data.putInt(SYNC_FINALE_SEC, finaleSec);
                if (data.getInt(SYNC_CD_SEC) != cdSec) data.putInt(SYNC_CD_SEC, cdSec);
            }
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
        float shield;
        float peakShield;
        float finaleBonusShield;
        int finaleTicks;
        int cooldownTicks;
        float lastTrackedHealth;
        int lastFoodLevel = -1;
    }
}
