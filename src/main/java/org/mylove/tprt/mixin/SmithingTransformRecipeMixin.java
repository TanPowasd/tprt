package org.mylove.tprt.mixin;

import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.SmithingTransformRecipe;
import org.mylove.tprt.common.ForgingProgress;
import org.mylove.tprt.registries.item.ItemsRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SmithingTransformRecipe.class)
public abstract class SmithingTransformRecipeMixin {
    @Unique
    private static final ResourceLocation tprt$FORGING_RECIPE =
            new ResourceLocation("tprt", "materials/tprt/forged_steel/forging");

    @Inject(method = "assemble(Lnet/minecraft/world/Container;Lnet/minecraft/core/RegistryAccess;)Lnet/minecraft/world/item/ItemStack;",
            at = @At("RETURN"), cancellable = true)
    private void tprt$advanceForging(Container container, RegistryAccess registries,
                                      CallbackInfoReturnable<ItemStack> cir) {
        if (!tprt$FORGING_RECIPE.equals(((SmithingTransformRecipe) (Object) this).getId())) {
            return;
        }
        ItemStack base = container.getItem(1);
        if (!base.is(ForgingProgress.STEEL)) {
            return;
        }
        int progress = Math.max(1, Math.min(ForgingProgress.TOTAL,
                ForgingProgress.getProgress(base) + 1));
        ItemStack result;
        if (progress == ForgingProgress.TOTAL) {
            result = new ItemStack(ItemsRegistry.FORGED_STEEL.get(), 1);
            if (base.hasTag()) {
                result.setTag(base.getTag().copy());
                result.getTag().remove(ForgingProgress.KEY);
                if (result.getTag().isEmpty()) result.setTag(null);
            }
        } else {
            result = base.copy();
            result.setCount(1);
            result.getOrCreateTag().putInt(ForgingProgress.KEY, progress);
        }
        // Consumption remains entirely vanilla: one template, base and addition per take.
        cir.setReturnValue(result);
    }
}
