package org.mylove.tprt.mixin;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.mylove.tprt.client.LanceDisplayModel;
import org.mylove.tprt.client.LanceObjClientEvents;
import org.mylove.tprt.registries.item.ItemsRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Resolve once with the real actor; never replace Tinkers' registry model. */
@Mixin(ItemRenderer.class)
public abstract class LanceItemRendererMixin {
    @Inject(method = "getModel", at = @At("RETURN"), cancellable = true)
    private void tprt$lanceDisplayModel(ItemStack stack, Level level, LivingEntity entity, int seed,
                                       CallbackInfoReturnable<BakedModel> cir) {
        if (!stack.is(ItemsRegistry.LANCE.get())) return;
        BakedModel display = LanceObjClientEvents.displayModel();
        if (display == null) return;
        BakedModel resolved = display.getOverrides().resolve(display, stack,
                level instanceof ClientLevel clientLevel ? clientLevel : null, entity, seed);
        if (resolved != null) display = resolved;
        cir.setReturnValue(new LanceDisplayModel(cir.getReturnValue(), display));
    }
}
