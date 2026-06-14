package org.mylove.tprt.compat.IronsSpellBooks.Modifiers.Curios;

import com.ssakura49.sakuratinker.compat.IronSpellBooks.ISSHooks;
import com.ssakura49.sakuratinker.compat.IronSpellBooks.context.SpellAttackContext;
import com.ssakura49.sakuratinker.compat.IronSpellBooks.hook.SpellLevelModifierHook;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;
import org.mylove.tprt.common.modifier.modifierModule.CuriosModifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

public class flame_paradise extends CuriosModifier implements SpellLevelModifierHook {

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
        if(entity instanceof Player player && context.getSchoolType() == SchoolRegistry.FIRE.get()){
            currentLevel = currentLevel + 2;
            if (currentLevel - 5 >= 0){
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 200,currentLevel - 4));
                player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 200,(currentLevel - 4)/2));
            }else {
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 200,1));
                player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 200,1));
            }
        }
        return currentLevel;
    }
}
