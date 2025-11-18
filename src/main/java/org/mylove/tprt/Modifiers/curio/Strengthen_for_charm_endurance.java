package org.mylove.tprt.Modifiers.curio;

import com.ssakura49.sakuratinker.generic.CurioModifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

public class Strengthen_for_charm_endurance extends CurioModifier {

    public Strengthen_for_charm_endurance(){}

    public void onCurioTakeHeal(IToolStackView curio, ModifierEntry entry, LivingHealEvent event, LivingEntity entity) {
        if (entity != null) {
            if (entity instanceof Player) {
                float bonus = (float) (event.getAmount() *( 1 + 0.04 * (curio.getModifierLevel(this))));
                event.setAmount(bonus);
            }
        }
    }
}
