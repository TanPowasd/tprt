package org.mylove.tprt.compat.Cataclysm.Modifiers;

import com.github.L_Ender.cataclysm.capabilities.RenderRushCapability;
import com.github.L_Ender.cataclysm.config.CMCommonConfig;
import com.github.L_Ender.cataclysm.entity.projectile.Phantom_Halberd_Entity;
import com.github.L_Ender.cataclysm.init.ModCapabilities;
import com.github.L_Ender.cataclysm.init.ModParticle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.interaction.GeneralInteractionModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.InteractionSource;
import slimeknights.tconstruct.library.modifiers.impl.NoLevelsModifier;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

public class Soul_render extends NoLevelsModifier implements GeneralInteractionModifierHook {

    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        hookBuilder.addHook(this, ModifierHooks.GENERAL_INTERACT);
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
    public @NotNull UseAnim getUseAction(@NotNull IToolStackView tool, @NotNull ModifierEntry modifier) {
        return UseAnim.BOW;
    }

    @Override
    public int getUseDuration(IToolStackView tool, ModifierEntry modifier) {
        return 72000;
    }

    @Override
    public void onStoppedUsing(IToolStackView tool, ModifierEntry modifier, LivingEntity entity, int timeLeft) {
        boolean hasSucceeded = false;
        if (entity instanceof Player player) {
            ToolStack mainhand = Modifier.getHeldTool(player, EquipmentSlot.MAINHAND);
            Level level = player.level();
            int i = this.getUseDuration(tool,modifier) - timeLeft;
            if(entity.isShiftKeyDown()) {
                StrikeWindmillHalberd(level,player,7, 5, 1.0, 1.0, 0.2, 1);
                if (!level.isClientSide) {
                    if (mainhand != null) {
                        player.getCooldowns().addCooldown(mainhand.getItem(), CMCommonConfig.SoulRender.cooldown);
                    }
                }
            }else{
                int t = Mth.clamp(i, 0, 60);
                if (t > 0) {
                    float f = 0.1F * t;
                    Vec3 vec3 = player.getDeltaMovement().add(player.getViewVector(1.0F).normalize().multiply(f, f * 0.15F, f));
                    entity.setDeltaMovement(vec3.add(0, (entity.onGround() ? 0.2F : 0), 0));
                    RenderRushCapability.IRenderRushCapability ChargeCapability = ModCapabilities.getCapability(entity, ModCapabilities.RENDER_RUSH_CAPABILITY);
                    if (ChargeCapability != null) {
                        ChargeCapability.setRush(true);
                        ChargeCapability.setTimer(t / 2);
                        ChargeCapability.setdamage((float) ((float) 5 * player.getAttributeValue(Attributes.ATTACK_DAMAGE)));
                        hasSucceeded = true;
                    }
                    if (!level.isClientSide) {
                        if (hasSucceeded) {
                            if (mainhand != null) {
                                player.getCooldowns().addCooldown(mainhand.getItem(), CMCommonConfig.SoulRender.cooldown);
                            }
                        }
                    }
                }
            }
        }
    }

    private void StrikeWindmillHalberd(Level level,LivingEntity player,int numberOfBranches, int particlesPerBranch, double initialRadius, double radiusIncrement, double curveFactor, int delay) {
        float angleIncrement = (float) (2 * Math.PI / numberOfBranches);
        for (int branch = 0; branch < numberOfBranches; ++branch) {
            float baseAngle = angleIncrement * branch;
            for (int i = 0; i < particlesPerBranch; ++i) {
                double currentRadius = initialRadius + i * radiusIncrement;
                float currentAngle = (float) (baseAngle + i * angleIncrement / initialRadius + (float) (i * curveFactor));
                double xOffset = currentRadius * Math.cos(currentAngle);
                double zOffset = currentRadius * Math.sin(currentAngle);
                double spawnX = player.getX() + xOffset;
                double spawnY = player.getY() + 0.3D;
                double spawnZ = player.getZ() + zOffset;
                int d3 = delay * (i + 1);
                double deltaX = level.getRandom().nextGaussian() * 0.007D;
                double deltaY = level.getRandom().nextGaussian() * 0.007D;
                double deltaZ = level.getRandom().nextGaussian() * 0.007D;
                if (level.isClientSide) {
                    level.addParticle(ModParticle.PHANTOM_WING_FLAME.get(), spawnX, spawnY, spawnZ, deltaX, deltaY, deltaZ);
                }
                this.spawnHalberd(spawnX, spawnZ, player.getY() -5, player.getY() + 3, currentAngle, d3,level,player);
            }
        }
    }

    private void spawnHalberd(double x, double z, double minY, double maxY, float rotation, int delay, Level world, LivingEntity player) {
        BlockPos blockpos = BlockPos.containing(x, maxY, z);
        boolean flag = false;
        double d0 = 0.0D;
        do {
            BlockPos blockpos1 = blockpos.below();
            BlockState blockstate = world.getBlockState(blockpos1);
            if (blockstate.isFaceSturdy(world, blockpos1, Direction.UP)) {
                if (!world.isEmptyBlock(blockpos)) {
                    BlockState blockstate1 = world.getBlockState(blockpos);
                    VoxelShape voxelshape = blockstate1.getCollisionShape(world, blockpos);
                    if (!voxelshape.isEmpty()) {
                        d0 = voxelshape.max(Direction.Axis.Y);
                    }
                }
                flag = true;
                break;
            }
            blockpos = blockpos.below();
        } while(blockpos.getY() >= Mth.floor(minY) - 1);
        if (flag) {
            world.addFreshEntity(new Phantom_Halberd_Entity(world, x, (double)blockpos.getY() + d0, z, rotation, delay, player, (float) ((float) CMCommonConfig.SoulRender.phantomHalberdDamage + 2 * player.getAttributeValue(Attributes.ATTACK_DAMAGE))));
        }
    }
}
