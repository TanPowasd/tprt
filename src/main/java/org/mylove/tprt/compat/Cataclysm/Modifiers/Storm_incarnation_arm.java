package org.mylove.tprt.compat.Cataclysm.Modifiers;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.FakePlayer;
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

import java.util.UUID;

public class Storm_incarnation_arm extends NoLevelsModifier implements ModifyDamageModifierHook, InventoryTickModifierHook, OnAttackedModifierHook {

    UUID uuid = UUID.fromString("b6b1e877-df0d-4b81-a924-d7c19c3e378b");

    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        super.registerHooks(hookBuilder);
        hookBuilder.addHook(this, ModifierHooks.MODIFY_DAMAGE);
        hookBuilder.addHook(this, ModifierHooks.INVENTORY_TICK);
        hookBuilder.addHook(this, ModifierHooks.ON_ATTACKED);
    }

    @Override
    public float modifyDamageTaken(IToolStackView tool, ModifierEntry modifier, EquipmentContext context, EquipmentSlot slotType, DamageSource source, float amount, boolean isDirectDamage) {
        LivingEntity entity =context.getEntity();
        LivingEntity attacker =null;
        if(source.getEntity() instanceof LivingEntity){
            attacker = (LivingEntity) source.getEntity();
        }
        if(attacker != null) {
            return (float) (amount * 0.85);
        }
        return amount;
    }

    @Override
    public void onInventoryTick(IToolStackView iToolStackView, ModifierEntry modifier, Level world, LivingEntity entity, int in, boolean boo, boolean bo1, ItemStack itemStack) {
        if (!world.isClientSide() && entity instanceof ServerPlayer player&&player.tickCount %20 ==0 && !(entity instanceof FakePlayer)) {
            int X = ModifierLEVEL.getTotalArmorModifierlevel(entity, ModifierIds.Storm_incarnation_arm);
            if(X > 0){
                if (player.isInWaterOrRain()){
                    player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE,40,1));
                    player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED,40,1));
                }
            }
        }
    }

    @Override
    public void onAttacked(IToolStackView tool, ModifierEntry modifier, EquipmentContext context, EquipmentSlot slotType, DamageSource source, float amount, boolean isDirectDamage){
        LivingEntity living = context.getEntity();
        if (living instanceof Player player){
            if (player.isInWaterOrRain()){
                living.invulnerableTime = 30;
            }
        }
    }
}
