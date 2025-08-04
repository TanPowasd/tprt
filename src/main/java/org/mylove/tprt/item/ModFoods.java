package org.mylove.tprt.item;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;

public class ModFoods {
    public static final FoodProperties LR_APPLE=new FoodProperties.Builder()
            .nutrition(20)
            .saturationMod(1.2f)
            .effect(()->new MobEffectInstance(MobEffects.MOVEMENT_SPEED,32000),1f)
            .effect(()->new MobEffectInstance(MobEffects.REGENERATION,32000,250),1f)
            .effect(()->new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE,32000,4),1f)
            .effect(()->new MobEffectInstance(MobEffects.ABSORPTION,32000,50),1f)
            .effect(()->new MobEffectInstance(MobEffects.DIG_SPEED,32000,2),1f)
            .effect(()->new MobEffectInstance(MobEffects.NIGHT_VISION,32000),1f)
            .effect(()->new MobEffectInstance(MobEffects.FIRE_RESISTANCE,32000),1f)
            .effect(()->new MobEffectInstance(MobEffects.LUCK,32000,2),1f)
            .effect(()->new MobEffectInstance(MobEffects.JUMP,32000,1),1f)
            .fast()
            .alwaysEat()
            .build();

    public static final FoodProperties PIG_BUN=new FoodProperties.Builder()
            .nutrition(9)
            .saturationMod(1)
            .effect(()->new MobEffectInstance(MobEffects.HUNGER,15,1),1f)
            .effect(()->new MobEffectInstance(MobEffects.CONFUSION,200,1),1f)
            .alwaysEat()
            .build();
}
