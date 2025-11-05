package org.mylove.tprt.Modifiers;

import com.ssakura49.sakuratinker.generic.BaseModifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.NotNull;
import org.mylove.tprt.ModUsed.DeBug;
import org.mylove.tprt.ModUsed.ModF;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.context.EquipmentChangeContext;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.nbt.IToolContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;
import slimeknights.tconstruct.library.tools.nbt.ModifierNBT;
import slimeknights.tconstruct.library.tools.stat.ModifierStatsBuilder;

import java.util.*;

import static org.mylove.tprt.ModUsed.SpellMagic.Radiographic_detection_GetEntity;

public class zenith_first_fractal extends BaseModifier {
    UUID uuid=UUID.fromString("8E4FD47A-27EF-4135-9D4D-09CB51BFA4FF");
    public Queue<UUID> atcUUIDs = new LinkedList<>();
    static int ticks=0;
    LivingEntity entity_will_be_hit=null;
    LivingEntity mySelf=null;
    ModF.Pair3<Double, Double, Double> VerToEntity=new ModF.Pair3<>(0.0,0.0,0.0);//召唤物对于目标的相对坐标（x，z，y）
    public ModF.Pair3<Double, Double, Double> GetRandomVer(){
        ModF.Pair3<Double, Double, Double> Ver=new ModF.Pair3<>(0.0,0.0,0.0);
        double x=Math.random()*2-1;
        return Ver;
    }
    @Override
    public void afterMeleeHit(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float damageDealt) {

    }
    @Override
    public float getMeleeDamage(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float baseDamage, float damage) {
        return damage;
    }
    @Override
    public void modifierOnUnequip(IToolStackView tool, ModifierEntry modifier, EquipmentChangeContext context) {

    }
    @Override
    public void modifierOnEquip(IToolStackView tool, ModifierEntry modifier, EquipmentChangeContext context) {
        mySelf=context.getEntity();
    }
    @Override
    public void addToolStats(IToolContext context, ModifierEntry modifier, ModifierStatsBuilder builder) {

    }
    @Override
    public void modifierOnInventoryTick(IToolStackView tool, ModifierEntry modifier, net.minecraft.world.level.Level level, LivingEntity holder, int itemSlot, boolean isSelected, boolean isCorrectSlot, net.minecraft.world.item.ItemStack itemStack) {
        if(mySelf!=null){
            List<HitResult> REC=new ArrayList<>();
            REC =Radiographic_detection_GetEntity(mySelf,32);//监测视线前方32格范围内的实体
            if(REC!=null){//检测到了
                //将监测到的第一个实体赋给entity_will_be_hit
                HitResult kbhit=REC.get(0);//找到第一个实体
                if(kbhit instanceof EntityHitResult entityHitResult && entityHitResult.getEntity() instanceof LivingEntity livingEntity){
                    entity_will_be_hit=livingEntity;
                    //DeBug.Logger.log("entity_will_be_hit="+entity_will_be_hit.getStringUUID().toString());
                }
            }
        }//问题:修改之后的ewbh！=null后导致视线转移后打击实体还是原来的，需要每5tick重新监测，然后给一个包含量：5tick内移开实体视线的判定
    }
    @Override
    public void modifierProjectileTick(ModifierNBT modifiers, ModifierEntry entry, Level level, @NotNull Projectile projectile, ModDataNBT persistentData, boolean hasBeenShot, boolean leftOwner) {

    }
}

