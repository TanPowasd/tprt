package org.mylove.tprt.Modifiers.melee;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import slimeknights.mantle.client.TooltipKey;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeDamageModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.display.TooltipModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.InventoryTickModifierHook;
import slimeknights.tconstruct.library.modifiers.impl.NoLevelsModifier;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import java.util.List;

/**
 * 每次实际受到正数火焰伤害增加一点（最多20）；200 tick无火焰伤害清零。
 * Actual damage is observed by BlazeforgedFireEvents, never inferred from burning ticks.
 */
public class kindled_rebirth extends NoLevelsModifier
        implements MeleeDamageModifierHook, InventoryTickModifierHook, TooltipModifierHook {

    @Override
    protected void registerHooks(ModuleHookMap.@NotNull Builder builder) {
        super.registerHooks(builder);
        builder.addHook(this, ModifierHooks.MELEE_DAMAGE, ModifierHooks.INVENTORY_TICK, ModifierHooks.TOOLTIP);
    }

    @Override
    public float getMeleeDamage(@NotNull IToolStackView tool, @NotNull ModifierEntry modifier,
                                @NotNull ToolAttackContext context, float baseDamage, float damage) {
        long now = context.getAttacker().level().getGameTime();
        return damage * (1.0F + BlazeforgedTraitData.damageBonus(tool, now));
    }

    @Override
    public void onInventoryTick(@NotNull IToolStackView tool, @NotNull ModifierEntry modifier,
                                @NotNull Level world, @NotNull LivingEntity holder, int itemSlot,
                                boolean isSelected, boolean isEquipped, @NotNull ItemStack stack) {
        if (!world.isClientSide && isSelected && holder.getMainHandItem() == stack && !tool.isBroken()) {
            BlazeforgedTraitData.points(tool, world.getGameTime());
        }
    }

    protected String tooltipKey() { return "modifier.tprt.kindled_rebirth.tooltip"; }

    @Override
    public void addTooltip(@NotNull IToolStackView tool, @NotNull ModifierEntry modifier,
                           @Nullable Player player, @NotNull List<Component> tooltip, @NotNull TooltipKey key,
                           @NotNull TooltipFlag flag) {
        int points = player == null ? BlazeforgedTraitData.rawPoints(tool)
                : BlazeforgedTraitData.displayPoints(tool, player.level().getGameTime());
        tooltip.add(Component.translatable(tooltipKey(),
                points, BlazeforgedTraitData.MAX_POINTS).withStyle(ChatFormatting.GOLD));
    }
}
