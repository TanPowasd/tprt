package org.mylove.tprt.tags;


import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.armor.OnAttackedModifierHook;
import slimeknights.tconstruct.library.modifiers.impl.NoLevelsModifier;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.context.EquipmentContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

///52一天赋：本性的坚守
public class persistence_of_nature extends NoLevelsModifier implements OnAttackedModifierHook {
    public static final int Persistence_of_natureCoolDown = 10;//tick

    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        hookBuilder.addHook(this, ModifierHooks.ON_ATTACKED);
    }

    public void onAttacked(IToolStackView tool, ModifierEntry modifier, EquipmentContext context, EquipmentSlot slotType, DamageSource source, float amount, boolean isDirectDamage) {
        if (slotType != EquipmentSlot.MAINHAND) return;

        LivingEntity living = context.getEntity();
        if (living instanceof Player player) {

            if (player.getHealth() / player.getMaxHealth() > 0.75) return;

            ToolStack mainhand = Modifier.getHeldTool(player, EquipmentSlot.MAINHAND);
            if (mainhand != null) {
                if (player.getCooldowns().isOnCooldown(mainhand.getItem())) return;
                else {
                    player.getCooldowns().addCooldown(mainhand.getItem(), Persistence_of_natureCoolDown);

                    double x=player.getMaxHealth();
                    double y=player.getHealth();
                    double z=0.03*(x-y);
                    player.heal((float) (5+z));
                    return;

                }
            }
        }
        return;
    }
}
