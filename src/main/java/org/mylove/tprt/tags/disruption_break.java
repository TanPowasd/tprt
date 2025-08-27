package org.mylove.tprt.tags;

import net.minecraft.world.entity.LivingEntity;
import org.mylove.tprt.ModUsed.DeBug;
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
            DeBug.Logger.log("目标护甲值:"+ArmorE);
            //DeBug.Logger.log("攻击者护甲值:"+ArmorA);
            DeBug.Logger.log("目标生命值:"+healthE);
            //DeBug.Logger.log("攻击者生命值:"+healthA);
            DeBug.Logger.log("目标最大生命值:"+maxHealthE);
            //DeBug.Logger.log("攻击者最大生命值:"+maxHealthA);
            DeBug.Logger.log("输出"+(damage+0.5f*maxHealthE));
            return damage+0.5f*maxHealthE;


        }
        return damage;
    }
}
