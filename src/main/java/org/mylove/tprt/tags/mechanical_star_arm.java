package org.mylove.tprt.tags;

import com.ssakura49.sakuratinker.generic.BaseModifier;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.context.EquipmentContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import java.util.UUID;

public class mechanical_star_arm extends BaseModifier {
    UUID uuid = UUID.fromString("9ACB057A-9A85-4C60-B54B-D521AF85CE08");
    @Override
    public boolean isNoLevels() {
        return true;
    }
    public float modifyDamageTaken(IToolStackView tool, ModifierEntry modifier, EquipmentContext context, EquipmentSlot slotType, DamageSource source, float amount, boolean isDirectDamage) {
        LivingEntity entity =context.getEntity();
        LivingEntity attacker =null;
        if(source.getEntity() instanceof LivingEntity){
            attacker = (LivingEntity) source.getEntity();
        }
        if(attacker != null && entity != null) {
            float ArmorE=entity.getArmorValue();
            return amount*(1-ArmorE*0.003f);
        }
        return amount;
    }
}
