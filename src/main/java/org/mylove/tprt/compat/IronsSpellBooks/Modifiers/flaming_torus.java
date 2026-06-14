package org.mylove.tprt.compat.IronsSpellBooks.Modifiers;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.armor.OnAttackedModifierHook;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.context.EquipmentContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.tools.modules.armor.CounterModule;

import java.util.UUID;

public class flaming_torus extends Modifier implements OnAttackedModifierHook {

    UUID uuid = UUID.fromString("bec8aff6-a186-4287-b445-33296f769ffd");

    @Override
    protected void registerHooks(ModuleHookMap.@NotNull Builder hookBuilder) {
        super.registerHooks(hookBuilder);
        hookBuilder.addHook(this, ModifierHooks.ON_ATTACKED);
    }

    @Override
    public void onAttacked(IToolStackView iToolStackView, ModifierEntry modifierEntry, EquipmentContext context, EquipmentSlot equipmentSlot, DamageSource damageSource, float v, boolean b) {
        Entity attacker = damageSource.getEntity();
        LivingEntity defender = context.getEntity();
        if (b && attacker != null && attacker != defender && defender instanceof Player player) {
            float a = CounterModule.getLevel(iToolStackView, modifierEntry, equipmentSlot, defender);
            AttributeInstance c = player.getAttribute(AttributeRegistry.SPELL_POWER.get());
            AttributeInstance e = player.getAttribute(AttributeRegistry.FIRE_SPELL_POWER.get());
            if (c != null && e != null) {
                attacker.hurt(defender.damageSources().onFire(), (float) ((5f + 2.5f*a) * c.getValue() * e.getValue()));
            }
        }
    }
}
