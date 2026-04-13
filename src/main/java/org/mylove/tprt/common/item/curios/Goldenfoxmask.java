package org.mylove.tprt.common.item.curios;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.ssakura49.sakuratinker.client.component.CuriosMutableComponent;
import com.ssakura49.sakuratinker.client.component.LoreHelper;
import com.ssakura49.sakuratinker.client.component.LoreStyle;
import com.ssakura49.sakuratinker.client.component.STFont;
import com.ssakura49.sakuratinker.client.component.ChatFormattingContext;
import com.ssakura49.sakuratinker.common.items.curios.SimpleDescriptiveCurio;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.function.Consumer;

public class Goldenfoxmask extends SimpleDescriptiveCurio {
    public Goldenfoxmask(Properties properties, String slotIdentifier, Multimap<Attribute, AttributeModifier> defaultModifiers) {
        super(new Properties().stacksTo(1).rarity(Rarity.EPIC), "fox_mask" , () ->{
            ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
            builder
                    .put(Attributes.LUCK, new AttributeModifier(UUID.fromString("b55b3a3f-5a4c-4927-a5cc-c530be4e3454"), "Golden Fox Mask", 3.0, AttributeModifier.Operation.ADDITION))
                    .put(Attributes.ATTACK_DAMAGE, new AttributeModifier(UUID.fromString("06c04ad4-c63b-4bee-a886-45a5684f1ffe"), "Golden Fox Mask", 0.25, AttributeModifier.Operation.MULTIPLY_BASE))
                    .put(Attributes.ATTACK_SPEED, new AttributeModifier(UUID.fromString("13c00ad2-9353-45ea-a214-3d3f4fa1ac27"), "Golden Fox Mask", 0.15, AttributeModifier.Operation.MULTIPLY_BASE));
            return builder.build();
        });
        this.defaultDesc(
                CuriosMutableComponent.create(LoreStyle.ATTRIBUTE_PREFIX).appendAttributeFormat(1, stack ->
                        new Object[]{
                                LoreHelper.codeMode(ChatFormatting.GOLD),
                                95F,
                                LoreHelper.codeMode(ChatFormattingContext.SAKURA_ORIGIN()),
                                I18n.get("item.tprt.golden_fox_mask.desc")
                        }
                )
        );
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