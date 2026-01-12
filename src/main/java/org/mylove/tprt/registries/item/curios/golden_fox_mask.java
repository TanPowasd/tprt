package org.mylove.tprt.registries.item.curios;

import com.google.common.collect.ImmutableMultimap;
import com.ssakura49.sakuratinker.client.component.STFont;
import com.ssakura49.sakuratinker.common.items.curios.SimpleDescriptiveCurio;
import net.minecraft.client.gui.Font;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.function.Consumer;

public class golden_fox_mask extends SimpleDescriptiveCurio {
    private static final Logger log = LoggerFactory.getLogger(golden_fox_mask.class);

    public golden_fox_mask() {
        super(new Properties().stacksTo(1).rarity(Rarity.EPIC), "golden_fox_mask" , () ->{
            ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
            builder
                    .put(Attributes.LUCK, new AttributeModifier(UUID.fromString("bf4f56a1-3f19-4a75-8faa-fae0e88f525e"), "Curios modifier", 3, AttributeModifier.Operation.ADDITION))
                    .put(Attributes.ATTACK_DAMAGE, new AttributeModifier(UUID.fromString("4c5f72a1-73aa-4ba2-80b0-aa68yda17fc1"), "Curios modifier", 0.25, AttributeModifier.Operation.MULTIPLY_BASE));
            return builder.build();
        });
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public @Nullable Font getFont(ItemStack stack, FontContext context) {
                return STFont.INSTANCE;
            }
        });
        super.initializeClient(consumer);
    }
}
