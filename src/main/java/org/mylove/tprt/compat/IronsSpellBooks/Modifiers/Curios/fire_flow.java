package org.mylove.tprt.compat.IronsSpellBooks.Modifiers.Curios;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import org.mylove.tprt.common.modifier.modifierModule.CuriosModifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

public class fire_flow extends CuriosModifier {

    public boolean isNoLevels() {
        return true;
    }

    public void onDamageTargetPre(IToolStackView curio, ModifierEntry entry, LivingHurtEvent event, LivingEntity attacker, LivingEntity target) {
        if (target != null) {
            if (!(attacker instanceof Player player)) {
                return;
            }
            AttributeInstance x = player.getAttribute(AttributeRegistry.FIRE_SPELL_POWER.get());
            float damage = event.getAmount();
            float newDamage = 0;
            if (x != null) {
                int X = (int) Math.sqrt(x.getValue() + 3);
                int Y = (int) Math.sqrt(X);
                newDamage = (damage + X) * Y;
            }
            event.setAmount(newDamage);
        }
    }

    public void onCurioTakeDamagePre(IToolStackView curio, ModifierEntry entry, LivingHurtEvent event, LivingEntity entity, DamageSource source) {
        if (entity != null) {
            if (!(entity instanceof Player)) {
                return;
            }
            float newAmount = event.getAmount() - 2f ;
            event.setAmount(newAmount);
        }
    }
}
