package org.mylove.tprt.compat.Cloudertinker.Modifiers.Curios;

import com.ssakura49.sakuratinker.generic.CurioModifier;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

public class Body_like_glass extends CurioModifier {
    public boolean isNoLevels() {
        return true;
    }

    public Body_like_glass(){}

    public void onDamageTargetPre(IToolStackView curio, ModifierEntry entry, LivingHurtEvent event, LivingEntity attacker, LivingEntity target) {
        if (target != null) {
            if (!(attacker instanceof Player)) {
                return;
            }
            float x = 1.35f;
            float damage = event.getAmount();
            float newDamage = damage * x;
            event.setAmount(newDamage);
        }
    }

    public void onCurioTakeDamagePre(IToolStackView curio, ModifierEntry entry, LivingHurtEvent event, LivingEntity entity, DamageSource source) {
        if (entity != null) {
            if (!(entity instanceof Player)) {
                return;
            }
            float newAmount = event.getAmount() * 2.0f;
            event.setAmount(newAmount);
        }
    }
}

