package org.mylove.tprt.tags;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import org.jetbrains.annotations.Nullable;
import org.mylove.tprt.Tprt;
import org.mylove.tprt.hooks.KillingHook;
import org.mylove.tprt.registries.ModHooks;
import slimeknights.mantle.client.TooltipKey;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.display.TooltipModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.GeneralInteractionModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.InteractionSource;
import slimeknights.tconstruct.library.modifiers.hook.interaction.InventoryTickModifierHook;
import slimeknights.tconstruct.library.modifiers.impl.NoLevelsModifier;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.tools.stats.ToolType;

import java.util.Arrays;
import java.util.List;

/// 千年, 即 630,720,000,000 tick
public class millennium extends NoLevelsModifier implements KillingHook, TooltipModifierHook, GeneralInteractionModifierHook, InventoryTickModifierHook {
    public static final ToolType[] CAN_BE_USE_ON_TYPES = {ToolType.MELEE};
    public static final int TickConsumePerT = 5;

    private static final ResourceLocation MILLENNIUM_TIME = ResourceLocation.fromNamespaceAndPath(Tprt.MODID, "millennium_time");
    private static final ResourceLocation MILLENNIUM_ACTIVE = ResourceLocation.fromNamespaceAndPath(Tprt.MODID, "millennium_active");

    public enum RANK {
        SSS(630720000000f),
        SS (63072000000f),
        S  (6307200000f),
        A  (630720000f),
        B  (63072000f),
        C  (6307200f),
        D  (630720f),
        E  (0f);

        public float tickRequired;

        RANK(float tickRequired) {
        }
    }

    @Override
    public int getPriority() {
        return 1;
    }

    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        hookBuilder.addHook(this, ModHooks.KILLING_HOOK, ModifierHooks.TOOLTIP, ModifierHooks.GENERAL_INTERACT, ModifierHooks.INVENTORY_TICK);
    }

    @Override
    public void onKillLivingTarget(IToolStackView tool, LivingDeathEvent event, LivingEntity attacker, LivingEntity target, int level) {
        if (!canModified(tool)) return;

        if(event.getSource().getEntity() == attacker) {
            float time0 = tool.getPersistentData().getFloat(MILLENNIUM_TIME);
            float addTime = target.tickCount;
            System.out.println(time0+ " : " +addTime);

            if (!Float.isNaN(time0)){
                tool.getPersistentData().putFloat(MILLENNIUM_TIME, time0 + addTime);
            } else {
                tool.getPersistentData().putFloat(MILLENNIUM_TIME, addTime);
            }
        }
    }

    @Override
    public void onInventoryTick(IToolStackView tool, ModifierEntry modifier, Level world, LivingEntity holder, int itemSlot, boolean isSelected, boolean isCorrectSlot, ItemStack stack) {
        if (!canModified(tool)) return;
        float time = tool.getPersistentData().getFloat(MILLENNIUM_TIME);
        if (time >= TickConsumePerT) {
            time -= TickConsumePerT;
        } else {
            time = 0;
            setActive(tool, false);
        }
        tool.getPersistentData().putFloat(MILLENNIUM_TIME, time);
    }

    @Override
    public InteractionResult onToolUse(IToolStackView tool, ModifierEntry modifier, Player player, InteractionHand hand, InteractionSource source) {
        if (source == InteractionSource.RIGHT_CLICK && !tool.isBroken()) {
            GeneralInteractionModifierHook.startUsing(tool, modifier.getId(), player, hand);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public void onFinishUsing(IToolStackView tool, ModifierEntry modifier, LivingEntity entity) {
        System.out.println("onFinishUsing: isClientSide? " + entity.level().isClientSide);
        if (!canModified(tool)) return;

        if (!tool.isBroken()) {
            triggerActive(tool);
        }
    }

    @Override
    public UseAnim getUseAction(IToolStackView tool, ModifierEntry modifier) {
        return UseAnim.BLOCK;
    }

    @Override
    public int getUseDuration(IToolStackView tool, ModifierEntry modifier) {
        return 0;
    }

    @Override
    public void addTooltip(IToolStackView tool, ModifierEntry modifier, @Nullable Player player, List<Component> tooltip, TooltipKey tooltipKey, TooltipFlag tooltipFlag) {
        if (!canModified(tool)) return;

        double sec = Math.floor(tool.getPersistentData().getFloat(MILLENNIUM_TIME) / 20);
        tooltip.add(Component.literal("已积攒时间: " + (Double.isNaN(sec) ? 0 : sec) + "秒"));
    }




    private static RANK calculateRank(IToolStackView tool) {
        float ticks = tool.getPersistentData().getFloat(MILLENNIUM_TIME);
        RANK rank = RANK.E;
        var optional = Arrays.stream(RANK.values()).filter(R -> ticks >= R.tickRequired).findFirst();
        if (optional.isPresent()) {
            rank = optional.get();
        }
        return rank;
    }

    private static boolean triggerActive(IToolStackView tool) {
        boolean mode = tool.getPersistentData().getBoolean(MILLENNIUM_ACTIVE);
        tool.getPersistentData().putBoolean(MILLENNIUM_ACTIVE, !mode);
        return !mode;
    }

    private static void setActive(IToolStackView tool, boolean mode) {
        tool.getPersistentData().putBoolean(MILLENNIUM_ACTIVE, mode);
    }

    private static boolean isActive(IToolStackView tool) {
        return tool.getPersistentData().getBoolean(MILLENNIUM_ACTIVE);
    }

    private static boolean canModified(IToolStackView tool) {
        ToolType type = ToolType.from(tool.getItem(), CAN_BE_USE_ON_TYPES);
        return type != null;
    }
}
