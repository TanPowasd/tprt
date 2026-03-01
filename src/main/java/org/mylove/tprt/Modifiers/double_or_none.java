package org.mylove.tprt.Modifiers;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import org.mylove.tprt.registries.ModBuffRegistry;
import org.mylove.tprt.registries.ModifierIds;
import org.mylove.tprt.utils.ModifierLEVEL;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.armor.ModifyDamageModifierHook;
import slimeknights.tconstruct.library.modifiers.impl.NoLevelsModifier;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.context.EquipmentContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import java.util.UUID;

public class double_or_none extends NoLevelsModifier implements ModifyDamageModifierHook {
    UUID uuid=UUID.fromString("95171EEA-3855-49D8-B44B-9F7B842FEB69");
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        super.registerHooks(hookBuilder);
        hookBuilder.addHook(this, ModifierHooks.MODIFY_DAMAGE);
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
                int X = ModifierLEVEL.getEachHandsTotalModifierlevel(entity, ModifierIds.double_or_none);
                if(X > 1){
                    if(ifdouble>=0.8){
                        MobEffectInstance healthgrow=new MobEffectInstance(MobEffects.REGENERATION,200,1);
                        entity.addEffect(healthgrow);
                        return amount*2;
                    }
                    else{
                        MobEffect effect = ModBuffRegistry.MOREDAMAGE.get();
                        MobEffectInstance instance = attacker.getEffect(effect);
                        if(instance==null){
                            attacker.addEffect(new MobEffectInstance(effect, 200));
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
