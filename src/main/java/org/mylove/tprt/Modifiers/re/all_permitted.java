package org.mylove.tprt.Modifiers.re;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mylove.tprt.Tprt;
import org.mylove.tprt.hooks.KillingHook;
import org.mylove.tprt.hooks.TprtHooks;
import org.mylove.tprt.registries.ModDamageSource;
import slimeknights.mantle.client.TooltipKey;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.behavior.AttributesModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeDamageModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeHitModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.display.TooltipModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.GeneralInteractionModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.InteractionSource;
import slimeknights.tconstruct.library.modifiers.hook.interaction.InventoryTickModifierHook;
import slimeknights.tconstruct.library.modifiers.impl.NoLevelsModifier;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.nbt.IModDataView;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.function.BiConsumer;

/*
  ============================================================
  万般皆允，百无禁忌 (all_permitted) —— 挚爱 · 近战核心词条之一
  信条: 万物皆虚，万事皆允

  核心循环: 潜行右键切换形态 | 【虚】掠夺蓄力 → 【允】连斩真伤 → 罪业与赎罪

  【虚】万物皆虚 (蓄力/掠夺/赎罪):
    - 命中叠 1 层偷取 (上限 5 层 / 持续 5 秒):
        * 读取目标真实未削减护甲与攻击力, 削减目标 2%/层 护甲
        * 主手持有时获得等量护甲与 +2%/层 攻击力总乘区 (切入【允】爆发期仍保留剩余时间)
        * 记录猎物质量 Q = max(5, 护甲 + 攻击力), 偷取有效期内保留最高 Q
    - 转化率 = 0.25% × Q^0.75 (每点禁忌转化为近战伤害对应比例的纯粹真伤)
    - 命中 (含一击秒杀) +4 禁忌; 击杀普通目标 +10, 击杀被偷取目标禁忌翻倍 (×2)
    - 禁忌蓄满 100 → 自动超载: 对周围 4 格敌对目标造成全量真伤 AOE, 事后陷入 10 秒强罪业 (-50% 伤害)

  【允】万事皆允 (爆发期):
    - 攻击无视目标受伤无敌帧
    - 每击等比递增消耗禁忌: 1, 2, 4, 8, 16 (封顶 16, 换目标重置; 普攻已直接击杀脆皮怪时不白耗禁忌)
    - 附加真伤 = 本击近战伤害 × 实际消耗 × 转化率 × (1 + 连击层数 × 15%), 连击上限 10 层
      (与自身攻击面板、增伤词条、暴击及【末日三问】狂暴自然乘算叠加)
    - 【允】形态击杀目标同样回充禁忌 (钳制至 99, 续航连斩而不误触超载自爆)
    - 禁忌不足支付本击消耗时: 该击普攻 -50% (贪刀惩罚), 随后耗尽切回【虚】并陷入 5 秒罪业 (-30% 伤害)

  【罪业与赎罪】:
    - 罪业期间常规近战伤害降低, 且锁定在【虚】形态无法切换
    - 【虚】形态命中敌人 (含一击秒杀) 满 5 次 → 赎罪成功, 立即解除罪业

  实现优化:
    - 计时器全部使用到期 gameTime 时间戳而非每 tick 递减写 NBT, 彻底消除主手武器鬼畜抽搐
    - 服务端 WeakHashMap 独立追踪所有被减甲目标, 换目标绝不泄漏减甲修饰符

  数值均为顶部常量, 可自由调整
  ============================================================
 */
