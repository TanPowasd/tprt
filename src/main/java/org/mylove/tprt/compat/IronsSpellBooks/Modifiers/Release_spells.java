package org.mylove.tprt.compat.IronsSpellBooks.Modifiers;

import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.api.util.Utils;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.interaction.GeneralInteractionModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.InteractionSource;
import slimeknights.tconstruct.library.modifiers.impl.NoLevelsModifier;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.definition.module.ToolHooks;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

public class Release_spells extends NoLevelsModifier implements GeneralInteractionModifierHook {
    @Override
    protected void registerHooks(ModuleHookMap.Builder builder) {
        builder.addHook(this, ModifierHooks.GENERAL_INTERACT);
    }
    @Override
    public @NotNull InteractionResult onToolUse(IToolStackView iToolStackView, ModifierEntry modifierEntry, Player player, InteractionHand interactionHand, InteractionSource interactionSource) {
        if (!iToolStackView.getHook(ToolHooks.INTERACTION).canInteract(iToolStackView, modifierEntry.getId(), interactionSource)) return InteractionResult.FAIL;
        ItemStack itemStack = player.getItemInHand(interactionHand);
        Level level = player.level();
        SpellSelectionManager spellSelectionManager = new SpellSelectionManager(player);
        SpellSelectionManager.SelectionOption selectionOption = spellSelectionManager.getSelection();
        if (selectionOption == null || selectionOption.spellData.equals(SpellData.EMPTY)) {
            return InteractionResultHolder.pass(itemStack).getResult();
        }
        SpellData spellData = selectionOption.spellData;
        int spellLevel = spellData.getSpell().getLevelFor(spellData.getLevel(), player);
        if (level.isClientSide()) {
            InteractionResult result = ClientCastHelper.check(itemStack, player, spellData, spellLevel);
            if (result == InteractionResult.CONSUME) {
                GeneralInteractionModifierHook.startUsing(iToolStackView, modifierEntry.getId(), player, interactionHand);
            }
            return result;
        }
        var castingSlot = interactionHand.ordinal() == 0 ? SpellSelectionManager.MAINHAND : SpellSelectionManager.OFFHAND;
        if (spellData.getSpell().attemptInitiateCast(itemStack, spellLevel, level, player, selectionOption.getCastSource(), true, castingSlot)) {
            GeneralInteractionModifierHook.startUsing(iToolStackView, modifierEntry.getId(), player, interactionHand);
            return InteractionResultHolder.consume(itemStack).getResult();
        } else {
            return InteractionResultHolder.fail(itemStack).getResult();
        }
    }

    @Override
    public int getUseDuration(@NotNull IToolStackView tool, @NotNull ModifierEntry modifier) {
        return 7200;
    }

    @Override
    public @NotNull UseAnim getUseAction(@NotNull IToolStackView tool, @NotNull ModifierEntry modifier) {
        return UseAnim.BOW;
    }

    @Override
    public void onStoppedUsing(@NotNull IToolStackView tool, @NotNull ModifierEntry modifier, @NotNull LivingEntity entity, int timeLeft) {
        GeneralInteractionModifierHook.finishUsing(tool);
        ItemStack itemStack = entity.getUseItem();
        if (itemStack.isEmpty()) {
            itemStack = entity.getItemInHand(entity.getUsedItemHand());
        }
        if (itemStack.isEmpty()) {
            itemStack = entity.getMainHandItem();
        }
        Utils.releaseUsingHelper(entity, itemStack, timeLeft);
    }

    private static class ClientCastHelper {
        private static java.lang.reflect.Method isCastingMethod;
        private static java.lang.reflect.Method getPlayerManaMethod;
        private static java.lang.reflect.Method getCooldownsMethod;
        private static java.lang.reflect.Method isOnCooldownMethod;
        private static java.lang.reflect.Method getSyncedSpellDataMethod;
        private static java.lang.reflect.Method isSpellLearnedMethod;
        private static boolean initialized = false;

        private static void init() throws Exception {
            if (initialized) return;
            Class<?> cmdClass = Class.forName("io.redspace.ironsspellbooks.player.ClientMagicData");
            isCastingMethod = cmdClass.getMethod("isCasting");
            getPlayerManaMethod = cmdClass.getMethod("getPlayerMana");
            getCooldownsMethod = cmdClass.getMethod("getCooldowns");
            Class<?> cooldownsClass = getCooldownsMethod.getReturnType();
            isOnCooldownMethod = cooldownsClass.getMethod("isOnCooldown", io.redspace.ironsspellbooks.api.spells.AbstractSpell.class);
            getSyncedSpellDataMethod = cmdClass.getMethod("getSyncedSpellData", LivingEntity.class);
            Class<?> syncedDataClass = getSyncedSpellDataMethod.getReturnType();
            isSpellLearnedMethod = syncedDataClass.getMethod("isSpellLearned", io.redspace.ironsspellbooks.api.spells.AbstractSpell.class);
            initialized = true;
        }

        static InteractionResult check(ItemStack itemStack, Player player, SpellData spellData, int spellLevel) {
            try {
                init();
                if ((boolean) isCastingMethod.invoke(null)) {
                    return InteractionResultHolder.consume(itemStack).getResult();
                }
                int mana = (int) getPlayerManaMethod.invoke(null);
                int manaCost = spellData.getSpell().getManaCost(spellLevel);
                Object cooldowns = getCooldownsMethod.invoke(null);
                boolean onCooldown = (boolean) isOnCooldownMethod.invoke(cooldowns, spellData.getSpell());
                Object syncedData = getSyncedSpellDataMethod.invoke(null, player);
                boolean learned = (boolean) isSpellLearnedMethod.invoke(syncedData, spellData.getSpell());
                if (mana < manaCost || onCooldown || !learned) {
                    return InteractionResultHolder.pass(itemStack).getResult();
                } else {
                    return InteractionResultHolder.consume(itemStack).getResult();
                }
            } catch (Throwable t) {
                return InteractionResultHolder.consume(itemStack).getResult();
            }
        }
    }

    @Override
    public @NotNull Component getDisplayName(@NotNull IToolStackView tool, @NotNull ModifierEntry entry, @Nullable RegistryAccess access) {
        return InteractionSource.formatModifierName(tool, this, super.getDisplayName(tool, entry, access));
    }

    @Override
    public int getPriority() {
        return 150;
    }
}

