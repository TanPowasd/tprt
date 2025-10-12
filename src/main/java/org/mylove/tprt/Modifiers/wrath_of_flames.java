package org.mylove.tprt.Modifiers;

import com.ssakura49.sakuratinker.library.damagesource.LegacyDamageSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.registries.ForgeRegistries;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeHitModifierHook;
import slimeknights.tconstruct.library.modifiers.impl.NoLevelsModifier;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import com.github.L_Ender.cataclysm.init.ModEffect;

import java.util.UUID;

public class wrath_of_flames extends NoLevelsModifier implements MeleeHitModifierHook {
    UUID uuid=UUID.fromString("d2ab3741-d1ad-4e3e-1145-f37f1aac9cf1");
    public static MobEffect getEffect(){
        ResourceLocation effectId=new ResourceLocation("cataclysm","blazing_brand");
        return ForgeRegistries.MOB_EFFECTS.getValue(effectId);
    }
    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        super.registerHooks(hookBuilder);
        hookBuilder.addHook(this, ModifierHooks.MELEE_HIT);
    }
    @Override
    public int getPriority() {
        return 51;
    }
    @Override
    public void afterMeleeHit(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float damageDealt)
    {
        LivingEntity target = context.getLivingTarget();
        LivingEntity attacker = context.getAttacker();
        if (target != null && target.hasEffect(ModEffect.EFFECTBLAZING_BRAND.get()) && attacker instanceof Player player) {
            LegacyDamageSource source = LegacyDamageSource.playerAttack(player).setFire();
            target.invulnerableTime = 0;
            MobEffect effect = getEffect();
            MobEffectInstance instance = target.getEffect(effect);
            int bufnum=instance.getAmplifier()+1;
            System.out.println(damageDealt);
            target.hurt(source, damageDealt*bufnum*0.3f);
        }
    }
}
