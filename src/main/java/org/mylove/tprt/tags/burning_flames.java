package org.mylove.tprt.tags;

import com.ssakura49.sakuratinker.library.damagesource.LegacyDamageSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
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
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeHitModifierHook;
import slimeknights.tconstruct.library.modifiers.impl.NoLevelsModifier;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import java.util.UUID;

public class  burning_flames extends Modifier implements MeleeHitModifierHook {
    UUID uuid = UUID.fromString("d2ab3741-d1ad-4e3e-1145-f37f1aac9cf1");
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        super.registerHooks(hookBuilder);
        hookBuilder.addHook(this, ModifierHooks.MELEE_HIT);
    }
    public static MobEffect getEffect(){
        ResourceLocation effectId=new ResourceLocation("cataclysm","blazing_brand");
        return ForgeRegistries.MOB_EFFECTS.getValue(effectId);
    }
    @Override
    public void afterMeleeHit(IToolStackView tool, ModifierEntry entry, ToolAttackContext context, float damageDealt) {
        if (!(context.getTarget() instanceof LivingEntity target)) return;
        MobEffect effect = getEffect();
        MobEffectInstance instance = target.getEffect(effect);
        if (instance != null) {
            int amplifier = instance.getAmplifier();
            int newAmplifier = amplifier + 1;
            int duration = instance.getDuration();
            boolean ambient = instance.isAmbient();
            boolean visible = instance.isVisible();
            boolean showIcon = instance.showIcon();
            if (newAmplifier >= entry.getLevel()) {
                newAmplifier = entry.getLevel()-1;
            }
            int a = instance.getAmplifier();
            MobEffectInstance newEffect = new MobEffectInstance(
                    effect,
                   // duration,
                    200,
                    newAmplifier,
                    ambient,
                    visible,
                    showIcon);
            target.addEffect(newEffect);
        } else {
            target.addEffect(new MobEffectInstance(getEffect(), 200));
        }
    }
}
