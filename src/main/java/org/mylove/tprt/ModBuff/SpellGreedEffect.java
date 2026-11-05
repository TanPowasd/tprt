package org.mylove.tprt.ModBuff;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import org.mylove.tprt.compat.IronsSpellBooks.Modifiers.SpellGreed;

/** Display/cleanup marker; attributes are owned by the spellbook's modifier hooks. */
public class SpellGreedEffect extends MobEffect {
    public SpellGreedEffect() { super(MobEffectCategory.BENEFICIAL, 0xC8A542); }
    @Override public boolean isDurationEffectTick(int duration, int amplifier) { return true; }
    @Override public void applyEffectTick(LivingEntity entity, int amplifier) { SpellGreed.tick(entity); }
}
