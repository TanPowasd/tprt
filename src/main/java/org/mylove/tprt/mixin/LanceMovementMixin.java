package org.mylove.tprt.mixin;

import net.minecraft.client.player.LocalPlayer;
import org.mylove.tprt.registries.ModifierRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import slimeknights.tconstruct.library.modifiers.hook.interaction.GeneralInteractionModifierHook;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

/** Only movement/sprint checks ignore active lance use; actual use state is untouched. */
@Mixin(LocalPlayer.class)
public abstract class LanceMovementMixin {
    @org.spongepowered.asm.mixin.Shadow
    protected int sprintTriggerTime;

    // aiStep's second sprintTriggerTime write is the item-use reset. Preserve
    // double-tap sprint timing only for the active charge, not crouching/etc.
    @Redirect(method = "aiStep", at = @At(value = "FIELD",
            target = "Lnet/minecraft/client/player/LocalPlayer;sprintTriggerTime:I",
            opcode = org.objectweb.asm.Opcodes.PUTFIELD, ordinal = 1))
    private void tprt$keepChargeSprintTap(LocalPlayer player, int value) {
        if (tprt$restrictsSprinting(player)) sprintTriggerTime = value;
    }

    @org.spongepowered.asm.mixin.injection.Inject(method = "stopUsingItem", at = @At("HEAD"))
    private void tprt$latchForcedStop(org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        LocalPlayer player = (LocalPlayer) (Object) this;
        if (player.isUsingItem() && player.getUseItem().getItem() instanceof org.mylove.tprt.common.item.LanceItem) {
            org.mylove.tprt.client.LanceUseInput.ended();
        }
    }

    // Keep aiStep ordinal 0: TC's use_item_speed=1 compensates vanilla's .2
    // input multiplier with x5. Skipping that multiplier would apply x5 twice.
    @Redirect(method = "aiStep", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/player/LocalPlayer;isUsingItem()Z", ordinal = 1))
    private boolean tprt$lanceAllowsSprintKey(LocalPlayer player) {
        return tprt$restrictsSprinting(player);
    }

    @Redirect(method = "canStartSprinting", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/player/LocalPlayer;isUsingItem()Z"))
    private boolean tprt$lanceAllowsStartingSprint(LocalPlayer player) {
        return tprt$restrictsSprinting(player);
    }

    @org.spongepowered.asm.mixin.Unique
    private static boolean tprt$restrictsSprinting(LocalPlayer player) {
        if (!player.isUsingItem()) return false;
        var stack = player.getUseItem();
        if (ToolStack.isInitialized(stack)
                && GeneralInteractionModifierHook.getActiveModifier(ToolStack.from(stack))
                .matches(ModifierRegistry.LANCE_CHARGE.getId())) return false;
        return true;
    }
}
