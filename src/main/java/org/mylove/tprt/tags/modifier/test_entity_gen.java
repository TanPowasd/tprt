package org.mylove.tprt.tags.modifier;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.mylove.tprt.utilities.DeBug;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.interaction.UsingToolModifierHook;
import slimeknights.tconstruct.library.modifiers.impl.SingleLevelModifier;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

public class test_entity_gen extends SingleLevelModifier implements UsingToolModifierHook {
    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        hookBuilder.addHook(this, ModifierHooks.TOOL_USING);
    }

    @Override
    public void afterStopUsing(IToolStackView tool, ModifierEntry modifier, LivingEntity entity, int useDuration, int timeLeft, ModifierEntry activeModifier) {
        Level level = entity.level();
        //if(!level.isClientSide){
            // ?
            entity.sendSystemMessage(Component.literal("afterStopUsing:"+useDuration+"\n"+timeLeft));
            Cow cow = new Cow(EntityType.COW, level);
            cow.setPos(entity.getX(), entity.getY(), entity.getZ());
            level.addFreshEntity(cow);

            DeBug.Console((Player) entity, cow.level().isClientSide+"含客户端？");
        //}
    }
}
