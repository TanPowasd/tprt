package org.mylove.tprt.compat.Cataclysm.Modifiers;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.EntityHitResult;
import org.mylove.tprt.hooks.Arrowmodifier;
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;
import slimeknights.tconstruct.library.tools.nbt.ModifierNBT;

public class Soul_puncture extends Arrowmodifier {
    public boolean havenolevel() {
        return true;
    }
    @Override
    public void arrowhurt(ModifierNBT modifiers, ModDataNBT persistentData, int level, Projectile projectile, EntityHitResult hit, AbstractArrow arrow, LivingEntity attacker, LivingEntity target) {
        if (target != null) {
            arrow.setBaseDamage(arrow.getBaseDamage()*1.4);
            arrow.setPierceLevel((byte)(arrow.getPierceLevel()+level));
        }
    }

}
