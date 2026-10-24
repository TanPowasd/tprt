package org.mylove.tprt.compat.IronsSpellBooks.ModEffects;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

/** 源质蓄能层数 (amplifier = 层数-1, 上限 5 层) */
public class source_charge extends MobEffect {
    public source_charge() {
        super(MobEffectCategory.BENEFICIAL, 0xB15CFF);
        this.addAttributeModifier(AttributeRegistry.SPELL_POWER.get(), "933b8c4c-5211-494b-af27-0ad4628cb713", 0.05, AttributeModifier.Operation.MULTIPLY_TOTAL);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return false;
    }
}