public class all_permitted extends NoLevelsModifier implements
        KillingHook, TooltipModifierHook, GeneralInteractionModifierHook, InventoryTickModifierHook,
        MeleeHitModifierHook, MeleeDamageModifierHook, AttributesModifierHook {

    // ===== 禁忌值 =====
    public static final float FORBIDDEN_MAX = 100.0f;       // 禁忌值上限 (虚形态蓄满自动超载)
    public static final float FORBIDDEN_YUN_KILL_CAP = 99.0f; // 允形态击杀回充上限 (防连斩中途误触超载)
    public static final float FORBIDDEN_PER_HIT = 4.0f;     // 虚形态每次命中积累
    public static final float FORBIDDEN_PER_KILL = 10.0f;   // 击杀普通目标积累

    // ===== 偷取 =====
    public static final int STEAL_MAX_STACKS = 5;           // 偷取最大层数
    public static final int STEAL_DURATION = 100;           // 偷取持续时间 (100 tick = 5 秒)
    public static final float STEAL_ARMOR_PERCENT = 0.02f;  // 每层偷取目标护甲比例 (2%, 满层 10%)
    public static final float STEAL_ATTACK_PERCENT = 0.02f; // 每层自身攻击力总乘区 (2%, 满层 +10%)

    // ===== 猎物质量 → 真伤转化率 =====
    public static final float MIN_QUALITY = 5.0f;           // 最低猎物质量保底 (防 0 甲无面板怪转化率为 0)
    public static final float TRANSFER_BASE = 0.0025f;      // 转化率基数: 每点禁忌 = 0.25% × Q^0.75 倍本击伤害真伤
    public static final double TRANSFER_EXP = 0.75;         // 质量幂指数
    public static final float MAX_TRANSFER_RATE = 0.0f;     // 可选软上限 (0 = 不封顶)

    // ===== 允形态: 等比消耗与连击 =====
    public static final int CONSUME_MAX_STEP = 4;           // 消耗档位上限: 2^4 = 16 (封顶)
    public static final float STARVATION_PENALTY = 0.5f;    // 禁忌不足支付本击时普攻倍率 (-50% 贪刀惩罚)
    public static final int COMBO_MAX = 10;                 // 连击层数上限
    public static final float COMBO_BONUS = 0.15f;          // 每层连击真伤加成 (+15%, 满层 ×2.5)

    // ===== 罪业与赎罪 =====
    public static final int SIN_DURATION = 100;             // 普通罪业持续 (100 tick = 5 秒)
    public static final int SIN_DURATION_OVERLOAD = 200;    // 超载强罪业持续 (200 tick = 10 秒)
    public static final float SIN_PENALTY_NORMAL = 0.7f;    // 普通罪业伤害倍率 (-30%)
    public static final float SIN_PENALTY_OVERLOAD = 0.5f;  // 超载强罪业伤害倍率 (-50%)
    public static final int ATONE_REQUIRED = 5;             // 赎罪所需虚形态命中次数

    // ===== 超载与切换 =====
    public static final float OVERLOAD_RADIUS = 4.0f;       // 超载真伤 AOE 半径 (格)
    public static final float OVERLOAD_MULTIPLIER = 1.5f;   // 超载真伤额外倍率
    public static final int SWITCH_COOLDOWN = 60;           // 形态切换冷却 (60 tick = 3 秒)

    // ===== 工具持久数据 Key =====
    private static final ResourceLocation FORBIDDEN = ResourceLocation.fromNamespaceAndPath(Tprt.MODID, "ap_forbidden");
    private static final ResourceLocation MODE = ResourceLocation.fromNamespaceAndPath(Tprt.MODID, "ap_mode");
    private static final ResourceLocation SWITCH_UNTIL = ResourceLocation.fromNamespaceAndPath(Tprt.MODID, "ap_switch_until");
    private static final ResourceLocation STEAL_STACKS = ResourceLocation.fromNamespaceAndPath(Tprt.MODID, "ap_steal_stacks");
    private static final ResourceLocation STEAL_UNTIL = ResourceLocation.fromNamespaceAndPath(Tprt.MODID, "ap_steal_until");
    private static final ResourceLocation STEAL_TARGET_ARMOR = ResourceLocation.fromNamespaceAndPath(Tprt.MODID, "ap_steal_target_armor");
    private static final ResourceLocation STEAL_QUALITY = ResourceLocation.fromNamespaceAndPath(Tprt.MODID, "ap_steal_quality");
    private static final ResourceLocation CONSUME_STEP = ResourceLocation.fromNamespaceAndPath(Tprt.MODID, "ap_consume_step");
    private static final ResourceLocation COMBO = ResourceLocation.fromNamespaceAndPath(Tprt.MODID, "ap_combo");
    private static final ResourceLocation COMBO_TARGET_ID = ResourceLocation.fromNamespaceAndPath(Tprt.MODID, "ap_combo_target_id");
    private static final ResourceLocation SIN_UNTIL = ResourceLocation.fromNamespaceAndPath(Tprt.MODID, "ap_sin_until");
    private static final ResourceLocation SIN_PENALTY = ResourceLocation.fromNamespaceAndPath(Tprt.MODID, "ap_sin_penalty");
    private static final ResourceLocation ATONE_COUNT = ResourceLocation.fromNamespaceAndPath(Tprt.MODID, "ap_atone_count");

    private static final String MODE_XU = "xu";   // 万物皆虚
    private static final String MODE_YUN = "yun"; // 万事皆允

    private static final UUID TARGET_ARMOR_UUID = UUID.fromString("1f6a3c9d-4b7e-4a52-8c1f-3e9d2b6a5c10");
    private static final UUID SELF_ARMOR_UUID = UUID.fromString("2f6a3c9d-4b7e-4a52-8c1f-3e9d2b6a5c11");
    private static final UUID SELF_DAMAGE_UUID = UUID.fromString("3f6a3c9d-4b7e-4a52-8c1f-3e9d2b6a5c12");

    /** 记录本刀护甲前真实近战伤害 (供 afterMeleeHit 计算允形态真伤) */
    private static final Map<ToolAttackContext, Float> SWING_DAMAGES = Collections.synchronizedMap(new WeakHashMap<>());
    /** 追踪所有被施加了偷取减甲的实体 -> 到期 gameTime (防换目标漏删减甲) */
    private static final Map<LivingEntity, Long> STOLEN_TARGETS = Collections.synchronizedMap(new WeakHashMap<>());
    private static int serverTickCounter = 0;

    static {
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, TickEvent.ServerTickEvent.class, all_permitted::onServerTick);
    }

    @Override
    public int getPriority() {
        return 500;
    }

    @Override
    protected void registerHooks(ModuleHookMap.@NotNull Builder hookBuilder) {
        super.registerHooks(hookBuilder);
        hookBuilder.addHook(this, TprtHooks.KILLING_HOOK, ModifierHooks.TOOLTIP, ModifierHooks.GENERAL_INTERACT,
                ModifierHooks.INVENTORY_TICK, ModifierHooks.MELEE_HIT, ModifierHooks.MELEE_DAMAGE, ModifierHooks.ATTRIBUTES);
    }

    // ============================================================
    //  伤害阶段: 罪业惩罚 + 贪刀惩罚 (护甲减免前)
    // ============================================================
    @Override
    public float getMeleeDamage(@NotNull IToolStackView tool, @NotNull ModifierEntry modifier,
                                @NotNull ToolAttackContext context, float baseDamage, float damage) {
        if (!canModified(tool)) return damage;

        long now = context.getAttacker().level().getGameTime();
        if (isSinActive(tool, now)) {
            damage *= getSinPenalty(tool);
        }

        if (isYun(tool)) {
            int consume = currentConsume(tool);
            if (tool.getPersistentData().getFloat(FORBIDDEN) < consume) {
                damage *= STARVATION_PENALTY;
            }
        }
        return damage;
    }

    // ============================================================
    //  命中前: 捕获护甲前近战伤害 + 允形态无视无敌帧
    // ============================================================
    @Override
    public float beforeMeleeHit(@NotNull IToolStackView tool, @NotNull ModifierEntry modifier,
                                @NotNull ToolAttackContext context, float damage, float baseKnockback, float knockback) {
        if (!canModified(tool)) return knockback;
        LivingEntity attacker = context.getAttacker();
        if (!attacker.level().isClientSide) {
            SWING_DAMAGES.put(context, damage);
        }
        LivingEntity target = context.getLivingTarget();
        if (isYun(tool) && target != null) {
            target.invulnerableTime = 0;
        }
        return knockback;
    }

    @Override
    public void failedMeleeHit(@NotNull IToolStackView tool, @NotNull ModifierEntry modifier,
                               @NotNull ToolAttackContext context, float damageNotDealt) {
        SWING_DAMAGES.remove(context);
    }

    // ============================================================
    //  命中后: 【虚】偷取/蓄力/赎罪(兼容秒杀), 【允】等比消耗/连击/真伤
    // ============================================================
    @Override
    public void afterMeleeHit(@NotNull IToolStackView tool, @NotNull ModifierEntry modifier,
                              @NotNull ToolAttackContext context, float damageDealt) {
        Float recorded = SWING_DAMAGES.remove(context);
        if (!canModified(tool)) return;

        LivingEntity attacker = context.getAttacker();
        LivingEntity target = context.getLivingTarget();
        if (attacker.level().isClientSide || target == null) return;

        long now = attacker.level().getGameTime();

        if (isXu(tool)) {
            // 【虚】即使普攻一刀秒杀目标, 依然正常记录质量、增加禁忌并计入赎罪
            steal(tool, target, now);
            addForbidden(tool, FORBIDDEN_PER_HIT, FORBIDDEN_MAX);
            atone(tool, attacker, now);
            checkOverload(tool, attacker, now);
        } else {
            // 【允】若普攻本身已直接砍死脆皮目标, 不白耗禁忌档位
            if (target.isDeadOrDying()) return;

            ModDataNBT data = tool.getPersistentData();
            boolean sameTarget = data.getInt(COMBO_TARGET_ID) == target.getId();
            int combo;
            if (sameTarget) {
                combo = Math.min(COMBO_MAX, data.getInt(COMBO) + 1);
            } else {
                combo = 1;
                data.putInt(CONSUME_STEP, 0);
            }
            data.putInt(COMBO, combo);
            data.putInt(COMBO_TARGET_ID, target.getId());

            int consume = currentConsume(tool);
            data.putInt(CONSUME_STEP, Math.min(CONSUME_MAX_STEP, data.getInt(CONSUME_STEP) + 1));

            float forbidden = data.getFloat(FORBIDDEN);
            float realConsume = Math.min(forbidden, consume);
            addForbidden(tool, -realConsume, FORBIDDEN_MAX);

            float swingDamage = recorded != null
                    ? recorded
                    : Math.max(damageDealt, (float) attacker.getAttributeValue(Attributes.ATTACK_DAMAGE));
            float rate = transferRate(tool);
            float trueDamage = swingDamage * realConsume * rate * (1.0f + combo * COMBO_BONUS);
            if (trueDamage > 0.0f) {
                target.invulnerableTime = 0;
                target.hurt(pureSource(attacker), trueDamage);
            }

            if (forbidden <= consume) {
                forceToXu(tool, attacker, now);
            }
        }
    }

    // ============================================================
    //  击杀: 仅主手持有本武器时生效; 被偷取目标死亡禁忌翻倍, 否则 +10
    // ============================================================
    @Override
    public void onKillLivingTarget(IToolStackView tool, LivingDeathEvent event, LivingEntity attacker, LivingEntity target, int level) {
        if (!canModified(tool) || target.level().isClientSide || event.isCanceled()) return;
        if (event.getSource().getEntity() != attacker) return;

        ToolStack mainHand = Modifier.getHeldTool(attacker, InteractionHand.MAIN_HAND);
        if (mainHand == null || mainHand.getModifierLevel(this) <= 0) return;

        AttributeInstance targetArmor = target.getAttribute(Attributes.ARMOR);
        boolean wasStolen = targetArmor != null && targetArmor.getModifier(TARGET_ARMOR_UUID) != null;

        float cap = isYun(tool) ? FORBIDDEN_YUN_KILL_CAP : FORBIDDEN_MAX;
        float gain = wasStolen ? Math.max(FORBIDDEN_PER_KILL, tool.getPersistentData().getFloat(FORBIDDEN)) : FORBIDDEN_PER_KILL;
        addForbidden(tool, gain, cap);

        if (isXu(tool)) {
            checkOverload(tool, attacker, attacker.level().getGameTime());
        }
    }

    // ============================================================
    //  属性: 仅主手生效; 偷取有效期内保留护甲与攻击增益 (含切入【允】爆发期)
    // ============================================================
    @Override
    public void addAttributes(@NotNull IToolStackView tool, @NotNull ModifierEntry modifier,
                              @NotNull EquipmentSlot slot, @NotNull BiConsumer<Attribute, AttributeModifier> consumer) {
        if (slot != EquipmentSlot.MAINHAND || !canModified(tool)) return;
        int stacks = tool.getPersistentData().getInt(STEAL_STACKS);
        if (stacks > 0) {
            float stolenArmor = tool.getPersistentData().getFloat(STEAL_TARGET_ARMOR) * STEAL_ARMOR_PERCENT * stacks;
            if (stolenArmor > 0.0f) {
                consumer.accept(Attributes.ARMOR, new AttributeModifier(
                        SELF_ARMOR_UUID, "all_permitted.steal_armor", stolenArmor, AttributeModifier.Operation.ADDITION));
            }
            consumer.accept(Attributes.ATTACK_DAMAGE, new AttributeModifier(
                    SELF_DAMAGE_UUID, "all_permitted.steal_damage", STEAL_ATTACK_PERCENT * stacks, AttributeModifier.Operation.MULTIPLY_TOTAL));
        }
    }

    // ============================================================
    //  每 tick (仅服务端): 仅在计时器到期的那一刻清理一次 NBT, 平时 0 写入不抽搐
    // ============================================================
    @Override
    public void onInventoryTick(@NotNull IToolStackView tool, @NotNull ModifierEntry modifier, @NotNull Level world,
                                @NotNull LivingEntity holder, int itemSlot, boolean isSelected, boolean isCorrectSlot, @NotNull ItemStack stack) {
        if (world.isClientSide || !canModified(tool)) return;

        long now = world.getGameTime();
        ModDataNBT data = tool.getPersistentData();

        long stealUntil = getLong(data, STEAL_UNTIL);
        if (stealUntil > 0L && !isTimerActive(now, stealUntil, STEAL_DURATION)) {
            data.remove(STEAL_UNTIL);
            data.putInt(STEAL_STACKS, 0);
        }

        long sinUntil = getLong(data, SIN_UNTIL);
        if (sinUntil > 0L && !isTimerActive(now, sinUntil, SIN_DURATION_OVERLOAD)) {
            clearSin(data);
        }

        long switchUntil = getLong(data, SWITCH_UNTIL);
        if (switchUntil > 0L && !isTimerActive(now, switchUntil, SWITCH_COOLDOWN)) {
            data.remove(SWITCH_UNTIL);
        }
    }

    /** 服务端全局回收所有到期目标身上的减甲修饰符 */
    private static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (++serverTickCounter % 10 != 0 || STOLEN_TARGETS.isEmpty()) return;

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

    // ============================================================
    //  潜行 + 右键瞬发切换形态 (不阻塞疾跑, 不抢骑枪冲锋/格挡/施法右键)
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
        if (isSinActive(tool, now)) {
            if (!level.isClientSide) {
                int remain = (int) ((getLong(tool.getPersistentData(), SIN_UNTIL) - now + 19L) / 20L);
                player.displayClientMessage(Component.translatable("message.tprt.all_permitted.sin_locked", remain), true);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (isSwitchOnCooldown(tool, now)) {
            if (!level.isClientSide) {
                int remain = (int) ((getLong(tool.getPersistentData(), SWITCH_UNTIL) - now + 19L) / 20L);
                player.displayClientMessage(Component.translatable("message.tprt.all_permitted.switch_cd", remain), true);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        if (!level.isClientSide) {
            switchMode(tool, player, now);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    // ============================================================
    //  Tooltip 实时双语面板
    // ============================================================
    @Override
    public void addTooltip(@NotNull IToolStackView tool, @NotNull ModifierEntry modifier, @Nullable Player player,
                           @NotNull List<Component> tooltip, @NotNull TooltipKey tooltipKey, @NotNull TooltipFlag tooltipFlag) {
        if (!canModified(tool)) return;

        IModDataView data = tool.getPersistentData();
        long now = player != null ? player.level().getGameTime() : 0L;

        tooltip.add(Component.translatable(isYun(tool)
                ? "tooltip.tprt.all_permitted.mode_yun"
                : "tooltip.tprt.all_permitted.mode_xu"));
        tooltip.add(Component.translatable("tooltip.tprt.all_permitted.forbidden",
                (int) data.getFloat(FORBIDDEN), (int) FORBIDDEN_MAX));

        float quality = data.getFloat(STEAL_QUALITY);
        if (quality > 0.0f) {
            tooltip.add(Component.translatable("tooltip.tprt.all_permitted.rate",
                    String.format("%.2f", transferRate(tool) * 100.0f), String.format("%.0f", quality)));
        }

        if (isYun(tool)) {
            tooltip.add(Component.translatable("tooltip.tprt.all_permitted.yun_status",
                    currentConsume(tool), data.getInt(COMBO), COMBO_MAX));
        }

        if (now > 0L && isSinActive(tool, now)) {
            int sinSec = (int) ((getLong(data, SIN_UNTIL) - now + 19L) / 20L);
            tooltip.add(Component.translatable("tooltip.tprt.all_permitted.sin",
                    sinSec, data.getInt(ATONE_COUNT), ATONE_REQUIRED));
        }

        if (now > 0L && isSwitchOnCooldown(tool, now)) {
            int cdSec = (int) ((getLong(data, SWITCH_UNTIL) - now + 19L) / 20L);
            tooltip.add(Component.translatable("tooltip.tprt.all_permitted.switch_cd", cdSec));
        }
    }

    // ============================================================
    //  内部核心逻辑
    // ============================================================

    private static DamageSource pureSource(LivingEntity attacker) {
        return new DamageSource(attacker.level().registryAccess()
                .registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(ModDamageSource.TPRT_PURE_DAMAGE_TYPE), attacker);
    }

    /** 虚形态偷取: 先剥离旧修饰符读取真实属性, 避免越偷越少或无攻击属性崩溃 */
    private static void steal(IToolStackView tool, LivingEntity target, long now) {
        ModDataNBT data = tool.getPersistentData();
        boolean stillActive = isTimerActive(now, getLong(data, STEAL_UNTIL), STEAL_DURATION);
        int stacks = stillActive ? Math.min(STEAL_MAX_STACKS, data.getInt(STEAL_STACKS) + 1) : 1;

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
        float hpFactor = Math.min(200.0f, Math.max(0.0f, target.getMaxHealth() - 20.0f) * 0.05f);
        float quality = Math.max(MIN_QUALITY, baseArmor + baseToughness * 1.5f + baseAttack + hpFactor);

        // 偷取有效期内保留更高猎物质量与护甲快照, 避免小怪挡刀污染 Boss 转化率
        float savedArmor = stillActive ? Math.max(data.getFloat(STEAL_TARGET_ARMOR), baseArmor) : baseArmor;
        float savedQuality = stillActive ? Math.max(data.getFloat(STEAL_QUALITY), quality) : quality;

        data.putInt(STEAL_STACKS, stacks);
        putLong(data, STEAL_UNTIL, now + STEAL_DURATION);
        data.putFloat(STEAL_TARGET_ARMOR, savedArmor);
        data.putFloat(STEAL_QUALITY, savedQuality);

        if (target.isAlive() && targetArmorAttr != null && baseArmor > 0.0f) {
            targetArmorAttr.addTransientModifier(new AttributeModifier(
                    TARGET_ARMOR_UUID, "stolen_by_all_permitted",
                    -baseArmor * STEAL_ARMOR_PERCENT * stacks, AttributeModifier.Operation.ADDITION));
            STOLEN_TARGETS.put(target, now + STEAL_DURATION);
        }
    }

    private static float transferRate(IToolStackView tool) {
        float quality = tool.getPersistentData().getFloat(STEAL_QUALITY);
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
        int step = Math.min(tool.getPersistentData().getInt(CONSUME_STEP), CONSUME_MAX_STEP);
        return 1 << step;
    }

    private static void addForbidden(IToolStackView tool, float delta, float cap) {
        ModDataNBT data = tool.getPersistentData();
        float next = Math.max(0.0f, Math.min(cap, data.getFloat(FORBIDDEN) + delta));
        data.putFloat(FORBIDDEN, next);
    }

    /** 虚形态禁忌满 100 自动超载: 敌对真伤 AOE + 10 秒强罪业 */
    private static void checkOverload(IToolStackView tool, LivingEntity attacker, long now) {
        ModDataNBT data = tool.getPersistentData();
        if (data.getFloat(FORBIDDEN) < FORBIDDEN_MAX) return;

        AttributeInstance selfAtkAttr = attacker.getAttribute(Attributes.ATTACK_DAMAGE);
        float attackPower = selfAtkAttr != null ? Math.max(1.0f, (float) selfAtkAttr.getValue()) : 10.0f;
        float damage = attackPower * FORBIDDEN_MAX * transferRate(tool) * OVERLOAD_MULTIPLIER;

        DamageSource source = pureSource(attacker);
        List<LivingEntity> targets = attacker.level().getEntitiesOfClass(
                LivingEntity.class,
                attacker.getBoundingBox().inflate(OVERLOAD_RADIUS),
                e -> isValidOverloadTarget(attacker, e));
        for (LivingEntity entity : targets) {
            entity.invulnerableTime = 0;
            entity.hurt(source, damage);
        }

        data.putFloat(FORBIDDEN, 0.0f);
        putLong(data, SIN_UNTIL, now + SIN_DURATION_OVERLOAD);
        data.putFloat(SIN_PENALTY, SIN_PENALTY_OVERLOAD);
        data.putInt(ATONE_COUNT, 0);

        Level level = attacker.level();
        level.playSound(null, attacker.getX(), attacker.getY(), attacker.getZ(),
                SoundEvents.DRAGON_FIREBALL_EXPLODE, SoundSource.PLAYERS, 0.9f, 1.05f);
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                    attacker.getX(), attacker.getY() + 1.0, attacker.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
            serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                    attacker.getX(), attacker.getY() + 1.0, attacker.getZ(), 28, 1.2, 0.6, 1.2, 0.08);
        }
        if (attacker instanceof Player player) {
            player.displayClientMessage(Component.translatable("message.tprt.all_permitted.overload"), true);
        }
    }

    private static boolean isValidOverloadTarget(LivingEntity attacker, LivingEntity candidate) {
        if (candidate == attacker || !candidate.isAlive() || candidate instanceof ArmorStand) return false;
        if (attacker.isAlliedTo(candidate)) return false;
        if (candidate instanceof OwnableEntity ownable && attacker.getUUID().equals(ownable.getOwnerUUID())) {
            return false;
        }
        return true;
    }

    /** 罪业期间虚形态命中赎罪 */
    private static void atone(IToolStackView tool, LivingEntity attacker, long now) {
        if (!isSinActive(tool, now)) return;
        ModDataNBT data = tool.getPersistentData();
        int count = data.getInt(ATONE_COUNT) + 1;
        if (count >= ATONE_REQUIRED) {
            clearSin(data);
            Level level = attacker.level();
            level.playSound(null, attacker.getX(), attacker.getY(), attacker.getZ(),
                    SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.8f, 1.4f);
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.END_ROD,
                        attacker.getX(), attacker.getY() + 1.0, attacker.getZ(), 12, 0.4, 0.5, 0.4, 0.04);
            }
            if (attacker instanceof Player player) {
                player.displayClientMessage(Component.translatable("message.tprt.all_permitted.atoned"), true);
            }
        } else {
            data.putInt(ATONE_COUNT, count);
        }
    }

    private static void forceToXu(IToolStackView tool, LivingEntity attacker, long now) {
        ModDataNBT data = tool.getPersistentData();
        data.putString(MODE, MODE_XU);
        putLong(data, SIN_UNTIL, now + SIN_DURATION);
        data.putFloat(SIN_PENALTY, SIN_PENALTY_NORMAL);
        data.putInt(ATONE_COUNT, 0);
        data.putInt(CONSUME_STEP, 0);
        data.putInt(COMBO, 0);

        attacker.level().playSound(null, attacker.getX(), attacker.getY(), attacker.getZ(),
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.6f, 0.9f);
        if (attacker instanceof Player player) {
            player.displayClientMessage(Component.translatable("message.tprt.all_permitted.exhausted"), true);
        }
    }

    private static void switchMode(IToolStackView tool, Player user, long now) {
        ModDataNBT data = tool.getPersistentData();
        boolean toYun = isXu(tool);
        data.putString(MODE, toYun ? MODE_YUN : MODE_XU);
        putLong(data, SWITCH_UNTIL, now + SWITCH_COOLDOWN);
        data.putInt(CONSUME_STEP, 0);
        data.putInt(COMBO, 0);
        data.putInt(COMBO_TARGET_ID, 0);

        Level level = user.level();
        if (toYun) {
            level.playSound(null, user.getX(), user.getY(), user.getZ(),
                    SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.6f, 1.25f);
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.GLOW,
                        user.getX(), user.getY() + 1.1, user.getZ(), 14, 0.45, 0.5, 0.45, 0.04);
            }
            user.displayClientMessage(Component.translatable("message.tprt.all_permitted.to_yun"), true);
        } else {
            level.playSound(null, user.getX(), user.getY(), user.getZ(),
                    SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.6f, 0.85f);
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                        user.getX(), user.getY() + 1.1, user.getZ(), 14, 0.45, 0.5, 0.45, 0.03);
            }
            user.displayClientMessage(Component.translatable("message.tprt.all_permitted.to_xu"), true);
        }
    }

    private static void clearSin(ModDataNBT data) {
        data.remove(SIN_UNTIL);
        data.putInt(ATONE_COUNT, 0);
        data.remove(SIN_PENALTY);
    }

    private static boolean isSinActive(IToolStackView tool, long now) {
        return isTimerActive(now, getLong(tool.getPersistentData(), SIN_UNTIL), SIN_DURATION_OVERLOAD);
    }

    private static boolean isSwitchOnCooldown(IToolStackView tool, long now) {
        return isTimerActive(now, getLong(tool.getPersistentData(), SWITCH_UNTIL), SWITCH_COOLDOWN);
    }

    private static boolean isTimerActive(long now, long until, int maxDuration) {
        return until > now && (until - now) <= maxDuration + 20L;
    }

    private static long getLong(IModDataView data, ResourceLocation key) {
        String raw = data.getString(key);
        if (raw.isEmpty()) return 0L;
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private static void putLong(ModDataNBT data, ResourceLocation key, long value) {
        if (value <= 0L) {
            data.remove(key);
        } else {
            data.putString(key, Long.toString(value));
        }
    }

    private static boolean isXu(IToolStackView tool) {
        return getMode(tool).equals(MODE_XU);
    }

    private static boolean isYun(IToolStackView tool) {
        return getMode(tool).equals(MODE_YUN);
    }

    private static String getMode(IToolStackView tool) {
        String mode = tool.getPersistentData().getString(MODE);
        return mode.isEmpty() ? MODE_XU : mode;
    }

    private static float getSinPenalty(IToolStackView tool) {
        float penalty = tool.getPersistentData().getFloat(SIN_PENALTY);
        return penalty <= 0.0f ? 1.0f : penalty;
    }

    private static boolean canModified(IToolStackView tool) {
        return tool.hasTag(TinkerTags.Items.MELEE);
    }
}
