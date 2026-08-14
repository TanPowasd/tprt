package org.mylove.tprt.Modifiers.re;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import org.jetbrains.annotations.Nullable;
import org.mylove.tprt.Tprt;
import org.mylove.tprt.hooks.KillingHook;
import org.mylove.tprt.hooks.TprtHooks;
import org.mylove.tprt.registries.ModDamageSource;
import slimeknights.mantle.client.TooltipKey;
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
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.tools.stats.ToolType;

import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;

/*
  ============================================================
  万般皆允，百无禁忌 (all_permitted) —— 毕业词条 · 终极版
  信条: 万物皆虚，万事皆允

  核心循环: 【虚】蓄力 →【允】爆发 → 罪业惩罚 → 赎罪重回

  【虚】万物皆虚 (蓄力期):
    - 命中偷取目标护甲与攻击 (5 层 / 5 秒), 目标失去等量护甲
    - 偷取时记录目标质量 Q = 护甲 + 攻击力
      真伤转化率 = 0.02% × Q^0.75  (连续函数, 无分档, 偷到强敌转化率更高)
    - 命中 +4 禁忌; 击杀被偷取中的目标 → 禁忌 ×2
    - 禁忌满 100 → 自动超载: 全量真伤 AOE (4 格), 事后 10 秒强罪业(伤害减半)

  【允】万事皆允 (爆发期):
    - 攻击无视目标受伤无敌帧
    - 每击消耗禁忌等比递增: 1, 2, 4, 8, 16 (封顶 16), 换目标重置
    - 真伤 = 本击消耗 × 转化率 × (1 + 连击层数 × 15%), 连击上限 10 层
    - 禁忌不足时该击伤害 -50% (贪刀惩罚)
    - 禁忌耗尽 → 强制切回【虚】+ 罪业 5 秒

  罪业:
    - 伤害 -30% (超载罪业 -50%), 期间无法切换形态
    - 用【虚】形态命中 5 次 → 赎罪, 提前解除

  数值均为顶部常量, 可自由调整
  ============================================================
 */
