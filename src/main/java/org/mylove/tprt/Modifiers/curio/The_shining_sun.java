package org.mylove.tprt.Modifiers.curio;

import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import org.mylove.tprt.common.modifier.modifierModule.CuriosModifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

public class The_shining_sun extends CuriosModifier {
    public The_shining_sun(){}

    public boolean isNoLevels() {
        return true;
    }

    public void onDamageTargetPre(IToolStackView curio, ModifierEntry entry, LivingHurtEvent event, LivingEntity attacker, LivingEntity target) {
        if (target != null) {
            if (!(attacker instanceof Player)) {
                return;
            }
            float x = 1.15F;
            if(attacker.hasEffect(MobEffects.GLOWING)){
                x = 1.45f;
            }
            float damage = event.getAmount();
            float newDamage = damage * x;
            event.setAmount(newDamage);
        }
    }
}
