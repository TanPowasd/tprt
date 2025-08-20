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
    /*
        public static final StaticModifier<lastone> lastone = MODIFIERS.register("lastone", lastone::new);
        public static final StaticModifier<for_the_love> for_the_love = MODIFIERS.register("for_the_love", for_the_love::new);
        public static final StaticModifier<sacred_sacrifice> sacred_sacrifice = MODIFIERS.register("sacred_sacrifice", sacred_sacrifice::new);
        public static final StaticModifier<burning_flames> burning_flames = MODIFIERS.register("burning_flames", burning_flames::new);
        public static final StaticModifier<wrath_of_flames> wrath_of_flames = MODIFIERS.register("wrath_of_flames", wrath_of_flames::new);
        public static final StaticModifier<real_attack> real_attack = MODIFIERS.register("real_attack",real_attack::new);
        public static final StaticModifier<cursed_spirit> cursed_spirit = MODIFIERS.register("cursed_spirit",cursed_spirit::new);
        public static final StaticModifier<double_or_none> double_or_none = MODIFIERS.register("double_or_none",double_or_none::new);
        public static final StaticModifier<soul_dodge> soul_dodge = MODIFIERS.register("soul_dodge",soul_dodge::new);
        public static final StaticModifier<disruption_break> disruption_break = MODIFIERS.register("disruption_break",disruption_break::new);
        public static final StaticModifier<lucky_strike> luck_strike = MODIFIERS.register("luck_strike",lucky_strike::new);
        public static final StaticModifier<the_dragon_power> the_dragon_power = MODIFIERS.register("the_dragon_power",the_dragon_power::new);
    */
    public static final StaticModifier<lastone> LASTONE = MODIFIERS.register("lastone", lastone::new);
    public static final StaticModifier<for_the_love> FOR_THE_LOVE = MODIFIERS.register("for_the_love", for_the_love::new);
    public static final StaticModifier<sacred_sacrifice> SACRED_SACRIFICE = MODIFIERS.register("sacred_sacrifice", sacred_sacrifice::new);
    public static final StaticModifier<burning_flames> BURNING_FLAMES = MODIFIERS.register("burning_flames", burning_flames::new);
    public static final StaticModifier<wrath_of_flames> WRATH_OF_FLAMES = MODIFIERS.register("wrath_of_flames", wrath_of_flames::new);
    public static final StaticModifier<real_attack> REAL_ATTACK = MODIFIERS.register("real_attack",real_attack::new);
    public static final StaticModifier<cursed_spirit> CURSED_SPIRIT = MODIFIERS.register("cursed_spirit",cursed_spirit::new);
    public static final StaticModifier<double_or_none> DOUBLE_OR_NONE = MODIFIERS.register("double_or_none",double_or_none::new);
    public static final StaticModifier<soul_dodge> SOUL_DODGE = MODIFIERS.register("soul_dodge",soul_dodge::new);
    public static final StaticModifier<disruption_break> DISRUPTION_BREAK = MODIFIERS.register("disruption_break",disruption_break::new);
    public static final StaticModifier<lucky_strike> LUCK_STRIKE = MODIFIERS.register("luck_strike",lucky_strike::new);
    public static final StaticModifier<the_dragon_power> THE_DRAGON_POWER = MODIFIERS.register("the_dragon_power",the_dragon_power::new);
    public static final StaticModifier<advanced_greed> ADVANCED_GREED = MODIFIERS.register("advanced_greed",advanced_greed::new);


}