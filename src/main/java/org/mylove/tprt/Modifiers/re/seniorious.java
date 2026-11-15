package org.mylove.tprt.Modifiers.re;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mylove.tprt.common.chtholly.ChthollyCombatHelper;
import org.mylove.tprt.common.chtholly.ChthollyDataKeys;
import org.mylove.tprt.hooks.KillingHook;
import org.mylove.tprt.hooks.TprtHooks;
import slimeknights.mantle.client.TooltipKey;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.behavior.AttributesModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeHitModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.display.TooltipModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.GeneralInteractionModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.InteractionSource;
import slimeknights.tconstruct.library.modifiers.hook.interaction.InventoryTickModifierHook;
import slimeknights.tconstruct.library.modifiers.impl.NoLevelsModifier;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.helper.ToolAttackUtil;
import slimeknights.tconstruct.library.tools.nbt.IModDataView;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.library.tools.stat.ToolStats;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.function.BiConsumer;

/*
  ============================================================
  瑟尼欧里斯 (seniorious) —— 挚爱 (chtholly) · 近战核心词条
  「纵使日薄西山，即便看不到未来，此时此刻的光辉，盼君勿忘。」

  核心循环 (心火上限 256):
    1) 【苍蓝 · 追忆调音】:
       - 命中减敌 80% 回血, 偷取护甲并加自身攻击 (4%/层, 上限 5 层), 按怪物护甲+攻击算真伤转化率
       - 命中 +10 心火, 击杀 +20, 击杀被偷甲目标获得双倍击杀心火 (+40)
       - 未攒满时可随时潜行右键切入【绯红】; 攒满 256 则自动切入【绯红】!
    2) 【绯红 · 前世燃魂】:
       - 攻击无视无敌帧、100% 禁疗; 同一目标连击叠层 (上限 10 层, 每层 +18% 真伤、+3% 攻速)
       - 每刀按 1, 2, 4, 8, 16 消耗心火附加纯粹真伤 (打满 10 刀共耗 111 点; 击杀重置档位回 1)
       - 打满第 10 层连击时自动绽放终结技【妖精乡之门】: 倾泻全部剩余心火, 剩余点数越多大招真伤越高!
    3) 【余温调息】:
       - 大招收尾或中途心火耗尽后退回【苍蓝】调息 3 秒 (无减伤惩罚, 命中 3 刀提前解开并送 16 点心火)

  数值均为顶部常量, 可自由调整
  ============================================================
 */
