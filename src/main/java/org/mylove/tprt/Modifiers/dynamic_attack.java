package org.mylove.tprt.Modifiers;

import com.ssakura49.sakuratinker.generic.BaseModifier;
import net.minecraft.world.entity.LivingEntity;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.stat.ToolStats;

import java.util.UUID;

public class dynamic_attack extends BaseModifier {
    //还没注册
    //动态攻击
    UUID uuid = UUID.fromString("d2ab3741-d1ad-ccdd-1145-f37f1aac9cf1");
    public int cooldown=0;//动态攻击冷却时间:刻
    @Override
    public float getMeleeDamage(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float baseDamage, float damage) {
        LivingEntity entity=context.getLivingTarget();
        LivingEntity attacker=context.getAttacker();
        if(entity!=null && attacker!=null){
            //cooldown=(int)(1.5f*(20/tool.getStats().get(ToolStats.ATTACK_SPEED)));
            if(cooldown==0){
                cooldown=(int)(1.5f*(20/tool.getStats().get(ToolStats.ATTACK_SPEED)));
                return damage*2.5f;
            }
            else if(cooldown>0&&cooldown<(int)((20/tool.getStats().get(ToolStats.ATTACK_SPEED)))){
                float mid=((20/tool.getStats().get(ToolStats.ATTACK_SPEED)))/2;
                float ht=damage*(1-1/(cooldown-mid));
                cooldown=(int)(1.5f*(20/tool.getStats().get(ToolStats.ATTACK_SPEED)));
                return ht;
            }
            else{
                return damage/cooldown;
            }
        }
        return 0;
    }
    @Override
    public void modifierOnInventoryTick(IToolStackView tool, ModifierEntry modifier, net.minecraft.world.level.Level level, LivingEntity holder, int itemSlot, boolean isSelected, boolean isCorrectSlot, net.minecraft.world.item.ItemStack itemStack) {
        if(cooldown>0){
            cooldown--;
        }
    }
}
