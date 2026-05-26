package org.mylove.tprt.compat.IronsSpellBooks.Modifiers.Curios;

import io.redspace.ironsspellbooks.api.events.ChangeManaEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import org.mylove.tprt.common.modifier.modifierModule.CuriosModifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import top.theillusivec4.curios.api.SlotContext;

import java.util.UUID;
import java.util.function.BiConsumer;

public class source_alloy_charm extends CuriosModifier {

    public boolean isNoLevels() {
        return true;
    }

    private static final UUID ID = UUID.fromString("c1753321-e847-4f24-8000-8e82df1afec2");

    public void modifyCurioAttribute(IToolStackView curio, ModifierEntry entry, SlotContext context, UUID uuid, BiConsumer<Attribute, AttributeModifier> consumer) {
        consumer.accept(AttributeRegistry.SPELL_POWER.get(), new AttributeModifier(ID, "source_alloy_charm", 0.35 * (float)entry.getLevel(), AttributeModifier.Operation.MULTIPLY_TOTAL));
    }

    @Override
    public void onCurioTick(IToolStackView curio, ModifierEntry entry, SlotContext context, LivingEntity entity, ItemStack stack) {
        if (entity instanceof ServerPlayer player && player.tickCount %2 == 0){
            int x = entry.getLevel();
            MagicData magicData = MagicData.getPlayerMagicData(player);
            int currentMana = (int) magicData.getMana();
            boolean noMana = currentMana <= 0;
            int manaCost = 10 * x ;
            if (noMana){
                manaCost = 0;
            }

            ChangeManaEvent event = new ChangeManaEvent(player, magicData, currentMana, currentMana - manaCost);
            magicData.setMana(event.getNewMana());
        }
    }
}
