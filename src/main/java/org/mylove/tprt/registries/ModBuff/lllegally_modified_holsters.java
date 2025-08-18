package org.mylove.tprt.registries.ModBuff;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeMap;

public class lllegally_modified_holsters extends MobEffect {
    public lllegally_modified_holsters() {
        super(MobEffectCategory.HARMFUL, 0xFF0000);
    }
    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return false;
    }
    @Override
    public void addAttributeModifiers(LivingEntity entity, AttributeMap attributes, int amplifier){
        super.addAttributeModifiers(entity,attributes,amplifier);
        //double newdamage=
    }
}
