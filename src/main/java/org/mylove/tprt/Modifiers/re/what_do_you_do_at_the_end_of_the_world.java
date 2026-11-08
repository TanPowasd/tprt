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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
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
import org.mylove.tprt.Tprt;
import org.mylove.tprt.registries.ModDamageSource;
import org.mylove.tprt.registries.ModifierIds;
import slimeknights.mantle.client.TooltipKey;
import slimeknights.tconstruct.common.TinkerTags;
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
  (what_do_you_do_at_the_end_of_the_world) —— 挚爱 · 护甲核心词条

  设计原则:
    - 零硬编码点名联动, 纯底层机制自洽, 与其他词条自然叠加
    - 全面兼容其他模组 (跨模组直接 setHealth 强制扣血/斩杀拦截、铁魔法/召唤物/远程投射物狂暴增伤与吸血、
      护甲 + 主副手 + Curios 饰品全耐久快照回溯)

  【第一问】末日时在做什么？ (常驻):
    - 基于已损失生命值比例提供动态全伤害减免
    - 在最终伤害结算层钳制单次受伤上限为最大生命值的 50% (防一击秒杀, 含模组直接改血拦截)

  【第二问】有没有空？ (生命值 ≤ 50% 触发):
    - 首次跌至 ≤ 50% 最大生命值时种下「记忆锚点」
      (记录 50% 血量、安全陆地坐标与维度、饱食度、全身装备与 Curios 饰品耐久)
    - 处于 ≤ 50% 血线时, 受到伤害的 50% 截留进「延迟伤害池」
    - 延迟伤害池按秒直接扣减生命值 (不走 hurt, 不误耗次数盾/抛光/受击触发器)

  【第三问】可以来拯救吗？ (受到致命伤触发):
    - 拦截致死并锁 1 血, 溢出致死量记入延迟伤害池, 进入「绯红狂暴」
    - 狂暴期间: 全类型造成伤害加成 (含法术/召唤物/远程) + 攻速/移速提升 + 全伤害源吸血
    - 生命吸满后的溢出吸血/治疗直接抵消「延迟伤害池」
    - 狂暴结束结算剩余延迟池:
        * 活得了 (当前生命 > 延迟池且未坠入虚空): 扣除延迟池原地存活, 短冷却
        * 活不了 (当前生命 ≤ 延迟池或深陷虚空): 引爆锚点【时光回溯】,
          清空延迟池与负面效果, 还原坐标、50% 血量、饱食度与全身装备/饰品耐久, 完整冷却

  数值均为顶部常量, 可自由调整
  ============================================================
 */
public class what_do_you_do_at_the_end_of_the_world extends NoLevelsModifier implements TooltipModifierHook {

    // ===== 第一问: 末日时在做什么？ (减伤 + 限伤) =====
    public static final float BASE_DAMAGE_REDUCTION = 0.15f;       // 基础常驻全伤害减免 (15%)
    public static final float MISSING_HP_EXTRA_REDUCTION = 0.25f;  // 残血最大额外减伤 (+25%, 空血时最高 40%)
    public static final float SINGLE_HIT_CAP_RATIO = 0.50f;        // 单次最终伤害上限 (最大生命值的 50%)

    // ===== 第二问: 有没有空？ (锚点 + 伤害延迟) =====
    public static final float SECOND_QUESTION_HP_RATIO = 0.50f;    // 进入第二问的血量阈值 (50% 最大生命值)
    public static final float DEFER_RATIO = 0.50f;                 // 第二问截留进延迟池的伤害比例 (50%)
    public static final int DRAIN_INTERVAL_TICKS = 20;             // 延迟伤害池结算间隔 (20 tick = 1 秒)
    public static final float DRAIN_RATIO_PER_STEP = 0.20f;        // 每秒流逝当前延迟池的比例 (20%)
    public static final float MIN_DRAIN_PER_STEP = 1.0f;           // 每秒最低流逝点数 (1.0 点)
    public static final int ANCHOR_EXPIRE_TICKS = 200;             // 回到半血以上且脱战多久后释放旧锚点 (10 秒)

