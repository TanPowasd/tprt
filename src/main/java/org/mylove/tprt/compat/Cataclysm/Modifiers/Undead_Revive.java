package org.mylove.tprt.compat.Cataclysm.Modifiers;

import com.github.L_Ender.cataclysm.init.ModEffect;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import org.jetbrains.annotations.Nullable;
import org.mylove.tprt.Tprt;
import org.mylove.tprt.registries.ModifierIds;
import org.mylove.tprt.utils.ModifierLEVEL;
import slimeknights.mantle.client.TooltipKey;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.display.TooltipModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.InventoryTickModifierHook;
import slimeknights.tconstruct.library.modifiers.impl.NoLevelsModifier;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.item.armor.ModifiableArmorItem;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

import java.util.List;
import java.util.UUID;

public class Undead_Revive extends NoLevelsModifier implements InventoryTickModifierHook, TooltipModifierHook {

    UUID uuid = UUID.fromString("09003a24-6160-4fe2-96b8-f686bb7ae27e");

    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        hookBuilder.addHook(this, ModifierHooks.INVENTORY_TICK);
        hookBuilder.addHook(this, ModifierHooks.TOOLTIP);
    }

    private static final ResourceLocation Undead_Revive_COOLDOWN = ResourceLocation.fromNamespaceAndPath(Tprt.MODID, "undead_revive_cooldown");
    private static final int COOLDOWN_TICKS = 7200;

    public Undead_Revive(){
        MinecraftForge.EVENT_BUS.addListener(this::onLivingDeath);
    }

    private void onLivingDeath(LivingDeathEvent event) {
        LivingEntity entity = event.getEntity();
        int level = ModifierLEVEL.getTotalArmorModifierlevel(entity, ModifierIds.Undead_Revive);
        if(level >= 1 && entity instanceof Player player){
            for (ItemStack equipment : player.getInventory().armor) {
                if (equipment.getItem() instanceof ModifiableArmorItem) {
                    ToolStack tool = ToolStack.from(equipment);
                    ModDataNBT toolData = tool.getPersistentData();
                    int Cooldown = toolData.getInt(Undead_Revive_COOLDOWN);
                    if (Cooldown == 0 && tool.getModifierLevel(this) > 0) {
                        toolData.putInt(Undead_Revive_COOLDOWN, COOLDOWN_TICKS);
                        event.setCanceled(true);
                        player.deathTime = -2;
                        player.fallDistance = 0;
                        player.setHealth((float) (player.getMaxHealth() * 0.2));
                        player.invulnerableTime = 100;
                        player.addEffect(new MobEffectInstance(ModEffect.EFFECTGHOST_SICKNESS.get(), 7200, 0, false, true));
                        player.sendSystemMessage(
                                Component.translatable("message.tprt.undead_revive")
                                        .withStyle(ChatFormatting.DARK_AQUA)
                        );
                        break;
                    }
                }
            }
        }
    }

    @Override
    public void onInventoryTick(IToolStackView tool, ModifierEntry modifier, Level level, LivingEntity entity, int i, boolean b, boolean b1, ItemStack itemStack) {
        if (entity instanceof Player ) {
            ModDataNBT modDataNBT = tool.getPersistentData();
            int currentCooldown = modDataNBT.getInt(Undead_Revive_COOLDOWN);
            if (currentCooldown > 0) {
                modDataNBT.putInt(Undead_Revive_COOLDOWN, currentCooldown - 1);
            }
        }
    }

    @Override
    public void addTooltip(IToolStackView tool, ModifierEntry modifierEntry, @Nullable Player player, List<Component> list, TooltipKey tooltipKey, TooltipFlag tooltipFlag) {
        if (player != null) {
            ModDataNBT modDataNBT = tool.getPersistentData();
            int currentCooldown = modDataNBT.getInt(Undead_Revive_COOLDOWN);

            if (currentCooldown > 0) {
                list.add(applyStyle(
                        Component.translatable("modifier.tprt.tooltip.undead_revive_cd")
                                .append(": ")
                                .append(String.valueOf(currentCooldown / 20)).withStyle(ChatFormatting.DARK_AQUA)
                ));
            } else {
                list.add(applyStyle(
                        Component.translatable("modifier.tprt.tooltip.undead_revive_done").withStyle(ChatFormatting.DARK_AQUA)
                ));
            }
        }
    }
}
