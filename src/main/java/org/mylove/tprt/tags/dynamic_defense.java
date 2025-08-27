package org.mylove.tprt.tags;

import com.ssakura49.sakuratinker.generic.BaseModifier;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.context.EquipmentContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

public class dynamic_defense extends BaseModifier {
    //还没注册
    //动态防守
    private int cooldown=15;//动态防守冷却时间:刻
    public int lastDamageTime=0;//上次受到伤害的时间
    @Override
    public float modifyDamageTaken(IToolStackView iToolStackView, ModifierEntry modifierEntry, EquipmentContext context, EquipmentSlot equipmentSlot, DamageSource damageSource, float amount, boolean isDirectDamage){
        LivingEntity entity =context.getEntity();
        LivingEntity attacker =null;//self
        if(damageSource.getEntity() instanceof LivingEntity){
            attacker = (LivingEntity) damageSource.getEntity();
        }
        if(attacker != null && entity != null) {
            if(cooldown==0){
                cooldown=15;
                return amount;
            }
            else {
                float ht=amount/cooldown;
                cooldown=15;
                return ht;
            }
        }
        return amount;
    }
    public void modifierOnInventoryTick(IToolStackView tool, ModifierEntry modifier, Level level, LivingEntity holder, int itemSlot, boolean isSelected, boolean isCorrectSlot, ItemStack itemStack) {
        if(cooldown>0){
            cooldown--;
        }
    }
}
