package org.mylove.tprt.tags;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.registries.ForgeRegistries;
import slimeknights.tconstruct.library.modifiers.Modifier;
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

import static org.mylove.tprt.tags.double_or_none.SLOT_IN_CHARGE;

public class soul_dodge extends NoLevelsModifier implements ModifyDamageModifierHook {
    UUID uuid=UUID.fromString("95171EEA-3855-49D8-B44B-9F7B842AEB69");
    static ResourceLocation XSF_sd=new ResourceLocation("tprt","soul_dodge");
    public static final TinkerDataCapability.TinkerDataKey<SlotInChargeModule.SlotInCharge> SLOT_IN_CHARGE = TinkerDataCapability.TinkerDataKey.of(XSF_sd);
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        super.registerHooks(hookBuilder);
        hookBuilder.addHook(this, ModifierHooks.MODIFY_DAMAGE);
        hookBuilder.addModule(new SlotInChargeModule(SLOT_IN_CHARGE));
    }
    @Override
    public float modifyDamageTaken(IToolStackView iToolStackView, ModifierEntry modifierEntry, EquipmentContext context, EquipmentSlot equipmentSlot, DamageSource damageSource, float amount, boolean isDirectDamage){
        double ifdodge=Math.random();
        LivingEntity entity = context.getEntity();
        LivingEntity attacker = null;
        if(damageSource.getEntity() instanceof LivingEntity){
            attacker = (LivingEntity) damageSource.getEntity();
        }
        if (attacker != null && entity != null) {
            if(SlotInChargeModule.isInCharge(context.getTinkerData(), SLOT_IN_CHARGE, equipmentSlot)){
                if(ifdodge>=0.85){
                    return 0;
                }
                else{
                    return amount;
                }
            }
        }
        return amount;
    }
}
