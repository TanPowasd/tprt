package org.mylove.tprt.compat.Cataclysm.Modifiers;

import com.github.L_Ender.cataclysm.config.CMCommonConfig;
import com.github.L_Ender.cataclysm.entity.effect.Flame_Strike_Entity;
import com.github.L_Ender.cataclysm.entity.effect.ScreenShake_Entity;
import com.github.L_Ender.cataclysm.init.ModSounds;
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

import java.util.Objects;

public class The_Incinerator extends NoLevelsModifier implements GeneralInteractionModifierHook {

    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        hookBuilder.addHook(this, ModifierHooks.GENERAL_INTERACT);
    }

    @Override
    public int getUseDuration(IToolStackView tool, ModifierEntry modifier) {return 72000;}

    @Override
    public @NotNull UseAnim getUseAction(@NotNull IToolStackView tool, @NotNull ModifierEntry modifier) {
        return UseAnim.BOW;
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
    public void onUsingTick(IToolStackView tool, ModifierEntry modifier, LivingEntity entity, int timeLeft){
        if (entity instanceof Player player) {
            int i = this.getUseDuration(tool,modifier) - timeLeft;
            if (i == 60) {
                player.playSound(ModSounds.FLAME_BURST.get(), 1.0F, 1.0f);
            }
        }
    }

    @Override
    public void onStoppedUsing(IToolStackView tool, ModifierEntry modifier, LivingEntity entity, int timeLeft) {
        if (entity instanceof Player player) {
            Level level = player.level();
            ToolStack mainhand = Modifier.getHeldTool(player, EquipmentSlot.MAINHAND);
            int i = this.getUseDuration(tool,modifier) - timeLeft;
            double headY = player.getY() + 1.0D;
            int standingOnY = Mth.floor(player.getY()) - 2;
            float yawRadians = (float) (Math.toRadians(90 + player.getYRot()));
            boolean hasSucceeded = false;
            if (i >= 60) {
                for (int l = 0; l < 10; l++) {
                    double d2 = 2.25D * (double) (l + 1);
                    int j2 = (int) (1.5F * l);
                    if (this.spawnFlameStrike(player.getX() + (double) Mth.cos(yawRadians) * d2, player.getZ() + (double) Mth.sin(yawRadians) * d2, standingOnY, headY, yawRadians, j2, j2, level, player)) {
                        hasSucceeded = true;
                    }
                }
                if (hasSucceeded) {
                    if (!level.isClientSide) {
                        if (mainhand != null) {
                            player.getCooldowns().addCooldown(mainhand.getItem(), CMCommonConfig.Incinerator.cooldown);
                        }
                    }
                    ScreenShake_Entity.ScreenShake(level, player.position(), 30, 0.15f, 0, 30);
                    player.playSound(ModSounds.SWORD_STOMP.get(), 1.0F, 1.0f);
                }
            }
        }
    }

    private boolean spawnFlameStrike(double x, double z, double minY, double maxY, float rotation, int wait, int delay, Level world, LivingEntity player) {
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
        } while (blockpos.getY() >= minY);
        if (flag) {
            int X = (int) Objects.requireNonNull(player.getAttribute(Attributes.ATTACK_DAMAGE)).getValue();
            world.addFreshEntity(new Flame_Strike_Entity(world, x, (double) blockpos.getY() + d0, z, rotation, 60, wait, delay, 1, (float) (6F + 1.5*X),2F, false, player));
            return true;
        }
        return false;
    }
}
