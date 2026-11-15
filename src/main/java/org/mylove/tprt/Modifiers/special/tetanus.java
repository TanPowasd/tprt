package org.mylove.tprt.Modifiers.special;

import com.ssakura49.sakuratinker.core.tools.modifiers.base.BaseModifier;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

public class tetanus extends BaseModifier {

    @Override
    public boolean isNoLevels() {
        return false;
    }

    @Override
    public float getMeleeDamage(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float baseDamage, float damage) {
        return damage;
    }

    @Override
    public void afterMeleeHit(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float damageDealt) {
        LivingEntity target = context.getLivingTarget();
        LivingEntity attacker = context.getAttacker();
        if (target != null && attacker != null && !attacker.level().isClientSide) {
            int duration = 200 * modifier.getLevel();
            int amp = Math.max(0, modifier.getLevel() - 1);
            target.addEffect(new MobEffectInstance(MobEffects.POISON, duration, amp));
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, duration, amp));
        }
    }
}
