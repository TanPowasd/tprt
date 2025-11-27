package org.mylove.tprt.Modifiers.curio;

import com.ssakura49.sakuratinker.generic.CurioModifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

public class Down_to_the_Bone extends CurioModifier {

    public Down_to_the_Bone(){}

    public void onDamageTargetPre(IToolStackView curio, ModifierEntry entry, LivingHurtEvent event, LivingEntity attacker, LivingEntity target) {
        if (target != null) {
            if (!(attacker instanceof Player)) {
                return;
            }
            float damage = event.getAmount();
            float newDamage = damage + ( 2 * curio.getModifierLevel(this) );
            event.setAmount(newDamage);
        }
    }
}
