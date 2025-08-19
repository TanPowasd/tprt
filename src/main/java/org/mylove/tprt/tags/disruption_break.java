package org.mylove.tprt.tags;

import net.minecraft.world.entity.LivingEntity;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeDamageModifierHook;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import java.util.UUID;

//瓦解破裂
public class disruption_break extends Modifier implements MeleeDamageModifierHook {
    UUID uuid= UUID.fromString("e9f8d7c6-b5a4-4e3d-8c2b-1a0f9e8d7c6b");
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        super.registerHooks(hookBuilder);
        hookBuilder.addHook(this, ModifierHooks.MELEE_DAMAGE);
    }
/** ToDo 重做词条 */
    @Override
    public float getMeleeDamage(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float baseDamage, float damage) {
        LivingEntity entity=context.getLivingTarget();
        LivingEntity attacker=context.getAttacker();
        if(entity != null && attacker != null) {
            float NewDamage=damage;
            int ArmorE=entity.getArmorValue();
            int ArmorA=attacker.getArmorValue();
            float healthE= entity.getHealth();
            float healthA= attacker.getHealth();
            float maxHealthE= entity.getMaxHealth();
            float maxHealthA= attacker.getMaxHealth();
            if(healthE/maxHealthE<=0.1f){
                //目标生命值不足10%直接斩杀
                //挚爱无法触发
                return Integer.MAX_VALUE;
            }
            else if(healthE<ArmorE){
                //目标生命值小于护甲值，斩杀
                return Integer.MAX_VALUE;
            }
            if (maxHealthE>maxHealthA&&healthE>healthA) {
                //目标最大生命值比己方大，打出伤害增加差值
                NewDamage+=(maxHealthE-maxHealthA)*modifier.getLevel();
            }
            else if(maxHealthE<healthA) {
                //目标最大生命值比己方小，斩杀
                //NewDamage*=Math.pow(1.2f,modifier.getLevel());
                return Integer.MAX_VALUE;
            }
             if(damage>ArmorE){
                NewDamage+=Math.max(5, (damage - ArmorE) * 0.5f)*modifier.getLevel();
            }
            if(ArmorA>ArmorE){
                //己方护甲值大于目标护甲值，打出压制
                NewDamage*=Math.pow(1.2f,modifier.getLevel());
            }
            else if(ArmorA<ArmorE){
                //己方护甲值小于目标护甲值,打出额外伤害
                NewDamage+=Math.max(5, (ArmorE - ArmorA) * 0.5f)*modifier.getLevel();
            }
            if(healthA>healthE||healthA>ArmorE){
                NewDamage+=Math.abs(healthE-ArmorE)*modifier.getLevel();
            }
            return NewDamage;
        }
        return damage;
    }
}
