package org.mylove.tprt.tags;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import org.mylove.tprt.registries.ModBuffRegistry;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeHitModifierHook;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import java.util.UUID;

public class d2_lucky_pants extends Modifier implements MeleeHitModifierHook {
    UUID uuid = UUID.fromString("A1B2C3D4-E5F6-7890-ABCD-EF1234567890");
    protected void registerHooks(slimeknights.tconstruct.library.module.ModuleHookMap.Builder hookBuilder) {
        super.registerHooks(hookBuilder);
        hookBuilder.addHook(this, slimeknights.tconstruct.library.modifiers.ModifierHooks.MELEE_HIT);
    }
    @Override
    public void afterMeleeHit(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float damageDealt) {
        LivingEntity entity= context.getLivingTarget();
        LivingEntity attacker = context.getAttacker();
        if (entity != null && attacker != null) {
            MobEffectInstance instance=attacker.getEffect(ModBuffRegistry.LLLEGALLY_MODIFIED_HOLSTERS.get());
            if(instance!=null){
                return;
            }
            double rd=Math.random();
            if(rd<=0.5f){
                attacker.addEffect(new MobEffectInstance(ModBuffRegistry.LLLEGALLY_MODIFIED_HOLSTERS.get(),100));
            }
        }
    }
}
