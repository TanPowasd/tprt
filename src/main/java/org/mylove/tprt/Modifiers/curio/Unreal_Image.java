package org.mylove.tprt.Modifiers.curio;

import com.ssakura49.sakuratinker.generic.CurioModifier;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.mylove.tprt.hooks.Curios.CurioEquipmentChangeModifierHook;
import org.mylove.tprt.hooks.Curios.CurioInventoryTickModifierHook;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import top.theillusivec4.curios.api.SlotContext;

import static net.minecraft.world.effect.MobEffects.INVISIBILITY;

public class Unreal_Image extends CurioModifier implements CurioEquipmentChangeModifierHook, CurioInventoryTickModifierHook {

    public Unreal_Image() {
    }

    public boolean isNoLevels() {
        return true;
    }

    @Override
    public void onCurioUnequip(IToolStackView curio, ModifierEntry entry, SlotContext context, LivingEntity entity, ItemStack newStack, ItemStack stack) {
        if (entity instanceof Player player) {
            player.removeEffect(INVISIBILITY);
            player.removeEffect(MobEffects.DAMAGE_RESISTANCE);
        }
    }


    @Override
    public void onCurioTick(IToolStackView curio, ModifierEntry entry, SlotContext context, LivingEntity entity, ItemStack stack) {
        LivingEntity attacker=context.entity();
        MobEffectInstance instance = attacker.getEffect(INVISIBILITY);
        if (entity instanceof Player player){
            if (instance==null) {
                player.addEffect(new MobEffectInstance(INVISIBILITY, Integer.MAX_VALUE, 0));
            }
            if (player.tickCount % 100 == 0){
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 1));
            }
        }

    }
}

