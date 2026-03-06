package org.mylove.tprt.compat.Cataclysm.Modifiers;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.interaction.GeneralInteractionModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.InteractionSource;
import slimeknights.tconstruct.library.modifiers.impl.NoLevelsModifier;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import java.util.List;
import java.util.Objects;

public class Gauntlet_of_Guard extends NoLevelsModifier implements GeneralInteractionModifierHook {

    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        hookBuilder.addHook(this, ModifierHooks.GENERAL_INTERACT);
    }

    @Override
    public @NotNull UseAnim getUseAction(@NotNull IToolStackView tool, @NotNull ModifierEntry modifier) {
        return UseAnim.BOW;
    }

    @Override
    public int getUseDuration(IToolStackView tool, ModifierEntry modifier) {
        return 72000;
    }

    @Override
    public @NotNull InteractionResult onToolUse(IToolStackView iToolStackView, ModifierEntry modifierEntry, Player player, InteractionHand interactionHand, InteractionSource source) {
        if (source == InteractionSource.RIGHT_CLICK && !iToolStackView.isBroken() && modifierEntry.intEffectiveLevel() > 0) {
            GeneralInteractionModifierHook.startUsing(iToolStackView, modifierEntry.getId(), player, interactionHand);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    @Override
    public void onUsingTick(IToolStackView tool, ModifierEntry modifier, LivingEntity entity, int timeLeft) {
        double radius = 11.0D;
        Level world = entity.level();
        List<LivingEntity> list = world.getEntitiesOfClass(LivingEntity.class, entity.getBoundingBox().inflate(radius));
        for (LivingEntity entitys : list) {
            if (entitys instanceof Player && ((Player) entitys).getAbilities().invulnerable) continue;
            Vec3 diff = entitys.position().subtract(entity.position().add(0,0,0));
            diff = diff.normalize().scale(0.1);
            entitys.setDeltaMovement(entitys.getDeltaMovement().subtract(diff));
            int x = (int) Objects.requireNonNull(entity.getAttribute(Attributes.ATTACK_DAMAGE)).getValue();
            entitys.hurt(entitys.damageSources().dragonBreath(), (float) (0.1 * x + 2));
        }
        if (world.isClientSide) {
            for (int i = 0; i < 3; ++i) {
                int j = world.random.nextInt(2) * 2 - 1;
                int k = world.random.nextInt(2) * 2 - 1;
                double d0 = entity.getX() + 0.25D * (double) j;
                double d1 = (float) entity.getY() + world.random.nextFloat();
                double d2 = entity.getZ() + 0.25D * (double) k;
                double d3 = world.random.nextFloat() * (float) j;
                double d4 = ((double) world.random.nextFloat() - 0.5D) * 0.125D;
                double d5 = world.random.nextFloat() * (float) k;
                world.addParticle(ParticleTypes.PORTAL, d0, d1, d2, d3, d4, d5);
            }
        }
    }
}
