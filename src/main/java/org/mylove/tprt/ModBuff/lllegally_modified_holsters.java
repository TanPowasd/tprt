package org.mylove.tprt.ModBuff;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.mylove.tprt.ModUsed.ModMaths.OiMaths;

import java.util.UUID;

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
        if(entity!=null){
            AttributeInstance attackSpeedAttr = entity.getAttribute(Attributes.ATTACK_SPEED);
            if(attackSpeedAttr == null){
                return;
            }
            int bufnum=amplifier+1;
            double NewAttackSpeed=  entity.getAttributeValue(Attributes.ATTACK_SPEED);
            double mid= OiMaths.PRIME.getPrime(bufnum);
            mid=Math.log10(mid)/ Math.log10(2.0);
            NewAttackSpeed= NewAttackSpeed * mid;          
            if(attackSpeedAttr!=null){
                UUID NEW_SPEED_UUID = UUID.fromString("874C97BF-6CF1-4E95-88D7-2B9828B42A2A");
                attackSpeedAttr.removeModifier(NEW_SPEED_UUID);
                attackSpeedAttr.addTransientModifier(
                        new AttributeModifier(
                                NEW_SPEED_UUID,
                                "attack speed modifier",
                                NewAttackSpeed,
                                AttributeModifier.Operation.ADDITION
                        )
                );
            }
        }
    }
}
