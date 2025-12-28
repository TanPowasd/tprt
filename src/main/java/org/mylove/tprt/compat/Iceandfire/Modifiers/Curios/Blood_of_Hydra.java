package org.mylove.tprt.compat.Iceandfire.Modifiers.Curios;

import com.ssakura49.sakuratinker.generic.CurioModifier;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.mylove.tprt.hooks.Curios.CurioEquipmentChangeModifierHook;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import top.theillusivec4.curios.api.SlotContext;

public class Blood_of_Hydra extends CurioModifier implements CurioEquipmentChangeModifierHook {
    @Override
    public boolean isNoLevels() {
        return true;
    }

    public Blood_of_Hydra(){}


    @Override
    public void onCurioEquip(IToolStackView curio, ModifierEntry entry, SlotContext context, LivingEntity entity, ItemStack prevStack, ItemStack stack) {
        if (entity instanceof Player player) {
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, Integer.MAX_VALUE, curio.getModifierLevel(this) + 1));
        }
    }
    @Override
    public void onCurioUnequip(IToolStackView curio, ModifierEntry entry, SlotContext context, LivingEntity entity, ItemStack newStack, ItemStack stack) {
        if (entity instanceof Player player) {
            player.removeEffect(MobEffects.REGENERATION);
        }
    }
}