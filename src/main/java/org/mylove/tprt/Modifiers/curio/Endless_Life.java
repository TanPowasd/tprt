package org.mylove.tprt.Modifiers.curio;

import com.ssakura49.sakuratinker.generic.CurioModifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import org.mylove.tprt.hooks.Curios.CurioEquipmentChangeModifierHook;
import org.mylove.tprt.hooks.Curios.CurioInventoryTickModifierHook;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import top.theillusivec4.curios.api.SlotContext;

import static net.minecraft.world.effect.MobEffects.REGENERATION;

public class Endless_Life extends CurioModifier implements CurioEquipmentChangeModifierHook, CurioInventoryTickModifierHook {

    public Endless_Life() {}

    @Override
    public void onCurioUnequip(IToolStackView curio, ModifierEntry entry, SlotContext context, LivingEntity entity, ItemStack newStack, ItemStack stack) {
        if (entity instanceof Player player) {
            player.removeEffect(MobEffects.REGENERATION);
        }
    }

    @Override
    public void onCurioTick(IToolStackView curio, ModifierEntry entry, SlotContext context, LivingEntity entity, ItemStack stack) {
        LivingEntity attacker=context.entity();
        MobEffectInstance instance = attacker.getEffect(REGENERATION);
        if (entity instanceof Player player){
            if (instance==null){
                player.addEffect(new MobEffectInstance(REGENERATION, Integer.MAX_VALUE,curio.getModifierLevel(this) * 2 ));
            }
        }
    }

    public void onCurioTakeHeal(IToolStackView curio, ModifierEntry entry, LivingHealEvent event, LivingEntity entity) {
        if (entity != null) {
            if (entity instanceof Player) {
                float bonus = (float) (event.getAmount() * (1.25f + (curio.getModifierLevel(this)) * 0.25));
                event.setAmount(bonus);
            }
        }
    }
}
