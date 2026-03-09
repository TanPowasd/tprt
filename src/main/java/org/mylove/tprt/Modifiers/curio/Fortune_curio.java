package org.mylove.tprt.Modifiers.curio;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import org.mylove.tprt.common.modifier.modifierModule.CuriosModifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import top.theillusivec4.curios.api.SlotContext;


/// 提供词条等级的 时运 & 抢夺
public class Fortune_curio extends CuriosModifier{

    @Override
    public int onCurioUpdateFortune(IToolStackView iToolStackView, ModifierEntry modifierEntry, SlotContext slotContext, LootContext lootContext, ItemStack itemStack, int i) {
        return iToolStackView.getModifierLevel(this);
    }

    @Override
    public int onCurioUpdateLooting(IToolStackView iToolStackView, ModifierEntry modifierEntry, SlotContext slotContext, DamageSource damageSource, LivingEntity livingEntity, ItemStack itemStack, int i) {
        return iToolStackView.getModifierLevel(this);
    }
}
