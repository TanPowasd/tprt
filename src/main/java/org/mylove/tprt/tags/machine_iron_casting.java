package org.mylove.tprt.tags;

import com.ssakura49.sakuratinker.generic.BaseModifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.context.EquipmentChangeContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

public class machine_iron_casting extends BaseModifier {
    @Override
    public boolean isNoLevels() {
        return false;
    }
    float speed=1.0f;
    float armor=1.0f;
    @Override
    public void onEquip(IToolStackView tool, ModifierEntry modifier, EquipmentChangeContext context) {
        this.modifierOnEquip(tool, modifier, context);
        LivingEntity self = context.getEntity();
        if (self != null) {
            speed=self.getSpeed();
            armor=self.getArmorValue();
            self.setSpeed(speed*(1-0.15f*modifier.getLevel()));
            AttributeInstance armorAttr = self.getAttribute(Attributes.ARMOR);
            if (armorAttr != null) {
                double newArmor = (armorAttr.getBaseValue() * (1 + 0.1f * modifier.getLevel()));
                armorAttr.setBaseValue(newArmor);
            }
        }
    }
    @Override
    public void onUnequip(IToolStackView tool, ModifierEntry modifier, EquipmentChangeContext context) {
        this.modifierOnUnequip(tool, modifier, context);
        LivingEntity self = context.getEntity();
        if (self != null) {
            self.setSpeed(speed);
            AttributeInstance armorAttr = self.getAttribute(Attributes.ARMOR);
            if (armorAttr != null) {
                armorAttr.setBaseValue(armor);
            }
        }
    }
}
