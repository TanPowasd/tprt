package org.mylove.tprt.Modifiers;

import com.ssakura49.sakuratinker.generic.BaseModifier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.modules.technical.SlotInChargeModule;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.capability.TinkerDataCapability;
import slimeknights.tconstruct.library.tools.context.EquipmentContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import java.util.UUID;

public class mechanical_star_arm extends BaseModifier {
    UUID uuid = UUID.fromString("9ACB057A-9A85-4C60-B54B-D521AF85CE08");

    static ResourceLocation XSF_don=new ResourceLocation("tprt","mechanical_star_arm");
    public static final TinkerDataCapability.TinkerDataKey<SlotInChargeModule.SlotInCharge> SLOT_IN_CHARGE = TinkerDataCapability.TinkerDataKey.of(XSF_don);
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        super.registerHooks(hookBuilder);
        hookBuilder.addModule(new SlotInChargeModule(SLOT_IN_CHARGE));
    }
    @Override
    public boolean isNoLevels() {
        return true;
    }
    @Override
    public float modifyDamageTaken(IToolStackView tool, ModifierEntry modifier, EquipmentContext context, EquipmentSlot slotType, DamageSource source, float amount, boolean isDirectDamage) {
        LivingEntity entity =context.getEntity();
        LivingEntity attacker =null;
        if(source.getEntity() instanceof LivingEntity){
            attacker = (LivingEntity) source.getEntity();
        }
        if(attacker != null && entity != null) {
            if(SlotInChargeModule.isInCharge(context.getTinkerData(), SLOT_IN_CHARGE, slotType)){
            float x=entity.getArmorValue();
            double y=x/(2*x+100);
            return (float) (amount*(1-y));
            }
        }
        return amount;
    }
}
