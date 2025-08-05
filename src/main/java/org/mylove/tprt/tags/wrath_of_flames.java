package org.mylove.tprt.tags;

import com.ssakura49.sakuratinker.library.damagesource.LegacyDamageSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.registries.ForgeRegistries;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeDamageModifierHook;
import slimeknights.tconstruct.library.modifiers.impl.NoLevelsModifier;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import java.util.Random;
import java.util.UUID;

public class wrath_of_flames extends Modifier implements MeleeDamageModifierHook {
    UUID uuid=UUID.fromString("d2a24321-daee-4e3e-afa2-f37f1aa11451");
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        super.registerHooks(hookBuilder);
        hookBuilder.addHook(this, ModifierHooks.MELEE_DAMAGE);
    }
    public static MobEffect getEffect(){
        ResourceLocation effectId=new ResourceLocation("cataclysm","blazing_brand");
        return ForgeRegistries.MOB_EFFECTS.getValue(effectId);
    }
    public float getMeleeDamage(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float baseDamage, float damage) {
        LivingEntity entity = context.getLivingTarget();
        // LegacyDamageSource source =LegacyDamageSource.playerAttack(context.getPlayerAttacker()).setFire();
        // entity.hurt(source,1);
        if(entity!=null){
            MobEffect effect=getEffect();
            MobEffectInstance instance=entity.getEffect(effect);
            int effectNum=instance.getAmplifier();
            LegacyDamageSource source = LegacyDamageSource.playerAttack(context.getPlayerAttacker()).setFire();
            entity.hurt(source,damage*effectNum*0.3f);

        }
        return damage;
    }
}
