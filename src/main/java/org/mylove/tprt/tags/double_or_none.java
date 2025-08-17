package org.mylove.tprt.tags;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.modifiers.hook.armor.ModifyDamageModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.armor.OnAttackedModifierHook;
import slimeknights.tconstruct.library.modifiers.impl.NoLevelsModifier;
import slimeknights.tconstruct.library.modifiers.modules.technical.SlotInChargeModule;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.capability.TinkerDataCapability;
import slimeknights.tconstruct.library.tools.context.EquipmentContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import java.util.UUID;

public class double_or_none extends NoLevelsModifier implements ModifyDamageModifierHook, OnAttackedModifierHook {
    UUID uuid=UUID.fromString("95171EEA-3855-49D8-B44B-9F7B842FEB69");
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        super.registerHooks(hookBuilder);
        hookBuilder.addHook(this, ModifierHooks.MODIFY_DAMAGE);
        hookBuilder.addHook(this,ModifierHooks.ON_ATTACKED);
       // hookBuilder.addModule(new SlotInChargeModule(SLOT_IN_CHARGE));
    }
    public static MobEffect getEffect(){
        ResourceLocation effectId=new ResourceLocation("tprt","moredamage");
        return ForgeRegistries.MOB_EFFECTS.getValue(effectId);
    }
    public static double ifdouble=Math.random();
    @Override
    public float modifyDamageTaken(IToolStackView iToolStackView, ModifierEntry modifierEntry, EquipmentContext context, EquipmentSlot equipmentSlot, DamageSource damageSource, float amount, boolean isDirectDamage){
        LivingEntity entity =context.getEntity();
        LivingEntity attacker =null;
            if(damageSource.getEntity() instanceof LivingEntity){
                attacker = (LivingEntity) damageSource.getEntity();
            }
            if (attacker != null && entity != null) {
                if(ifdouble>=0.8){
                    return amount*2;
                }
                else{
                    return 0;
                }
            }
        return amount;
    }
    @Override
    public void onAttacked(IToolStackView tool, ModifierEntry modifier, EquipmentContext context, EquipmentSlot slotType, DamageSource source, float amount, boolean isDirectDamage) {
        LivingEntity entity =context.getEntity();
        LivingEntity attacker =null;
        if(source.getEntity() instanceof LivingEntity){
            attacker = (LivingEntity) source.getEntity();
        }
        if(entity!=null&&attacker!=null){

            if(ifdouble>=0.8){
                MobEffectInstance healthgrow=new MobEffectInstance(MobEffects.REGENERATION,200,1);
                entity.addEffect(healthgrow);
            }
            else{
                MobEffect effect = getEffect();
                MobEffectInstance instance = attacker.getEffect(effect);
                if(instance==null){
                    attacker.addEffect(new MobEffectInstance(getEffect(), 200));
                }
                else{
                    int amplifier = instance.getAmplifier();
                    int newAmplifier = amplifier + 1;
                    int duration = instance.getDuration()+10;
                    boolean ambient = instance.isAmbient();
                    boolean visible = instance.isVisible();
                    boolean showIcon = instance.showIcon();
                    MobEffectInstance newEffect = new MobEffectInstance(
                            effect,
                            duration,
                            newAmplifier,
                            ambient,
                            visible,
                            showIcon);
                    attacker.addEffect(newEffect);
                }
            }/*
            MobEffect effect = getEffect();
            MobEffectInstance instance = attacker.getEffect(effect);
            if(instance==null){
                attacker.addEffect(new MobEffectInstance(getEffect(), 200));
            }
            else{
                int amplifier = instance.getAmplifier();
                int newAmplifier = amplifier + 1;
                int duration = instance.getDuration()+10;
                boolean ambient = instance.isAmbient();
                boolean visible = instance.isVisible();
                boolean showIcon = instance.showIcon();
                MobEffectInstance newEffect = new MobEffectInstance(
                        effect,
                        duration,
                        newAmplifier,
                        ambient,
                        visible,
                        showIcon);
                attacker.addEffect(newEffect);
            }*/
        }
    }
}
