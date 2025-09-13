package org.mylove.tprt.registries;

import net.minecraft.resources.ResourceLocation;
import org.mylove.tprt.Tprt;
import org.mylove.tprt.hooks.KillingHook;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.module.ModuleHook;

public class ModHooks {
    public static final ModuleHook<KillingHook> KILLING_HOOK;

    static {
        KILLING_HOOK = ModifierHooks.register(
                ResourceLocation.fromNamespaceAndPath(Tprt.MODID, "hook_killing"),
                KillingHook.class,
                KillingHook.AllMerge::new,
                new KillingHook() {}
        );
    }
}
