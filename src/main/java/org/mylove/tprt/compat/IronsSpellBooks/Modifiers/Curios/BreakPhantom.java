package org.mylove.tprt.compat.IronsSpellBooks.Modifiers.Curios;

import com.ssakura49.sakuratinker_tools.compat.irons_spellbooks.library.hook.ISSHooks;
import com.ssakura49.sakuratinker_tools.compat.irons_spellbooks.library.hook.spellbook.SpellLevelModifierHook;
import com.ssakura49.sakuratinker_tools.compat.irons_spellbooks.tool.context.SpellAttackContext;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;
import org.mylove.tprt.common.modifier.modifierModule.CuriosModifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import static net.minecraft.world.effect.MobEffects.INVISIBILITY;

public class BreakPhantom extends CuriosModifier implements SpellLevelModifierHook {

    public boolean isNoLevels() {
        return true;
    }

    protected void registerHooks(ModuleHookMap.@NotNull Builder builder) {
        super.registerHooks(builder);
        builder.addHook(this, ISSHooks.SPELL_LEVEL);
    }

    @Override
    public int getSpellLevel(IToolStackView iToolStackView, ModifierEntry modifierEntry, SpellAttackContext context, int baseLevel, int currentLevel) {
        LivingEntity entity = context.getAttacker();
        if(entity instanceof Player player && player.hasEffect(INVISIBILITY)){
            currentLevel = currentLevel + 2;
            player.removeEffect(INVISIBILITY);
        }
        return currentLevel;
    }
}