public class seniorious extends NoLevelsModifier implements
        KillingHook, TooltipModifierHook, GeneralInteractionModifierHook, InventoryTickModifierHook,
        MeleeHitModifierHook, AttributesModifierHook {

    // ===== 心火 (上限 256) =====
    public static final float HEARTFIRE_MAX = 256.0f;            // 心火上限 (苍蓝攒满自动进入绯红)
    public static final float HEARTFIRE_PER_HIT = 10.0f;         // 苍蓝每次命中积累
    public static final float HEARTFIRE_PER_KILL = 20.0f;        // 普通击杀积累
    public static final float TUNE_COMPLETE_BONUS = 16.0f;       // 提前解除调息赠送心火

    // ===== 护符调音掠夺 =====
    public static final int STEAL_MAX_STACKS = 5;                // 调音最大层数
    public static final int STEAL_DURATION = 120;                // 调音持续时间 (120 tick = 6 秒)
    public static final float STEAL_ARMOR_PERCENT = 0.04f;       // 每层削减/获得护甲比例 (4%, 满层 20%)
    public static final float STEAL_ATTACK_PERCENT = 0.04f;      // 每层攻击力总乘区 (4%, 满层 +20%)

    // ===== 猎物质量 → 圣剑真伤转化率 =====
    public static final float MIN_QUALITY = 8.0f;                // 最低质量保底
    public static final float TRANSFER_BASE = 0.0030f;           // 转化率基数: 每点心火 = 0.30% × Q^0.75 倍本击真伤
    public static final double TRANSFER_EXP = 0.75;              // 质量幂指数
    public static final float MAX_TRANSFER_RATE = 0.0f;          // 可选软上限 (0 = 不封顶)

    // ===== 绯红燃魂: 等比消耗与连击加速 =====
    public static final int CONSUME_MAX_STEP = 4;                // 消耗档位上限: 2^4 = 16 (10 刀共耗 111)
    public static final int COMBO_MAX = 10;                      // 连击上限 (打满 10 层开启大招)
    public static final float COMBO_TRUE_DMG_BONUS = 0.18f;      // 每层连击真伤加成 (+18%, 满层 ×2.8)
    public static final float COMBO_ATK_SPEED_BONUS = 0.03f;     // 每层连击攻速加成 (+3%, 满层 +30%)

    // ===== 赋予死亡 (禁疗死印) =====
    public static final int SEVER_DURATION_TICKS = 100;          // 死印持续时间 (100 tick = 5 秒)
    public static final float SEVER_HEAL_CUT_AZURE = 0.80f;      // 苍蓝死印减疗比例 (80%)
    public static final float SEVER_HEAL_CUT_CRIMSON = 1.00f;    // 绯红/大招死印减疗比例 (100% 完全禁疗)

    // ===== 终结大招【妖精乡之门】 & 余温调息 & 切换 =====
    public static final float FAIRYLAND_RADIUS = 6.0f;           // 大招 AOE 半径 (6 格)
    public static final float FAIRYLAND_BASE_FUEL = 32.0f;       // 大招保底燃料点数 (防刚好卡 111 点剩 0 时无伤害)
    public static final float FAIRYLAND_MULTIPLIER = 2.0f;       // 大招真伤倍率: 攻击 × (保底 + 剩余心火) × 转化率 × 2.0
    public static final int REST_DURATION = 60;                  // 余温调息时间 (60 tick = 3 秒, 无减伤负面)
    public static final int TUNE_REQUIRED = 3;                   // 提前解除调息所需苍蓝命中次数
    public static final int SWITCH_COOLDOWN = 20;                // 手动切换冷却 (20 tick = 1 秒)

    public static final String MODE_AZURE = "azure";             // 苍蓝 · 追忆调音
    public static final String MODE_CRIMSON = "crimson";         // 绯红 · 前世燃魂

    private static final UUID TARGET_ARMOR_UUID = UUID.fromString("7a2e41b8-9c14-4d63-8f52-1b9c4e7a2d10");
    private static final UUID SELF_ARMOR_UUID = UUID.fromString("7a2e41b8-9c14-4d63-8f52-1b9c4e7a2d11");
    private static final UUID SELF_DAMAGE_UUID = UUID.fromString("7a2e41b8-9c14-4d63-8f52-1b9c4e7a2d12");
    private static final UUID CRIMSON_SPEED_UUID = UUID.fromString("7a2e41b8-9c14-4d63-8f52-1b9c4e7a2d13");

    private static final Map<ToolAttackContext, Float> SWING_DAMAGES = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<LivingEntity, Long> STOLEN_TARGETS = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<LivingEntity, SeverMark> SEVERED_TARGETS = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<LivingDeathEvent, Boolean> PROCESSED_DEATHS = Collections.synchronizedMap(new WeakHashMap<>());
    private static int serverTickCounter = 0;

    static {
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST, false, LivingHealEvent.class, seniorious::onLivingHeal);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, TickEvent.ServerTickEvent.class, seniorious::onServerTick);
    }

    public static void clearServerState() {
        SWING_DAMAGES.clear();
        STOLEN_TARGETS.clear();
        SEVERED_TARGETS.clear();
        PROCESSED_DEATHS.clear();
        serverTickCounter = 0;
    }

    @Override
    public int getPriority() {
        return 500;
    }

    @Override
    protected void registerHooks(ModuleHookMap.@NotNull Builder hookBuilder) {
        super.registerHooks(hookBuilder);
        hookBuilder.addHook(this, TprtHooks.KILLING_HOOK, ModifierHooks.TOOLTIP, ModifierHooks.GENERAL_INTERACT,
                ModifierHooks.INVENTORY_TICK, ModifierHooks.MELEE_HIT, ModifierHooks.ATTRIBUTES);
    }

    // ============================================================
    //  圣剑权能「赋予死亡」: 削减/阻断目标回血
    // ============================================================
    private static void onLivingHeal(LivingHealEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide || event.getAmount() <= 0.0f) return;

        SeverMark mark = SEVERED_TARGETS.get(entity);
        if (mark == null) return;
        if (entity.level().getGameTime() >= mark.until) {
            SEVERED_TARGETS.remove(entity);
            return;
        }
        if (mark.reduction >= 1.0f) {
            event.setCanceled(true);
        } else {
            event.setAmount(event.getAmount() * (1.0f - mark.reduction));
        }
        mark.allowedHealth = entity.getHealth() + (event.isCanceled() ? 0.0f : event.getAmount());
    }

    // ============================================================
    //  命中前: 记录护甲前近战伤害 + 绯红无视无敌帧 (含多部件 Boss)
    // ============================================================
    @Override
    public float beforeMeleeHit(@NotNull IToolStackView tool, @NotNull ModifierEntry modifier,
                                @NotNull ToolAttackContext context, float damage, float baseKnockback, float knockback) {
        if (!canModified(tool)) return knockback;
        LivingEntity attacker = context.getAttacker();
        if (!attacker.level().isClientSide) {
            SWING_DAMAGES.put(context, damage);
        }
        LivingEntity target = ChthollyCombatHelper.resolveLivingTarget(context.getTarget());
        if (target == null) {
            target = context.getLivingTarget();
        }
        if (isCrimson(tool) && target != null) {
            ChthollyCombatHelper.resetInvulnerableFrames(target);
        }
        return knockback;
    }

    @Override
    public void failedMeleeHit(@NotNull IToolStackView tool, @NotNull ModifierEntry modifier,
                               @NotNull ToolAttackContext context, float damageNotDealt) {
        SWING_DAMAGES.remove(context);
    }

    // ============================================================
    //  命中后: 【苍蓝】满 256 自动进绯红 | 【绯红】满 10 层按剩余心火炸大招
    // ============================================================
    @Override
    public void afterMeleeHit(@NotNull IToolStackView tool, @NotNull ModifierEntry modifier,
                              @NotNull ToolAttackContext context, float damageDealt) {
        Float recorded = SWING_DAMAGES.remove(context);
        if (!canModified(tool)) return;

        LivingEntity attacker = context.getAttacker();
        LivingEntity target = ChthollyCombatHelper.resolveLivingTarget(context.getTarget());
        if (target == null) {
            target = context.getLivingTarget();
        }
        if (attacker.level().isClientSide || target == null) return;

        long now = attacker.level().getGameTime();

        if (isAzure(tool)) {
            if (target.isAlive()) {
                applySeverMark(target, now + SEVER_DURATION_TICKS, SEVER_HEAL_CUT_AZURE);
            }
            steal(tool, target, now);
            addHeartfire(tool, HEARTFIRE_PER_HIT);
            progressTuning(tool, attacker, now);
            checkAutoEnterCrimson(tool, attacker);
        } else {
            if (target.isAlive()) {
                applySeverMark(target, now + SEVER_DURATION_TICKS, SEVER_HEAL_CUT_CRIMSON);
            }
            if (target.isDeadOrDying()) return;

            ModDataNBT data = tool.getPersistentData();
            boolean sameTarget = data.getInt(ChthollyDataKeys.SN_COMBO_TARGET_ID) == target.getId();
            int combo = sameTarget ? Math.min(COMBO_MAX, data.getInt(ChthollyDataKeys.SN_COMBO) + 1) : 1;
            if (!sameTarget) {
                data.putInt(ChthollyDataKeys.SN_CONSUME_STEP, 0);
            }
            data.putInt(ChthollyDataKeys.SN_COMBO, combo);
            data.putInt(ChthollyDataKeys.SN_COMBO_TARGET_ID, target.getId());

            int consume = currentConsume(tool);
            data.putInt(ChthollyDataKeys.SN_CONSUME_STEP,
                    Math.min(CONSUME_MAX_STEP, data.getInt(ChthollyDataKeys.SN_CONSUME_STEP) + 1));

            float heartfireBefore = data.getFloat(ChthollyDataKeys.SN_HEARTFIRE);
            float realConsume = Math.min(heartfireBefore, consume);
            addHeartfire(tool, -realConsume);

            float swingDamage = recorded != null
                    ? recorded
                    : Math.max(damageDealt, getToolAttackPower(tool, attacker));
            float rate = transferRate(tool);
            float trueDamage = swingDamage * realConsume * rate * (1.0f + combo * COMBO_TRUE_DMG_BONUS);
            if (trueDamage > 0.0f) {
                ChthollyCombatHelper.dealPureDamage(attacker, target, trueDamage);
                SeverMark mark = SEVERED_TARGETS.get(target);
                if (mark != null) {
                    mark.allowedHealth = target.getHealth();
                }
            }

            // 完整支付第 10 刀并打满 10 层连击 → 按剩余全部心火绽放【妖精乡之门】!
            if (combo >= COMBO_MAX && heartfireBefore >= consume) {
                triggerFairylandGate(tool, attacker, target, now);
                return;
            }

            // 未满 10 层但心火已烧干 → 退回【苍蓝】余温调息
            if (heartfireBefore <= consume) {
                enterAzureRest(tool, attacker, now);
            }
        }
    }

    // ============================================================
    //  击杀: 苍蓝攒满自动进绯红; 绯红击杀回充心火并重置消耗档位
    // ============================================================
    @Override
    public void onKillLivingTarget(IToolStackView tool, LivingDeathEvent event, LivingEntity attacker, LivingEntity target, int level) {
        if (!canModified(tool) || target.level().isClientSide || event.isCanceled()) return;
        if (event.getSource().getEntity() != attacker) return;

        // EventTinkerHook 会同时遍历主副手两把工具触发 KillingHook:
        // 通过事件去重 + 精确结算当前主手（含自律飞斩/斜阳御剑临时映射至主手的圣剑），彻底消除双持重复充能 Bug
        ToolStack activeWeapon = Modifier.getHeldTool(attacker, InteractionHand.MAIN_HAND);
        IToolStackView targetTool = (activeWeapon != null && activeWeapon.getModifierLevel(this) > 0) ? activeWeapon : tool;
        if (targetTool == null || !canModified(targetTool) || targetTool.getModifierLevel(this) <= 0) {
            return;
        }
        if (PROCESSED_DEATHS.put(event, Boolean.TRUE) != null) {
            return;
        }

        AttributeInstance targetArmor = target.getAttribute(Attributes.ARMOR);
        boolean wasStolen = targetArmor != null && targetArmor.getModifier(TARGET_ARMOR_UUID) != null;

        // 修复: 击杀被调音偷甲目标获得双倍击杀心火 (+40)，而非将整池心火指数翻倍
        float gain = wasStolen ? HEARTFIRE_PER_KILL * 2.0f : HEARTFIRE_PER_KILL;
        addHeartfire(targetTool, gain);

        if (isCrimson(targetTool)) {
            targetTool.getPersistentData().putInt(ChthollyDataKeys.SN_CONSUME_STEP, 0);
        } else {
            checkAutoEnterCrimson(targetTool, attacker);
        }
    }

    // ============================================================
    //  属性 (仅主手): 调音攻防增益 + 绯红连击攻速加成
    // ============================================================
    @Override
    public void addAttributes(@NotNull IToolStackView tool, @NotNull ModifierEntry modifier,
                              @NotNull EquipmentSlot slot, @NotNull BiConsumer<Attribute, AttributeModifier> consumer) {
        if (slot != EquipmentSlot.MAINHAND || !canModified(tool)) return;
        IModDataView data = tool.getPersistentData();

        int stacks = data.getInt(ChthollyDataKeys.SN_STEAL_STACKS);
        if (stacks > 0) {
            float stolenArmor = data.getFloat(ChthollyDataKeys.SN_STEAL_TARGET_ARMOR) * STEAL_ARMOR_PERCENT * stacks;
            if (stolenArmor > 0.0f) {
                consumer.accept(Attributes.ARMOR, new AttributeModifier(
                        SELF_ARMOR_UUID, "seniorious.tune_armor", stolenArmor, AttributeModifier.Operation.ADDITION));
            }
            consumer.accept(Attributes.ATTACK_DAMAGE, new AttributeModifier(
                    SELF_DAMAGE_UUID, "seniorious.tune_damage", STEAL_ATTACK_PERCENT * stacks, AttributeModifier.Operation.MULTIPLY_TOTAL));
        }

        if (isCrimson(tool)) {
            int combo = data.getInt(ChthollyDataKeys.SN_COMBO);
            if (combo > 0) {
                consumer.accept(Attributes.ATTACK_SPEED, new AttributeModifier(
                        CRIMSON_SPEED_UUID, "seniorious.crimson_speed", COMBO_ATK_SPEED_BONUS * combo, AttributeModifier.Operation.MULTIPLY_TOTAL));
            }
        }
    }

    // ============================================================
    //  每 tick (仅服务端): 仅在时间戳到期单 tick 清理一次 NBT
    // ============================================================
    @Override
    public void onInventoryTick(@NotNull IToolStackView tool, @NotNull ModifierEntry modifier, @NotNull Level world,
                                @NotNull LivingEntity holder, int itemSlot, boolean isSelected, boolean isCorrectSlot, @NotNull ItemStack stack) {
        if (world.isClientSide || !canModified(tool)) return;

        long now = world.getGameTime();
        ModDataNBT data = tool.getPersistentData();

        long stealUntil = ChthollyDataKeys.getLong(data, ChthollyDataKeys.SN_STEAL_UNTIL);
        if (stealUntil > 0L && !ChthollyDataKeys.isTimerActive(now, stealUntil, STEAL_DURATION)) {
            data.remove(ChthollyDataKeys.SN_STEAL_UNTIL);
            data.putInt(ChthollyDataKeys.SN_STEAL_STACKS, 0);
        }

        long restUntil = ChthollyDataKeys.getLong(data, ChthollyDataKeys.SN_REST_UNTIL);
        if (restUntil > 0L && !ChthollyDataKeys.isTimerActive(now, restUntil, REST_DURATION)) {
            clearRest(data);
        }

        long switchUntil = ChthollyDataKeys.getLong(data, ChthollyDataKeys.SN_SWITCH_UNTIL);
        if (switchUntil > 0L && !ChthollyDataKeys.isTimerActive(now, switchUntil, SWITCH_COOLDOWN)) {
            data.remove(ChthollyDataKeys.SN_SWITCH_UNTIL);
        }
    }

    private static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        serverTickCounter++;

        // 每 tick 拦截带死印目标的跨模组直接 setHealth 回血 (如灾变/莱特兰等绕过 LivingHealEvent 的自愈)
        if (!SEVERED_TARGETS.isEmpty()) {
            synchronized (SEVERED_TARGETS) {
                Iterator<Map.Entry<LivingEntity, SeverMark>> it = SEVERED_TARGETS.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry<LivingEntity, SeverMark> entry = it.next();
                    LivingEntity target = entry.getKey();
                    SeverMark mark = entry.getValue();
                    if (target == null || !target.isAlive() || target.isRemoved()
                            || target.level().getGameTime() >= mark.until) {
                        it.remove();
                        continue;
                    }
                    float currentHp = target.getHealth();
                    if (currentHp > mark.allowedHealth + 0.05f) {
                        float directHeal = currentHp - mark.allowedHealth;
                        float keptHeal = mark.reduction >= 1.0f ? 0.0f : directHeal * (1.0f - mark.reduction);
                        // 修复: 当目标血量已不足 1.0f 时不抬高其血线，同时保证不会设成 0.0f 绕过正常死亡结算
                        float floorHp = Math.min(1.0f, Math.max(0.01f, mark.allowedHealth));
                        float clampedHp = Math.max(floorHp, mark.allowedHealth + keptHeal);
                        target.setHealth(clampedHp);
                        mark.allowedHealth = clampedHp;
                    } else {
                        mark.allowedHealth = currentHp;
                    }
                }
            }
        }

        if (serverTickCounter % 10 != 0) return;

        if (!STOLEN_TARGETS.isEmpty()) {
            synchronized (STOLEN_TARGETS) {
                Iterator<Map.Entry<LivingEntity, Long>> it = STOLEN_TARGETS.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry<LivingEntity, Long> entry = it.next();
                    LivingEntity target = entry.getKey();
                    if (target == null || !target.isAlive() || target.isRemoved()) {
                        it.remove();
                        continue;
                    }
                    if (target.level().getGameTime() >= entry.getValue()) {
                        AttributeInstance armor = target.getAttribute(Attributes.ARMOR);
                        if (armor != null) {
                            armor.removeModifier(TARGET_ARMOR_UUID);
                        }
                        it.remove();
                    }
                }
            }
        }
    }

    // ============================================================
    //  潜行 + 右键: 不打满也可随时切入【绯红】或切回【苍蓝】
    // ============================================================
    @Override
    public @NotNull InteractionResult onToolUse(@NotNull IToolStackView tool, @NotNull ModifierEntry modifier,
                                                @NotNull Player player, @NotNull InteractionHand hand, @NotNull InteractionSource source) {
        if (source != InteractionSource.RIGHT_CLICK || tool.isBroken() || !canModified(tool)) {
            return InteractionResult.PASS;
        }
        if (!player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }

        Level level = player.level();
        long now = level.getGameTime();
        if (isRestActive(tool, now)) {
            if (!level.isClientSide) {
                int remain = ChthollyDataKeys.getRemainingSeconds(now,
                        ChthollyDataKeys.getLong(tool.getPersistentData(), ChthollyDataKeys.SN_REST_UNTIL), REST_DURATION);
                player.displayClientMessage(Component.translatable("message.tprt.seniorious.rest_locked", remain), true);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (isSwitchOnCooldown(tool, now)) {
            if (!level.isClientSide) {
                int remain = ChthollyDataKeys.getRemainingSeconds(now,
                        ChthollyDataKeys.getLong(tool.getPersistentData(), ChthollyDataKeys.SN_SWITCH_UNTIL), SWITCH_COOLDOWN);
                player.displayClientMessage(Component.translatable("message.tprt.seniorious.switch_cd", remain), true);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (isAzure(tool) && tool.getPersistentData().getFloat(ChthollyDataKeys.SN_HEARTFIRE) <= 0.0f) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.translatable("message.tprt.seniorious.no_heartfire"), true);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        if (!level.isClientSide) {
            switchMode(tool, player, now);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    // ============================================================
    //  Tooltip 面板
    // ============================================================
    @Override
    public void addTooltip(@NotNull IToolStackView tool, @NotNull ModifierEntry modifier, @Nullable Player player,
                           @NotNull List<Component> tooltip, @NotNull TooltipKey tooltipKey, @NotNull TooltipFlag tooltipFlag) {
        if (!canModified(tool)) return;

        IModDataView data = tool.getPersistentData();
        long now = player != null ? player.level().getGameTime() : 0L;

        tooltip.add(Component.translatable(isCrimson(tool)
                ? "tooltip.tprt.seniorious.mode_crimson"
                : "tooltip.tprt.seniorious.mode_azure"));
        tooltip.add(Component.translatable("tooltip.tprt.seniorious.heartfire",
                (int) data.getFloat(ChthollyDataKeys.SN_HEARTFIRE), (int) HEARTFIRE_MAX));

        float quality = data.getFloat(ChthollyDataKeys.SN_STEAL_QUALITY);
        if (quality > 0.0f) {
            tooltip.add(Component.translatable("tooltip.tprt.seniorious.rate",
                    String.format("%.2f", transferRate(tool) * 100.0f), String.format("%.0f", quality)));
        }

        if (isCrimson(tool)) {
            tooltip.add(Component.translatable("tooltip.tprt.seniorious.crimson_status",
                    currentConsume(tool), data.getInt(ChthollyDataKeys.SN_COMBO), COMBO_MAX));
        }

        if (now > 0L && isRestActive(tool, now)) {
            int restSec = ChthollyDataKeys.getRemainingSeconds(now,
                    ChthollyDataKeys.getLong(data, ChthollyDataKeys.SN_REST_UNTIL), REST_DURATION);
            tooltip.add(Component.translatable("tooltip.tprt.seniorious.rest",
                    restSec, data.getInt(ChthollyDataKeys.SN_TUNE_COUNT), TUNE_REQUIRED));
        }

        if (now > 0L && isSwitchOnCooldown(tool, now)) {
            int cdSec = ChthollyDataKeys.getRemainingSeconds(now,
                    ChthollyDataKeys.getLong(data, ChthollyDataKeys.SN_SWITCH_UNTIL), SWITCH_COOLDOWN);
            tooltip.add(Component.translatable("tooltip.tprt.seniorious.switch_cd", cdSec));
        }
    }

    // ============================================================
    //  内部实现
    // ============================================================

    private static void applySeverMark(LivingEntity target, long until, float reduction) {
        SeverMark prev = SEVERED_TARGETS.get(target);
        float bestReduction = prev != null && target.level().getGameTime() < prev.until
                ? Math.max(prev.reduction, reduction)
                : reduction;
        long bestUntil = prev != null ? Math.max(prev.until, until) : until;
        SEVERED_TARGETS.put(target, new SeverMark(bestUntil, bestReduction, target.getHealth()));
    }

    private static void steal(IToolStackView tool, LivingEntity target, long now) {
        ModDataNBT data = tool.getPersistentData();
        boolean stillActive = ChthollyDataKeys.isTimerActive(now,
                ChthollyDataKeys.getLong(data, ChthollyDataKeys.SN_STEAL_UNTIL), STEAL_DURATION);
        int stacks = stillActive ? Math.min(STEAL_MAX_STACKS, data.getInt(ChthollyDataKeys.SN_STEAL_STACKS) + 1) : 1;

        AttributeInstance targetArmorAttr = target.getAttribute(Attributes.ARMOR);
        float baseArmor;
        if (targetArmorAttr != null) {
            targetArmorAttr.removeModifier(TARGET_ARMOR_UUID);
            baseArmor = Math.max(0.0f, (float) targetArmorAttr.getValue());
        } else {
            baseArmor = Math.max(0.0f, target.getArmorValue());
        }

        AttributeInstance targetToughnessAttr = target.getAttribute(Attributes.ARMOR_TOUGHNESS);
        float baseToughness = targetToughnessAttr != null ? Math.max(0.0f, (float) targetToughnessAttr.getValue()) : 0.0f;

        AttributeInstance targetAtkAttr = target.getAttribute(Attributes.ATTACK_DAMAGE);
        float baseAttack = targetAtkAttr != null ? Math.max(0.0f, (float) targetAtkAttr.getValue()) : 0.0f;

        // 综合目标护甲、盔甲韧性、攻击力与生命上限折算质量, 全面兼容高血量/高韧性/施法型跨模组 Boss
        float hpFactor = Math.min(200.0f, Math.max(0.0f, target.getMaxHealth() - 20.0f) * 0.05f);
        float quality = Math.max(MIN_QUALITY, baseArmor + baseToughness * 1.5f + baseAttack + hpFactor);

        float savedArmor = stillActive ? Math.max(data.getFloat(ChthollyDataKeys.SN_STEAL_TARGET_ARMOR), baseArmor) : baseArmor;
        float savedQuality = stillActive ? Math.max(data.getFloat(ChthollyDataKeys.SN_STEAL_QUALITY), quality) : quality;

        data.putInt(ChthollyDataKeys.SN_STEAL_STACKS, stacks);
        ChthollyDataKeys.putLong(data, ChthollyDataKeys.SN_STEAL_UNTIL, now + STEAL_DURATION);
        data.putFloat(ChthollyDataKeys.SN_STEAL_TARGET_ARMOR, savedArmor);
        data.putFloat(ChthollyDataKeys.SN_STEAL_QUALITY, savedQuality);

        if (target.isAlive() && targetArmorAttr != null && baseArmor > 0.0f) {
            targetArmorAttr.addTransientModifier(new AttributeModifier(
                    TARGET_ARMOR_UUID, "stolen_by_seniorious",
                    -baseArmor * STEAL_ARMOR_PERCENT * stacks, AttributeModifier.Operation.ADDITION));
            STOLEN_TARGETS.put(target, now + STEAL_DURATION);
        }
    }

    private static float transferRate(IToolStackView tool) {
        float quality = tool.getPersistentData().getFloat(ChthollyDataKeys.SN_STEAL_QUALITY);
        if (quality <= 0.0f) {
            quality = MIN_QUALITY;
        }
        float rate = TRANSFER_BASE * (float) Math.pow(quality, TRANSFER_EXP);
        if (MAX_TRANSFER_RATE > 0.0f && rate > MAX_TRANSFER_RATE) {
            return MAX_TRANSFER_RATE;
        }
        return rate;
    }

    private static int currentConsume(IToolStackView tool) {
        int step = Math.min(tool.getPersistentData().getInt(ChthollyDataKeys.SN_CONSUME_STEP), CONSUME_MAX_STEP);
        return 1 << step;
    }

    private static void addHeartfire(IToolStackView tool, float delta) {
        ModDataNBT data = tool.getPersistentData();
        float next = Math.max(0.0f, Math.min(HEARTFIRE_MAX, data.getFloat(ChthollyDataKeys.SN_HEARTFIRE) + delta));
        data.putFloat(ChthollyDataKeys.SN_HEARTFIRE, next);
    }

    /** 苍蓝攒满 256 心火时自动拔剑进入【绯红】 */
    private static void checkAutoEnterCrimson(IToolStackView tool, LivingEntity attacker) {
        ModDataNBT data = tool.getPersistentData();
        if (data.getFloat(ChthollyDataKeys.SN_HEARTFIRE) < HEARTFIRE_MAX) return;

        clearRest(data);
        data.putString(ChthollyDataKeys.SN_MODE, MODE_CRIMSON);
        data.putInt(ChthollyDataKeys.SN_CONSUME_STEP, 0);
        data.putInt(ChthollyDataKeys.SN_COMBO, 0);
        data.putInt(ChthollyDataKeys.SN_COMBO_TARGET_ID, 0);

        Level level = attacker.level();
        level.playSound(null, attacker.getX(), attacker.getY(), attacker.getZ(),
                SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.75f, 1.35f);
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.CHERRY_LEAVES,
                    attacker.getX(), attacker.getY() + 1.1, attacker.getZ(), 22, 0.5, 0.6, 0.5, 0.06);
            serverLevel.sendParticles(ParticleTypes.FLAME,
                    attacker.getX(), attacker.getY() + 1.0, attacker.getZ(), 14, 0.4, 0.5, 0.4, 0.04);
        }
        if (attacker instanceof Player player) {
            player.displayClientMessage(Component.translatable("message.tprt.seniorious.auto_crimson"), true);
        }
    }

    @SuppressWarnings({"removal", "deprecation"})
    private static float getToolAttackPower(IToolStackView tool, LivingEntity attacker) {
        float toolStat = tool.getStats().get(ToolStats.ATTACK_DAMAGE);
        float attrDamage = ToolAttackUtil.getAttributeAttackDamage(tool, attacker, EquipmentSlot.MAINHAND);
        float result = Math.max(toolStat, attrDamage);
        if (result <= 0.0f) {
            AttributeInstance selfAtkAttr = attacker.getAttribute(Attributes.ATTACK_DAMAGE);
            result = selfAtkAttr != null ? (float) selfAtkAttr.getValue() : 10.0f;
        }
        return Math.max(1.0f, result);
    }

    /** 绯红打满第 10 层连击时开启【妖精乡之门】: 剩余心火越多, 大招伤害越高 */
    private static void triggerFairylandGate(IToolStackView tool, LivingEntity attacker, @Nullable LivingEntity target, long now) {
        ModDataNBT data = tool.getPersistentData();
        float remaining = Math.max(0.0f, data.getFloat(ChthollyDataKeys.SN_HEARTFIRE));
        float fuel = FAIRYLAND_BASE_FUEL + remaining;

        float attackPower = getToolAttackPower(tool, attacker);
        float damage = attackPower * fuel * transferRate(tool) * FAIRYLAND_MULTIPLIER;

        AABB searchBox = target != null
                ? target.getBoundingBox().inflate(FAIRYLAND_RADIUS)
                : attacker.getBoundingBox().inflate(FAIRYLAND_RADIUS);
        List<LivingEntity> targets = attacker.level().getEntitiesOfClass(
                LivingEntity.class,
                searchBox,
                e -> ChthollyCombatHelper.isValidHostileTarget(attacker, e));

        // 清空剩余心火, 刷新自身满层调音增益, 回归【苍蓝】余温调息 (先于造成纯粹真伤执行，确保大招击杀怪物的奖励心火不被清零覆写)
        data.putFloat(ChthollyDataKeys.SN_HEARTFIRE, 0.0f);
        data.putString(ChthollyDataKeys.SN_MODE, MODE_AZURE);
        data.putInt(ChthollyDataKeys.SN_CONSUME_STEP, 0);
        data.putInt(ChthollyDataKeys.SN_COMBO, 0);
        data.putInt(ChthollyDataKeys.SN_COMBO_TARGET_ID, 0);
        data.putInt(ChthollyDataKeys.SN_STEAL_STACKS, STEAL_MAX_STACKS);
        ChthollyDataKeys.putLong(data, ChthollyDataKeys.SN_STEAL_UNTIL, now + STEAL_DURATION);
        ChthollyDataKeys.putLong(data, ChthollyDataKeys.SN_REST_UNTIL, now + REST_DURATION);
        data.putInt(ChthollyDataKeys.SN_TUNE_COUNT, 0);

        for (LivingEntity entity : targets) {
            applySeverMark(entity, now + SEVER_DURATION_TICKS * 2L, SEVER_HEAL_CUT_CRIMSON);
            ChthollyCombatHelper.dealPureDamage(attacker, entity, damage);
            SeverMark mark = SEVERED_TARGETS.get(entity);
            if (mark != null) {
                mark.allowedHealth = entity.getHealth();
            }
        }

        Level level = attacker.level();
        level.playSound(null, attacker.getX(), attacker.getY(), attacker.getZ(),
                SoundEvents.TRIDENT_THUNDER, SoundSource.PLAYERS, 0.85f, 1.25f);
        level.playSound(null, attacker.getX(), attacker.getY(), attacker.getZ(),
                SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0f, 1.15f);
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.END_ROD,
                    attacker.getX(), attacker.getY() + 1.1, attacker.getZ(), 42, 1.8, 0.8, 1.8, 0.10);
            serverLevel.sendParticles(ParticleTypes.CHERRY_LEAVES,
                    attacker.getX(), attacker.getY() + 1.3, attacker.getZ(), 28, 1.8, 0.8, 1.8, 0.06);
            if (target != null && target != attacker) {
                serverLevel.sendParticles(ParticleTypes.END_ROD,
                        target.getX(), target.getY() + 1.1, target.getZ(), 32, 1.5, 0.8, 1.5, 0.08);
                serverLevel.sendParticles(ParticleTypes.CHERRY_LEAVES,
                        target.getX(), target.getY() + 1.3, target.getZ(), 24, 1.5, 0.8, 1.5, 0.06);
            }
        }
        if (attacker instanceof Player player) {
            player.displayClientMessage(Component.translatable("message.tprt.seniorious.fairyland", (int) remaining), true);
        }
    }

    private static void progressTuning(IToolStackView tool, LivingEntity attacker, long now) {
        if (!isRestActive(tool, now)) return;
        ModDataNBT data = tool.getPersistentData();
        int count = data.getInt(ChthollyDataKeys.SN_TUNE_COUNT) + 1;
        if (count >= TUNE_REQUIRED) {
            clearRest(data);
            addHeartfire(tool, TUNE_COMPLETE_BONUS);
            Level level = attacker.level();
            level.playSound(null, attacker.getX(), attacker.getY(), attacker.getZ(),
                    SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.9f, 1.35f);
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.END_ROD,
                        attacker.getX(), attacker.getY() + 1.0, attacker.getZ(), 14, 0.4, 0.5, 0.4, 0.04);
            }
            if (attacker instanceof Player player) {
                player.displayClientMessage(Component.translatable("message.tprt.seniorious.tuned", (int) TUNE_COMPLETE_BONUS), true);
            }
        } else {
            data.putInt(ChthollyDataKeys.SN_TUNE_COUNT, count);
        }
    }

    private static void enterAzureRest(IToolStackView tool, LivingEntity attacker, long now) {
        ModDataNBT data = tool.getPersistentData();
        data.putString(ChthollyDataKeys.SN_MODE, MODE_AZURE);
        ChthollyDataKeys.putLong(data, ChthollyDataKeys.SN_REST_UNTIL, now + REST_DURATION);
        data.putInt(ChthollyDataKeys.SN_TUNE_COUNT, 0);
        data.putInt(ChthollyDataKeys.SN_CONSUME_STEP, 0);
        data.putInt(ChthollyDataKeys.SN_COMBO, 0);

        attacker.level().playSound(null, attacker.getX(), attacker.getY(), attacker.getZ(),
                SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8f, 0.95f);
        if (attacker instanceof Player player) {
            player.displayClientMessage(Component.translatable("message.tprt.seniorious.exhausted"), true);
        }
    }

    private static void switchMode(IToolStackView tool, Player user, long now) {
        ModDataNBT data = tool.getPersistentData();
        boolean toCrimson = isAzure(tool);
        data.putString(ChthollyDataKeys.SN_MODE, toCrimson ? MODE_CRIMSON : MODE_AZURE);
        ChthollyDataKeys.putLong(data, ChthollyDataKeys.SN_SWITCH_UNTIL, now + SWITCH_COOLDOWN);
        data.putInt(ChthollyDataKeys.SN_CONSUME_STEP, 0);
        data.putInt(ChthollyDataKeys.SN_COMBO, 0);
        data.putInt(ChthollyDataKeys.SN_COMBO_TARGET_ID, 0);

        Level level = user.level();
        if (toCrimson) {
            level.playSound(null, user.getX(), user.getY(), user.getZ(),
                    SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.65f, 1.3f);
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.CHERRY_LEAVES,
                        user.getX(), user.getY() + 1.1, user.getZ(), 16, 0.45, 0.5, 0.45, 0.05);
                serverLevel.sendParticles(ParticleTypes.FLAME,
                        user.getX(), user.getY() + 1.0, user.getZ(), 10, 0.35, 0.4, 0.35, 0.03);
            }
            user.displayClientMessage(Component.translatable("message.tprt.seniorious.to_crimson"), true);
        } else {
            level.playSound(null, user.getX(), user.getY(), user.getZ(),
                    SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8f, 1.2f);
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.END_ROD,
                        user.getX(), user.getY() + 1.1, user.getZ(), 16, 0.45, 0.5, 0.45, 0.03);
            }
            user.displayClientMessage(Component.translatable("message.tprt.seniorious.to_azure"), true);
        }
    }

    private static void clearRest(ModDataNBT data) {
        data.remove(ChthollyDataKeys.SN_REST_UNTIL);
        data.putInt(ChthollyDataKeys.SN_TUNE_COUNT, 0);
    }

    private static boolean isRestActive(IToolStackView tool, long now) {
        return ChthollyDataKeys.isTimerActive(now,
                ChthollyDataKeys.getLong(tool.getPersistentData(), ChthollyDataKeys.SN_REST_UNTIL), REST_DURATION);
    }

    private static boolean isSwitchOnCooldown(IToolStackView tool, long now) {
        return ChthollyDataKeys.isTimerActive(now,
                ChthollyDataKeys.getLong(tool.getPersistentData(), ChthollyDataKeys.SN_SWITCH_UNTIL), SWITCH_COOLDOWN);
    }

    private static boolean isAzure(IToolStackView tool) {
        return getMode(tool).equals(MODE_AZURE);
    }

    private static boolean isCrimson(IToolStackView tool) {
        return getMode(tool).equals(MODE_CRIMSON);
    }

    private static String getMode(IToolStackView tool) {
        String mode = tool.getPersistentData().getString(ChthollyDataKeys.SN_MODE);
        return mode.isEmpty() ? MODE_AZURE : mode;
    }

    private static boolean canModified(IToolStackView tool) {
        return tool.hasTag(TinkerTags.Items.MELEE);
    }

    private static final class SeverMark {
        final long until;
        final float reduction;
        float allowedHealth;

        SeverMark(long until, float reduction, float allowedHealth) {
            this.until = until;
            this.reduction = reduction;
            this.allowedHealth = allowedHealth;
        }
    }
}
