package org.mylove.tprt.registries;

import com.mojang.blaze3d.shaders.Effect;
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
            .nutrition(10)
            .saturationMod(1)
            .effect(()->new MobEffectInstance(MobEffects.HUNGER,15,1),1f)
            .effect(()->new MobEffectInstance(MobEffects.CONFUSION,200,1),1f)
            .alwaysEat()
            .build();
    public static final FoodProperties RED_TURTLE = new FoodProperties.Builder()
            .nutrition(8)
            .saturationMod(1.0f)
            .effect(() -> new MobEffectInstance(ModBuffRegistry.MOREDAMAGE.get(), 200, 0), 1f) // 10秒 moredamage
            .effect(() -> new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 400, 1), 1f) // 20秒 抗性提升2
            .effect(() -> new MobEffectInstance(MobEffects.ABSORPTION, 200, 0), 1f) // 10秒 伤害吸收1
            .effect(() -> new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 400, 1), 1f) // 20秒 力量2
            .effect(() -> new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 400, 0), 1f) // 20秒 缓慢1
            .alwaysEat()
            .build();
    public static final FoodProperties KWAT_WHEAT_GRAIN_INGOT = new FoodProperties.Builder()
            .nutrition(8)
            .saturationMod(1.5f)
            .effect(() -> new MobEffectInstance(MobEffects.WEAKNESS, 20*60, 9), 1f) // 60秒 虚弱10
            .effect(() -> new MobEffectInstance(MobEffects.WITHER, 20*60, 19), 1f) // 60秒 凋零20
            .alwaysEat()
            .build();
}
