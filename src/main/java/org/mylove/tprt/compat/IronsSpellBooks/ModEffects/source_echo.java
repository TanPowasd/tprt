package org.mylove.tprt.compat.IronsSpellBooks.ModEffects;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/** 源质回响: 下一次施法获得等级提升 (满级时改为冷却减免) */
public class source_echo extends MobEffect {
    public source_echo() {
        super(MobEffectCategory.BENEFICIAL, 0xE0B0FF);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return false;
    }
}