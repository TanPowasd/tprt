package org.mylove.tprt.compat.geckolib;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.mylove.tprt.common.item.LanceItem;
import slimeknights.tconstruct.library.tools.definition.ToolDefinition;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;
import software.bernie.geckolib.constant.DataTickets;

import java.util.function.Consumer;

/** GeckoLib-only implementation of the same Tinkers lance item. */
public class GeoLanceItem extends LanceItem implements GeoItem {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.tprt.lance_idle");
    private static final RawAnimation CHARGE = RawAnimation.begin().thenPlayAndHold("animation.tprt.lance_charge");
    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);

    public GeoLanceItem(Properties properties, ToolDefinition definition) {
        super(properties, definition);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "lance", 4, state -> {
            ItemStack stack = state.getData(DataTickets.ITEMSTACK);
            var entity = state.getData(DataTickets.ENTITY);
            if (entity instanceof LivingEntity living && stack != null && living.isUsingItem()
                    && living.getUseItem() == stack) {
                state.getController().setAnimation(CHARGE);
            } else {
                state.getController().setAnimation(IDLE);
            }
            return software.bernie.geckolib.core.object.PlayState.CONTINUE;
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public net.minecraft.client.model.HumanoidModel.ArmPose getArmPose(LivingEntity entity,
                    net.minecraft.world.InteractionHand hand, ItemStack stack) {
                return org.mylove.tprt.client.LanceChargePose.armPose(entity, hand, stack);
            }

            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return GeoLanceItemRenderer.INSTANCE;
            }
        });
    }
}