public class all_permitted extends NoLevelsModifier implements
        KillingHook, TooltipModifierHook, GeneralInteractionModifierHook, InventoryTickModifierHook,
        MeleeHitModifierHook, MeleeDamageModifierHook, AttributesModifierHook
{
    public static final ToolType[] CAN_BE_USE_ON_TYPES = {ToolType.MELEE};

    // ===== 禁忌值 =====
    public static final float FORBIDDEN_MAX = 100.0f;      // 禁忌值上限, 满值自动超载
    public static final float FORBIDDEN_PER_HIT = 4.0f;    // 虚形态每次命中积累
    public static final float FORBIDDEN_PER_KILL = 10.0f;  // 击杀普通目标积累
    // ===== 偷取 =====
    public static final int STEAL_MAX_STACKS = 5;          // 偷取最大层数
    public static final int STEAL_DURATION = 100;          // 偷取持续 tick (5 秒)
    public static final float STEAL_ARMOR_PERCENT = 0.02f; // 每层偷取目标护甲值的比例 (2%)
    public static final float STEAL_ATTACK_PERCENT = 0.02f;// 每层自身攻击增益 (2%)
    // ===== 质量 → 转化率 连续函数 (B) =====
    public static final float TRANSFER_BASE = 0.0002f;     // 转化率基数: 0.02%
    public static final double TRANSFER_EXP = 0.75;        // 幂指数: 转化率 = 基数 × Q^指数
    public static final float MAX_TRANSFER_RATE = 0.0f;    // 可选软上限 (0 = 不限, 毕业词条允许爆发)
    // ===== 允形态: 等比消耗 (C) =====
    public static final int CONSUME_MAX_STEP = 4;          // 消耗档位上限: 2^4 = 16 (封顶)
    public static final float STARVATION_PENALTY = 0.5f;   // 禁忌不足时该击伤害倍率 (贪刀惩罚)
    // ===== 连击 =====
    public static final int COMBO_MAX = 10;                // 连击层数上限
    public static final float COMBO_BONUS = 0.15f;         // 每层连击真伤加成 (15%)
    // ===== 罪业 (A) =====
    public static final int SIN_DURATION = 100;            // 普通罪业 tick (5 秒, 禁忌耗尽)
    public static final int SIN_DURATION_OVERLOAD = 200;   // 超载罪业 tick (10 秒)
    public static final float SIN_PENALTY_NORMAL = 0.7f;   // 普通罪业伤害倍率 (-30%)
    public static final float SIN_PENALTY_OVERLOAD = 0.5f; // 超载罪业伤害倍率 (-50%)
    public static final int ATONE_REQUIRED = 5;            // 赎罪所需虚形态命中次数
    // ===== 超载 =====
    public static final float OVERLOAD_RADIUS = 4.0f;      // 超载 AOE 半径 (格)
    public static final float OVERLOAD_MULTIPLIER = 1.5f;  // 超载伤害乘区
    // ===== 切换 =====
    public static final int SWITCH_COOLDOWN = 60;          // 切换冷却 tick (3 秒)

    // ===== 工具持久数据 Key =====
    private static final ResourceLocation FORBIDDEN = ResourceLocation.fromNamespaceAndPath(Tprt.MODID, "ap_forbidden");
    private static final ResourceLocation MODE = ResourceLocation.fromNamespaceAndPath(Tprt.MODID, "ap_mode");
    private static final ResourceLocation SWITCH_CD = ResourceLocation.fromNamespaceAndPath(Tprt.MODID, "ap_switch_cd");
    private static final ResourceLocation STEAL_STACKS = ResourceLocation.fromNamespaceAndPath(Tprt.MODID, "ap_steal_stacks");
    private static final ResourceLocation STEAL_TIMER = ResourceLocation.fromNamespaceAndPath(Tprt.MODID, "ap_steal_timer");
    private static final ResourceLocation STEAL_TARGET_ARMOR = ResourceLocation.fromNamespaceAndPath(Tprt.MODID, "ap_steal_target_armor");
    private static final ResourceLocation STEAL_TARGET_ID = ResourceLocation.fromNamespaceAndPath(Tprt.MODID, "ap_steal_target_id");
    private static final ResourceLocation STEAL_QUALITY = ResourceLocation.fromNamespaceAndPath(Tprt.MODID, "ap_steal_quality");
    private static final ResourceLocation CONSUME_STEP = ResourceLocation.fromNamespaceAndPath(Tprt.MODID, "ap_consume_step");
    private static final ResourceLocation COMBO = ResourceLocation.fromNamespaceAndPath(Tprt.MODID, "ap_combo");
    private static final ResourceLocation COMBO_TARGET_ID = ResourceLocation.fromNamespaceAndPath(Tprt.MODID, "ap_combo_target_id");
    private static final ResourceLocation SIN_TIMER = ResourceLocation.fromNamespaceAndPath(Tprt.MODID, "ap_sin_timer");
    private static final ResourceLocation SIN_PENALTY = ResourceLocation.fromNamespaceAndPath(Tprt.MODID, "ap_sin_penalty");
    private static final ResourceLocation ATONE_COUNT = ResourceLocation.fromNamespaceAndPath(Tprt.MODID, "ap_atone_count");

    private static final String MODE_XU = "xu";   // 万物皆虚
    private static final String MODE_YUN = "yun"; // 万事皆允

    // 属性修饰符 UUID (固定, 避免重复叠加)
    private static final UUID TARGET_ARMOR_UUID = UUID.fromString("1f6a3c9d-4b7e-4a52-8c1f-3e9d2b6a5c10");
    private static final UUID SELF_ARMOR_UUID = UUID.fromString("2f6a3c9d-4b7e-4a52-8c1f-3e9d2b6a5c11");
    private static final UUID SELF_DAMAGE_UUID = UUID.fromString("3f6a3c9d-4b7e-4a52-8c1f-3e9d2b6a5c12");

    @Override
    public int getPriority() {
        return 500;
    }

    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        hookBuilder.addHook(this, TprtHooks.KILLING_HOOK, ModifierHooks.TOOLTIP, ModifierHooks.GENERAL_INTERACT,
                ModifierHooks.INVENTORY_TICK, ModifierHooks.MELEE_HIT, ModifierHooks.MELEE_DAMAGE, ModifierHooks.ATTRIBUTES);
    }

    // ============================================================
    //  伤害阶段: 罪业惩罚 + 贪刀惩罚 (发生在护甲减免之前)
    // ============================================================
    @Override
    public float getMeleeDamage(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float baseDamage, float damage) {
        if (!canModified(tool)) return damage;

        // 罪业期间伤害削减
        if (tool.getPersistentData().getInt(SIN_TIMER) > 0) {
            damage *= getSinPenalty(tool);
        }

        // 允形态: 禁忌不足以支付本击消耗 → 贪刀惩罚 (-50%)
        if (isYun(tool)) {
            int consume = currentConsume(tool);
            if (tool.getPersistentData().getFloat(FORBIDDEN) < consume) {
                damage *= STARVATION_PENALTY;
            }
        }
        return damage;
    }

    // ============================================================
    //  命中前: 允形态无视目标无敌帧
    // ============================================================
    @Override
    public float beforeMeleeHit(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float damage, float baseKnockback, float knockback) {
        LivingEntity target = context.getLivingTarget();
        if (isYun(tool) && target != null) {
            target.invulnerableTime = 0;
        }
        return knockback;
    }

    // ============================================================
    //  命中后: 核心结算 —— 虚形态偷取/蓄力, 允形态消耗/爆发
    // ============================================================
    @Override
    public void afterMeleeHit(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float damageDealt) {
        if (!canModified(tool)) return;

        LivingEntity target = context.getLivingTarget();
        LivingEntity attacker = context.getAttacker();
        if (target == null || target.isDeadOrDying()) return;

        if (isXu(tool)) {
            // ===== 万物皆虚: 偷取 + 蓄力 + 赎罪 =====
            steal(tool, target);
            addForbidden(tool, FORBIDDEN_PER_HIT);
            atone(tool, attacker);              // 罪业期间命中计数 (赎罪)
            checkOverload(tool, attacker);      // 蓄满 100 自动超载
        } else if (isYun(tool)) {
            // ===== 万事皆允: 等比消耗 + 连击 + 真伤 =====
            boolean sameTarget = tool.getPersistentData().getInt(COMBO_TARGET_ID) == target.getId();
            int combo;
            if (sameTarget) {
                // 同一目标: 连击 +1 (上限 COMBO_MAX)
                combo = Math.min(COMBO_MAX, tool.getPersistentData().getInt(COMBO) + 1);
            } else {
                // 换目标: 连击与消耗档位全部重置
                combo = 1;
                tool.getPersistentData().putInt(CONSUME_STEP, 0);
            }
            tool.getPersistentData().putInt(COMBO, combo);
            tool.getPersistentData().putInt(COMBO_TARGET_ID, target.getId());

            // 本击消耗 (等比 2^step, 封顶 16), 结算后档位 +1
            int consume = currentConsume(tool);
            tool.getPersistentData().putInt(CONSUME_STEP, Math.min(CONSUME_MAX_STEP, tool.getPersistentData().getInt(CONSUME_STEP) + 1));

            // 扣除禁忌 (不足则扣光)
            float forbidden = tool.getPersistentData().getFloat(FORBIDDEN);
            float realConsume = Math.min(forbidden, consume);
            addForbidden(tool, -realConsume);

            // 真伤 = 消耗 × 转化率 × (1 + 连击加成)
            float rate = transferRate(tool);
            float trueDamage = realConsume * rate * (1 + combo * COMBO_BONUS);
            if (trueDamage > 0.0f) {
                target.hurt(pureSource(attacker), trueDamage);
            }

            // 禁忌耗尽 → 强制切回【虚】并陷入罪业
            if (forbidden <= consume) {
                forceToXu(tool, attacker);
            }
        }
    }

    // ============================================================
    //  击杀: 被偷取中的目标死亡 → 禁忌 ×2, 否则 +10
    // ============================================================
    @Override
    public void onKillLivingTarget(IToolStackView tool, LivingDeathEvent event, LivingEntity attacker, LivingEntity target, int level) {
        if (!canModified(tool)) return;
        if (event.getSource().getEntity() != attacker) return;

        // 目标身上还带着偷取减益 → 掠夺: 禁忌翻倍
        AttributeInstance targetArmor = target.getAttribute(Attributes.ARMOR);
        boolean wasStolen = targetArmor != null && targetArmor.getModifier(TARGET_ARMOR_UUID) != null;
        if (wasStolen) {
            addForbidden(tool, tool.getPersistentData().getFloat(FORBIDDEN)); // +当前值 = ×2 (超上限自动钳制)
        } else {
            addForbidden(tool, FORBIDDEN_PER_KILL);
        }
        checkOverload(tool, attacker);
    }

    // ============================================================
    //  属性: 虚形态偷取层数 → 自身护甲/攻击加成
    // ============================================================
    @Override
    public void addAttributes(IToolStackView tool, ModifierEntry modifier, EquipmentSlot slot, BiConsumer<Attribute, AttributeModifier> consumer) {
        int stacks = tool.getPersistentData().getInt(STEAL_STACKS);
        if (stacks > 0 && isXu(tool)) {
            // 护甲: +目标护甲值 × 2% × 层数
            float stolenArmor = tool.getPersistentData().getFloat(STEAL_TARGET_ARMOR) * STEAL_ARMOR_PERCENT * stacks;
            consumer.accept(Attributes.ARMOR, new AttributeModifier(SELF_ARMOR_UUID, "all_permitted.steal_armor", stolenArmor, AttributeModifier.Operation.ADDITION));
            // 攻击: +2% × 层数
            consumer.accept(Attributes.ATTACK_DAMAGE, new AttributeModifier(SELF_DAMAGE_UUID, "all_permitted.steal_damage", STEAL_ATTACK_PERCENT * stacks, AttributeModifier.Operation.MULTIPLY_TOTAL));
        }
    }

    // ============================================================
    //  每 tick: 偷取计时/归还, 罪业计时, 切换冷却
    // ============================================================
    @Override
    public void onInventoryTick(IToolStackView tool, ModifierEntry modifier, Level world, LivingEntity holder, int itemSlot, boolean isSelected, boolean isCorrectSlot, ItemStack stack) {
        if (!canModified(tool)) return;

        // 偷取计时: 到期归还目标护甲并清层数
        int timer = tool.getPersistentData().getInt(STEAL_TIMER);
        if (timer > 0) {
            timer--;
            tool.getPersistentData().putInt(STEAL_TIMER, timer);
            if (timer <= 0) {
                tool.getPersistentData().putInt(STEAL_STACKS, 0);
                Entity target = world.getEntity(tool.getPersistentData().getInt(STEAL_TARGET_ID));
                if (target instanceof LivingEntity living) {
                    AttributeInstance targetArmor = living.getAttribute(Attributes.ARMOR);
                    if (targetArmor != null) {
                        targetArmor.removeModifier(TARGET_ARMOR_UUID);
                    }
                }
            }
        }

        // 罪业计时: 到期解除
        int sin = tool.getPersistentData().getInt(SIN_TIMER);
        if (sin > 0) {
            sin--;
            tool.getPersistentData().putInt(SIN_TIMER, sin);
            if (sin <= 0) {
                tool.getPersistentData().putInt(ATONE_COUNT, 0);
                tool.getPersistentData().putFloat(SIN_PENALTY, 1.0f);
            }
        }

        if (world.isClientSide) return; // 以下仅服务端

        // 切换冷却递减
        int cd = tool.getPersistentData().getInt(SWITCH_CD);
        if (cd > 0) {
            cd--;
            tool.getPersistentData().putInt(SWITCH_CD, cd);
        }
    }

    // ============================================================
    //  右键切换形态 (罪业/冷却中禁止)
    // ============================================================
    @Override
    public InteractionResult onToolUse(IToolStackView tool, ModifierEntry modifier, Player player, InteractionHand hand, InteractionSource source) {
        if (source == InteractionSource.RIGHT_CLICK && !tool.isBroken()) {
            if (tool.getPersistentData().getInt(SIN_TIMER) > 0) return InteractionResult.PASS; // 罪业中无法切换
            if (tool.getPersistentData().getInt(SWITCH_CD) > 0) return InteractionResult.PASS; // 冷却中
            GeneralInteractionModifierHook.startUsing(tool, modifier.getId(), player, hand);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public void onFinishUsing(IToolStackView tool, ModifierEntry modifier, LivingEntity entity) {
        if (!canModified(tool)) return;
        if (!tool.isBroken() && entity instanceof Player player) {
            switchMode(tool, player);
        }
    }

    @Override
    public UseAnim getUseAction(IToolStackView tool, ModifierEntry modifier) {
        return UseAnim.BLOCK;
    }

    @Override
    public int getUseDuration(IToolStackView tool, ModifierEntry modifier) {
        return 1;
    }

    // ============================================================
    //  Tooltip: 形态 / 禁忌 / 消耗档位 / 连击 / 罪业 / 冷却
    // ============================================================
    @Override
    public void addTooltip(IToolStackView tool, ModifierEntry modifier, @Nullable Player player, List<Component> tooltip, TooltipKey tooltipKey, TooltipFlag tooltipFlag) {
        if (!canModified(tool)) return;

        tooltip.add(Component.literal(isYun(tool) ? "形态: §c[允] 万事皆允§r" : "形态: §d[虚] 万物皆虚§r"));
        tooltip.add(Component.literal("禁忌: §e" + (int) tool.getPersistentData().getFloat(FORBIDDEN) + "§r / 100"));

        if (isYun(tool)) {
            tooltip.add(Component.literal("本击消耗: §6" + currentConsume(tool) + "§r | 连击: §6" + tool.getPersistentData().getInt(COMBO) + "§r / " + COMBO_MAX));
            tooltip.add(Component.literal("转化率: §a" + String.format("%.2f", transferRate(tool) * 100) + "%§r / 点禁忌"));
        }

        int sin = tool.getPersistentData().getInt(SIN_TIMER);
        if (sin > 0) {
            tooltip.add(Component.literal("罪业: §c" + ((sin + 19) / 20) + " 秒§r (赎罪 " + tool.getPersistentData().getInt(ATONE_COUNT) + "/" + ATONE_REQUIRED + ")"));
        }

        int cd = tool.getPersistentData().getInt(SWITCH_CD);
        if (cd > 0) {
            tooltip.add(Component.literal("切换冷却: §7" + ((cd + 19) / 20) + " 秒§r"));
        }
    }

    // ****************************************************************************
    //  私有工具方法
    // ****************************************************************************

    // 真伤伤害源 (无视护甲 / 无敌帧 / 魔法保护 / 效果)
    private static DamageSource pureSource(LivingEntity attacker) {
        return new DamageSource(attacker.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(ModDamageSource.TPRT_PURE_DAMAGE_TYPE), attacker);
    }

    // 虚形态偷取: 叠层 + 记录质量 + 目标减护甲
    private static void steal(IToolStackView tool, LivingEntity target) {
        int stacks = Math.min(STEAL_MAX_STACKS, tool.getPersistentData().getInt(STEAL_STACKS) + 1);
        tool.getPersistentData().putInt(STEAL_STACKS, stacks);
        tool.getPersistentData().putInt(STEAL_TIMER, STEAL_DURATION);

        // 记录目标质量 Q = 护甲 + 攻击力 (决定允形态转化率)
        float armor = target.getArmorValue();
        float quality = armor + (float) target.getAttributeValue(Attributes.ATTACK_DAMAGE);
        tool.getPersistentData().putFloat(STEAL_TARGET_ARMOR, armor);
        tool.getPersistentData().putFloat(STEAL_QUALITY, quality);
        tool.getPersistentData().putInt(STEAL_TARGET_ID, target.getId());

        // 目标失去等量护甲 (STEAL_TIMER 到期时手动归还)
        AttributeInstance targetArmor = target.getAttribute(Attributes.ARMOR);
        if (targetArmor != null) {
            targetArmor.removeModifier(TARGET_ARMOR_UUID);
            targetArmor.addTransientModifier(new AttributeModifier(TARGET_ARMOR_UUID, "stolen_by_all_permitted", -armor * STEAL_ARMOR_PERCENT * stacks, AttributeModifier.Operation.ADDITION));
        }
    }

    // 质量 → 转化率: 连续函数 0.02% × Q^0.75 (可选软上限 MAX_TRANSFER_RATE)
    private static float transferRate(IToolStackView tool) {
        float quality = tool.getPersistentData().getFloat(STEAL_QUALITY);
        if (quality <= 0) return 0.0f;
        float rate = TRANSFER_BASE * (float) Math.pow(quality, TRANSFER_EXP);
        if (MAX_TRANSFER_RATE > 0 && rate > MAX_TRANSFER_RATE) return MAX_TRANSFER_RATE;
        return rate;
    }

    // 本击禁忌消耗: 等比 2^step (1,2,4,8,16), 封顶 CONSUME_MAX_STEP
    private static int currentConsume(IToolStackView tool) {
        int step = Math.min(tool.getPersistentData().getInt(CONSUME_STEP), CONSUME_MAX_STEP);
        return 1 << step;
    }

    // 禁忌增减 (0 ~ FORBIDDEN_MAX 钳制)
    private static void addForbidden(IToolStackView tool, float amount) {
        float value = Math.max(0.0f, Math.min(FORBIDDEN_MAX, tool.getPersistentData().getFloat(FORBIDDEN) + amount));
        tool.getPersistentData().putFloat(FORBIDDEN, value);
    }

    // 自动超载: 禁忌满 100 → 全量真伤 AOE + 10 秒强罪业
    private static void checkOverload(IToolStackView tool, LivingEntity attacker) {
        if (tool.getPersistentData().getFloat(FORBIDDEN) < FORBIDDEN_MAX) return;

        // 全量真伤 AOE (以攻击者为中心, 4 格, 含队友/玩家 —— 毕业词条的代价)
        float rate = transferRate(tool);
        float damage = FORBIDDEN_MAX * rate * OVERLOAD_MULTIPLIER;
        DamageSource source = pureSource(attacker);
        for (LivingEntity entity : attacker.level().getEntitiesOfClass(LivingEntity.class, attacker.getBoundingBox().inflate(OVERLOAD_RADIUS), e -> e != attacker)) {
            entity.hurt(source, damage);
        }

        // 禁忌清空 + 强罪业 (伤害减半)
        tool.getPersistentData().putFloat(FORBIDDEN, 0.0f);
        tool.getPersistentData().putInt(SIN_TIMER, SIN_DURATION_OVERLOAD);
        tool.getPersistentData().putFloat(SIN_PENALTY, SIN_PENALTY_OVERLOAD);

        // 演出: 爆炸音效 + 粒子
        Level level = attacker.level();
        level.playSound(null, attacker.getX(), attacker.getY(), attacker.getZ(), SoundEvents.DRAGON_FIREBALL_EXPLODE, SoundSource.PLAYERS, 1.0F, 1.0F);
        level.addParticle(ParticleTypes.EXPLOSION_EMITTER, attacker.getX(), attacker.getY() + 1.0, attacker.getZ(), 0, 0, 0);
    }

    // 赎罪: 罪业期间虚形态命中, 累计 ATONE_REQUIRED 次解除
    private static void atone(IToolStackView tool, LivingEntity attacker) {
        if (tool.getPersistentData().getInt(SIN_TIMER) <= 0) return;
        int count = tool.getPersistentData().getInt(ATONE_COUNT) + 1;
        if (count >= ATONE_REQUIRED) {
            // 赎罪成功: 解除罪业
            tool.getPersistentData().putInt(SIN_TIMER, 0);
            tool.getPersistentData().putInt(ATONE_COUNT, 0);
            tool.getPersistentData().putFloat(SIN_PENALTY, 1.0f);
            attacker.level().playSound(null, attacker.getX(), attacker.getY(), attacker.getZ(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.8F, 1.4F);
        } else {
            tool.getPersistentData().putInt(ATONE_COUNT, count);
        }
    }

    // 禁忌耗尽: 强制切回【虚】+ 普通罪业 5 秒
    private static void forceToXu(IToolStackView tool, LivingEntity attacker) {
        tool.getPersistentData().putString(MODE, MODE_XU);
        tool.getPersistentData().putInt(SIN_TIMER, SIN_DURATION);
        tool.getPersistentData().putFloat(SIN_PENALTY, SIN_PENALTY_NORMAL);
        tool.getPersistentData().putInt(CONSUME_STEP, 0);
        tool.getPersistentData().putInt(COMBO, 0);
        attacker.level().playSound(null, attacker.getX(), attacker.getY(), attacker.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.6F, 0.9F);
    }

    // 右键切换形态: 重置消耗档位/连击, 附带音效与粒子
    private static void switchMode(IToolStackView tool, LivingEntity user) {
        boolean toYun = isXu(tool);
        tool.getPersistentData().putString(MODE, toYun ? MODE_YUN : MODE_XU);
        tool.getPersistentData().putInt(SWITCH_CD, SWITCH_COOLDOWN);
        tool.getPersistentData().putInt(CONSUME_STEP, 0);
        tool.getPersistentData().putInt(COMBO, 0);
        tool.getPersistentData().putInt(COMBO_TARGET_ID, 0);

        // 演出: 允=金色纹路, 虚=紫黑雾气
        Level level = user.level();
        if (toYun) {
            level.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.6F, 1.2F);
            for (int i = 0; i < 8; i++) {
                level.addParticle(ParticleTypes.GLOW, user.getX() + level.random.nextDouble() * 1.5 - 0.75, user.getY() + 1 + level.random.nextDouble() * 1.2, user.getZ() + level.random.nextDouble() * 1.5 - 0.75, 0, 0.2, 0);
            }
        } else {
            level.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.6F, 0.8F);
            for (int i = 0; i < 8; i++) {
                level.addParticle(ParticleTypes.SOUL_FIRE_FLAME, user.getX() + level.random.nextDouble() * 1.5 - 0.75, user.getY() + 1 + level.random.nextDouble() * 1.2, user.getZ() + level.random.nextDouble() * 1.5 - 0.75, 0, 0.2, 0);
            }
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
        return penalty <= 0 ? 1.0f : penalty; // 未存储时默认无惩罚
    }

    private static boolean canModified(IToolStackView tool) {
        ToolType type = ToolType.from(tool.getItem(), CAN_BE_USE_ON_TYPES);
        return type != null;
    }
}
