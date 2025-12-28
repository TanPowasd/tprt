package org.mylove.tprt.compat.Iceandfire;

import com.ssakura49.sakuratinker.generic.CurioModifier;
import org.mylove.tprt.Tprt;
import org.mylove.tprt.compat.Iceandfire.Modifiers.Curios.Blood_of_Hydra;
import org.mylove.tprt.compat.Iceandfire.Modifiers.The_dragon_lord;
import org.mylove.tprt.compat.Iceandfire.Modifiers.the_dragon_power;
import slimeknights.tconstruct.library.modifiers.util.ModifierDeferredRegister;
import slimeknights.tconstruct.library.modifiers.util.StaticModifier;

public class IceandfireCompat {
    public static ModifierDeferredRegister Iceandfire_MODIFIERS = ModifierDeferredRegister.create(Tprt.MODID);

    public static final StaticModifier<The_dragon_lord> The_dragon_lord;
    public static final StaticModifier<the_dragon_power> the_dragon_power;
    public static final StaticModifier<CurioModifier> Blood_of_Hydra;
    static {
        The_dragon_lord = Iceandfire_MODIFIERS.register("the_dragon_lord", The_dragon_lord::new);
        the_dragon_power = Iceandfire_MODIFIERS.register("the_dragon_power", the_dragon_power::new);
        Blood_of_Hydra = Iceandfire_MODIFIERS.register("blood_of_hydra", Blood_of_Hydra::new);
    }
}
