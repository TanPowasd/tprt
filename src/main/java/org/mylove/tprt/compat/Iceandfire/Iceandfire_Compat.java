package org.mylove.tprt.compat.Iceandfire;

import org.mylove.tprt.Tprt;
import org.mylove.tprt.compat.Iceandfire.Modifires.The_dragon_lord;
import org.mylove.tprt.compat.Iceandfire.Modifires.Tide_Guardian;
import org.mylove.tprt.compat.Iceandfire.Modifires.the_dragon_power;
import slimeknights.tconstruct.library.modifiers.util.ModifierDeferredRegister;
import slimeknights.tconstruct.library.modifiers.util.StaticModifier;

public class Iceandfire_Compat {
    public static ModifierDeferredRegister Iceandfire_MODIFIERS = ModifierDeferredRegister.create(Tprt.MODID);

    public static StaticModifier<the_dragon_power> the_dragon_power;
    public static StaticModifier<The_dragon_lord> The_dragon_lord;
    public static StaticModifier<Tide_Guardian> Tide_Guardian;
    static {
        the_dragon_power = Iceandfire_MODIFIERS.register("the_dragon_power", the_dragon_power::new);
        The_dragon_lord = Iceandfire_MODIFIERS.register("the_dragon_lord", The_dragon_lord::new);
        Tide_Guardian = Iceandfire_MODIFIERS.register("tide_guardian", Tide_Guardian::new);
    }
}
