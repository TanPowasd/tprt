package org.mylove.tprt.tags;

import com.ssakura49.sakuratinker.generic.BaseModifier;
import net.minecraft.world.entity.LivingEntity;
import org.mylove.tprt.ModUsed.ModF;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import java.util.ArrayDeque;
import java.util.LinkedList;
import java.util.Queue;

public class tetanus extends BaseModifier {
    Queue<LivingEntity> StickyEntities=new ArrayDeque<>();
    Queue<ModF.Pair<LivingEntity,Integer> > StrickF=new ArrayDeque<>();
    @Override
    public boolean isNoLevels() {
        return false;
    }
    @Override
    public float getMeleeDamage(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float baseDamage, float damage) {
        //return !context.getAttacker().level().isClientSide() && context.getLivingTarget() != null ? this.onModifyMeleeDamage(tool, modifier, context, context.getAttacker(), context.getLivingTarget(), baseDamage, damage) : damage;
        LivingEntity target = context.getLivingTarget();
        LivingEntity attacker = context.getAttacker();
        if(target!=null&&attacker!=null){
            //如果目标不在队列，则加入队列
            if(!StickyEntities.contains(target)){
                StickyEntities.add(target);
                StrickF.add(new ModF.Pair<>(target,200));
            }
        }

        return 0;
    }
}
