package org.mylove.tprt.compat.Immortalers_Delight;

import org.mylove.tprt.Tprt;
import org.mylove.tprt.compat.Immortalers_Delight.Modifiers.flourishing;
import org.mylove.tprt.compat.Immortalers_Delight.Modifiers.gas_poison;
import org.mylove.tprt.compat.Immortalers_Delight.Modifiers.lingering_infusion;
import slimeknights.tconstruct.library.modifiers.util.ModifierDeferredRegister;
import slimeknights.tconstruct.library.modifiers.util.StaticModifier;

public class Immortalers_Delight_Compat {
    public static ModifierDeferredRegister Id_MODIFIERS = ModifierDeferredRegister.create(Tprt.MODID);

    public static StaticModifier<flourishing> flourishing;
    public static StaticModifier<gas_poison> gas_poison;
    public static StaticModifier<lingering_infusion> lingering_infusion;
    static {
        flourishing = Id_MODIFIERS.register("flourishing", flourishing::new);
        gas_poison = Id_MODIFIERS.register("gas_poison", gas_poison::new);
        lingering_infusion = Id_MODIFIERS.register("lingering_infusion", lingering_infusion::new);
    }
}
