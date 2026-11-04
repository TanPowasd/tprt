package org.mylove.tprt.ModBuff;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/** Display-only charge counter: amplifier 0..3 represents one..four charges. */
public final class OverlimitProgressEffect extends MobEffect {
    public OverlimitProgressEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xE6AC48);
    }
}
