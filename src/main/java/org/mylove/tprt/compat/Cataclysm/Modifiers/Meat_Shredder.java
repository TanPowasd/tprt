package org.mylove.tprt.compat.Cataclysm.Modifiers;

import com.github.L_Ender.cataclysm.Cataclysm;
import com.github.L_Ender.cataclysm.init.ModSounds;
import com.github.L_Ender.cataclysm.util.CMDamageTypes;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.interaction.GeneralInteractionModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.InteractionSource;
import slimeknights.tconstruct.library.modifiers.hook.interaction.InventoryTickModifierHook;
import slimeknights.tconstruct.library.modifiers.impl.NoLevelsModifier;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import java.util.List;
import java.util.Optional;


public class Meat_Shredder extends NoLevelsModifier implements GeneralInteractionModifierHook, InventoryTickModifierHook {

    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        hookBuilder.addHook(this, ModifierHooks.GENERAL_INTERACT);
        hookBuilder.addHook(this, ModifierHooks.INVENTORY_TICK);
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
    public void onInventoryTick(IToolStackView iToolStackView, ModifierEntry modifier, Level world, LivingEntity living, int in, boolean boo, boolean bo1, ItemStack stack) {
        if (!world.isClientSide&&living.isUsingItem()&&living.getUseItem()==stack){
            double range = 2.5D;
            Vec3 srcVec = living.getEyePosition();
            Vec3 lookVec = living.getViewVector(1.0F);
            Vec3 destVec = srcVec.add(lookVec.x() * range, lookVec.y() * range, lookVec.z() * range);
            float var9 = 1.0F;
            List<Entity> possibleList = world.getEntities(living, living.getBoundingBox().expandTowards(lookVec.x() * range, lookVec.y() * range, lookVec.z() * range).inflate(var9, var9, var9));
            boolean flag = false;
            Cataclysm.PROXY.playWorldSound(living, (byte) 1);
            for (Entity entity : possibleList) {
                if (entity instanceof LivingEntity) {
                    float borderSize = 0.5F;
                    AABB collisionBB = entity.getBoundingBox().inflate(borderSize, borderSize, borderSize);
                    Optional<Vec3> interceptPos = collisionBB.clip(srcVec, destVec);
                    if (collisionBB.contains(srcVec)) {
                        flag =true;
                    } else if (interceptPos.isPresent()) {
                        flag =true;
                    }
                    if (flag) {
                        if (entity.hurt(CMDamageTypes.causeShredderDamage(living), (float) living.getAttributeValue(Attributes.ATTACK_DAMAGE) / 4F + 4)) {
                            int X = EnchantmentHelper.getFireAspect(living);
                            //world.playSound(null, living.getX(), living.getY(), living.getZ(), ModSounds.SHREDDER_LOOP.get(), SoundSource.PLAYERS, 1.5f, 1F / (living.getRandom().nextFloat() * 0.4F + 0.8F));
                            if (X > 0 && !entity.isOnFire()) {
                                entity.setSecondsOnFire(X * 4);
                            }
                        }
                        double d0 = (world.getRandom().nextFloat() - 0.5F) + entity.getDeltaMovement().x;
                        double d1 = (world.getRandom().nextFloat() - 0.5F) + entity.getDeltaMovement().y;
                        double d2 = (world.getRandom().nextFloat() - 0.5F) + entity.getDeltaMovement().z;
                        double dist = 1F + world.getRandom().nextFloat() * 0.2F;
                        double d3 = d0 * dist;
                        double d4 = d1 * dist;
                        double d5 = d2 * dist;
                        entity.level().addParticle(ParticleTypes.LAVA, entity.getX(), living.getEyeY() - 0.1D + (entity.getEyePosition().y - living.getEyeY()), entity.getZ(), d3, d4, d5);
                    }
                }
            }
        }
    }

    @Override
    public void onStoppedUsing(IToolStackView tool, ModifierEntry modifier, LivingEntity entity, int timeLeft) {
        if (entity instanceof Player player) {
            Level world = player.level();
            world.playSound(null, entity.getX(), entity.getY(), entity.getZ(), ModSounds.SHREDDER_END.get(), SoundSource.PLAYERS, 1.5f, 1F / (entity.getRandom().nextFloat() * 0.4F + 0.8F));
            Cataclysm.PROXY.clearSoundCacheFor(entity);
        }
    }

    @Override
    public void onUsingTick(IToolStackView tool, ModifierEntry modifier, LivingEntity entity, int timeLeft){
        if (entity instanceof Player player) {
            Level world = player.level();
            if (timeLeft == 72000){
                world.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.SHREDDER_START.get(), SoundSource.PLAYERS, 1.5f, 1F / (player.getRandom().nextFloat() * 0.4F + 0.8F));
            }
        }
    }
}
