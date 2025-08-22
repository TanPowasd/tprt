package org.mylove.tprt.tags.modifier;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.mylove.tprt.entities.flying_sword.FlyingSword;
import org.mylove.tprt.utilities.Abbr;
import org.mylove.tprt.utilities.DeBug;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.interaction.UsingToolModifierHook;
import slimeknights.tconstruct.library.modifiers.impl.SingleLevelModifier;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;


public class nine_sword_tag extends SingleLevelModifier implements UsingToolModifierHook {
    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        hookBuilder.addHook(this, ModifierHooks.TOOL_USING);
    }

    @Override
    public void onUsingTick(IToolStackView tool, ModifierEntry modifier, LivingEntity entity, int useDuration, int timeLeft, ModifierEntry activeModifier) {
        if(!(entity instanceof Player player)) return;
        //if(player.tickCount % 2 != 1) return;
        Level level = player.level();

        if(!level.isClientSide){
            for (int i=0; i<9; i++){
                FlyingSword sword = Abbr.getSword(player, i);
                if(sword != null){
                    Vec3 target = player.getLookAngle().scale(4);
                    sword.triggerLunch(target);
                }
            }
        } else {
            DeBug.Console(player, "onUsingTick");
        }

    }
}
