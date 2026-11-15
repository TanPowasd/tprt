package org.mylove.tprt.Modifiers.armor;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EquipmentSlot;
import org.mylove.tprt.Tprt;
import slimeknights.tconstruct.library.modifiers.impl.NoLevelsModifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.armor.ModifyDamageModifierHook;
import slimeknights.tconstruct.library.modifiers.modules.technical.SlotInChargeModule;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.capability.TinkerDataCapability;
import slimeknights.tconstruct.library.tools.context.EquipmentContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

/** 浴火: one full-body split, with a genuinely fire-tagged second damage transaction. */
public class FlameBathedModifier extends NoLevelsModifier implements ModifyDamageModifierHook {
    public static final ModifierId ID = new ModifierId(Tprt.MODID, "flame_bathed");
    static final slimeknights.mantle.data.predicate.damage.DamageSourcePredicate CAN_PROTECT =
            slimeknights.mantle.data.predicate.damage.DamageSourcePredicate.CAN_PROTECT;
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
        if (!(amount > 0) || !SlotInChargeModule.isInCharge(context.getTinkerData(), SLOT_IN_CHARGE, slot)) {
            return amount;
        }
        return source.is(DamageTypeTags.IS_FIRE) ? amount * 0.5F : amount;
    }
}
