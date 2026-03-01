package org.mylove.tprt.compat.Cataclysm.Modifiers;

import com.github.L_Ender.cataclysm.capabilities.ChargeCapability;
import com.github.L_Ender.cataclysm.config.CMConfig;
import com.github.L_Ender.cataclysm.init.ModCapabilities;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
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

public class Bulwark_of_The_Flame extends NoLevelsModifier implements GeneralInteractionModifierHook{

    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        hookBuilder.addHook(this, ModifierHooks.GENERAL_INTERACT);
    }

    @Override
    public int getUseDuration(IToolStackView tool, ModifierEntry modifier) {return 72000;}

    @Override
    public @NotNull InteractionResult onToolUse(IToolStackView tool, ModifierEntry modifier, Player player, InteractionHand hand, InteractionSource source) {
        if (source == InteractionSource.RIGHT_CLICK && !tool.isBroken() && modifier.intEffectiveLevel() > 0) {
            GeneralInteractionModifierHook.startUsing(tool, modifier.getId(), player, hand);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    @Override
    public void onStoppedUsing(IToolStackView tool, ModifierEntry modifier, LivingEntity entity, int timeLeft) {
        if (entity instanceof Player player) {
            if(entity.isShiftKeyDown()) {
                if(!entity.isFallFlying()) {
                    ToolStack offhand = Modifier.getHeldTool(player, EquipmentSlot.OFFHAND);
                    Level level = entity.level();
                    int X = (int) Objects.requireNonNull(entity.getAttribute(Attributes.ARMOR)).getValue();
                    int Y = (int) Objects.requireNonNull(entity.getAttribute(Attributes.ARMOR_TOUGHNESS)).getValue();
                    int Z = (int) Objects.requireNonNull(entity.getAttribute(Attributes.ATTACK_DAMAGE)).getValue();
                    int i = this.getUseDuration(tool,modifier) - timeLeft;
                    int t = Mth.clamp(i, 1, 4);
                    float f7 = entity.getYRot();
                    float f = entity.getXRot();
                    float f1 = -Mth.sin(f7 * ((float) Math.PI / 180F)) * Mth.cos(f * ((float) Math.PI / 180F));
                    float f2 = -Mth.sin(f * ((float) Math.PI / 180F));
                    float f3 = Mth.cos(f7 * ((float) Math.PI / 180F)) * Mth.cos(f * ((float) Math.PI / 180F));
                    float f4 = Mth.sqrt(f1 * f1 + f2 * f2 + f3 * f3);
                    float f5 = 3.0F * (t / 6.0F);
                    f1 *= f5 / f4;
                    f3 *= f5 / f4;
                    entity.push(f1, 0, f3);
                    if (entity.onGround()) {
                        float f6 = 1.1999999F;
                        entity.move(MoverType.SELF, new Vec3(0.0D, (double) f6 / 2, 0.0D));
                    }
                    ChargeCapability.IChargeCapability ChargeCapability = ModCapabilities.getCapability(entity, ModCapabilities.CHARGE_CAPABILITY);
                    if (ChargeCapability != null) {
                        ChargeCapability.setCharge(true);
                        ChargeCapability.setTimer(t * 2);
                        ChargeCapability.seteffectiveChargeTime(t * 2);
                        ChargeCapability.setknockbackSpeedIndex(t * 2);
                        ChargeCapability.setdamagePerEffectiveCharge((float) (0.6F + 0.5*X + Y + 0.2 * Z));
                        ChargeCapability.setdx(f1 * 0.1F);
                        ChargeCapability.setdZ(f3 * 0.1F);
                    }
                    if (!level.isClientSide) {
                        if (offhand != null) {
                            player.getCooldowns().addCooldown(offhand.getItem(), CMConfig.BulwarkOfTheFlameCooldown);
                        }
                    }
                }
            }
        }
    }
}
