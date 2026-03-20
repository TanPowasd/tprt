package org.mylove.tprt.compat.Cataclysm.Modifiers;

import com.github.L_Ender.cataclysm.init.ModEffect;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.FakePlayer;
import org.mylove.tprt.registries.ModifierIds;
import org.mylove.tprt.utils.ModifierLEVEL;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.interaction.InventoryTickModifierHook;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import java.util.List;
import java.util.UUID;

public class Monstrous extends Modifier implements InventoryTickModifierHook {
    UUID uuid = UUID.fromString("1f7931f3-c244-4954-8738-f2fea0b56f2a");

    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        super.registerHooks(hookBuilder);
        hookBuilder.addHook(this, ModifierHooks.INVENTORY_TICK);
    }

    @Override
    public void onInventoryTick(IToolStackView tool, ModifierEntry modifier, Level world, LivingEntity livingentity, int in, boolean boo, boolean bo1, ItemStack itemStack) {
        if (!world.isClientSide() && livingentity instanceof ServerPlayer player&&player.tickCount %10 ==0 && !(livingentity instanceof FakePlayer)) {
            int X = ModifierLEVEL.getTotalArmorModifierlevel(livingentity, ModifierIds.Monstrous);
            if(X > 0){
                boolean berserk = player.getMaxHealth() * 1 / 2 >= player.getHealth();
                double radius = 4.0D;
                List<Entity> list = world.getEntities(player, player.getBoundingBox().inflate(radius));
                if (berserk && !(player.getCooldowns().isOnCooldown(tool.getItem()))) {
                    for (Entity entity : list) {
                        if (entity instanceof LivingEntity) {
                            entity.hurt(world.damageSources().mobAttack(player), (float) ((float) X * player.getAttributeValue(Attributes.ATTACK_DAMAGE) * 5 / 2));
                            double d0 = entity.getX() - player.getX();
                            double d1 = entity.getZ() - player.getZ();
                            double d2 = Math.max(d0 * d0 + d1 * d1, 0.001D);
                            entity.push(d0 / d2 * 1.5, 0.15D, d1 / d2 * 1.5);
                        }
                    }
                    player.getCooldowns().addCooldown(tool.getItem(), 350);
                    player.addEffect(new MobEffectInstance(ModEffect.EFFECTMONSTROUS.get(), 200, X - 1, false, true));
                    player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, X - 1, false, false));
                }
            }
        }
    }
}
