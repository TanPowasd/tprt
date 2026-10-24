package org.mylove.tprt.compat.Cataclysm.Modifiers;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeDamageModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeHitModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.display.TooltipModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.InventoryTickModifierHook;
import slimeknights.tconstruct.library.modifiers.impl.NoLevelsModifier;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.helper.ToolAttackUtil;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * 机械之星 (攻击): 护甲值增伤 + 红石充能 + 三头打击。
 *
 * 三头打击: 攻击会记录目标 (最多 3 个, 10 秒失效);
 * 再次攻击已记录的目标时, 其它被记录的目标同步受到一次完全相同的工具攻击。
 * 每有一个协同目标消耗 10 点充能 —— 机器不动就不耗电。
 *
 * 协同攻击通过 ToolAttackUtil 发起, 会再次触发 afterMeleeHit,
 * 因此必须用 COORDINATING 标记阻断递归。
 */
public class mechanical_star_att extends NoLevelsModifier
        implements MeleeDamageModifierHook, MeleeHitModifierHook, InventoryTickModifierHook, TooltipModifierHook {

    /** 充能 (红石) */
    public static final ResourceLocation MECH_CHARGE = new ResourceLocation("tprt", "mech_charge");
    public static final int MAX_CHARGE = 160;
    public static final int CHARGE_PER_TICK = 10;
    public static final int REDSTONE_COST = 4;
    /** 红石块充能量 */
    public static final int CHARGE_PER_BLOCK = 24;

    /** 三个头的目标记录 */
    public static final ResourceLocation[] TARGET_KEYS = new ResourceLocation[]{
            new ResourceLocation("tprt", "mech_target_0"),
            new ResourceLocation("tprt", "mech_target_1"),
            new ResourceLocation("tprt", "mech_target_2")};
    public static final ResourceLocation TARGET_TIME = new ResourceLocation("tprt", "mech_target_time");
    /** 记录有效时长 (10 秒) */
    public static final int RECORD_TICKS = 200;
    /** 每个协同目标消耗的充能 */
    public static final int COST_PER_TARGET = 10;

    /** 协同攻击进行中: 阻断递归触发 */
    private static boolean COORDINATING = false;

    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        super.registerHooks(hookBuilder);
        hookBuilder.addHook(this, ModifierHooks.MELEE_DAMAGE, ModifierHooks.MELEE_HIT,
                ModifierHooks.INVENTORY_TICK, ModifierHooks.TOOLTIP);
    }

    /** 护甲值 + 韧性增伤 */
    @Override
    public float getMeleeDamage(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float baseDamage, float damage) {
        LivingEntity entity = context.getLivingTarget();
        LivingEntity attacker = context.getAttacker();
        if (entity != null) {
            float x = (float) (attacker.getArmorValue()
                    + Objects.requireNonNull(attacker.getAttribute(Attributes.ARMOR_TOUGHNESS)).getValue());
            double y = 1.5 * x / (2 * x + 200);
            return (float) (damage * (1 + 2 * y));
        }
        return damage;
    }

    /** 三头打击: 记录目标, 攻击已记录目标时其它记录目标同步受伤 */
    @Override
    public void afterMeleeHit(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float damageDealt) {
        // 协同攻击内部不再记录/协同, 避免无限递归
        if (COORDINATING) {
            return;
        }
        if (!(context.getAttacker() instanceof Player player)) {
            return;
        }
        LivingEntity target = context.getLivingTarget();
        if (target == null || player.level().isClientSide) {
            return;
        }
        var data = tool.getPersistentData();
        long now = player.level().getGameTime();
        // 超时清除
        int stamp = data.getInt(TARGET_TIME);
        if (stamp > 0 && now - stamp > RECORD_TICKS) {
            for (ResourceLocation key : TARGET_KEYS) {
                data.putString(key, "");
            }
        }
        List<UUID> recorded = readTargets(data);
        UUID targetId = target.getUUID();
        if (!recorded.contains(targetId)) {
            if (recorded.size() >= TARGET_KEYS.length) {
                recorded.remove(0);
            }
            recorded.add(targetId);
            writeTargets(data, recorded);
            data.putInt(TARGET_TIME, (int) now);
            return;
        }
        // 已记录: 其它目标同步受伤, 每个消耗 10 充能
        int charge = data.getInt(MECH_CHARGE);
        int others = 0;
        COORDINATING = true;
        try {
            for (UUID uuid : recorded) {
                if (uuid.equals(targetId)) {
                    continue;
                }
                if (charge < (others + 1) * COST_PER_TARGET) {
                    break;
                }
                var entity = ((ServerLevel) player.level()).getEntity(uuid);
                if (!(entity instanceof LivingEntity other) || !other.isAlive()) {
                    continue;
                }
                other.invulnerableTime = 0;
                ToolAttackUtil.attackEntity(tool, player, other);
                others++;
            }
        } finally {
            COORDINATING = false;
        }
        if (others > 0) {
            data.putInt(MECH_CHARGE, Math.max(0, charge - others * COST_PER_TARGET));
        }
        data.putInt(TARGET_TIME, (int) now);
    }

    private static List<UUID> readTargets(ModDataNBT data) {
        List<UUID> list = new ArrayList<>();
        for (ResourceLocation key : TARGET_KEYS) {
            String raw = data.getString(key);
            if (!raw.isEmpty()) {
                try {
                    list.add(UUID.fromString(raw));
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
        return list;
    }

    private static void writeTargets(ModDataNBT data, List<UUID> ids) {
        for (int i = 0; i < TARGET_KEYS.length; i++) {
            data.putString(TARGET_KEYS[i], i < ids.size() ? ids.get(i).toString() : "");
        }
    }

    /** 每 0.5 秒用 4 个红石补充 10 点充能, 并给记录目标挂黑烟 */
    @Override
    public void onInventoryTick(IToolStackView tool, ModifierEntry modifier, Level level, LivingEntity entity,
                                int itemSlot, boolean isSelected, boolean isCorrectSlot, net.minecraft.world.item.ItemStack stack) {
        if (level.isClientSide || !(entity instanceof Player player)) {
            return;
        }
        if (level.getGameTime() % 10L != 0L) {
            return;
        }
        var data = tool.getPersistentData();
        int charge = data.getInt(MECH_CHARGE);
        // 优先红石块; 只在不会溢出时消耗
        if (charge <= MAX_CHARGE - CHARGE_PER_BLOCK && consumeRedstoneBlock(player)) {
            data.putInt(MECH_CHARGE, charge + CHARGE_PER_BLOCK);
        } else if (charge <= MAX_CHARGE - CHARGE_PER_TICK && consumeRedstone(player, REDSTONE_COST)) {
            data.putInt(MECH_CHARGE, charge + CHARGE_PER_TICK);
        }
        // 记录目标的黑色烟雾提示
        List<UUID> ids = readTargets(data);
        if (!ids.isEmpty() && level instanceof ServerLevel serverLevel) {
            int stamp = data.getInt(TARGET_TIME);
            if (level.getGameTime() - stamp > RECORD_TICKS) {
                // 超时: 清空记录, 让 tooltip 与粒子同步失效
                for (ResourceLocation key : TARGET_KEYS) {
                    data.putString(key, "");
                }
                return;
            }
            for (UUID uuid : ids) {
                var target = serverLevel.getEntity(uuid);
                if (target != null) {
                    serverLevel.sendParticles(ParticleTypes.SMOKE,
                            target.getX(), target.getY() + 1.0D, target.getZ(),
                            4, 0.25D, 0.4D, 0.25D, 0.01D);
                }
            }
        }
    }

    /** 消耗 1 个红石块 */
    private static boolean consumeRedstoneBlock(Player player) {
        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            var stack = inv.getItem(i);
            if (!stack.isEmpty() && stack.is(net.minecraft.world.item.Items.REDSTONE_BLOCK)) {
                stack.shrink(1);
                return true;
            }
        }
        return false;
    }

    private static boolean consumeRedstone(Player player, int count) {
        int need = count;
        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize() && need > 0; i++) {
            var stack = inv.getItem(i);
            if (!stack.isEmpty() && stack.is(net.minecraft.world.item.Items.REDSTONE)) {
                int take = Math.min(need, stack.getCount());
                stack.shrink(take);
                need -= take;
            }
        }
        return need == 0;
    }

    /** tooltip: 当前充能 + 已锁定目标数 */
    @Override
    public void addTooltip(IToolStackView tool, ModifierEntry modifier, @javax.annotation.Nullable Player player,
                           List<net.minecraft.network.chat.Component> list, slimeknights.mantle.client.TooltipKey tooltipKey,
                           net.minecraft.world.item.TooltipFlag tooltipFlag) {
        int charge = tool.getPersistentData().getInt(MECH_CHARGE);
        var data = tool.getPersistentData();
        int stamp = data.getInt(TARGET_TIME);
        boolean expired = stamp > 0 && player != null
                && player.level().getGameTime() - stamp > RECORD_TICKS;
        int targets = expired ? 0 : readTargets(data).size();
        list.add(net.minecraft.network.chat.Component.translatable("modifier.tprt.mechanical_star_att.tooltip", charge, MAX_CHARGE)
                .withStyle(net.minecraft.ChatFormatting.DARK_RED));
        if (targets > 0) {
            list.add(net.minecraft.network.chat.Component.translatable("modifier.tprt.mechanical_star_att.targets", targets)
                    .withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
        }
    }
}
