package org.mylove.tprt.common.item;

import slimeknights.tconstruct.library.tools.definition.ToolDefinition;
import slimeknights.tconstruct.library.tools.item.ModifiableItem;

/** Tinkers tool shell. Charging is supplied by its intrinsic lance_charge modifier. */
public class LanceItem extends ModifiableItem {
    public LanceItem(Properties properties, ToolDefinition definition) {
        super(properties, definition);
    }

    @Override
    public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
        consumer.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions() {
            @Override
            public net.minecraft.client.model.HumanoidModel.ArmPose getArmPose(net.minecraft.world.entity.LivingEntity entity,
                    net.minecraft.world.InteractionHand hand, net.minecraft.world.item.ItemStack stack) {
                return org.mylove.tprt.client.LanceChargePose.armPose(entity, hand, stack);
            }
        });
    }
}
