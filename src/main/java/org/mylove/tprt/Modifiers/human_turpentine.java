package org.mylove.tprt.Modifiers;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.registries.ForgeRegistries;
import org.mylove.tprt.registries.TagsRegistry;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.build.ToolStatsModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeDamageModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeHitModifierHook;
import slimeknights.tconstruct.library.modifiers.impl.NoLevelsModifier;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.nbt.IToolContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.stat.ModifierStatsBuilder;
import slimeknights.tconstruct.library.tools.stat.ToolStats;

public class human_turpentine extends NoLevelsModifier implements MeleeHitModifierHook, MeleeDamageModifierHook, ToolStatsModifierHook {
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        super.registerHooks(hookBuilder);
        hookBuilder.addHook(this, ModifierHooks.MELEE_DAMAGE);
        hookBuilder.addHook(this, ModifierHooks.MELEE_HIT);
        hookBuilder.addHook(this, ModifierHooks.TOOL_STATS);
    }

    public static MobEffect getEffect(){
        ResourceLocation effectId=new ResourceLocation("irons_spellbooks","echoing_strikes");
        return ForgeRegistries.MOB_EFFECTS.getValue(effectId);
    }

    @Override
    public void addToolStats(IToolContext context, ModifierEntry modifier, ModifierStatsBuilder builder) {
        if(context.hasTag(TagsRegistry.ItemsTag.sickle))
        {
            ToolStats.ATTACK_DAMAGE.multiply(builder, 1.75);
            ToolStats.ATTACK_SPEED.multiply(builder, 1.75);
        }
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
