package org.mylove.tprt.registries;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import org.mylove.tprt.Tprt;
import org.mylove.tprt.tags.*;
import slimeknights.tconstruct.library.modifiers.util.ModifierDeferredRegister;
import slimeknights.tconstruct.library.modifiers.util.StaticModifier;

public class ModifierRegistry {
    private static final ModifierDeferredRegister MODIFIERS = ModifierDeferredRegister.create(Tprt.MODID);
    public static void register(IEventBus eventBus) {
        MODIFIERS.register(eventBus);
    }
    public static final StaticModifier<lastone> lastone = MODIFIERS.register("lastone", lastone::new);
    public static final StaticModifier<for_the_love> for_the_love = MODIFIERS.register("for_the_love", for_the_love::new);
    public static final StaticModifier<sacred_sacrifice> sacred_sacrifice = MODIFIERS.register("sacred_sacrifice", sacred_sacrifice::new);
    public static final StaticModifier<burning_flames> burning_flames = MODIFIERS.register("burning_flames", burning_flames::new);
    public static final StaticModifier<wrath_of_flames> wrath_of_flames = MODIFIERS.register("wrath_of_flames", wrath_of_flames::new);
}