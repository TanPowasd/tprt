package org.mylove.tprt.compat.Cataclysm.Modifiers;

import com.github.L_Ender.cataclysm.config.CMCommonConfig;
import com.github.L_Ender.cataclysm.entity.projectile.Void_Rune_Entity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.util.FakePlayer;
import org.mylove.tprt.Tprt;
import org.mylove.tprt.registries.ModifierIds;
import org.mylove.tprt.utils.ModifierLEVEL;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.armor.ModifyDamageModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.armor.OnAttackedModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.InventoryTickModifierHook;
import slimeknights.tconstruct.library.modifiers.impl.NoLevelsModifier;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.context.EquipmentContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;

import java.util.Objects;
import java.util.UUID;

public class Ender_Armor extends NoLevelsModifier implements OnAttackedModifierHook, InventoryTickModifierHook, ModifyDamageModifierHook {
    UUID uuid = UUID.fromString("b340688d-2e27-489f-acf8-69e54ab30315");

    public static final ResourceLocation Void_Power = Tprt.getResource("void_power");

    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        hookBuilder.addHook(this, ModifierHooks.MODIFY_DAMAGE,ModifierHooks.INVENTORY_TICK,ModifierHooks.ON_ATTACKED);
    }

    @Override
    public void onInventoryTick(IToolStackView tool, ModifierEntry modifier, Level world, LivingEntity entity, int i, boolean b, boolean b1, ItemStack itemStack) {
        if (!world.isClientSide() && entity instanceof ServerPlayer player&&player.tickCount %20 ==0 && !(entity instanceof FakePlayer)) {
            int x = ModifierLEVEL.getTotalArmorModifierlevel(entity, ModifierIds.Ender_Armor);
            if(x > 0){
                ModDataNBT tooldata = tool.getPersistentData();
                if(tooldata.getInt(Void_Power) == 0){
                    entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 2, false, true));
                }
            }
        }
    }

    @Override
    public float modifyDamageTaken(IToolStackView tool, ModifierEntry modifier, EquipmentContext context, EquipmentSlot equipmentSlot, DamageSource damageSource, float amount, boolean isDirectDamage) {
        LivingEntity entity =context.getEntity();
        int x = ModifierLEVEL.getTotalArmorModifierlevel(entity, ModifierIds.Ender_Armor);
        if(x > 0){
            ModDataNBT tooldata = tool.getPersistentData();
            if(tooldata.getInt(Void_Power) > 0){
                return (float) (amount * 0.7);
            }
        }
        return amount;
    }

    @Override
    public void onAttacked(IToolStackView iToolStackView, ModifierEntry modifier, EquipmentContext context, EquipmentSlot equipmentSlot, DamageSource damageSource, float v, boolean b) {
        LivingEntity entity=context.getEntity();
        int x = ModifierLEVEL.getTotalArmorModifierlevel(entity, ModifierIds.Ender_Armor);
        if(x > 0 && entity instanceof Player player){
            ModDataNBT tooldata = iToolStackView.getPersistentData();
            Level world = player.level();
            if(tooldata.getInt(Void_Power) > 0){
                if(tooldata.getInt(Void_Power) <= 50){
                    tooldata.putInt(Void_Power,tooldata.getInt(Void_Power)-1);
                }else {
                    int standingOnY = Mth.floor(player.getY()) - 3;
                    if (!player.getCooldowns().isOnCooldown(iToolStackView.getItem())) {
                        Vec3 looking = player.getLookAngle();
                        double headY = player.getY() + 1.0D;
                        Vec3[] all = new Vec3[]{looking, looking.yRot(0.3f), looking.yRot(-0.3f), looking.yRot(0.6f), looking.yRot(-0.6f), looking.yRot(0.9f), looking.yRot(-0.9f)};
                        for (Vec3 vector3d : all) {
                            float f = (float) Mth.atan2(vector3d.z, vector3d.x);
                            for (int i = 0; i < 5; i++) {
                                double d2 = 1.75D * (double) (i + 1);
                                this.spawnFangs(player.getX() + (double) Mth.cos(f) * d2, headY, player.getZ() + (double) Mth.sin(f) * d2, standingOnY, f, i, world, player);
                            }
                        }
                    }
                    player.getCooldowns().addCooldown(iToolStackView.getItem(), CMCommonConfig.VoidCore.cooldown);
                    tooldata.putInt(Void_Power, tooldata.getInt(Void_Power) - 20);
                }
            }
        }
    }

    private boolean spawnFangs(double x, double y, double z, int lowestYCheck, float yRot, int warmupDelayTicks, Level world, Player player) {
        BlockPos blockpos = BlockPos.containing(x, y, z);
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
        } while (blockpos.getY() >= lowestYCheck);
        if (flag) {
            int A = (int) Objects.requireNonNull(player.getAttribute(Attributes.ATTACK_DAMAGE)).getValue();
            world.addFreshEntity(new Void_Rune_Entity(world, x, (double) blockpos.getY() + d0, z, yRot, warmupDelayTicks, 1.5f * A + 8, player));
            return true;
        }
        return false;
    }
}
