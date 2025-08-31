package org.mylove.tprt.tags;

import com.ssakura49.sakuratinker.generic.BaseModifier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.modules.technical.SlotInChargeModule;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.capability.TinkerDataCapability;
import slimeknights.tconstruct.library.tools.context.EquipmentChangeContext;
import slimeknights.tconstruct.library.tools.context.EquipmentContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import com.ssakura49.sakuratinker.utils.tinker.ToolUtil;

import java.util.UUID;
import java.util.function.BiConsumer;

import static com.ssakura49.sakuratinker.utils.tinker.ToolUtil.getSingleModifierArmorAllLevel;

public class machine_iron_casting extends BaseModifier {
    @Override
    public boolean isNoLevels() {
        return false;
    }
    //private static final UUID BASE_ARMOR_UUID = UUID.fromString("a0a7a0a7-a0a7-a0a7a0a7a0a7");
    private static final UUID BONUS_ARMOR_UUID = UUID.fromString("4EC371FD-3F8B-4052-881E-FC32AEB7968F");
    //private static final UUID BASE_MOVESPEED_UUID = UUID.fromString("c0c7c0c7-c0c7-c0c7c0c7c0c7");
    private static final UUID BONUS_MOVESPEED_UUID = UUID.fromString("295198B3-94CA-48B2-9FFD-B76C622796B1");
    @Override
    public void addAttributes(IToolStackView tool, ModifierEntry modifier, EquipmentSlot slot, BiConsumer<Attribute, AttributeModifier> consumer) {
        consumer.accept(Attributes.ARMOR,new AttributeModifier(
                BONUS_ARMOR_UUID,
                "Bonus Armor",
                0.1f,
                AttributeModifier.Operation.MULTIPLY_TOTAL
        ));
        consumer.accept(Attributes.MOVEMENT_SPEED,new AttributeModifier(
                BONUS_MOVESPEED_UUID,
                "Bonus Movement Speed",
                -0.15f,
                AttributeModifier.Operation.MULTIPLY_TOTAL
        ));
    }
}
