package org.mylove.tprt.ModBuff;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.*;

import java.util.UUID;

public class moredamage extends MobEffect{
    public moredamage(){
        super(MobEffectCategory.HARMFUL,0x3f3f3f);
    }
    @Override
    public boolean isDurationEffectTick(int duration,int amplifier){
        return false;
    }
    @Override
    public void addAttributeModifiers(LivingEntity entity, AttributeMap attributes, int amplifier){
        super.addAttributeModifiers(entity,attributes,amplifier);
        double armorPenalty=-0.2*(amplifier+1);
        double currentArmor=entity.getAttributeValue(Attributes.ARMOR);
        double minArmorPenalty=-currentArmor;// most in 0
        armorPenalty=Math.max(armorPenalty,minArmorPenalty);
        AttributeInstance armorAttr=entity.getAttribute(Attributes.ARMOR);
        if(armorAttr!=null){
            UUID ARMOR_PENALTY_UUID = UUID.fromString("874C97BF-6CF1-4E95-88D7-2B8828B42A2A");
            armorAttr.removeModifier(ARMOR_PENALTY_UUID);
            armorAttr.addTransientModifier(new AttributeModifier(
                    ARMOR_PENALTY_UUID,
                    "armor penalty",
                    armorPenalty,
                    AttributeModifier.Operation.MULTIPLY_TOTAL
            ));
        }
    }
}

