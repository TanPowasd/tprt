package org.mylove.tprt.tags;

import com.ssakura49.sakuratinker.library.damagesource.LegacyDamageSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.registries.ForgeRegistries;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeDamageModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeHitModifierHook;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import java.util.UUID;

public class wrath_of_flames extends Modifier implements MeleeHitModifierHook {
    UUID uuid=UUID.fromString("d2ab3741-d1ad-4e3e-1145-f37f1aac9cf1");
    public static MobEffect getEffect(){
        ResourceLocation effectId=new ResourceLocation("cataclysm","blazing_brand");
        return ForgeRegistries.MOB_EFFECTS.getValue(effectId);
    }
    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        //hookBuilder.addHook(this);
        super.registerHooks(hookBuilder);
        hookBuilder.addHook(this,ModifierHooks.MELEE_HIT);
    }
    //尽可能下降优先级来保证增伤害吃满
    public int getPriority() {
        return 50;
    }
    @Override
    public void afterMeleeHit(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float damageDealt)
    {
        LivingEntity target = context.getLivingTarget();
        LivingEntity attacker = context.getAttacker();
        MobEffect effect=getEffect();
        MobEffectInstance instance=target.getEffect(effect);
        if(target != null){
            if(instance!=null){
                 LegacyDamageSource source =LegacyDamageSource.playerAttack(context.getPlayerAttacker()).setFire();
                 target.invulnerableTime=0;
                 target.hurt(source,damageDealt*(instance.getAmplifier()+1)*0.3f);
            }
        }
    }
}
