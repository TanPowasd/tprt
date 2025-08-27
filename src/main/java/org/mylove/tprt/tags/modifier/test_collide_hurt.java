package org.mylove.tprt.tags.modifier;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.mylove.tprt.utilities.DeBug;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeHitModifierHook;
import slimeknights.tconstruct.library.modifiers.impl.SingleLevelModifier;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

/** 试试能不能调用非手持武器进行一次攻击 */
public class test_collide_hurt extends SingleLevelModifier implements MeleeHitModifierHook {
    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        hookBuilder.addHook(this, ModifierHooks.MELEE_HIT);
    }

    @Override
    public void afterMeleeHit(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float damageDealt) {
        Player player = context.getPlayerAttacker();
        Entity target = context.getTarget();
        Level level = context.getLevel();
        if(player == null || !(target instanceof LivingEntity)) return;

        int index;
        index = context.getSlotType().getIndex();
        DeBug.Console(player, "slot: "+index);

        ItemStack item =  player.getInventory().getItem(index+1);
        DeBug.Console(player, item.getDisplayName().toString());
        item.hurtEnemy((LivingEntity) target, player);
    }
}
