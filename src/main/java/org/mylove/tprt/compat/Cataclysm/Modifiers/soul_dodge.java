package org.mylove.tprt.compat.Cataclysm.Modifiers;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import org.mylove.tprt.registries.ModifierIds;
import org.mylove.tprt.utils.ModifierLEVEL;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.armor.ModifyDamageModifierHook;
import slimeknights.tconstruct.library.modifiers.modules.technical.SlotInChargeModule;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.capability.TinkerDataCapability;
import slimeknights.tconstruct.library.tools.context.EquipmentContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import java.util.UUID;

public class soul_dodge extends Modifier implements ModifyDamageModifierHook {
    UUID uuid=UUID.fromString("95171EEA-3855-49D8-B44B-9F7B842AEB69");
    public static final TinkerDataCapability.TinkerDataKey<SlotInChargeModule.SlotInCharge> SLOT_IN_CHARGE = TinkerDataCapability.TinkerDataKey.of(ModifierIds.soul_dodge);

    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        super.registerHooks(hookBuilder);
        hookBuilder.addHook(this, ModifierHooks.MODIFY_DAMAGE);
        hookBuilder.addModule(new SlotInChargeModule(SLOT_IN_CHARGE));
    }

    @Override
    public float modifyDamageTaken(IToolStackView iToolStackView, ModifierEntry modifierEntry, EquipmentContext context, EquipmentSlot equipmentSlot, DamageSource damageSource, float amount, boolean isDirectDamage){
        if (!SlotInChargeModule.isInCharge(context.getTinkerData(), SLOT_IN_CHARGE, equipmentSlot)) {
            return amount;
        }
        double ifdodge=Math.random();
        double A = 1;
        LivingEntity entity = context.getEntity();
        LivingEntity attacker = null;
        if(damageSource.getEntity() instanceof LivingEntity){
            attacker = (LivingEntity) damageSource.getEntity();
        }
        if (attacker != null) {
            int X = ModifierLEVEL.getTotalArmorModifierlevel(entity, ModifierIds.soul_dodge);
            if(X > 0){
                if (X >= 4){
                    if(ifdodge>=0.50f){
                        A = 0;
                    }
                }else {
                    if(ifdodge>=(0.9 - 0.1 * X)){
                        A = 0;
                    }
                }
            }
        }
        return (float) (amount * A);
    }
}
