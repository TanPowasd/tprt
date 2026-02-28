package org.mylove.tprt.compat.Cataclysm.Modifiers;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeDamageModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeHitModifierHook;
import slimeknights.tconstruct.library.modifiers.impl.NoLevelsModifier;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

public class Storm_incarnation_att extends NoLevelsModifier implements MeleeDamageModifierHook, MeleeHitModifierHook {

    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        super.registerHooks(hookBuilder);
        hookBuilder.addHook(this, ModifierHooks.MELEE_DAMAGE,ModifierHooks.MELEE_HIT);
    }

    @Override
    public float getMeleeDamage(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float baseDamage, float damage) {
        LivingEntity entity= context.getLivingTarget();
        LivingEntity attacker = context.getPlayerAttacker();
        if (attacker != null && entity != null){
            if (entity.isInWaterOrRain()){
                return (float) (baseDamage*1.4);
            }else {
                return (float) (baseDamage*1.15);
            }
        }
        return baseDamage;
    }

    @Override
    public void afterMeleeHit(IToolStackView tool, @NotNull ModifierEntry modifier, @NotNull ToolAttackContext context, float damageDealt) {
        Player attacker = context.getPlayerAttacker();
        LivingEntity target = context.getLivingTarget();
        if (attacker != null && target != null){
            if (attacker.isInWaterOrRain()) {
                target.invulnerableTime = 0;
                target.hurt(target.damageSources().lightningBolt(),damageDealt * 0.20f);
            }
        }
    }
}