    // ===== 第三问: 可以来拯救吗？ (锁血 + 吸血 + 狂暴 + 回溯) =====
    public static final int BERSERK_DURATION_TICKS = 120;          // 绯红狂暴持续时间 (120 tick = 6 秒)
    public static final float BERSERK_DAMAGE_BONUS = 0.35f;        // 狂暴期间全类型造成伤害加成 (+35%)
    public static final float BERSERK_ATTACK_SPEED_BONUS = 0.40f;  // 狂暴期间攻击速度加成 (+40%)
    public static final float BERSERK_MOVE_SPEED_BONUS = 0.20f;    // 狂暴期间移动速度加成 (+20%)
    public static final float BERSERK_LIFESTEAL_RATIO = 0.35f;     // 狂暴期间全伤害吸血比例 (35%, 溢出抵消延迟池)
    public static final int SURVIVE_COOLDOWN_TICKS = 600;          // 狂暴结束成功自救的冷却 (600 tick = 30 秒)
    public static final int REWIND_COOLDOWN_TICKS = 1200;          // 狂暴结束触发回溯的冷却 (1200 tick = 60 秒)
    public static final int REWIND_INVULNERABLE_TICKS = 40;        // 回溯落地短暂无敌帧 (40 tick = 2 秒)

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
    //  进攻增益: 狂暴期间全类型造成伤害加成 (兼容近战/远程/铁魔法法术/召唤物)
    // ============================================================
    private static void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity().level().isClientSide || event.getAmount() <= 0.0f) return;
        Player attacker = resolvePlayerAttacker(event.getSource());
        if (attacker == null || attacker == event.getEntity()) return;

        PlayerState state = STATES.get(attacker.getUUID());
        if (state != null && state.berserkTicks > 0 && isWearing(attacker)) {
            event.setAmount(event.getAmount() * (1.0f + BERSERK_DAMAGE_BONUS));
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
            if (atkState != null && atkState.berserkTicks > 0 && isWearing(attacker)) {
                applyBerserkLifesteal(attacker, atkState, event.getAmount() * BERSERK_LIFESTEAL_RATIO);
            }
        }

        // 2) 受击者穿戴本词条 -> 执行第一问/第二问/第三问
        if (!(entity instanceof Player player)) return;
        if (event.getSource().is(DamageTypes.GENERIC_KILL)) return; // 放行 /kill 指令
        if (SETTLING_LETHAL.contains(player.getUUID())) return;    // 冷却期延迟池致死结算不重复拦截
        if (!isWearing(player)) return;

        PlayerState state = getOrCreateState(player);
        state.lastCombatTick = player.level().getGameTime();

        float maxHealth = Math.max(1.0f, player.getMaxHealth());
        float health = player.getHealth();

        // ----- 第一问: 已损生命动态减伤 + 50% 单次限伤 -----
        float missingRatio = Mth.clamp(1.0f - health / maxHealth, 0.0f, 1.0f);
        float reduction = Mth.clamp(BASE_DAMAGE_REDUCTION + missingRatio * MISSING_HP_EXTRA_REDUCTION, 0.0f, 0.90f);
        float amount = event.getAmount() * (1.0f - reduction);
        amount = Math.min(amount, maxHealth * SINGLE_HIT_CAP_RATIO);

        // ----- 若已处于第三问【绯红狂暴】: 强制锁 1 血, 溢出伤害记入延迟池 -----
        if (state.berserkTicks > 0) {
            float allowed = Math.max(0.0f, health - 1.0f);
            if (amount > allowed) {
                state.delayedPool += (amount - allowed);
                amount = allowed;
            }
            event.setAmount(amount);
            state.lastTrackedHealth = Math.max(1.0f, health - amount);
            syncArmorNbt(player, state);
            return;
        }

        // ----- 第二问: 跌入 ≤ 50% 血线种下记忆锚点 + 截留 50% 伤害进延迟池 -----
        float halfHp = maxHealth * SECOND_QUESTION_HP_RATIO;
        if (health > halfHp) {
            float aboveHalf = Math.min(amount, health - halfHp);
            float belowHalf = amount - aboveHalf;
            if (health - aboveHalf <= halfHp + 1.0e-4f) {
                if (state.anchor == null && state.cooldownTicks <= 0) {
                    plantAnchor(player, state, halfHp);
                }
            }
            if (belowHalf > 0.0f) {
                float deferred = belowHalf * DEFER_RATIO;
                state.delayedPool += deferred;
                amount = aboveHalf + (belowHalf - deferred);
            }
        } else {
            if (state.anchor == null && state.cooldownTicks <= 0) {
                plantAnchor(player, state, halfHp);
            }
            float deferred = amount * DEFER_RATIO;
            state.delayedPool += deferred;
            amount -= deferred;
        }

        // ----- 第三问: 即将致死时锁 1 血并开启【绯红狂暴】 -----
        if (amount >= health && state.cooldownTicks <= 0) {
            if (state.anchor == null) {
                plantAnchor(player, state, halfHp);
            }
            float allowed = Math.max(0.0f, health - 1.0f);
            state.delayedPool += (amount - allowed);
            event.setAmount(allowed);
            if (health < 1.0f) {
                player.setHealth(1.0f);
            }
            state.lastTrackedHealth = 1.0f;
            enterBerserk(player, state);
            return;
        }

        event.setAmount(amount);
        state.lastTrackedHealth = Math.max(0.0f, health - amount);
        syncArmorNbt(player, state);
    }

    // ============================================================
    //  致死兜底拦截 (优先级 HIGHEST, 天然早于不死新生等 NORMAL 死亡监听)
    // ============================================================
    private static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide) return;
        if (event.getSource().is(DamageTypes.GENERIC_KILL)) return;
        if (SETTLING_LETHAL.contains(player.getUUID())) return;
        if (!isWearing(player)) return;

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
                plantAnchor(player, state, player.getMaxHealth() * SECOND_QUESTION_HP_RATIO);
            }
            player.setHealth(1.0f);
            state.lastTrackedHealth = 1.0f;
            enterBerserk(player, state);
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

        if (!isWearing(player)) {
            PlayerState removed = STATES.remove(player.getUUID());
            if (removed != null) {
                removeBerserkModifiers(player);
            }
            return;
        }

        PlayerState state = getOrCreateState(player);
        long now = player.level().getGameTime();
        float maxHp = Math.max(1.0f, player.getMaxHealth());
        float halfHp = maxHp * SECOND_QUESTION_HP_RATIO;

        // 0) 跨模组直接 setHealth 强制扣血/百分比斩杀拦截 (防其他模组绕过 LivingDamageEvent 直接改血秒杀)
        float currentHp = player.getHealth();
        if (state.lastTrackedHealth > 0.0f && currentHp < state.lastTrackedHealth - 0.05f) {
            float rawDrop = state.lastTrackedHealth - currentHp;
            state.lastCombatTick = now;

            // 套用第一问动态减伤与 50% 单次限伤
            float missingRatio = Mth.clamp(1.0f - state.lastTrackedHealth / maxHp, 0.0f, 1.0f);
            float reduction = Mth.clamp(BASE_DAMAGE_REDUCTION + missingRatio * MISSING_HP_EXTRA_REDUCTION, 0.0f, 0.90f);
            float allowedDrop = Math.min(rawDrop * (1.0f - reduction), maxHp * SINGLE_HIT_CAP_RATIO);

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
                // 第二问: 跌破半血部分截留 50% 入延迟池
                float startHp = state.lastTrackedHealth;
                if (startHp > halfHp) {
                    float aboveHalf = Math.min(allowedDrop, startHp - halfHp);
                    float belowHalf = allowedDrop - aboveHalf;
                    if (startHp - aboveHalf <= halfHp + 1.0e-4f && state.anchor == null && state.cooldownTicks <= 0) {
                        plantAnchor(player, state, halfHp);
                    }
                    if (belowHalf > 0.0f) {
                        float deferred = belowHalf * DEFER_RATIO;
                        state.delayedPool += deferred;
                        allowedDrop = aboveHalf + (belowHalf - deferred);
                    }
                } else {
                    if (state.anchor == null && state.cooldownTicks <= 0) {
                        plantAnchor(player, state, halfHp);
                    }
                    float deferred = allowedDrop * DEFER_RATIO;
                    state.delayedPool += deferred;
                    allowedDrop -= deferred;
                }

                float targetHp = startHp - allowedDrop;
                if (targetHp <= 1.0f && state.cooldownTicks <= 0) {
                    if (state.anchor == null) {
                        plantAnchor(player, state, halfHp);
                    }
                    state.delayedPool += Math.max(0.0f, 1.0f - targetHp);
                    currentHp = 1.0f;
                    player.setHealth(1.0f);
                    enterBerserk(player, state);
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
                finishBerserk(player, state);
            }
            state.lastTrackedHealth = player.getHealth();
            syncArmorNbt(player, state);
            return;
        }

        // 4) 第二问血线检测
        if (player.getHealth() <= halfHp && state.anchor == null && state.cooldownTicks <= 0) {
            plantAnchor(player, state, halfHp);
        }

        // 5) 延迟伤害池按秒流逝 (非致死走 setHealth, 不污染受击/次数盾/抛光)
        if (state.delayedPool > 0.0f && player.tickCount % DRAIN_INTERVAL_TICKS == 0) {
            float drain = Math.min(state.delayedPool, Math.max(MIN_DRAIN_PER_STEP, state.delayedPool * DRAIN_RATIO_PER_STEP));
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
                    plantAnchor(player, state, halfHp);
                }
                float unabsorbed = drain - Math.max(0.0f, currentHp - 1.0f);
                state.delayedPool += unabsorbed;
                player.setHealth(1.0f);
                state.lastTrackedHealth = 1.0f;
                enterBerserk(player, state);
                syncArmorNbt(player, state);
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
                syncArmorNbt(player, state);
                return;
            }
        }

        // 6) 回到半血以上、延迟池清空且脱战一段时间后, 释放旧锚点以便下一场战斗重新立锚
        if (state.anchor != null && state.delayedPool <= 0.0f && player.getHealth() > halfHp) {
            if (now - state.lastCombatTick >= ANCHOR_EXPIRE_TICKS) {
                state.anchor = null;
            }
        }

        state.lastTrackedHealth = player.getHealth();
        syncArmorNbt(player, state);
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
    //  Tooltip 显示
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

        if (phase == 3) {
            tooltip.add(Component.translatable("tooltip.tprt.world_end.phase_3", berserkSec));
        } else if (phase == 2) {
            tooltip.add(Component.translatable("tooltip.tprt.world_end.phase_2"));
        } else {
            tooltip.add(Component.translatable("tooltip.tprt.world_end.phase_1"));
        }

        if (poolTenths > 0) {
            tooltip.add(Component.translatable("tooltip.tprt.world_end.delayed_pool",
                    String.format("%.1f", poolTenths / 10.0f)));
        }

        if (cdSec > 0) {
            tooltip.add(Component.translatable("tooltip.tprt.world_end.cooldown", cdSec));
        } else {
            tooltip.add(Component.translatable("tooltip.tprt.world_end.ready"));
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
    private static void enterBerserk(Player player, PlayerState state) {
        state.berserkTicks = BERSERK_DURATION_TICKS;
        applyBerserkModifiers(player);

        Level level = player.level();
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 0.7f, 1.25f);
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.FLAME,
                    player.getX(), player.getY() + 1.0, player.getZ(), 24, 0.5, 0.6, 0.5, 0.06);
            serverLevel.sendParticles(ParticleTypes.CHERRY_LEAVES,
                    player.getX(), player.getY() + 1.2, player.getZ(), 16, 0.6, 0.6, 0.6, 0.05);
        }
        player.displayClientMessage(Component.translatable("message.tprt.world_end.berserk"), true);
        syncArmorNbt(player, state);
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

    /** 狂暴结束结算: 活得了原地续战, 活不了触发锚点【时光回溯】 */
    private static void finishBerserk(Player player, PlayerState state) {
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
            state.cooldownTicks = SURVIVE_COOLDOWN_TICKS;
            state.lastTrackedHealth = player.getHealth();
            saveCooldown(player, state.cooldownTicks);

            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.7f, 1.3f);
            player.displayClientMessage(Component.translatable("message.tprt.world_end.survived"), true);
        } else {
            // 活不了: 引爆记忆锚点【时光回溯】
            rewindToAnchor(player, state);
        }
    }

    /** 执行锚点【时光回溯】 */
    private static void rewindToAnchor(Player player, PlayerState state) {
        AnchorSnapshot anchor = state.anchor;
        state.anchor = null;
        state.delayedPool = 0.0f;
        state.cooldownTicks = REWIND_COOLDOWN_TICKS;
        saveCooldown(player, state.cooldownTicks);

        // 1) 清除所有负面状态、火焰与细雪冰冻
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
        player.fallDistance = 0.0f;
        player.setDeltaMovement(Vec3.ZERO);

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

            // 3) 回溯生命值 (至少恢复至当前最大生命值的 50%)
            float targetHp = Math.min(player.getMaxHealth(),
                    Math.max(anchor.health, player.getMaxHealth() * SECOND_QUESTION_HP_RATIO));
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
            player.setHealth(Math.max(1.0f, player.getMaxHealth() * SECOND_QUESTION_HP_RATIO));
        }

        state.lastTrackedHealth = player.getHealth();
        player.invulnerableTime = Math.max(player.invulnerableTime, REWIND_INVULNERABLE_TICKS);

        Level level = player.level();
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.CHORUS_FRUIT_TELEPORT, SoundSource.PLAYERS, 0.9f, 1.15f);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0f, 1.1f);
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.REVERSE_PORTAL,
                    player.getX(), player.getY() + 1.0, player.getZ(), 32, 0.4, 0.7, 0.4, 0.08);
            serverLevel.sendParticles(ParticleTypes.END_ROD,
                    player.getX(), player.getY() + 1.0, player.getZ(), 16, 0.4, 0.6, 0.4, 0.04);
        }
        player.displayClientMessage(Component.translatable("message.tprt.world_end.rewound"), true);
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

    private static boolean isWearing(Player player) {
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack stack = player.getItemBySlot(slot);
            if (!stack.isEmpty() && stack.getItem() instanceof IModifiable && ToolStack.isInitialized(stack)) {
                ToolStack tool = ToolStack.from(stack);
                if (!tool.isBroken() && tool.getModifierLevel(ModifierIds.what_do_you_do_at_the_end_of_the_world) > 0) {
                    return true;
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
            if (player.onGround() && !player.isInLava()) {
                s.safeDimension = player.level().dimension();
                s.safePos = player.position();
                s.safeYRot = player.getYRot();
                s.safeXRot = player.getXRot();
            }
            return s;
        });
    }

    private static void applyBerserkModifiers(Player player) {
        AttributeInstance atkSpeed = player.getAttribute(Attributes.ATTACK_SPEED);
        if (atkSpeed != null && atkSpeed.getModifier(BERSERK_ATK_SPEED_UUID) == null) {
            atkSpeed.addTransientModifier(new AttributeModifier(
                    BERSERK_ATK_SPEED_UUID, "world_end_berserk_attack_speed",
                    BERSERK_ATTACK_SPEED_BONUS, AttributeModifier.Operation.MULTIPLY_TOTAL));
        }
        AttributeInstance moveSpeed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (moveSpeed != null && moveSpeed.getModifier(BERSERK_MOVE_SPEED_UUID) == null) {
            moveSpeed.addTransientModifier(new AttributeModifier(
                    BERSERK_MOVE_SPEED_UUID, "world_end_berserk_move_speed",
                    BERSERK_MOVE_SPEED_BONUS, AttributeModifier.Operation.MULTIPLY_TOTAL));
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
    private static void syncArmorNbt(Player player, PlayerState state) {
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
