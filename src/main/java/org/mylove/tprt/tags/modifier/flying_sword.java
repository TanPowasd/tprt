package org.mylove.tprt.tags.modifier;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.mylove.tprt.Entities.FlyingSword;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.armor.EquipmentChangeModifierHook;
import slimeknights.tconstruct.library.modifiers.impl.SingleLevelModifier;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.context.EquipmentChangeContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import java.util.UUID;

public class flying_sword extends SingleLevelModifier implements EquipmentChangeModifierHook {
    // UUID uuid =

    private void generateFlyingSword(IToolStackView tool, EquipmentChangeContext context){
        LivingEntity player = context.getEntity();
        player.sendSystemMessage(Component.literal("generateFlyingSword!"+"\n"+1));

        FlyingSword flyingSword = new FlyingSword(context.getLevel());
        flyingSword.setPos(player.getX(), player.getY(), player.getZ());
        context.getLevel().addFreshEntity(flyingSword);
    }

    private void degenerateFlyingSword(){}

    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        hookBuilder.addHook(this, ModifierHooks.EQUIPMENT_CHANGE);
    }

    @Override
    public void onEquip(IToolStackView tool, ModifierEntry modifier, EquipmentChangeContext context) {
//        degenerateFlyingSword(tool, context);
    }

    @Override
    public void onUnequip(IToolStackView tool, ModifierEntry modifier, EquipmentChangeContext context) {
        generateFlyingSword(tool, context);
    }
}
