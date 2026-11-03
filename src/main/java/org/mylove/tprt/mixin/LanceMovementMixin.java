package org.mylove.tprt.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.player.LocalPlayer;
import org.mylove.tprt.client.LanceUseInput;
import org.mylove.tprt.common.item.LanceItem;
import org.mylove.tprt.registries.ModifierRegistry;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import slimeknights.tconstruct.library.modifiers.hook.interaction.GeneralInteractionModifierHook;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

/** Only the item-use sprint restrictions ignore active charge; actual use state is untouched. */
@Mixin(LocalPlayer.class)
public abstract class LanceMovementMixin {
    @Shadow protected int sprintTriggerTime;

    // Write 0 decrements the timer, write 1 resets it for item use, and write 2
    // resets it for crouching. Preserve the already-decremented timer only at 1.
    // Always invoke the operation so other wrappers still participate.
    @WrapOperation(method = "aiStep", at = @At(value = "FIELD",
            target = "Lnet/minecraft/client/player/LocalPlayer;sprintTriggerTime:I",
            opcode = Opcodes.PUTFIELD, ordinal = 1))
    private void tprt$keepChargeSprintTap(LocalPlayer player, int value, Operation<Void> original) {
        original.call(player, tprt$isActiveCharge(player) ? sprintTriggerTime : value);
    }

    @Inject(method = "stopUsingItem", at = @At("HEAD"))
    private void tprt$latchForcedStop(CallbackInfo ci) {
        LocalPlayer player = (LocalPlayer) (Object) this;
        if (player.isUsingItem() && player.getUseItem().getItem() instanceof LanceItem) {
            LanceUseInput.ended();
        }
    }

    // Do NOT modify aiStep ordinal 0: TC's use_item_speed=1 multiplies input
    // by 5 before vanilla's .2 slowdown. Skipping slowdown would leave x5 speed.
    // Transform the incoming expression, not the entire sprint predicate.
    @ModifyExpressionValue(method = "aiStep", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/player/LocalPlayer;isUsingItem()Z", ordinal = 1))
    private boolean tprt$lanceAllowsSprintKey(boolean usingItem) {
        return usingItem && !tprt$isActiveCharge((LocalPlayer) (Object) this);
    }

    @ModifyExpressionValue(method = "canStartSprinting", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/player/LocalPlayer;isUsingItem()Z"))
    private boolean tprt$lanceAllowsStartingSprint(boolean usingItem) {
        return usingItem && !tprt$isActiveCharge((LocalPlayer) (Object) this);
    }

    @Unique
    private static boolean tprt$isActiveCharge(LocalPlayer player) {
        if (!player.isUsingItem()) return false;
        var stack = player.getUseItem();
        return ToolStack.isInitialized(stack)
                && GeneralInteractionModifierHook.getActiveModifier(ToolStack.from(stack))
                .matches(ModifierRegistry.LANCE_CHARGE.getId());
    }
}
