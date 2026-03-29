package org.mylove.tprt.compat.Cataclysm.Modifiers;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.mylove.tprt.Tprt;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.build.ToolStatsModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeDamageModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeHitModifierHook;
import slimeknights.tconstruct.library.modifiers.impl.NoLevelsModifier;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.helper.ToolAttackUtil;
import slimeknights.tconstruct.library.tools.nbt.IToolContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;
import slimeknights.tconstruct.library.tools.stat.ModifierStatsBuilder;
import slimeknights.tconstruct.library.tools.stat.ToolStats;

import java.util.UUID;

public class Void_Armed extends NoLevelsModifier implements ToolStatsModifierHook, MeleeDamageModifierHook, MeleeHitModifierHook {
    UUID uuid = UUID.fromString("9c333f64-200c-4970-a001-f92af1b60baf");

    public static final ResourceLocation Void_Power = Tprt.getResource("void_power");

    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        hookBuilder.addHook(this, ModifierHooks.TOOL_STATS,ModifierHooks.MELEE_HIT,ModifierHooks.MELEE_DAMAGE);
    }

    @Override
    public void addToolStats(IToolContext iToolContext, ModifierEntry modifier, ModifierStatsBuilder builder) {
        ModDataNBT tooldata = (ModDataNBT) iToolContext.getPersistentData();
        if(tooldata.getInt(Void_Power) > 0){
            ToolStats.ATTACK_DAMAGE.multiply(builder, 1.0 + 0.15);
            ToolStats.ATTACK_SPEED.multiply(builder, 1.0 + 0.15);
            ToolStats.DURABILITY.multiply(builder, 1.0 + 0.15);
        }else {
            ToolStats.ATTACK_DAMAGE.multiply(builder, 0.3);
            ToolStats.ATTACK_SPEED.multiply(builder, 0.3);
            ToolStats.DURABILITY.multiply(builder, 0.3);
        }
    }

    @Override
    public float getMeleeDamage(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float baseDamage, float damage) {
        LivingEntity entity=context.getLivingTarget();
        double x = 1;
        if(entity != null){
            ModDataNBT tooldata = tool.getPersistentData();
            if (tooldata.getInt(Void_Power) > 0){
                if(tooldata.getInt(Void_Power) <= 50){
                    x= 1.3;
                }else {
                    x= 0.75;
                }
            }
        }
        return (float) (baseDamage * x);
    }

    @Override
    public void afterMeleeHit(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float damageDealt) {
        LivingEntity target=context.getLivingTarget();
        LivingEntity attacker = context.getAttacker();
        if(target != null){
            ModDataNBT tooldata = tool.getPersistentData();
            if (tooldata.getInt(Void_Power) > 0 && !((Player)attacker).getCooldowns().isOnCooldown(tool.getItem())){
                if(tooldata.getInt(Void_Power) <= 50){
                    tooldata.putInt(Void_Power,tooldata.getInt(Void_Power)-1);
                }else {
                    ((Player) attacker).getCooldowns().addCooldown(tool.getItem(), 5);
                    tooldata.putInt(Void_Power,tooldata.getInt(Void_Power)-10);
                    target.invulnerableTime = 0;
                    ToolAttackUtil.attackEntity(tool, (Player) attacker,target);
                }
            }
        }
    }
}
