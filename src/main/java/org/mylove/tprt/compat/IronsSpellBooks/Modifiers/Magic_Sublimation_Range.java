package org.mylove.tprt.compat.IronsSpellBooks.Modifiers;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.EntityHitResult;
import org.mylove.tprt.hooks.Arrowmodifier;
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;
import slimeknights.tconstruct.library.tools.nbt.ModifierNBT;

public class Magic_Sublimation_Range extends Arrowmodifier {

    public boolean havenolevel() {
        return true;
    }

    @Override
    public void arrowhurt(ModifierNBT modifiers, ModDataNBT persistentData, int level, Projectile projectile, EntityHitResult hit, AbstractArrow arrow, LivingEntity attacker, LivingEntity target) {
        AttributeInstance X = null;
        if (attacker != null) {
            X = attacker.getAttribute(AttributeRegistry.SPELL_POWER.get());
        }
        if(attacker!=null && target != null){
            if (X!= null){X.getValue();}
            if (X != null) {
                float y = (float) Math.sqrt(X.getValue());
                arrow.setBaseDamage(arrow.getBaseDamage() * y);
                arrow.setPierceLevel((byte)(arrow.getPierceLevel()+level));
            }
        }
    }
}
