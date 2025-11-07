package org.mylove.tprt.Modifiers;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.registries.ForgeRegistries;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeDamageModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeHitModifierHook;
import slimeknights.tconstruct.library.modifiers.impl.NoLevelsModifier;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

public class human_turpentine extends NoLevelsModifier implements MeleeHitModifierHook, MeleeDamageModifierHook {
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        super.registerHooks(hookBuilder);
        hookBuilder.addHook(this, ModifierHooks.MELEE_DAMAGE);
        hookBuilder.addHook(this, ModifierHooks.MELEE_HIT);
    }

    public static MobEffect getEffect(){
        ResourceLocation effectId=new ResourceLocation("irons_spellbooks","echoing_strikes");
        return ForgeRegistries.MOB_EFFECTS.getValue(effectId);
    }

    @Override
    public float getMeleeDamage(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float baseDamage, float damage) {
        return 1.25f*damage;
    }

    @Override
    public void afterMeleeHit(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float damageDealt){
        if (!(context.getTarget() instanceof LivingEntity target)) return;
        LivingEntity entity= context.getLivingTarget();
        LivingEntity attacker=context.getAttacker();
        if (entity!=null && attacker!=null) {
            MobEffect effect = getEffect();
            MobEffectInstance instance = target.getEffect(effect);
            if (instance == null) {
                attacker.addEffect(new MobEffectInstance(effect, 40, 0));
            }
        }
    }
}
