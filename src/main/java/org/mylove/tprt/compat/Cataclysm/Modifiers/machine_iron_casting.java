package org.mylove.tprt.compat.Cataclysm.Modifiers;

import com.ssakura49.sakuratinker.generic.BaseModifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import java.util.UUID;
import java.util.function.BiConsumer;

public class machine_iron_casting extends BaseModifier {
    @Override
    public boolean isNoLevels() {
        return true;
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
                0.2f,
                AttributeModifier.Operation.MULTIPLY_TOTAL
        ));
        consumer.accept(Attributes.MOVEMENT_SPEED,new AttributeModifier(
                BONUS_MOVESPEED_UUID,
                "Bonus Movement Speed",
                -0.25f,
                AttributeModifier.Operation.MULTIPLY_TOTAL
        ));
    }
}
