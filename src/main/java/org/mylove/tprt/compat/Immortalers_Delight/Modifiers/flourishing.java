package org.mylove.tprt.compat.Immortalers_Delight.Modifiers;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AirBlock;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.impl.NoLevelsModifier;
import slimeknights.tconstruct.library.modifiers.modules.armor.ArmorWalkRadiusModule;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.helper.ToolDamageUtil;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.tools.TinkerModifiers;

public class flourishing extends NoLevelsModifier implements ArmorWalkRadiusModule<Void> {
    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        super.registerHooks(hookBuilder);
        hookBuilder.addModule(this);
    }

    @Override
    public float getRadius(IToolStackView tool, ModifierEntry modifier) {
        return (2.5f + tool.getModifierLevel(TinkerModifiers.expanded.getId()) * tool.getModifierLevel(TinkerModifiers.expanded.getId()));
    }

    @Override
    public boolean walkOn(IToolStackView tool, ModifierEntry entry, LivingEntity living, Level world, BlockPos target, BlockPos.MutableBlockPos mutable, Void context) {
        BlockState upblock = world.getBlockState(target);
        BlockState downblock = world.getBlockState(target.below());
        if(upblock.getBlock() instanceof BonemealableBlock bonemealableBlock) {
            boolean flag = false;
            if (downblock.getBlock() instanceof FarmBlock farmBlock && farmBlock.canSurvive(downblock, world, target.below()))
                flag = true;
            else if (downblock.getBlock() instanceof BonemealableBlock) flag = true;
            else if (downblock.getBlock() instanceof AirBlock) flag = true;
            if (flag) {
                if (bonemealableBlock.isValidBonemealTarget(world, target, upblock, world.isClientSide) &&
                        bonemealableBlock.isBonemealSuccess(world, living.getRandom(), target, upblock)) {
                    bonemealableBlock.performBonemeal((ServerLevel) world, living.getRandom(), target, upblock);
                    world.levelEvent(2005, target, 0);
                }
            }
        }
        return false;
    }
    @Override
    public void onWalk(IToolStackView tool, ModifierEntry modifier, LivingEntity living, BlockPos prevPos, BlockPos newPos)
    {
        Level world = living.level();
        if (living.onGround() && !tool.isBroken() && !world.isClientSide) {
            ToolDamageUtil.damageAnimated(tool, 1, living, EquipmentSlot.FEET);
            Void context = this.getContext(tool, modifier, living, prevPos, newPos);
            float trueRadius = Math.min(16.0F, this.getRadius(tool, modifier));
            int radius = Mth.floor(trueRadius);
            BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
            Vec3 posVec = living.position();
            BlockPos center = BlockPos.containing(posVec.x, posVec.y + 0.5, posVec.z);
            for(BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -1, -radius), center.offset(radius, 1, radius))) {
                if (pos.closerToCenterThan(new Vec3(living.position().x, pos.getY(), living.position().z), trueRadius) && this.walkOn(tool, modifier, living, world, pos, mutable, context)) {
                    break;
                }
            }
        }
    }
}
