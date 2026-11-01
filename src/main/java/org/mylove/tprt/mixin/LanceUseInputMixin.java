package org.mylove.tprt.mixin;

import net.minecraft.client.Minecraft;
import org.mylove.tprt.client.LanceUseInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class LanceUseInputMixin {
    @Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
    private void tprt$requireReleaseAfterCharge(CallbackInfo ci) {
        if (LanceUseInput.blocked()) ci.cancel();
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void tprt$observeUseRelease(CallbackInfo ci) {
        LanceUseInput.blocked();
    }
}
