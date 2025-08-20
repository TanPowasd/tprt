package org.mylove.tprt.tags;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.registries.ForgeRegistries;
import org.mylove.tprt.registries.ModBuffRegistry;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeDamageModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeHitModifierHook;
import slimeknights.tconstruct.library.modifiers.impl.NoLevelsModifier;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import java.util.UUID;

public class advanced_greed extends NoLevelsModifier implements MeleeDamageModifierHook, MeleeHitModifierHook {
    UUID uuid = UUID.fromString("e2b7c1a4-8f3d-4c2a-9b6e-1d7f8a9c3b2a");
    protected void HookRegister(ModuleHookMap.Builder hookBuilder) {
        super.registerHooks(hookBuilder);
        hookBuilder.addHook(this, ModifierHooks.MELEE_DAMAGE);
        hookBuilder.addHook(this, ModifierHooks.MELEE_HIT);
    }
    public static MobEffect getEffect(){
        ResourceLocation effectId=new ResourceLocation("tprt","myl_girig_pro");
        return ForgeRegistries.MOB_EFFECTS.getValue(effectId);
    }
    @Override
    public float getMeleeDamage(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float baseDamage, float damage) {
        LivingEntity entity= context.getLivingTarget();
        LivingEntity attacker=context.getAttacker();
        float dl=1.0f;
        if(entity != null && attacker != null) {

            MobEffect effect= getEffect();
            MobEffectInstance instance = attacker.getEffect(effect);
            if(instance==null){
                //attacker.addEffect(new MobEffectInstance(effect,200,0));
                dl=1.15f;
            }
            else {
               int Amplifier = instance.getAmplifier();
               int NewAmplifier = Amplifier+1;
               if(NewAmplifier==10){
                   NewAmplifier=9;
               }
              // attacker.addEffect(new MobEffectInstance(effect,200,NewAmplifier));
               dl=1.0f+0.15f*NewAmplifier;
            }
            return damage*dl;
        }
        return damage*dl;
    }
    public void afterMeleeHit(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float damageDealt){

    }
}
