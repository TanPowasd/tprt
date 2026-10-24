package org.mylove.tprt.compat.Cataclysm.Modifiers;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.modifiers.hook.armor.OnAttackedModifierHook;
import slimeknights.tconstruct.library.modifiers.impl.NoLevelsModifier;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.context.EquipmentContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 机械之星 (护甲): 相位护盾。
 *
 * 受到伤害时, 将 20% 的伤害转化为吸收盾 (上限为最大生命的 50%);
 * 护盾存在期间免疫击退; 护盾被清空时 "装甲过载":
 * 获得 3 秒速度提升与击退免疫 (喷射脱离), 并进入 10 秒冷却。
 * 5 秒未受伤则护盾自然衰减。
 */
public class mechanical_star_arm extends NoLevelsModifier implements OnAttackedModifierHook {

    public static final ModifierId ID = new ModifierId("tprt", "mechanical_star_arm");
    /** 伤害转护盾的比例 */
    public static final float CONVERT_RATIO = 0.20F;
    /** 护盾上限 (最大生命的比例) */
    public static final float CAP_RATIO = 0.50F;
    /** 多久未受伤开始衰减 */
    public static final long DECAY_DELAY = 100L;
    /** 衰减间隔 */
    public static final long DECAY_STEP = 20L;
    /** 过载持续时间 */
    public static final int OVERLOAD_TICKS = 60;
    /** 过载冷却 */
    public static final int OVERLOAD_COOLDOWN = 200;

    /** 玩家 -> (最近一次受伤时间, 过载可用时间) */
    private record State(long lastHurt, long overloadReady) {
    }

    private static final Map<UUID, State> STATES = new HashMap<>();
    /** 上一 tick 判定为"护盾将被击破"的玩家 */
    private static final java.util.Set<UUID> PENDING_OVERLOAD = new java.util.HashSet<>();

    static {
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, LivingDamageEvent.class, mechanical_star_arm::onDamage);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, LivingKnockBackEvent.class, mechanical_star_arm::onKnockBack);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, TickEvent.PlayerTickEvent.class, mechanical_star_arm::onTick);
    }

    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        super.registerHooks(hookBuilder);
        hookBuilder.addHook(this, ModifierHooks.ON_ATTACKED);
    }

    @Override
    public void onAttacked(IToolStackView tool, ModifierEntry modifier, EquipmentContext context,
                           EquipmentSlot slotType, DamageSource source, float amount, boolean isDirectDamage) {
        LivingEntity entity = context.getEntity();
        if (!(entity instanceof Player player) || player.level().isClientSide) {
            return;
        }
        long now = player.level().getGameTime();
        State state = STATES.getOrDefault(player.getUUID(), new State(0L, 0L));
        // 冷却中不转化
        if (now < state.overloadReady()) {
            return;
        }
        float cap = player.getMaxHealth() * CAP_RATIO;
        float current = player.getAbsorptionAmount();
        if (current >= cap) {
            return;
        }
        // 预判: 这一击会把护盾打空 (吸收值会被原版先扣)
        if (current > 0.0F && amount >= current) {
            PENDING_OVERLOAD.add(player.getUUID());
        }
        float add = Math.min(cap - current, amount * CONVERT_RATIO);
        if (add > 0.0F) {
            player.setAbsorptionAmount(current + add);
            STATES.put(player.getUUID(), new State(now, state.overloadReady()));
        }
        return;
    }

    /** 只有穿戴带本词条的护甲才生效 */
    private static boolean wearing(Player player) {
        for (net.minecraft.world.item.ItemStack stack : player.getArmorSlots()) {
            if (!stack.isEmpty() && ToolStack.isInitialized(stack)
                    && ToolStack.from(stack).getModifierLevel(ID) > 0) {
                return true;
            }
        }
        return false;
    }

    /** 伤害结算后: 若本次判定护盾被击破 -> 装甲过载 */
    private static void onDamage(LivingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide) {
            return;
        }
        if (!PENDING_OVERLOAD.remove(player.getUUID())) {
            return;
        }
        long now = player.level().getGameTime();
        State state = STATES.getOrDefault(player.getUUID(), new State(0L, 0L));
        STATES.put(player.getUUID(), new State(now, now + OVERLOAD_COOLDOWN));
        player.setAbsorptionAmount(0.0F);
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, OVERLOAD_TICKS, 1, false, false, true));
        // 护盾碎裂音效 (原版盾牌破裂声)
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                net.minecraft.sounds.SoundEvents.SHIELD_BREAK, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);
        player.displayClientMessage(
                Component.translatable("tooltip.tprt.mechanical_star_arm.overload").withStyle(ChatFormatting.RED), true);
    }
    /** 护盾存在或过载期间免疫击退 */
    private static void onKnockBack(LivingKnockBackEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (player.getAbsorptionAmount() > 0.0F
                || player.hasEffect(MobEffects.MOVEMENT_SPEED)) {
            event.setCanceled(true);
        }
    }

    /** 5 秒未受伤则护盾自然衰减 */
    private static void onTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) {
            return;
        }
        Player player = event.player;
        State state = STATES.get(player.getUUID());
        if (state == null) {
            return;
        }
        long now = player.level().getGameTime();
        if (now - state.lastHurt() < DECAY_DELAY) {
            return;
        }
        float shield = player.getAbsorptionAmount();
        if (shield <= 0.0F) {
            STATES.remove(player.getUUID());
            return;
        }
        // 每 DECAY_STEP tick 掉 10% 上限
        if ((now - state.lastHurt() - DECAY_DELAY) % DECAY_STEP == 0) {
            float step = player.getMaxHealth() * CAP_RATIO * 0.1F;
            player.setAbsorptionAmount(Math.max(0.0F, shield - step));
        }
    }
}