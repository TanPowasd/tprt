package org.mylove.tprt.compat.Immortalers_Delight.Modifiers;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.registries.ForgeRegistries;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.armor.OnAttackedModifierHook;
import slimeknights.tconstruct.library.modifiers.impl.NoLevelsModifier;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.context.EquipmentContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

public class lingering_infusion extends NoLevelsModifier implements OnAttackedModifierHook {

    public void registerHooks(ModuleHookMap.Builder hookBuilder) {
        hookBuilder.addHook(this, ModifierHooks.ON_ATTACKED);
    }

    public static MobEffect getEffect(){
        ResourceLocation effectId=new ResourceLocation("immortalers_delight","lingering_infusion");
        return ForgeRegistries.MOB_EFFECTS.getValue(effectId);
    }

    public void onAttacked(IToolStackView tool, ModifierEntry modifier, EquipmentContext context, EquipmentSlot slotType, DamageSource source, float amount, boolean isDirectDamage){
        LivingEntity living = context.getEntity();
        if (living instanceof Player player){
            MobEffect effect = getEffect();
            MobEffectInstance x = player.getEffect(effect);
            if ( x == null){
                player.addEffect(new MobEffectInstance(effect, 80, 3));
            }
            else {
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 80, 2));
            }
        }
    }
}
