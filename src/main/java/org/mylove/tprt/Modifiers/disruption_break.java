package org.mylove.tprt.Modifiers;

import com.ssakura49.sakuratinker.generic.BaseModifier;
import com.ssakura49.sakuratinker.library.damagesource.LegacyDamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import org.mylove.tprt.ModUsed.ModF;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.UUID;

//瓦解破裂
public class disruption_break extends BaseModifier {
    UUID uuid= UUID.fromString("e9f8d7c6-b5a4-4e3d-8c2b-1a0f9e8d7c6b");
    //攻击后记录实体，之后每刻进行攻击
    static Queue<ModF.Pair3> entityQueue = new LinkedList<>();
    @Override
    public boolean isNoLevels() {
        return true;
    }
    @Override
    public void afterMeleeHit(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float damageDealt) {
        LivingEntity attacker = context.getAttacker();
        LivingEntity target = context.getLivingTarget();
        if(attacker!= null && target!= null){
            ModF.Pair3<LivingEntity, LivingEntity,Integer> pair= new ModF.Pair3<>(attacker, target,30);
            if(!entityQueue.contains(pair)){
                entityQueue.add(pair);
            }
        }
    }
    @Override
    public void modifierOnInventoryTick(IToolStackView tool, ModifierEntry modifier, Level level, LivingEntity holder, int itemSlot, boolean isSelected, boolean isCorrectSlot, ItemStack itemStack) {
        if(!entityQueue.isEmpty()){
            //对个体的侵蚀
            ModF.Pair3<LivingEntity, LivingEntity, Integer> pairTop= entityQueue.peek();
            if(pairTop.third>0&&pairTop.first.isAlive()&&pairTop.second.isAlive()){
                LegacyDamageSource source = LegacyDamageSource.playerAttack((Player) pairTop.first).setFire();
                if(pairTop.second.getHealth()-100>0){
                    pairTop.second.setHealth(pairTop.second.getHealth()-100);
                }
                else {
                    pairTop.second.hurt(source, 100);
                    pairTop.second.invulnerableTime=0;//去除无敌帧
                }
                pairTop.third--;
                entityQueue.add(new ModF.Pair3<>(pairTop.first, pairTop.second, pairTop.third));
                //扩散破坏
                if(pairTop.third%5==0){
                    Level world=pairTop.second.level();
                    //对周围进行实体查找，若半径5格内有不在队列的实体，则加入队列
                    //创建一个实体包围盒，然后对包围盒内的实体查找
                    //若有实体，将其加入队列
                    AABB range= new AABB(pairTop.second.getX()-5,pairTop.second.getY()-5,pairTop.second.getZ()-5,pairTop.second.getX()+5,pairTop.second.getY()+5,pairTop.second.getZ()+5);
                    List<HitResult> pHit=new LinkedList<>();
                    List<? extends Entity> entityList= level.getEntities(pairTop.second, range, entity -> entity.isPickable() && entity.isAlive());
                    for(Entity entity:entityList){
                        //获取列表的每一个实体
                        Queue<LivingEntity> qtp=new LinkedList<>();
                        for(ModF.Pair3 getP:entityQueue){
                            qtp.add((LivingEntity) getP.second);
                        }
                        if(!qtp.contains(entity)&&entity!=pairTop.first){
                            entityQueue.add(new ModF.Pair3<>(pairTop.first, (LivingEntity) entity, 30));
                        }
                    }
                }
            }
        }
    }
    @Override
    public float getMeleeDamage(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float baseDamage, float damage) {
        LivingEntity attacker = context.getAttacker();
        LivingEntity target = context.getLivingTarget();
        if(attacker!= null && target != null){
            int MUCH=entityQueue.size();
            if(MUCH>0){
                return (damage*(1+0.2f*MUCH));
            }
        }
        return damage;
    }
}