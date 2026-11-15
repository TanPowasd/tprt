package org.mylove.tprt.Modifiers.armor;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import org.mylove.tprt.Tprt;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.armor.ModifyDamageModifierHook;
import slimeknights.tconstruct.library.modifiers.modules.technical.SlotInChargeModule;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.capability.TinkerDataCapability;
import slimeknights.tconstruct.library.tools.context.EquipmentContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

/** 淬火: entity-shared stacks, not one counter or reduction per armor piece. */
public class QuenchingModifier extends Modifier implements ModifyDamageModifierHook {
    public static final ModifierId ID = new ModifierId(Tprt.MODID, "quenching");
    public static final TinkerDataCapability.TinkerDataKey<SlotInChargeModule.SlotInCharge> SLOT_IN_CHARGE =
            TinkerDataCapability.TinkerDataKey.of(ID);

    @Override
    protected void registerHooks(ModuleHookMap.Builder builder) {
        super.registerHooks(builder);
        builder.addHook(this, ModifierHooks.MODIFY_DAMAGE);
        builder.addModule(new SlotInChargeModule(SLOT_IN_CHARGE));
    }

    @Override
    public float modifyDamageTaken(IToolStackView tool, ModifierEntry modifier, EquipmentContext context,
                                   EquipmentSlot slot, DamageSource source, float amount, boolean direct) {
        if (!SlotInChargeModule.isInCharge(context.getTinkerData(), SLOT_IN_CHARGE, slot)) return amount;
        int level = Math.min(4, Math.max(0,
                SlotInChargeModule.getLevel(context.getTinkerData(), SLOT_IN_CHARGE, slot)));
        return amount * (1F - Math.min(0.6F,
                0.015F * level * BlazeforgedArmorEvents.getStacks(context.getEntity())));
    }
}
