package org.mylove.tprt.Modifiers.curio;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mylove.tprt.common.modifier.modifierModule.CuriosModifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;
import slimeknights.tconstruct.library.tools.nbt.ModifierNBT;

public class Strengthen_for_charm_ranged extends CuriosModifier {

    public Strengthen_for_charm_ranged(){}

    @Override
    public float getProjectileDamage(ModDataNBT persistentData, ModifierEntry entry, ModifierNBT modifiers, @NotNull Projectile projectile, @Nullable AbstractArrow arrow, @Nullable LivingEntity attacker, @NotNull Entity target, float baseDamage, float damage) {
        if(attacker != null){
        int x = entry.getLevel();
        return (float) (baseDamage * 1.04 * x);
        }
        return baseDamage;
    }
}
