package org.mylove.tprt.Modifiers;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;
import org.mylove.tprt.registries.ModifierIds;
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

public class Arrogant extends Modifier implements ModifyDamageModifierHook {

    UUID uuid = UUID.fromString("9ACB057A-9A85-4C60-B54B-D491A01AF91");

    public static final TinkerDataCapability.TinkerDataKey<SlotInChargeModule.SlotInCharge> SLOT_IN_CHARGE = TinkerDataCapability.TinkerDataKey.of(ModifierIds.Arrogant);

    protected void registerHooks(ModuleHookMap.@NotNull Builder hookBuilder) {
        super.registerHooks(hookBuilder);
        hookBuilder.addHook(this, ModifierHooks.MODIFY_DAMAGE);
        hookBuilder.addModule(new SlotInChargeModule(SLOT_IN_CHARGE));
    }

    @Override
    public float modifyDamageTaken(@NotNull IToolStackView tool, @NotNull ModifierEntry modifier, EquipmentContext context, @NotNull EquipmentSlot slotType, DamageSource source, float amount, boolean isDirectDamage) {
        LivingEntity entity =context.getEntity();
        LivingEntity attacker =null;
        if(source.getEntity() instanceof LivingEntity){
            attacker = (LivingEntity) source.getEntity();
        }
        if(attacker != null) {
            if(SlotInChargeModule.isInCharge(context.getTinkerData(), SLOT_IN_CHARGE, slotType)){
                int level = SlotInChargeModule.getLevel(context.getTinkerData(), SLOT_IN_CHARGE, slotType);
                if (entity.getHealth() == entity.getMaxHealth()){
                    if (level >= 8){
                        return (float) (amount*0.6);
                    }
                    else {
                        return (float) (amount*(1-0.05*level));
                    }
                }
                if (entity.getHealth() / entity.getMaxHealth() >= 0.5){
                    if (level >= 8){
                        return (float) (amount*0.76);
                    }
                    else {
                        return (float) (amount*(1-0.03*level));
                    }
                }
            }
        }
        return amount;
    }
}
