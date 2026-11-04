package org.mylove.tprt.ModBuff;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.mylove.tprt.registries.ModBuffRegistry;

/** Existing greed buff: amplifier + 1 is the stack count. */
@Mod.EventBusSubscriber(modid = "tprt")
public class myl_girig_pro extends MobEffect {
    public myl_girig_pro() {
        super(MobEffectCategory.HARMFUL, 0x0f1f2f);
    }

    @SubscribeEvent
    public static void decay(LivingEvent.LivingTickEvent event) {
        var entity = event.getEntity();
        if (entity.level().isClientSide) return;
        var effect = ModBuffRegistry.MYL_GIRIG_PRO.get();
        var current = entity.getEffect(effect);
        // LivingTick runs before the effects iterator, so replacement cannot invalidate it.
        if (current != null && current.getDuration() == 1 && current.getAmplifier() > 0) {
            int next = Math.min(5, current.getAmplifier()) - 1;
            entity.removeEffect(effect);
            entity.addEffect(new MobEffectInstance(effect, 21, next));
        }
    }
}
