package org.mylove.tprt.compat.Cataclysm.Modifiers;

import com.github.L_Ender.cataclysm.config.CMConfig;
import com.github.L_Ender.cataclysm.entity.effect.ScreenShake_Entity;
import com.github.L_Ender.cataclysm.init.ModSounds;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeHitModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.BlockInteractionModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.GeneralInteractionModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.InteractionSource;
import slimeknights.tconstruct.library.modifiers.impl.NoLevelsModifier;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.tools.TinkerModifiers;

import java.util.List;

public class Infernal_Forge extends NoLevelsModifier implements GeneralInteractionModifierHook , MeleeHitModifierHook , BlockInteractionModifierHook {

    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        hookBuilder.addHook(this, ModifierHooks.GENERAL_INTERACT);
        hookBuilder.addHook(this, ModifierHooks.MELEE_HIT);
        hookBuilder.addHook(this, ModifierHooks.BLOCK_INTERACT);
    }

    @Override
    public @NotNull InteractionResult onToolUse(IToolStackView tool, ModifierEntry modifier, Player player, InteractionHand hand, InteractionSource source) {
        if (source == InteractionSource.RIGHT_CLICK && !tool.isBroken() && modifier.intEffectiveLevel() > 0) {
            GeneralInteractionModifierHook.startUsing(tool, modifier.getId(), player, hand);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    @Override
    public @NotNull InteractionResult beforeBlockUse(IToolStackView tool, ModifierEntry modifier, UseOnContext context, InteractionSource source) {
        ItemStack stack = context.getItemInHand();
        Player player = context.getPlayer();
        if (player != null && player.getMainHandItem() == stack) {
            ToolStack mainhand = Modifier.getHeldTool(player, EquipmentSlot.MAINHAND);
            if (player.getCooldowns().isOnCooldown(mainhand.getItem())){
                return BlockInteractionModifierHook.super.beforeBlockUse(tool,modifier,context,source);
            }
            EarthQuake(context,tool,modifier);
            player.getCooldowns().addCooldown(mainhand.getItem(), CMConfig.InfernalForgeCooldown);
            return InteractionResult.SUCCESS;
        }
        return BlockInteractionModifierHook.super.beforeBlockUse(tool,modifier,context,source);
    }

    @Override
    public void afterMeleeHit(IToolStackView tool, ModifierEntry modifierEntry, ToolAttackContext context, float damageDealt) {
        LivingEntity entity = context.getAttacker();
        ToolStack stack = Modifier.getHeldTool(entity, EquipmentSlot.MAINHAND);
        if (!entity.level().isClientSide) {
            entity.playSound(ModSounds.HAMMERTIME.get(), 0.5F, 0.5F);
        }
    }

    public float getRadius(IToolStackView tool, ModifierEntry modifier) {
        return (4.0f + 2 * tool.getModifierLevel(TinkerModifiers.expanded.getId()));
    }

    private void EarthQuake(UseOnContext context,IToolStackView tool,ModifierEntry modifier) {
        Player player = context.getPlayer();
        Level world = context.getLevel();
        boolean berserk = false;
        if (player != null) {
            berserk = player.getMaxHealth() * 1 / 2 >= player.getHealth();
        }
        double radius = this.getRadius(tool, modifier);
        if (player != null) {
            ScreenShake_Entity.ScreenShake(world, player.position(), 30, 0.1f, 0, 30);
        }
        world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.5f, 1F / (player.getRandom().nextFloat() * 0.4F + 0.8F));
        List<Entity> list = world.getEntities(player, player.getBoundingBox().inflate(radius, radius, radius));
        for (Entity entity : list) {
            if (entity instanceof LivingEntity) {
                entity.hurt(world.damageSources().mobAttack(player), (float) (2.25 * (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE)));
                entity.setDeltaMovement(entity.getDeltaMovement().multiply(0.0, 2.0, 0.0));
                if (berserk) {
                    entity.setSecondsOnFire((int) 5.0);
                }
            }
        }
        if (world.isClientSide) {
            BlockState block = world.getBlockState(player.blockPosition().below());
            double NumberofParticles = radius * 4.0D;
            for (double i = 0.0D; i < 80; i++) {
                double d0 = player.getX() + radius * Mth.sin((float) (i / NumberofParticles * 360.0f));
                double d1 = player.getY() + 0.15;
                double d2 = player.getZ() + radius * Mth.cos((float) (i / NumberofParticles * 360.0f));
                double d3 = world.getRandom().nextGaussian() * 0.2D;
                double d4 = world.getRandom().nextGaussian() * 0.2D;
                double d5 = world.getRandom().nextGaussian() * 0.2D;
                world.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, block), d0, d1, d2, d3, d4, d5);
                if (berserk) {
                    world.addParticle(ParticleTypes.FLAME, d0, d1, d2, d3, d4, d5);
                }
            }
        }
    }
}
