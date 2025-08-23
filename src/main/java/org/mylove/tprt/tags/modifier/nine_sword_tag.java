package org.mylove.tprt.tags.modifier;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.mylove.tprt.entities.flying_sword.FlyingSword;
import org.mylove.tprt.utilities.Abbr;
import org.mylove.tprt.utilities.DeBug;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.interaction.InventoryTickModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.UsingToolModifierHook;
import slimeknights.tconstruct.library.modifiers.impl.SingleLevelModifier;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;


public class nine_sword_tag extends SingleLevelModifier implements UsingToolModifierHook, InventoryTickModifierHook {
    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        hookBuilder.addHook(this, ModifierHooks.TOOL_USING, ModifierHooks.INVENTORY_TICK);
    }

    @Override
    public void onUsingTick(IToolStackView tool, ModifierEntry modifier, LivingEntity entity, int useDuration, int timeLeft, ModifierEntry activeModifier) {
        if(!(entity instanceof Player player)) return;
        Level level = player.level();

        if(!level.isClientSide){
            int total = Abbr.getFlyingSwordCount(player);
            // 发射需要满足的条件: 1.处于发射窗口(即下述判定) 2.存在飞剑冷却完毕
            // 富含魔力的算式*1
            if(player.tickCount % (22 - total*2) != 0) return;
            for (int i=0; i<9; i++){
                FlyingSword sword = Abbr.getSword(player, i);
                if(sword != null){
                    Vec3 target = player.position().add(player.getLookAngle().scale(10));
                    boolean lunched = sword.triggerLunch(target);
                    if(lunched) break;
                }
            }
        } else {
            // DeBug.Console(player, "onUsingTickClientSide");
        }

    }

    @Override
    public void onInventoryTick(IToolStackView tool, ModifierEntry modifier, Level world, LivingEntity holder, int itemSlot, boolean isSelected, boolean isCorrectSlot, ItemStack stack) {
        if(!isSelected) return;
        if(!(holder instanceof Player player)) return;
        // player.hold
    }
}
