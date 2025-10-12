package org.mylove.tprt.Modifiers;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.armor.OnAttackedModifierHook;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.context.EquipmentContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

/// 骇人之饿
public class terror_starvation extends Modifier implements OnAttackedModifierHook {
    public static final int StarvationCoolDown = 1200; // tick

    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        hookBuilder.addHook(this, ModifierHooks.ON_ATTACKED);
    }

    @Override
    public void onAttacked(IToolStackView tool, ModifierEntry modifier, EquipmentContext context, EquipmentSlot slotType, DamageSource source, float amount, boolean isDirectDamage) {
        if (slotType != EquipmentSlot.HEAD) return;

        LivingEntity living = context.getEntity();
        if (living instanceof Player player) {
            // 半血以下触发
            if (player.getHealth() / player.getMaxHealth() > 0.5) return;

            // 获取物品实例, tc不让我们直接用IToolStackView
            ToolStack head = Modifier.getHeldTool(player, EquipmentSlot.HEAD);
            if (head != null) {
                if (player.getCooldowns().isOnCooldown(head.getItem())) return;
                else {
                    player.getCooldowns().addCooldown(head.getItem(), StarvationCoolDown);
                    // 在这里触发效果
                    /// {@link net.minecraft.world.food.FoodData}
                    float power = 0;
                    power += player.getFoodData().getFoodLevel();
                    power += player.getFoodData().getSaturationLevel();

                    // *** 使用power做点什么

                    // ***

                    player.getFoodData().setSaturation(0);
                    player.getFoodData().setFoodLevel(0);
                }
            }
        }
    }
}
