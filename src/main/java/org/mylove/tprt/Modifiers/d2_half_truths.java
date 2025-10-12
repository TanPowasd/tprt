package org.mylove.tprt.Modifiers;

import com.ssakura49.sakuratinker.generic.BaseModifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.mylove.tprt.registries.ModBuffRegistry;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.context.EquipmentChangeContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import java.util.UUID;

public class d2_half_truths extends BaseModifier {
    UUID uuid = UUID.fromString("d2ab3741-d1ad-4e3e-1145-f37f1aac9cf1");
    private Vec3 inertia = Vec3.ZERO;
    @Override
    public void modifierOnEquip(IToolStackView tool, ModifierEntry modifier, EquipmentChangeContext context) {
        LivingEntity self = context.getEntity();
        MobEffect effect_ee= ModBuffRegistry.EAGER_EDGE.get();
        MobEffect effect_de= ModBuffRegistry.DE_EAGER_EDGE.get();
        MobEffectInstance instance_ee = self.getEffect(effect_ee);
        MobEffectInstance instance_de = self.getEffect(effect_de);
        if(instance_ee==null && instance_de==null && self!=null){
            self.addEffect(new MobEffectInstance(effect_ee,60,0));
        }
    }
    @Override
    public void onLeftClickEmpty(IToolStackView tool, ModifierEntry entry, Player player, Level level, EquipmentSlot equipmentSlot) {
        MobEffect effect_ee= ModBuffRegistry.EAGER_EDGE.get();
        MobEffect effect_de= ModBuffRegistry.DE_EAGER_EDGE.get();
        MobEffectInstance instance_ee = player.getEffect(effect_ee);
        MobEffectInstance instance_de = player.getEffect(effect_de);
        if(instance_ee==null||instance_de!=null){
            return;
        }
        else{
            LivingEntity self = player;
            if(self!=null){
                self.removeEffect(effect_ee);
                self.addEffect(new MobEffectInstance(effect_de,100,0));
                double getx= self.getX(),
                        gety= self.getY(),
                        getz= self.getZ();
                self.setDeltaMovement(self.getLookAngle().scale(15));

            }
        }
    }
}
