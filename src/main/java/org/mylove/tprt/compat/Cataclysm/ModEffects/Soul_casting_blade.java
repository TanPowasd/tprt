package org.mylove.tprt.compat.Cataclysm.ModEffects;

import com.github.L_Ender.cataclysm.init.ModAttribute;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

public class Soul_casting_blade extends MobEffect {
    public Soul_casting_blade(){
        super(MobEffectCategory.HARMFUL, 0x39d2b2);
        this.addAttributeModifier(ModAttribute.ADDITIONAL_CRITICAL_DAMAGE.get(), "e6222ded-db34-4e86-8873-5d1cb4b49f09", 25, AttributeModifier.Operation.ADDITION);
    }
    @Override
    public boolean isDurationEffectTick(int duration,int amplifier){
        return false;
    }
}
