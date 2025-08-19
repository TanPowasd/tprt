package org.mylove.tprt.tags;

import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.mylove.tprt.ModUsed.ModMaths.OiMaths;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeDamageModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeHitModifierHook;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

public class lucky_strike extends Modifier implements MeleeDamageModifierHook, MeleeHitModifierHook {
    boolean isLucky;
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        super.registerHooks(hookBuilder);
        hookBuilder.addHook(this, ModifierHooks.MELEE_DAMAGE, ModifierHooks.MELEE_HIT);
    }
    @Override
    public float getMeleeDamage(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float baseDamage, float damage) {
        // 伤害概率变高，概率变低；幸运越高，伤害变高概率越低，但触发倍率越高；幸运越高，伤害变低概率越高，但降低的量越少；平均伤害随着随幸运和词条等级提高
        LivingEntity attacker = context.getPlayerAttacker();
        LivingEntity target = context.getLivingTarget();
        if(attacker!=null && target != null){
            float luck = (float) attacker.getAttributeValue(Attributes.LUCK);
            int t = modifier.getLevel();
            float cl = Math.abs(luck) + 1;
            // x -> 5/6, min -> 90%, max -> infinity
            float x = .834f - 1 / (3 * cl);
            float min = .9f - 1 / (2 * cl);
            // Q: 为简化函数，请求出下列曲线的渐近线（4分）
            float max = (24*t*cl*cl + (300-24*t)*cl + 860 -200/cl)/(200*cl + 400);
            // 最终平均伤害为 100% + 2% * 幸运 * 词条等级
            double random = Math.random()/Math.nextDown(1.0);
            isLucky = random > x;
            return isLucky ? max * damage : min * damage;
        }
        return damage;
    }

    /** Todo: 1.finish attack feedback 2.add material recipe */
    @Override
    public void afterMeleeHit(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float damageDealt) {
        LivingEntity attacker = context.getPlayerAttacker();
        // if(context.getAttacker().level().isClientSide && attacker != null){
        // TinkerConstruct 钩子自带服务端判断
        if(attacker != null){
            Level pLevel = context.getPlayerAttacker().level();
            // attacker.sendSystemMessage(Component.literal("!"+attacker.getX()+isLucky));
            if(isLucky){
                // ding ding ding
                pLevel.playSound((Player)null, attacker.getX(), attacker.getY(), attacker.getZ(), SoundEvents.VILLAGER_YES, SoundSource.PLAYERS, 1.0F, 1.0F);
            } else {
                pLevel.playSound((Player)null, attacker.getX(), attacker.getY(), attacker.getZ(), SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 1.0F, 1.0F);
            }
        }

    }
}
