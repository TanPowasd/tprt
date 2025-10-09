package org.mylove.tprt.tags.curio;

import com.ssakura49.sakuratinker.generic.CurioModifier;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.mylove.tprt.hooks.CurioTick;
import org.mylove.tprt.hooks.onCurioUnequip;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import top.theillusivec4.curios.api.SlotContext;

public class blood_of_hydra extends CurioModifier implements CurioTick, onCurioUnequip {
    public boolean isNoLevels() {
        return true;
    }

    @Override
    public void CurioTick(IToolStackView curio, ModifierEntry entry, SlotContext context, LivingEntity entity, int level, ItemStack stack) {
        if (entity instanceof Player player){
            if (player.getHealth() / player.getMaxHealth() < 0.6){
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, Integer.MAX_VALUE, curio.getModifierLevel(this) + 1));
            }
            else player.removeEffect(MobEffects.REGENERATION);
        }
    }

    @Override
    public void onCurioUnequip(IToolStackView curio, ModifierEntry entry, SlotContext context, LivingEntity entity, ItemStack newStack, ItemStack stack) {
        if (entity instanceof Player player) {
            player.removeEffect(MobEffects.REGENERATION);
        }
    }
}
