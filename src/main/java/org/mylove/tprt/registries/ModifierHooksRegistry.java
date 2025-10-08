package org.mylove.tprt.registries;


import java.util.Collection;
import java.util.function.Function;
import javax.annotation.Nullable;

import org.mylove.tprt.Tprt;
import org.mylove.tprt.hooks.AttackerWithEquipmentModifyDamageModifierHook;
import org.mylove.tprt.hooks.MeleeDamagePercentModifierHook;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.module.ModuleHook;

public class ModifierHooksRegistry {

    public static final ModuleHook<MeleeDamagePercentModifierHook> MELEE_DAMAGE_PERCENT;

    private static <T> ModuleHook<T> register(String name, Class<T> filter, @Nullable Function<Collection<T>, T> merger, T defaultInstance) {
        return ModifierHooks.register(Tprt.getResource(name), filter, merger, defaultInstance);
    }

    private static <T> ModuleHook<T> register(String name, Class<T> filter, T defaultInstance) {
        return register(name, filter, (Function)null, defaultInstance);
    }

    static {
        Function<Collection<AttackerWithEquipmentModifyDamageModifierHook>, AttackerWithEquipmentModifyDamageModifierHook> merger = AttackerWithEquipmentModifyDamageModifierHook.AllMerger::new;
        AttackerWithEquipmentModifyDamageModifierHook fallback = (tool, modifier, target, context, slotType, source, baseamount, amount, isDirectDamage) -> {
        };

        MELEE_DAMAGE_PERCENT = register("melee_damage_percent_hook", MeleeDamagePercentModifierHook.class, MeleeDamagePercentModifierHook.AllMerger::new, new MeleeDamagePercentModifierHook() {
        });
    }
}
