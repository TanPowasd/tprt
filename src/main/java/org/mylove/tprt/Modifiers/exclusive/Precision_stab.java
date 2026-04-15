package org.mylove.tprt.Modifiers.exclusive;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import slimeknights.tconstruct.library.modifiers.impl.NoLevelsModifier;
import slimeknights.tconstruct.library.tools.item.IModifiable;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

public class Precision_stab extends NoLevelsModifier {

    public Precision_stab(){
        MinecraftForge.EVENT_BUS.addListener(this::onLivingDamage);
    }

    @SubscribeEvent
    public void onLivingDamage(LivingDamageEvent event) {
        DamageSource source = event.getSource();
        float x = event.getAmount();
        if (source.getEntity() instanceof Player player) {
            ItemStack item = player.getMainHandItem();
            int y = (int) (player.getAttributeValue(Attributes.ATTACK_DAMAGE));
            if (this.getLevel(item) > 0 && x < y) {
                if (y > 1000){
                    y = 1000;
                }
                event.setAmount(y);
            }
        }
    }

    private int getLevel(ItemStack stack) {
        if (stack.getItem() instanceof IModifiable) {
            IToolStackView tool = ToolStack.from(stack);
            return tool.getModifierLevel(this);
        }
        return 0;
    }
}
