package org.mylove.tprt.Modifiers.re;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeHitModifierHook;
import slimeknights.tconstruct.library.modifiers.impl.NoLevelsModifier;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

public class combat_echo extends NoLevelsModifier implements MeleeHitModifierHook {

    /** 回响伤害占本击实际伤害的比例 */
    private static final float ECHO_RATIO = 0.5F;

    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        super.registerHooks(hookBuilder);
        hookBuilder.addHook(this, ModifierHooks.MELEE_HIT);
    }

    @Override
    public void afterMeleeHit(@NotNull IToolStackView tool, @NotNull ModifierEntry modifier,
                              @NotNull ToolAttackContext context, float damageDealt) {
        // 近战命中必为玩家攻击（怪物使用工具走 MONSTER_MELEE_HIT，不触发本词条）
        Player attacker = context.getPlayerAttacker();
        LivingEntity target = context.getLivingTarget();
        // 只有命中实体才有回响；目标已死或本击无伤害则跳过
        if (attacker == null || target == null || target.isDeadOrDying() || damageDealt <= 0.0F) return;
        Level level = target.level();
        if (level.isClientSide) return;

        // 伤害来源为玩家：归属击杀/经验给攻击者
        DamageSource source = attacker.damageSources().playerAttack(attacker);
        // 忽略无敌帧：清零后立即结算；不携带 bypasses 标记，其余规则（护甲等）正常生效
        target.invulnerableTime = 0;
        target.hurt(source, damageDealt * ECHO_RATIO);
    }
}
