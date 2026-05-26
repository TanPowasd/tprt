package org.mylove.tprt.compat.IronsSpellBooks.Modifiers.Curios;

import com.ssakura49.sakuratinker.compat.IronSpellBooks.ISSHooks;
import com.ssakura49.sakuratinker.compat.IronSpellBooks.context.SpellAttackContext;
import com.ssakura49.sakuratinker.compat.IronSpellBooks.hook.SpellLevelModifierHook;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import org.jetbrains.annotations.NotNull;
import org.mylove.tprt.common.modifier.modifierModule.CuriosModifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

public class Thorough extends CuriosModifier implements SpellLevelModifierHook{

    public boolean isNoLevels() {
        return true;
    }

    protected void registerHooks(ModuleHookMap.@NotNull Builder builder) {
        super.registerHooks(builder);
        builder.addHook(this, ISSHooks.SPELL_LEVEL);
    }

    public static int getLight(Level world, BlockPos pos) {
        return Math.max(world.getBrightness(LightLayer.SKY, pos) - world.getSkyDarken(), world.getBrightness(LightLayer.BLOCK, pos));
    }

    @Override
    public int getSpellLevel(IToolStackView iToolStackView, ModifierEntry modifierEntry, SpellAttackContext context, int baseLevel, int currentLevel) {
        LivingEntity entity = context.getAttacker();
        if(entity instanceof Player player){
            Level world = player.getCommandSenderWorld();
            BlockPos pos = player.getOnPos().above();
            int light = getLight(world, pos) + 1;
            if (light >= 11){
                currentLevel = currentLevel + 1;
                if (context.getSchoolType() == SchoolRegistry.HOLY.get()){
                    currentLevel = currentLevel + 3;
                }
            }
        }
        return currentLevel;
    }
}
