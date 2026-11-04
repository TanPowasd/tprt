package org.mylove.tprt.Modifiers.armor;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;
import org.mylove.tprt.registries.ModifierIds;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.armor.ModifyDamageModifierHook;
import slimeknights.tconstruct.library.modifiers.modules.technical.SlotInChargeModule;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.capability.TinkerDataCapability;
import slimeknights.tconstruct.library.tools.context.EquipmentContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

public class Arrogant extends Modifier implements ModifyDamageModifierHook {
    private static final TagKey<DamageType> CAN_PROTECT = TagKey.create(Registries.DAMAGE_TYPE,
            new ResourceLocation("mantle", "can_protect"));
    public static final TinkerDataCapability.TinkerDataKey<SlotInChargeModule.SlotInCharge> SLOT_IN_CHARGE = TinkerDataCapability.TinkerDataKey.of(ModifierIds.Arrogant);

    @Override
    protected void registerHooks(ModuleHookMap.@NotNull Builder builder) {
        super.registerHooks(builder);
        builder.addHook(this, ModifierHooks.MODIFY_DAMAGE);
        builder.addModule(new SlotInChargeModule(SLOT_IN_CHARGE));
    }

    @Override
    public float modifyDamageTaken(@NotNull IToolStackView tool, @NotNull ModifierEntry modifier,
            EquipmentContext context, @NotNull EquipmentSlot slotType, DamageSource source,
            float amount, boolean isDirectDamage) {
        if (!source.is(CAN_PROTECT)
                || !SlotInChargeModule.isInCharge(context.getTinkerData(), SLOT_IN_CHARGE, slotType)) return amount;
        int level = Math.max(0, Math.min(5,
                SlotInChargeModule.getLevel(context.getTinkerData(), SLOT_IN_CHARGE, slotType)));
        LivingEntity entity = context.getEntity();
        float health = entity.getHealth();
        float maxHealth = entity.getMaxHealth();
        float reduction = health >= maxHealth ? 0.08F : health >= maxHealth * 0.5F ? 0.06F : 0.03F;
        return amount * (1.0F - reduction * level);
    }
}
