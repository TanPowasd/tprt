package org.mylove.tprt.mixin;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.LivingEntity;
import org.mylove.tprt.client.LanceChargePose;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Uses an existing ArmPose enum and applies lance-only rotations after vanilla. */
@Mixin(HumanoidModel.class)
public abstract class LanceArmPoseMixin {
    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("TAIL"))
    private void tprt$braceActiveLance(LivingEntity entity, float limbSwing, float limbSwingAmount,
                                     float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        LanceChargePose.apply((HumanoidModel<?>) (Object) this, entity);
    }
}
