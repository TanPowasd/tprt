package org.mylove.tprt.registries;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import org.mylove.tprt.Tprt;
import org.mylove.tprt.tags.for_the_love;
import org.mylove.tprt.tags.lastone;
import slimeknights.tconstruct.library.modifiers.util.ModifierDeferredRegister;
import slimeknights.tconstruct.library.modifiers.util.StaticModifier;

public class ModifierRegistry {
    private static final ModifierDeferredRegister MODIFIERS = ModifierDeferredRegister.create(Tprt.MODID);
    public static void register(IEventBus eventBus) {
        MODIFIERS.register(eventBus);
    }
    public static final StaticModifier<lastone> lastone = MODIFIERS.register("lastone", lastone::new);
    public static final StaticModifier<for_the_love> for_the_love = MODIFIERS.register("for_the_love", for_the_love::new);
    //public static final StaticModifier<mixeddragon_armor>mixeddragon_armor=MODIFIERS.register("mixeddragon_armor", mixeddragon_armor::new);
}