package org.mylove.tprt.Modifiers;

import com.ssakura49.sakuratinker.library.logic.helper.FlyingHelper;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.armor.ModifyDamageModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.InventoryTickModifierHook;
import slimeknights.tconstruct.library.modifiers.impl.NoLevelsModifier;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.context.EquipmentContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

public class The_dragon_lord extends NoLevelsModifier implements ModifyDamageModifierHook, InventoryTickModifierHook {

    protected void registerHooks(ModuleHookMap.@NotNull Builder hookBuilder) {
        super.registerHooks(hookBuilder);
        hookBuilder.addHook(this, ModifierHooks.MODIFY_DAMAGE);
        hookBuilder.addHook(this, ModifierHooks.INVENTORY_TICK);
    }

    @Override
    public float modifyDamageTaken(@NotNull IToolStackView tool, @NotNull ModifierEntry modifier, EquipmentContext context, @NotNull EquipmentSlot slotType, DamageSource source, float amount, boolean isDirectDamage) {
        LivingEntity entity =context.getEntity();
        LivingEntity attacker =null;
        if(source.getEntity() instanceof LivingEntity){
            attacker = (LivingEntity) source.getEntity();
        }
        if(attacker != null) {
            float x = entity.getMaxHealth();
            if (x <= 150){
                float y = x/(x+150);
                return amount*(1-y);
            }
            if (x > 150){
                double lnx = Math.log(x);
                double log5x = Math.log(x)/Math.log(5);
                double log10x = Math.log10(x);
                float y = (float) (0.5f + 0.2*(lnx*log5x)/(log10x+2.55));
                if (y<=0.996){
                    return amount*(1-y);
                }
                if (y>0.996){
                    return (float) (amount*(1-0.996));
                }
            }
        }
        return amount;
    }

    @Override
    public void onInventoryTick(@NotNull IToolStackView tool, @NotNull ModifierEntry modifier, net.minecraft.world.level.@NotNull Level world, @NotNull LivingEntity entity, int itemSlot, boolean isSelected, boolean isCorrectSlot, @NotNull ItemStack stack) {
        if (entity instanceof Player player) {
            float x = entity.getMaxHealth();
            if (x >= 100){
                FlyingHelper.tickFlying(player);
            }
        }
    }
}
