package org.mylove.tprt.compat.Goety;

import org.mylove.tprt.Tprt;
import org.mylove.tprt.compat.Goety.Modifiers.Dark_Condensation;
import slimeknights.tconstruct.library.modifiers.util.ModifierDeferredRegister;
import slimeknights.tconstruct.library.modifiers.util.StaticModifier;

public class GoetyCompat {
    public static ModifierDeferredRegister Goety_MODIFIERS = ModifierDeferredRegister.create(Tprt.MODID);

    public static final StaticModifier<Dark_Condensation> Dark_Condensation;
    static {
        Dark_Condensation = Goety_MODIFIERS.register("dark_condensation", Dark_Condensation::new);
    }
}
