package org.mylove.tprt.tags;

import com.ssakura49.sakuratinker.generic.BaseModifier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.modules.technical.SlotInChargeModule;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.capability.TinkerDataCapability;
import slimeknights.tconstruct.library.tools.context.EquipmentChangeContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import com.ssakura49.sakuratinker.utils.tinker.ToolUtil;

import static com.ssakura49.sakuratinker.utils.tinker.ToolUtil.getSingleModifierArmorAllLevel;

public class machine_iron_casting extends BaseModifier {
    @Override
    public boolean isNoLevels() {
        return false;
    }
    static ResourceLocation XSF_sd=new ResourceLocation("tprt","soul_dodge");
    public static final TinkerDataCapability.TinkerDataKey<SlotInChargeModule.SlotInCharge> SLOT_IN_CHARGE = TinkerDataCapability.TinkerDataKey.of(XSF_sd);
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        super.registerHooks(hookBuilder);
        hookBuilder.addModule(new SlotInChargeModule(SLOT_IN_CHARGE));
    }
    static float speed=1.0f;
    static float armor=1.0f;
    static int std=0;
    @Override
    public void onEquip(IToolStackView tool, ModifierEntry modifier, EquipmentChangeContext context) {
        //this.modifierOnEquip(tool, modifier, context);
        LivingEntity self = context.getEntity();
        if (self != null) {
            if(SlotInChargeModule.isInCharge(context.getTinkerData(), SLOT_IN_CHARGE, context.getChangedSlot())){
            AttributeInstance armorAttr = self.getAttribute(Attributes.ARMOR);
            AttributeInstance speedAttr= self.getAttribute(Attributes.MOVEMENT_SPEED);
            std=getSingleModifierArmorAllLevel(self, modifier.getModifier());
            if(speedAttr!=null){
                speed=(float) speedAttr.getValue();
                double newSpeed = (speedAttr.getValue() * (1 -0.15f * std));
                speedAttr.setBaseValue(newSpeed);
            }
            if (armorAttr != null) {
                armor = (float) armorAttr.getValue();
                double newArmor = (armorAttr.getValue() * (1 + 0.1f * std));
                armorAttr.setBaseValue(newArmor);
            }
            }
        }
    }
    @Override
    public void onUnequip(IToolStackView tool, ModifierEntry modifier, EquipmentChangeContext context) {
        //this.modifierOnUnequip(tool, modifier, context);
        LivingEntity self = context.getEntity();
        if (self != null) {
            if(SlotInChargeModule.isInCharge(context.getTinkerData(), SLOT_IN_CHARGE, context.getChangedSlot())){
                AttributeInstance armorAttr = self.getAttribute(Attributes.ARMOR);
                AttributeInstance speedAttr= self.getAttribute(Attributes.MOVEMENT_SPEED);
                if(speedAttr!=null){
                    speedAttr.setBaseValue(0.1f);
                }
                if (armorAttr != null) {
                    armorAttr.setBaseValue(0);
                }
            }
        }
    }
}
