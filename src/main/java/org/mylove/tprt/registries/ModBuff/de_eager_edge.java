package org.mylove.tprt.registries.ModBuff;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeMap;

public class de_eager_edge extends MobEffect {
    public de_eager_edge(){
        super(MobEffectCategory.HARMFUL,0x0f1f2f);
    }
    @Override
    public boolean isDurationEffectTick(int duration,int amplifier){
        return false;
    }
    @Override
    public void addAttributeModifiers(LivingEntity entity, AttributeMap attributes, int amplifier){
        super.addAttributeModifiers(entity, attributes, amplifier);
    }

}
