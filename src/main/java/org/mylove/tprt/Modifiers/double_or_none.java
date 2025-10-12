package org.mylove.tprt.Modifiers;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.registries.ForgeRegistries;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.armor.ModifyDamageModifierHook;
import slimeknights.tconstruct.library.modifiers.impl.NoLevelsModifier;
import slimeknights.tconstruct.library.modifiers.modules.technical.SlotInChargeModule;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.capability.TinkerDataCapability;
import slimeknights.tconstruct.library.tools.context.EquipmentContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import java.util.UUID;

public class double_or_none extends NoLevelsModifier implements ModifyDamageModifierHook {
    UUID uuid=UUID.fromString("95171EEA-3855-49D8-B44B-9F7B842FEB69");
    static ResourceLocation XSF_don=new ResourceLocation("tprt","double_or_none");
    public static final TinkerDataCapability.TinkerDataKey<SlotInChargeModule.SlotInCharge> SLOT_IN_CHARGE = TinkerDataCapability.TinkerDataKey.of(XSF_don);
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        super.registerHooks(hookBuilder);
        hookBuilder.addHook(this, ModifierHooks.MODIFY_DAMAGE);
        hookBuilder.addModule(new SlotInChargeModule(SLOT_IN_CHARGE));
    }
    public static MobEffect getEffect(){
        ResourceLocation effectId=new ResourceLocation("tprt","moredamage");
        return ForgeRegistries.MOB_EFFECTS.getValue(effectId);
    }
    @Override
    public float modifyDamageTaken(IToolStackView iToolStackView, ModifierEntry modifierEntry, EquipmentContext context, EquipmentSlot equipmentSlot, DamageSource damageSource, float amount, boolean isDirectDamage){
        LivingEntity entity =context.getEntity();
        LivingEntity attacker =null;
            if(damageSource.getEntity() instanceof LivingEntity){
                attacker = (LivingEntity) damageSource.getEntity();
            }
            if (attacker != null && entity != null) {
                double ifdouble=Math.random();
                if(SlotInChargeModule.isInCharge(context.getTinkerData(), SLOT_IN_CHARGE, equipmentSlot)){
                    if(ifdouble>=0.8){
                        MobEffectInstance healthgrow=new MobEffectInstance(MobEffects.REGENERATION,200,1);
                        entity.addEffect(healthgrow);
                        return amount*2;
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
                        return 0;
                    }
                }
            }
        return amount;
    }
}
