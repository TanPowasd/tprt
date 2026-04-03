package org.mylove.tprt.compat.Cataclysm.Modifiers;

import com.github.L_Ender.cataclysm.config.CMCommonConfig;
import com.github.L_Ender.cataclysm.entity.projectile.Lightning_Spear_Entity;
import com.github.L_Ender.cataclysm.init.ModSounds;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.UseAnim;
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

import static com.github.L_Ender.cataclysm.items.Astrape.getPowerForTime;

public class Thunder extends NoLevelsModifier implements GeneralInteractionModifierHook {

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
        return UseAnim.SPEAR;
    }

    @Override
    public int getUseDuration(IToolStackView tool, ModifierEntry modifier) {
        return 72000;
    }

    @Override
    public void onStoppedUsing(IToolStackView tool, ModifierEntry modifier, LivingEntity entity, int timeLeft) {
        if (entity instanceof Player player) {
            int chargeTime = getUseDuration(tool, modifier) - timeLeft;
            ToolStack mainhand = Modifier.getHeldTool(player, EquipmentSlot.MAINHAND);
            if (chargeTime > 10) {
                Level level = player.level();
                float f = getPowerForTime(chargeTime);
                AttributeInstance X = player.getAttribute(Attributes.ATTACK_DAMAGE);
                if (X != null) {if (!((double) f < 0.5D)) {
                    level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.EMP_ACTIVATED.get(), SoundSource.PLAYERS,1.0F, 0.8F);
                    if (!level.isClientSide) {
                        Vec3 lookDirection = player.getLookAngle();
                        Vec3 vec3 = new Vec3(lookDirection.x, lookDirection.y, lookDirection.z);
                        float yRot = (float) (Mth.atan2(vec3.z, vec3.x) * (180F / Math.PI)) + 90F;
                        float xRot = (float) -(Mth.atan2(vec3.y, Math.sqrt(vec3.x * vec3.x + vec3.z * vec3.z)) * (180F / Math.PI));
                        Lightning_Spear_Entity lightning = new Lightning_Spear_Entity(player, vec3, level, (float) ((float) CMCommonConfig.Astrape.damage + 3 * X.getValue()), 2.5F);
                        lightning.setYRot(yRot);
                        lightning.setXRot(xRot);
                        lightning.setPos(lightning.getX(), player.getY(0.75), lightning.getZ());
                        lightning.setAreaDamage((float) ((float) CMCommonConfig.Astrape.areaDamage + X.getValue()));
                        lightning.setAreaRadius(2);
                        boolean flag = level.addFreshEntity(lightning);
                        if(flag){
                            if (mainhand != null) {
                                player.getCooldowns().addCooldown(mainhand.getItem(), CMCommonConfig.Astrape.cooldown);
                            }
                        }
                    }
                }
            }
            }
        }
    }
}
