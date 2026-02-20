package org.mylove.tprt.compat.Cataclysm.Modifiers;

import com.ssakura49.sakuratinker.generic.BaseModifier;
import net.minecraft.world.entity.LivingEntity;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import java.util.UUID;

public class mechanical_star_att extends BaseModifier {
        UUID uuid = UUID.fromString("509C5AA1-0436-48BD-852B-E1CB1016EB26");

    @Override
    public boolean isNoLevels() {
        return true;
    }
    @Override
    public float getMeleeDamage(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float baseDamage, float damage) {
        LivingEntity entity = context.getLivingTarget();
        LivingEntity attacker = context.getAttacker();
        if (entity != null) {
            float x=attacker.getArmorValue();
            double y=x/(2*x+100);
            return (float) (damage*(1+2*y));
        }
        return damage;
    }
}
