package org.mylove.tprt.ModBuff;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/** Display only: authoritative ranged stacks remain independent in the shooter's data. */
public class RangedGreedEffect extends MobEffect {
    public RangedGreedEffect() {
        super(MobEffectCategory.HARMFUL, 0x0f1f2f);
    }
}
