package org.mylove.tprt.Modifiers.re;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mylove.tprt.common.chtholly.ChthollyCombatHelper;
import org.mylove.tprt.common.chtholly.ChthollySyncData;
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
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/*
  ============================================================
  末日时在做什么？有没有空？可以来拯救吗？
  (what_do_you_do_at_the_end_of_the_world) —— 挚爱 · 有等级护甲核心词条 (Modifier)

  等级体系 (统计全身护甲槽 + 副手盾牌总等级 L, L = 1 即拥有原版完整基准强度, 每多 1 级进一步突破上限!):
    - 单件单部件: Lv.1 (原版完整基准) | 单件纯挚爱: Lv.2 | 四件套主甲片: Lv.4 | 四件套纯挚爱满配: Lv.8
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
    public static final int MILESTONE_SURVIVE_SHIELD_LEVEL = 2;    // 解锁「救赎余温」(自救化盾) 所需总等级 (Lv.2)
    public static final float SURVIVE_SHIELD_BASE_RATIO = 0.20f;   // 救赎余温基础护盾比例 (20% + 4% × L)
    public static final float SURVIVE_SHIELD_PER_LEVEL = 0.04f;    // 每级额外提升护盾比例 (+4%)

    public static final int MILESTONE_REWIND_BURST_LEVEL = 4;      // 解锁「因果逆流」(回溯奉还延迟池真伤) 所需总等级 (Lv.4)
    public static final double REWIND_BURST_RADIUS = 6.0D;         // 因果逆流真伤震爆半径 (6 格)
    public static final float REWIND_BURST_POOL_BASE = 0.80f;      // 因果逆流奉还延迟池基础倍率 (80% + 10% × L)
    public static final float REWIND_BURST_POOL_PER_LEVEL = 0.10f; // 每级额外提升奉还倍率 (+10%)
    public static final float REWIND_BURST_MAX_HP_RATIO = 0.30f;   // 因果逆流附加自身最大生命值真伤比例 (30%)

    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD
    };

    private static final String PLAYER_NBT_COOLDOWN = "tprt_world_end_cd";
    private static final String PLAYER_NBT_DELAYED_POOL = "tprt_world_end_delayed_pool";
    private static final String PLAYER_NBT_BERSERK_TICKS = "tprt_world_end_berserk_ticks";

    private static final UUID BERSERK_ATK_SPEED_UUID = UUID.fromString("c47a91e2-6d18-4f3b-9a21-8e5c3f1d70a1");
    private static final UUID BERSERK_MOVE_SPEED_UUID = UUID.fromString("c47a91e2-6d18-4f3b-9a21-8e5c3f1d70a2");

    private static final Map<UUID, PlayerState> STATES = new ConcurrentHashMap<>();
    private static final Set<UUID> SETTLING_LETHAL = ConcurrentHashMap.newKeySet();

    static {
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, LivingHurtEvent.class, what_do_you_do_at_the_end_of_the_world::onLivingHurt);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, false, LivingDamageEvent.class, what_do_you_do_at_the_end_of_the_world::onLivingDamage);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST, false, LivingDeathEvent.class, what_do_you_do_at_the_end_of_the_world::onLivingDeath);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, false, LivingHealEvent.class, what_do_you_do_at_the_end_of_the_world::onLivingHeal);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, TickEvent.PlayerTickEvent.class, what_do_you_do_at_the_end_of_the_world::onPlayerTick);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, PlayerEvent.Clone.class, what_do_you_do_at_the_end_of_the_world::onPlayerClone);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, PlayerEvent.PlayerLoggedOutEvent.class, what_do_you_do_at_the_end_of_the_world::onPlayerLogout);
    }

    public static void clearServerState() {
        STATES.clear();
        SETTLING_LETHAL.clear();
    }

    @Override
    protected void registerHooks(ModuleHookMap.@NotNull Builder hookBuilder) {
        super.registerHooks(hookBuilder);
        hookBuilder.addHook(this, ModifierHooks.TOOLTIP);
    }

    // ============================================================
    //  等级数值曲线计算
    // ============================================================

    public static float getBaseDamageReduction(int level) {
        int extra = Math.max(0, level - 1);
        return Mth.clamp(BASE_DAMAGE_REDUCTION + 0.03f * extra, BASE_DAMAGE_REDUCTION, 0.45f);
    }

    public static float getMissingHpExtraReduction(int level) {
        int extra = Math.max(0, level - 1);
        return Mth.clamp(MISSING_HP_EXTRA_REDUCTION + 0.04f * extra, MISSING_HP_EXTRA_REDUCTION, 0.60f);
    }

    public static float getSingleHitCapRatio(int level) {
        int extra = Math.max(0, level - 1);
        return Mth.clamp(SINGLE_HIT_CAP_RATIO - 0.04f * extra, 0.20f, SINGLE_HIT_CAP_RATIO);
    }

    public static float getSecondQuestionHpRatio(int level) {
        int extra = Math.max(0, level - 1);
        return Mth.clamp(SECOND_QUESTION_HP_RATIO + 0.03f * extra, SECOND_QUESTION_HP_RATIO, 0.75f);
    }

    public static float getDeferRatio(int level) {
        int extra = Math.max(0, level - 1);
        return Mth.clamp(DEFER_RATIO + 0.04f * extra, DEFER_RATIO, 0.80f);
    }

    public static float getDrainRatioPerStep(int level) {
        int extra = Math.max(0, level - 1);
        return Mth.clamp(DRAIN_RATIO_PER_STEP - 0.015f * extra, 0.08f, DRAIN_RATIO_PER_STEP);
    }

    public static int getBerserkDurationTicks(int level) {
        int extra = Math.max(0, level - 1);
        return Mth.clamp(BERSERK_DURATION_TICKS + 10 * extra, BERSERK_DURATION_TICKS, 240);
    }

    public static float getBerserkDamageBonus(int level) {
        int extra = Math.max(0, level - 1);
        return Mth.clamp(BERSERK_DAMAGE_BONUS + 0.05f * extra, BERSERK_DAMAGE_BONUS, 0.90f);
    }

    public static float getBerserkLifestealRatio(int level) {
        int extra = Math.max(0, level - 1);
        return Mth.clamp(BERSERK_LIFESTEAL_RATIO + 0.05f * extra, BERSERK_LIFESTEAL_RATIO, 0.90f);
    }

    public static float getBerserkAttackSpeedBonus(int level) {
        int extra = Math.max(0, level - 1);
        return Mth.clamp(BERSERK_ATTACK_SPEED_BONUS + 0.05f * extra, BERSERK_ATTACK_SPEED_BONUS, 0.90f);
    }

    public static float getBerserkMoveSpeedBonus(int level) {
        int extra = Math.max(0, level - 1);
        return Mth.clamp(BERSERK_MOVE_SPEED_BONUS + 0.03f * extra, BERSERK_MOVE_SPEED_BONUS, 0.50f);
    }

    public static int getSurviveCooldownTicks(int level) {
        int extra = Math.max(0, level - 1);
        return Math.max(240, SURVIVE_COOLDOWN_TICKS - 40 * extra);
    }

    public static int getRewindCooldownTicks(int level) {
        int extra = Math.max(0, level - 1);
        return Math.max(480, REWIND_COOLDOWN_TICKS - 80 * extra);
    }

    public static float getSurviveShieldRatio(int level) {
        return Mth.clamp(SURVIVE_SHIELD_BASE_RATIO + SURVIVE_SHIELD_PER_LEVEL * Math.max(1, level), 0.24f, 0.60f);
    }

    public static float getRewindBurstPoolMult(int level) {
        return Mth.clamp(REWIND_BURST_POOL_BASE + REWIND_BURST_POOL_PER_LEVEL * Math.max(1, level), 1.00f, 2.00f);
    }

    // ============================================================
    //  进攻增益: 狂暴期间全类型造成伤害加成
    // ============================================================
    private static void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity().level().isClientSide || event.getAmount() <= 0.0f) return;
        Player attacker = ChthollyCombatHelper.resolvePlayerAttacker(event.getSource());
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
    //  最终伤害层: 1) 狂暴全伤害吸血  2) 三问受击结算
    // ============================================================
    private static void onLivingDamage(LivingDamageEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide || event.getAmount() <= 0.0f || !Float.isFinite(event.getAmount())) return;

        // 1) 攻击者处于【绯红狂暴】 -> 全伤害源吸血, 溢出抵消延迟伤害池
        Player attacker = ChthollyCombatHelper.resolvePlayerAttacker(event.getSource());
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
        if (event.getSource().is(DamageTypes.GENERIC_KILL)) return;
        if (SETTLING_LETHAL.contains(player.getUUID())) return;
        int level = getTotalLevel(player);
        if (level <= 0) return;

        PlayerState state = getOrCreateState(player);
        state.lastCombatTick = player.level().getGameTime();

        float maxHealth = Math.max(1.0f, player.getMaxHealth());
        float health = player.getHealth();

        // ----- 第一问: 已损生命动态减伤 + 单次限伤锁 -----
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
            happiest_girl_in_the_world.notifyInternalHealthChange(player, state.lastTrackedHealth);
            return;
        }

        // ----- 第二问: 跌入锚点血线种下记忆锚点 + 按等级截留伤害进延迟池 -----
        float anchorHp = maxHealth * getSecondQuestionHpRatio(level);
        float deferRatio = getDeferRatio(level);

        // Q3 处于冷却期且伤害致死: Q3 无法保护，不再截留致死伤害进延迟池，直接结算完整致死伤害
        if (state.cooldownTicks > 0 && amount >= health) {
            event.setAmount(amount);
            state.lastTrackedHealth = Math.max(0.0f, health - amount);
            happiest_girl_in_the_world.notifyInternalHealthChange(player, state.lastTrackedHealth);
            return;
        }

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
                setPlayerHealthInternal(player, state, 1.0f);
            } else {
                state.lastTrackedHealth = 1.0f;
                happiest_girl_in_the_world.notifyInternalHealthChange(player, 1.0f);
            }
            enterBerserk(player, state, level);
            return;
        }

        event.setAmount(amount);
        state.lastTrackedHealth = Math.max(0.0f, health - amount);
        happiest_girl_in_the_world.notifyInternalHealthChange(player, state.lastTrackedHealth);
    }

    // ============================================================
    //  致死兜底拦截
    // ============================================================
    private static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide) return;
        if (event.getSource().is(DamageTypes.GENERIC_KILL)) return;
        if (SETTLING_LETHAL.contains(player.getUUID())) return;
        int level = getTotalLevel(player);
        if (level <= 0) return;

        PlayerState state = getOrCreateState(player);
        if (state.berserkTicks > 0) {
            event.setCanceled(true);
            setPlayerHealthInternal(player, state, 1.0f);
            return;
        }
        if (state.cooldownTicks <= 0) {
            event.setCanceled(true);
            if (state.anchor == null) {
                plantAnchor(player, state, player.getMaxHealth() * getSecondQuestionHpRatio(level));
            }
            setPlayerHealthInternal(player, state, 1.0f);
            enterBerserk(player, state, level);
        }
    }

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
    //  每 tick 驱动
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

        // 卸载增血装备时生命上限下降导致的自然血量截断，先扣除自然差值，不把该部分视为外界伤害
        if (state.lastTrackedMaxHealth > 0.0f && maxHp < state.lastTrackedMaxHealth - 0.05f) {
            state.lastTrackedHealth = Math.min(state.lastTrackedHealth, maxHp);
        }
        state.lastTrackedMaxHealth = maxHp;

        // 0) 跨模组直接 setHealth 强制扣血拦截 (若穿戴护符，护符已在 HIGH 优先级先行吸收部分伤害，此处对剩余穿透血损套用三问减免/入池)
        float currentHp = player.getHealth();
        if (state.lastTrackedHealth > 0.0f && currentHp < state.lastTrackedHealth - 0.05f) {
            float rawDrop = state.lastTrackedHealth - currentHp;
            state.lastCombatTick = now;

            float missingRatio = Mth.clamp(1.0f - state.lastTrackedHealth / maxHp, 0.0f, 1.0f);
            float reduction = Mth.clamp(
                    getBaseDamageReduction(level) + missingRatio * getMissingHpExtraReduction(level),
                    0.0f, 0.90f);
            float allowedDrop = Math.min(rawDrop * (1.0f - reduction), maxHp * getSingleHitCapRatio(level));

            if (state.berserkTicks > 0) {
                float maxCanLose = Math.max(0.0f, state.lastTrackedHealth - 1.0f);
                if (allowedDrop > maxCanLose) {
                    state.delayedPool += (allowedDrop - maxCanLose);
                    allowedDrop = maxCanLose;
                }
                currentHp = Math.max(1.0f, state.lastTrackedHealth - allowedDrop);
                setPlayerHealthInternal(player, state, currentHp);
            } else {
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
                    setPlayerHealthInternal(player, state, 1.0f);
                    enterBerserk(player, state, level);
                } else if (targetHp > currentHp + 0.05f) {
                    currentHp = Math.min(maxHp, targetHp);
                    setPlayerHealthInternal(player, state, currentHp);
                }
            }
        }

        // 1) 记录最近一次安全陆地坐标
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
                setPlayerHealthInternal(player, state, 1.0f);
            }
            state.berserkTicks--;
            if (state.berserkTicks <= 0) {
                finishBerserk(player, state, level);
            }
            state.lastTrackedHealth = player.getHealth();
            happiest_girl_in_the_world.notifyInternalHealthChange(player, state.lastTrackedHealth);
            return;
        }

        // 4) 第二问血线检测
        if (player.getHealth() <= anchorHp && state.anchor == null && state.cooldownTicks <= 0) {
            plantAnchor(player, state, anchorHp);
        }

        // 5) 延迟伤害池按秒流逝 (通过 setPlayerHealthInternal 通知护符词条，彻底消除护符误吞延迟池 Bug)
        if (state.delayedPool > 0.0f && player.tickCount % DRAIN_INTERVAL_TICKS == 0) {
            float drainRatio = getDrainRatioPerStep(level);
            float drain = Math.min(state.delayedPool, Math.max(MIN_DRAIN_PER_STEP, state.delayedPool * drainRatio));
            state.delayedPool = Math.max(0.0f, state.delayedPool - drain);
            if (state.delayedPool < 0.05f) {
                state.delayedPool = 0.0f;
            }

            currentHp = player.getHealth();
            if (currentHp > drain) {
                setPlayerHealthInternal(player, state, currentHp - drain);
            } else if (state.cooldownTicks <= 0) {
                if (state.anchor == null) {
                    plantAnchor(player, state, anchorHp);
                }
                float unabsorbed = drain - Math.max(0.0f, currentHp - 1.0f);
                state.delayedPool += unabsorbed;
                setPlayerHealthInternal(player, state, 1.0f);
                enterBerserk(player, state, level);
                return;
            } else {
                // 第三问处于冷却中 -> 走标准致死伤害, 让幸福护盾/原版图腾/不死新生等正常接管救命
                float lethalDamage = currentHp + 10.0f;
                UUID id = player.getUUID();
                SETTLING_LETHAL.add(id);
                try {
                    player.hurt(ChthollyCombatHelper.createPureDamageSource(player), lethalDamage);
                } finally {
                    SETTLING_LETHAL.remove(id);
                }
                if (player.isDeadOrDying()) {
                    state.delayedPool = 0.0f;
                }
                state.lastTrackedHealth = player.getHealth();
                state.lastTrackedMaxHealth = maxHp;
                happiest_girl_in_the_world.notifyInternalHealthChange(player, state.lastTrackedHealth);
                return;
            }
        }

        // 6) 回到锚点血线以上、延迟池清空且脱战一段时间后, 释放旧锚点
        if (state.anchor != null && state.delayedPool <= 0.0f && player.getHealth() > anchorHp) {
            if (now - state.lastCombatTick >= ANCHOR_EXPIRE_TICKS) {
                state.anchor = null;
            }
        }

        state.lastTrackedHealth = player.getHealth();
        state.lastTrackedMaxHealth = maxHp;
    }

    private static void setPlayerHealthInternal(Player player, PlayerState state, float newHealth) {
        player.setHealth(newHealth);
        state.lastTrackedHealth = player.getHealth();
        happiest_girl_in_the_world.notifyInternalHealthChange(player, state.lastTrackedHealth);
    }

    private static void onPlayerClone(PlayerEvent.Clone event) {
        UUID id = event.getEntity().getUUID();
        STATES.remove(id);
        SETTLING_LETHAL.remove(id);
        if (event.isWasDeath()) {
            saveCooldown(event.getEntity(), 0);
            event.getEntity().getPersistentData().remove(PLAYER_NBT_DELAYED_POOL);
            event.getEntity().getPersistentData().remove(PLAYER_NBT_BERSERK_TICKS);
        } else {
            int cd = loadCooldown(event.getOriginal());
            saveCooldown(event.getEntity(), cd);
            CompoundTag origData = event.getOriginal().getPersistentData();
            if (origData.contains(PLAYER_NBT_DELAYED_POOL)) {
                event.getEntity().getPersistentData().putFloat(PLAYER_NBT_DELAYED_POOL, origData.getFloat(PLAYER_NBT_DELAYED_POOL));
            }
            if (origData.contains(PLAYER_NBT_BERSERK_TICKS)) {
                event.getEntity().getPersistentData().putInt(PLAYER_NBT_BERSERK_TICKS, origData.getInt(PLAYER_NBT_BERSERK_TICKS));
            }
        }
    }

    private static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        Player player = event.getEntity();
        PlayerState state = STATES.remove(player.getUUID());
        SETTLING_LETHAL.remove(player.getUUID());
        if (state != null) {
            removeBerserkModifiers(player);
            savePlayerState(player, state, Math.max(1, getTotalLevel(player)));
        }
    }

    // ============================================================
    //  Tooltip 显示 & HUD 快照导出 (支持全身护甲与副手盾牌, 零 ItemStack NBT 污染)
    // ============================================================

    public record WorldEndHudSnapshot(int totalLevel, int phase, int poolTenths, int berserkSec, int cdSec) {
    }

    public static WorldEndHudSnapshot getHudSnapshot(Player player) {
        int totalLevel = getTotalLevel(player);
        PlayerState state = STATES.get(player.getUUID());
        if (totalLevel <= 0 || state == null) {
            return new WorldEndHudSnapshot(totalLevel, 1, 0, 0, 0);
        }
        int phase = state.berserkTicks > 0 ? 3 : (state.anchor != null ? 2 : 1);
        return new WorldEndHudSnapshot(
                totalLevel,
                phase,
                Math.round(state.delayedPool * 10.0f),
                (state.berserkTicks + 19) / 20,
                (state.cooldownTicks + 19) / 20
        );
    }

    @Override
    public void addTooltip(@NotNull IToolStackView tool, @NotNull ModifierEntry modifier, @Nullable Player player,
                           @NotNull List<Component> tooltip, @NotNull TooltipKey tooltipKey, @NotNull TooltipFlag tooltipFlag) {
        if (!tool.hasTag(TinkerTags.Items.ARMOR)) return;

        ChthollySyncData sync = ChthollySyncData.getClient();
        int phase = sync.wePhase();
        int poolTenths = sync.wePoolTenths();
        int berserkSec = sync.weBerserkSec();
        int cdSec = sync.weCdSec();

        int effectiveLevel = player != null
                ? Math.max(modifier.getLevel(), Math.max(sync.weLevel(), getTotalLevel(player)))
                : Math.max(1, modifier.getLevel());

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

    /** 种下第二问「记忆锚点」: 采用槽位标识 + 物品类型快照代替 IdentityHashMap, 彻底修复跨维度传送后耐久快照丢失 Bug */
    private static void plantAnchor(Player player, PlayerState state, float anchorHealth) {
        ResourceKey<Level> dim = player.level().dimension();
        Vec3 pos = player.position();
        float yRot = player.getYRot();
        float xRot = player.getXRot();

        if ((!player.onGround() || player.isInLava()) && state.safePos != null && dim.equals(state.safeDimension)) {
            pos = state.safePos;
            yRot = state.safeYRot;
            xRot = state.safeXRot;
        }

        Map<String, SavedSlotDamage> slotDamages = new HashMap<>();
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            recordSlotDamage("eq:" + slot.getName(), player.getItemBySlot(slot), slotDamages);
        }
        CuriosApi.getCuriosInventory(player).resolve().ifPresent(inv -> {
            for (var entry : inv.getCurios().entrySet()) {
                String slotId = entry.getKey();
                var stacks = entry.getValue().getStacks();
                for (int i = 0; i < stacks.getSlots(); i++) {
                    recordSlotDamage("curio:" + slotId + ":" + i, stacks.getStackInSlot(i), slotDamages);
                }
            }
        });

        FoodData food = player.getFoodData();
        state.anchor = new AnchorSnapshot(
                dim, pos, yRot, xRot,
                Math.max(1.0f, anchorHealth),
                food.getFoodLevel(), food.getSaturationLevel(),
                slotDamages
        );

        Level level = player.level();
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.9f, 1.2f);
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.END_ROD,
                    pos.x, pos.y + 1.0, pos.z, 12, 0.4, 0.5, 0.4, 0.03);
        }
    }

    private static void recordSlotDamage(String slotKey, ItemStack stack, Map<String, SavedSlotDamage> map) {
        if (stack.isEmpty()) return;
        if (stack.getItem() instanceof IModifiable && ToolStack.isInitialized(stack)) {
            map.put(slotKey, new SavedSlotDamage(stack.getItem(), ToolStack.from(stack).getDamage()));
        } else if (stack.isDamageableItem()) {
            map.put(slotKey, new SavedSlotDamage(stack.getItem(), stack.getDamageValue()));
        }
    }

    private static void restoreSlotDamage(String slotKey, ItemStack stack, Map<String, SavedSlotDamage> map) {
        if (stack.isEmpty()) return;
        SavedSlotDamage saved = map.get(slotKey);
        if (saved == null || stack.getItem() != saved.item) return;
        if (stack.getItem() instanceof IModifiable && ToolStack.isInitialized(stack)) {
            ToolStack tool = ToolStack.from(stack);
            if (tool.getDamage() > saved.damage) {
                tool.setDamage(saved.damage);
            }
        } else if (stack.isDamageableItem() && stack.getDamageValue() > saved.damage) {
            stack.setDamageValue(saved.damage);
        }
    }

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
    }

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

    private static void finishBerserk(Player player, PlayerState state, int level) {
        state.berserkTicks = 0;
        removeBerserkModifiers(player);

        boolean inVoid = player.getY() < player.level().getMinBuildHeight() - 16;
        float health = player.getHealth();

        if (!inVoid && health > state.delayedPool) {
            if (state.delayedPool > 0.0f) {
                setPlayerHealthInternal(player, state, health - state.delayedPool);
                state.delayedPool = 0.0f;
            }
            state.anchor = null;
            state.cooldownTicks = getSurviveCooldownTicks(level);

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
            rewindToAnchor(player, state, level);
        }
    }

    /** 执行锚点【时光回溯】(含锚点落点防窒息/防岩浆安全修正与跨维装备耐久还原) */
    private static void rewindToAnchor(Player player, PlayerState state, int level) {
        AnchorSnapshot anchor = state.anchor;
        float clearedPool = state.delayedPool;
        state.anchor = null;
        state.delayedPool = 0.0f;
        state.cooldownTicks = getRewindCooldownTicks(level);
        saveCooldown(player, state.cooldownTicks);

        Set<UUID> burstHitRoots = new HashSet<>();
        if (level >= MILESTONE_REWIND_BURST_LEVEL) {
            unleashCausalityBurst(player, clearedPool, level, burstHitRoots);
        }

        cleanseHarmfulEffects(player);
        player.fallDistance = 0.0f;
        player.setDeltaMovement(Vec3.ZERO);

        float rewindHpRatio = getSecondQuestionHpRatio(level);
        if (anchor != null) {
            if (player instanceof ServerPlayer serverPlayer) {
                ServerLevel targetLevel = serverPlayer.server.getLevel(anchor.dimension);
                if (targetLevel == null) {
                    targetLevel = serverPlayer.serverLevel();
                }
                Vec3 safeLanding = findSafeRewindPos(targetLevel, serverPlayer, anchor.pos);
                if (targetLevel != serverPlayer.serverLevel()) {
                    serverPlayer.teleportTo(targetLevel, safeLanding.x, safeLanding.y, safeLanding.z, anchor.yRot, anchor.xRot);
                } else {
                    serverPlayer.teleportTo(safeLanding.x, safeLanding.y, safeLanding.z);
                }
                serverPlayer.fallDistance = 0.0f;
            }

            float targetHp = Math.min(player.getMaxHealth(),
                    Math.max(anchor.health, player.getMaxHealth() * rewindHpRatio));
            setPlayerHealthInternal(player, state, Math.max(1.0f, targetHp));

            FoodData food = player.getFoodData();
            if (food.getFoodLevel() < anchor.foodLevel) {
                food.setFoodLevel(anchor.foodLevel);
            }
            if (food.getSaturationLevel() < anchor.saturation) {
                food.setSaturation(anchor.saturation);
            }

            for (EquipmentSlot slot : EquipmentSlot.values()) {
                restoreSlotDamage("eq:" + slot.getName(), player.getItemBySlot(slot), anchor.slotDamages);
            }
            CuriosApi.getCuriosInventory(player).resolve().ifPresent(inv -> {
                for (var entry : inv.getCurios().entrySet()) {
                    String slotId = entry.getKey();
                    var stacks = entry.getValue().getStacks();
                    for (int i = 0; i < stacks.getSlots(); i++) {
                        restoreSlotDamage("curio:" + slotId + ":" + i, stacks.getStackInSlot(i), anchor.slotDamages);
                    }
                }
            });
        } else {
            setPlayerHealthInternal(player, state, Math.max(1.0f, player.getMaxHealth() * rewindHpRatio));
        }

        if (level >= MILESTONE_REWIND_BURST_LEVEL) {
            unleashCausalityBurst(player, clearedPool, level, burstHitRoots);
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

    /** 校验回溯锚点是否已被后续放置的方块填埋或被岩浆淹没, 自动寻找附近安全站立空间防卡墙窒息 */
    private static Vec3 findSafeRewindPos(ServerLevel level, Player player, Vec3 desired) {
        if (isSpaceSafe(level, player, desired)) {
            return desired;
        }
        for (int dy = 1; dy <= 4; dy++) {
            Vec3 up = desired.add(0.0D, dy, 0.0D);
            if (isSpaceSafe(level, player, up)) {
                return up;
            }
        }
        for (int r = 1; r <= 2; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.abs(dx) != r && Math.abs(dz) != r) continue;
                    for (int dy = 0; dy <= 2; dy++) {
                        Vec3 candidate = desired.add(dx, dy, dz);
                        if (isSpaceSafe(level, player, candidate)) {
                            return candidate;
                        }
                    }
                }
            }
        }
        return desired;
    }

    private static boolean isSpaceSafe(ServerLevel level, Player player, Vec3 pos) {
        double halfW = player.getBbWidth() * 0.45D;
        double h = player.getBbHeight();
        AABB box = new AABB(pos.x - halfW, pos.y, pos.z - halfW, pos.x + halfW, pos.y + h, pos.z + halfW);
        if (!level.noCollision(player, box)) {
            return false;
        }
        BlockPos feet = BlockPos.containing(pos.x, pos.y, pos.z);
        BlockPos head = BlockPos.containing(pos.x, pos.y + 1.0D, pos.z);
        return !level.getFluidState(feet).is(FluidTags.LAVA) && !level.getFluidState(head).is(FluidTags.LAVA);
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

    /** Lv.4+ 终末因果震爆: 将回溯清空的延迟伤害池转化为 6 格跨模组纯粹真伤奉还给敌人 (跨起点与落点共享去重集合，防止锚点重叠时双倍暴击) */
    private static void unleashCausalityBurst(Player player, float clearedPool, int level, Set<UUID> hitRoots) {
        float burstDamage = Math.max(0.0f, clearedPool) * getRewindBurstPoolMult(level)
                + player.getMaxHealth() * REWIND_BURST_MAX_HP_RATIO;
        if (burstDamage <= 0.0f) return;

        for (Entity entity : player.level().getEntities(player, player.getBoundingBox().inflate(REWIND_BURST_RADIUS))) {
            LivingEntity target = ChthollyCombatHelper.resolveLivingTarget(entity);
            if (target == null || !ChthollyCombatHelper.isValidHostileTarget(player, target)) continue;
            if (!hitRoots.add(target.getUUID())) continue;

            ChthollyCombatHelper.dealPureDamage(player, target, burstDamage);
        }
    }

    // ============================================================
    //  辅助方法与内部状态结构
    // ============================================================

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
            CompoundTag data = player.getPersistentData();
            if (data.contains(PLAYER_NBT_DELAYED_POOL)) {
                s.delayedPool = data.getFloat(PLAYER_NBT_DELAYED_POOL);
                data.remove(PLAYER_NBT_DELAYED_POOL);
            }
            if (data.contains(PLAYER_NBT_BERSERK_TICKS)) {
                s.berserkTicks = data.getInt(PLAYER_NBT_BERSERK_TICKS);
                data.remove(PLAYER_NBT_BERSERK_TICKS);
                if (s.berserkTicks > 0) {
                    applyBerserkModifiers(player, Math.max(1, getTotalLevel(player)));
                }
            }
            s.lastTrackedHealth = player.getHealth();
            s.lastTrackedMaxHealth = player.getMaxHealth();
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

    private static void savePlayerState(Player player, PlayerState state, int level) {
        CompoundTag data = player.getPersistentData();
        int cd = state.cooldownTicks;
        if (state.berserkTicks > 0 && cd <= 0) {
            cd = getRewindCooldownTicks(level);
        }
        if (cd > 0) {
            data.putInt(PLAYER_NBT_COOLDOWN, cd);
        } else {
            data.remove(PLAYER_NBT_COOLDOWN);
        }
        if (state.delayedPool > 0.0f) {
            data.putFloat(PLAYER_NBT_DELAYED_POOL, state.delayedPool);
        } else {
            data.remove(PLAYER_NBT_DELAYED_POOL);
        }
        if (state.berserkTicks > 0) {
            data.putInt(PLAYER_NBT_BERSERK_TICKS, state.berserkTicks);
        } else {
            data.remove(PLAYER_NBT_BERSERK_TICKS);
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

    private record SavedSlotDamage(Item item, int damage) {
    }

    private static final class PlayerState {
        AnchorSnapshot anchor;
        float delayedPool;
        int berserkTicks;
        int cooldownTicks;
        long lastCombatTick;
        float lastTrackedHealth;
        float lastTrackedMaxHealth;
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
            Map<String, SavedSlotDamage> slotDamages
    ) {
    }
}
