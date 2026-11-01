package org.mylove.tprt.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.mylove.tprt.common.item.LanceItem;

/** Body-relative brace without late runtime ArmPose enum extension. */
public final class LanceChargePose {
    private LanceChargePose() {}

    public static HumanoidModel.ArmPose armPose(LivingEntity entity, InteractionHand hand, ItemStack stack) {
        return entity.isUsingItem() && entity.getUsedItemHand() == hand && entity.getUseItem() == stack
                ? HumanoidModel.ArmPose.ITEM : null;
    }

    /** Applied after vanilla animation; does not change the camera or the other arm. */
    public static void apply(HumanoidModel<?> model, LivingEntity entity) {
        if (!entity.isUsingItem() || !(entity.getUseItem().getItem() instanceof LanceItem)) return;
        HumanoidArm arm = entity.getUsedItemHand() == InteractionHand.MAIN_HAND
                ? entity.getMainArm() : entity.getMainArm().getOpposite();
        var part = arm == HumanoidArm.RIGHT ? model.rightArm : model.leftArm;
        part.xRot = -(float) Math.PI / 2.0F;
        part.yRot = 0.0F;
        part.zRot = 0.0F;
    }
}
