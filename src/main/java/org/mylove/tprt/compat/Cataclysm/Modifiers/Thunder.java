package org.mylove.tprt.compat.Cataclysm.Modifiers;

import com.github.L_Ender.cataclysm.config.CMConfig;
import com.github.L_Ender.cataclysm.entity.projectile.Lightning_Spear_Entity;
import com.github.L_Ender.cataclysm.init.ModSounds;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
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
                if (!((double) f < 0.5D)) {
                    level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.EMP_ACTIVATED.get(), SoundSource.PLAYERS,1.0F, 0.8F);
                    if (!level.isClientSide) {
                        float d7 = entity.getYRot();
                        float d = entity.getXRot();
                        float d1 = -Mth.sin(d7 * ((float) Math.PI / 180F)) * Mth.cos(d * ((float) Math.PI / 180F));
                        float d2 = -Mth.sin(d * ((float) Math.PI / 180F));
                        float d3 = Mth.cos(d7 * ((float) Math.PI / 180F)) * Mth.cos(d * ((float) Math.PI / 180F));
                        double theta = d7 * (Math.PI / 180);
                        theta += Math.PI / 2;
                        double vecX = Math.cos(theta);
                        double vecZ = Math.sin(theta);
                        double x = entity.getX() + vecX;
                        double y = entity.getY() + entity.getBbHeight()/2;
                        double Z = entity.getZ() + vecZ;
                        Vec3 vec3 = new Vec3(d1, d2, d3).normalize();
                        float yRot = (float) (Mth.atan2(vec3.z, vec3.x) * (180F / Math.PI)) + 90F;
                        float xRot = (float) -(Mth.atan2(vec3.y, Math.sqrt(vec3.x * vec3.x + vec3.z * vec3.z)) * (180F / Math.PI));
                        Lightning_Spear_Entity lightning = new Lightning_Spear_Entity(player, vec3.normalize(),level,(float) CMConfig.AstrapeDamage);
                        lightning.accelerationPower = 0.15D;
                        lightning.setYRot(yRot);
                        lightning.setXRot(xRot);
                        lightning.setPosRaw(x, y, Z);
                        lightning.setAreaDamage((float) CMConfig.AstrapeAreaDamage);
                        lightning.setAreaRadius(1);
                        boolean flag = level.addFreshEntity(lightning);
                        if(flag){
                            if (mainhand != null) {
                                player.getCooldowns().addCooldown(mainhand.getItem(), CMConfig.AstrapeCooldown);
                            }
                        }
                    }
                }
            }
        }
    }
}
