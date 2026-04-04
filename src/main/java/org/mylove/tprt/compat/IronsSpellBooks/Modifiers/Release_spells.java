package org.mylove.tprt.compat.IronsSpellBooks.Modifiers;

import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.player.ClientMagicData;
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
            if (ClientMagicData.isCasting()) {
                return InteractionResultHolder.consume(itemStack).getResult();
            } else if (ClientMagicData.getPlayerMana() < spellData.getSpell().getManaCost(spellLevel)
                    || ClientMagicData.getCooldowns().isOnCooldown(spellData.getSpell())
                    || !ClientMagicData.getSyncedSpellData(player).isSpellLearned(spellData.getSpell())) {
                return InteractionResultHolder.pass(itemStack).getResult();
            } else {
                return InteractionResultHolder.consume(itemStack).getResult();
            }
        }
        var castingSlot = interactionHand.ordinal() == 0 ? SpellSelectionManager.MAINHAND : SpellSelectionManager.OFFHAND;
        if (spellData.getSpell().attemptInitiateCast(itemStack, spellLevel, level, player, selectionOption.getCastSource(), true, castingSlot)) {
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
        Utils.releaseUsingHelper(entity, tool.getItem().getDefaultInstance(), timeLeft);
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

