package org.mylove.tprt.compat.IronsSpellBooks.Modifiers;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import org.mylove.tprt.compat.IronsSpellBooks.IssCompat;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.modifiers.hook.armor.ModifyDamageModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.armor.OnAttackedModifierHook;
import slimeknights.tconstruct.library.modifiers.impl.NoLevelsModifier;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.context.EquipmentContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 魔力涌动 (护甲特性, 与魔源裹身并存)。
 *
 * - 受击时获得一层「源质蓄能」(复用武器的 buff, 上限 5 层) —— onAttacked
 * - 每层 5% 伤害减免, 满层 25% —— modifyDamageTaken
 * - 满层且单次伤害 > 50% 最大生命: 伤害压到原值的 25%,
 *   差额按 50 法力 = 1 点由法力支付 (最低 1 点, 法力最多扣到 0),
 *   扣法力与清层在 onAttacked 里完成, 2 秒冷却
 */
public class Mana_surge extends NoLevelsModifier implements ModifyDamageModifierHook, OnAttackedModifierHook {

    public static final ModifierId ID = new ModifierId("tprt", "mana_surge");
    public static final float REDUCTION_PER_LAYER = 0.05F;
    public static final int STACK_DURATION = 240;
    public static final int COOLDOWN_TICKS = 40;
    public static final float HEAVY_HIT_RATIO = 0.5F;
    public static final float FLOOR_RATIO = 0.25F;
    public static final float MANA_PER_DAMAGE = 50.0F;

    /** 冷却 */
    private static final Map<UUID, Long> COOLDOWN = new HashMap<>();
    /** 本次受伤待扣的法力 */
    private static final Map<UUID, Integer> PENDING_MANA = new HashMap<>();
    /** 本次受伤是否触发满层抵消 */
    private static final Set<UUID> PENDING_TRIGGER = new HashSet<>();

    static {
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, TickEvent.PlayerTickEvent.class, Mana_surge::onPlayerTick);
    }

    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        super.registerHooks(hookBuilder);
        hookBuilder.addHook(this, ModifierHooks.MODIFY_DAMAGE, ModifierHooks.ON_ATTACKED);
    }

    /** 减伤: 每层 5%; 满层高伤时压到原伤害的 25% */
    @Override
    public float modifyDamageTaken(IToolStackView tool, ModifierEntry modifier, EquipmentContext context,
                                   EquipmentSlot slotType, DamageSource source, float amount, boolean isDirectDamage) {
        if (!(context.getEntity() instanceof Player player) || player.level().isClientSide) {
            return amount;
        }
        MobEffectInstance charge = player.getEffect(IssCompat.SOURCE_CHARGE.get());
        if (charge == null) {
            return amount;
        }
        int layers = Math.min(5, charge.getAmplifier() + 1);
        float reduced = amount * (1.0F - REDUCTION_PER_LAYER * layers);
        // 满层 + 高伤: 进一步压到 25% 下限, 差额由法力支付
        if (layers >= 5 && amount > player.getMaxHealth() * HEAVY_HIT_RATIO && cooldownReady(player)) {
            float floor = amount * FLOOR_RATIO;
            int manaNeeded = (int) ((reduced - floor) * MANA_PER_DAMAGE);
            int mana = getMana(player);
            int paid = Math.min(manaNeeded, Math.max(0, mana));
            PENDING_MANA.put(player.getUUID(), paid);
            PENDING_TRIGGER.add(player.getUUID());
            float finalDamage = Math.max(floor, reduced - paid / MANA_PER_DAMAGE);
            return Math.max(1.0F, finalDamage);
        }
        return reduced;
    }

    /** 受击副作用: 叠层、满层提示、扣法力、清层、进冷却 */
    @Override
    public void onAttacked(IToolStackView tool, ModifierEntry modifier, EquipmentContext context,
                           EquipmentSlot slotType, DamageSource source, float amount, boolean isDirectDamage) {
        if (!(context.getEntity() instanceof Player player) || player.level().isClientSide) {
            return;
        }
        // 叠层
        MobEffectInstance current = player.getEffect(IssCompat.SOURCE_CHARGE.get());
        int previous = current == null ? -1 : current.getAmplifier();
        int amplifier = Math.min(4, previous + 1);
        player.addEffect(new MobEffectInstance(IssCompat.SOURCE_CHARGE.get(), STACK_DURATION, amplifier, false, true, true));
        if (amplifier == 4 && previous < 4) {
            player.displayClientMessage(Component.translatable("tooltip.tprt.source_charge.full"), true);
        }
        // 满层抵消的收尾
        if (!PENDING_TRIGGER.remove(player.getUUID())) {
            return;
        }
        Integer cost = PENDING_MANA.remove(player.getUUID());
        if (cost != null && cost > 0) {
            setMana(player, getMana(player) - cost);
        }
        player.removeEffect(IssCompat.SOURCE_CHARGE.get());
        COOLDOWN.put(player.getUUID(), player.level().getGameTime());
        player.displayClientMessage(Component.translatable("tooltip.tprt.mana_surge.trigger"), true);
    }

    private static boolean cooldownReady(Player player) {
        Long last = COOLDOWN.get(player.getUUID());
        if (last == null) {
            return true;
        }
        long now = player.level().getGameTime();
        return now < last || now - last >= COOLDOWN_TICKS;
    }

    private static int getMana(Player player) {
        return Math.max(0, (int) MagicData.getPlayerMagicData(player).getMana());
    }

    private static void setMana(Player player, int value) {
        MagicData.getPlayerMagicData(player).setMana(Math.max(0, value));
    }

    /** 冷却结束时给一次字幕提示 */
    private static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Player player = event.player;
        if (player.level().isClientSide) {
            return;
        }
        Long last = COOLDOWN.get(player.getUUID());
        if (last == null) {
            return;
        }
        long now = player.level().getGameTime();
        if (now >= last && now - last >= COOLDOWN_TICKS) {
            COOLDOWN.remove(player.getUUID());
            player.displayClientMessage(Component.translatable("tooltip.tprt.mana_surge.ready"), true);
        }
    }
}