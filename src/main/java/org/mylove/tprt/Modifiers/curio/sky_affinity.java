package org.mylove.tprt.Modifiers.curio;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;
import org.mylove.tprt.common.modifier.modifierModule.CuriosModifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.shared.TinkerAttributes;
import top.theillusivec4.curios.api.SlotContext;

import java.util.UUID;
import java.util.function.BiConsumer;

public class sky_affinity extends CuriosModifier {

    public static @Nullable Attribute getAttribute(){
        ResourceLocation AttributeId=new ResourceLocation("forge","entity_gravity");
        return ForgeRegistries.ATTRIBUTES.getValue(AttributeId);
    }

    private static final UUID ID = UUID.fromString("cf0cb3c7-1723-4f4c-9ae3-39ed2cd5d037");

    public void modifyCurioAttribute(IToolStackView curio, ModifierEntry entry, SlotContext context, UUID uuid, BiConsumer<Attribute, AttributeModifier> consumer) {
        consumer.accept(TinkerAttributes.JUMP_COUNT.get(), new AttributeModifier(ID, "sky_affinity", 0.5 * (float)entry.getLevel(), AttributeModifier.Operation.ADDITION));
        consumer.accept(getAttribute(), new AttributeModifier(ID, "sky_affinity", -0.2 * (float)entry.getLevel(), AttributeModifier.Operation.ADDITION));
    }
}
