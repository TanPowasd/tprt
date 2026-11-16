package org.mylove.tprt.mixin;

import com.github.L_Ender.cataclysm.blockentities.AltarOfAmethyst_Block_Entity;
import com.github.L_Ender.cataclysm.crafting.AltarOfAmethystRecipe;
import com.github.L_Ender.cataclysm.init.ModRecipeTypes;
import net.minecraft.world.SimpleContainer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.mylove.tprt.compat.Cataclysm.recipe.AmethystToolBlessingRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

/** Cataclysm 3.31 uses getResult().copy(), so dynamic tools need their own completion path. */
@Pseudo
@Mixin(targets = "com.github.L_Ender.cataclysm.blockentities.AltarOfAmethyst_Block_Entity", remap = false)
public abstract class AltarOfAmethystMixin {
    /** Inventory synchronization is not persistence: mark every server-side mutation dirty. */
    @Inject(method = "setItem", at = @At("RETURN"), remap = false)
    private void tprt$persistInventoryChange(int slot, ItemStack stack, CallbackInfo ci) {
        AltarOfAmethyst_Block_Entity altar = (AltarOfAmethyst_Block_Entity) (Object) this;
        if (altar.getLevel() != null && !altar.getLevel().isClientSide) {
            altar.blessingProgress = 0;
            altar.setChanged();
        }
    }

    @Inject(method = {"clearContent", "m_6211_"}, at = @At("RETURN"), remap = false)
    private void tprt$persistInventoryClear(CallbackInfo ci) {
        AltarOfAmethyst_Block_Entity altar = (AltarOfAmethyst_Block_Entity) (Object) this;
        if (altar.getLevel() != null && !altar.getLevel().isClientSide) {
            altar.blessingProgress = 0;
            altar.setChanged();
        }
    }

    @Inject(method = "cookTick", at = @At("HEAD"), cancellable = true, remap = false)
    private static void tprt$blessActualTool(Level level, BlockPos pos, BlockState state,
                                           AltarOfAmethyst_Block_Entity altar, CallbackInfo ci) {
        ItemStack input = altar.getItem(0);
        if (input.isEmpty()) return;
        // Cataclysm 3.31's helper returns empty when its occupied one-slot inventory
        // has no empty stacks (noneMatch(ItemStack::isEmpty)). Query the same recipe
        // type as native cookTick directly, without that inverted inventory guard.
        Optional<AltarOfAmethystRecipe> selected = level.getRecipeManager().getRecipeFor(
                ModRecipeTypes.AMETHYST_BLESS.get(), new SimpleContainer(input), level);
        if (selected.isEmpty() || !(selected.get() instanceof AmethystToolBlessingRecipe recipe)) return;

        // Cancel only our recipe. The original crab recipe keeps the entire native ticker.
        ci.cancel();
        altar.tickCounts++;
        altar.brightThisTick = true;
        altar.blessingProgress = Math.min(altar.blessingProgress + 1, recipe.getTime());
        if (altar.blessingProgress >= recipe.getTime() && !level.isClientSide) {
            ItemStack result = recipe.bless(input);
            altar.blessingProgress = 0;
            if (!result.isEmpty()) {
                // setItem broadcasts Cataclysm's inventory update; no client-side consumption.
                altar.setItem(0, result);
                altar.setChanged();
            } else {
                altar.brightThisTick = false;
            }
        }
    }
}
