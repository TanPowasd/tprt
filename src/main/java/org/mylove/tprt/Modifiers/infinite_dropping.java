package org.mylove.tprt.Modifiers;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import slimeknights.mantle.client.TooltipKey;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeHitModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.display.TooltipModifierHook;
import slimeknights.tconstruct.library.modifiers.impl.SingleLevelModifier;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import java.util.List;

/// 无限"下坠"
public class infinite_dropping extends SingleLevelModifier implements MeleeHitModifierHook, TooltipModifierHook {
    public static final RandomSource random = RandomSource.create();

    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        hookBuilder.addHook(this, ModifierHooks.MELEE_HIT, ModifierHooks.TOOLTIP);
    }

    @Override
    public void afterMeleeHit(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float damageDealt) {
        Player attacker = context.getPlayerAttacker();
        LivingEntity target = context.getLivingTarget();
        if(target == null || attacker == null) return;
        Level level = attacker.level();

        Vec3 tar = target.position();
        target.playSound(SoundEvents.HONEY_BLOCK_FALL);
        for (int i = 0; i < 160; i++) {
            ((ServerLevel)level).sendParticles(
                    ParticleTypes.LANDING_HONEY,
                    tar.x,
                    tar.y+1,
                    tar.z,
                    1,
                    random.nextGaussian() * .3,
                    random.nextGaussian(),
                    random.nextGaussian() * .3,
                    1.0);
        }
    }

    @Override
    public void addTooltip(IToolStackView tool, ModifierEntry modifier, @Nullable Player player, List<Component> tooltip, TooltipKey key, TooltipFlag tooltipFlag) {
        if (player != null && key == TooltipKey.SHIFT) {
            tooltip.add(applyStyle(Component.literal("屎!?")));
        }
    }
}
