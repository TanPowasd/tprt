package org.mylove.tprt.ModBuff;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/** Display-only timer. Damage benefits require a currently equipped valid charm. */
public final class OverlimitEmpoweredEffect extends MobEffect {
    public OverlimitEmpoweredEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xFF583C);
    }
}
