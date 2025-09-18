package org.mylove.tprt.hooks;

import net.minecraft.resources.ResourceLocation;
import org.mylove.tprt.Tprt;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.module.ModuleHook;

public class TprtHooks {
    public static final ModuleHook<KillingHook> KILLING_HOOK;
    public static final ModuleHook<DamageRedirectHook> DAMAGE_REDIRECT_HOOK;

    static {
        KILLING_HOOK = ModifierHooks.register(
                ResourceLocation.fromNamespaceAndPath(Tprt.MODID, "hook_killing"),
                KillingHook.class,
                KillingHook.AllMerge::new,
                new KillingHook() {}
        );

        DAMAGE_REDIRECT_HOOK = ModifierHooks.register(
                ResourceLocation.fromNamespaceAndPath(Tprt.MODID, "hook_living_hurt"),
                DamageRedirectHook.class,
                DamageRedirectHook.AllMerge::new,
                (tool, attacker, target, level, source) -> source
        );
    }
}
