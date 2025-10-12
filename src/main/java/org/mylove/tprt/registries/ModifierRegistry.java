package org.mylove.tprt.registries;

import com.ssakura49.sakuratinker.generic.CurioModifier;
import net.minecraftforge.eventbus.api.IEventBus;
import org.mylove.tprt.Tprt;
import org.mylove.tprt.Modifiers.*;
import org.mylove.tprt.Modifiers.curio.FortuneCurio;
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
    public static final StaticModifier<gas_poison> GAS_POISON = MODIFIERS.register("gas_poison",gas_poison::new);
    public static final StaticModifier<fire_addition> FIRE_ADDITION = MODIFIERS.register("fire_addition",fire_addition::new);
    public static final StaticModifier<strong_but_pliable> STRONG_BUT_PLIABLE = MODIFIERS.register("strong_but_pliable",strong_but_pliable::new);
    public static final StaticModifier<ancient_sandstorm> ANCIENT_SANDSTORM = MODIFIERS.register("ancient_sandstorm",ancient_sandstorm::new);
    public static final StaticModifier<d2_half_truths> D2_HALF_TRUTHS = MODIFIERS.register("d2_half_truths",d2_half_truths::new);
    public static final StaticModifier<d2_lucky_pants> D2_LUCKY_PANTS = MODIFIERS.register("d2_lucky_pants",d2_lucky_pants::new);
    public static final StaticModifier<mechanical_star_att> MECHANICAL_STAR_ATT = MODIFIERS.register("mechanical_star_att",mechanical_star_att::new);
    public static final StaticModifier<mechanical_star_arm> MECHANICAL_STAR_ARM = MODIFIERS.register("mechanical_star_arm",mechanical_star_arm::new);
    public static final StaticModifier<machine_iron_casting> MACHINE_IRON_CASTING = MODIFIERS.register("machine_iron_casting",machine_iron_casting::new);
    public static final StaticModifier<dynamic_attack> DYNAMIC_ATTACK = MODIFIERS.register("dynamic_attack",dynamic_attack::new);
    public static final StaticModifier<dynamic_defense> DYNAMIC_DEFENSE = MODIFIERS.register("dynamic_defense",dynamic_defense::new);
    public static final StaticModifier<cinder_slash> CINDER_SLASH = MODIFIERS.register("cinder_slash",cinder_slash::new);
    public static final StaticModifier<infinite_dropping> INFINITE_DROPPING = MODIFIERS.register("infinite_dropping",infinite_dropping::new);
    public static final StaticModifier<millennium> MILLENNIUM = MODIFIERS.register("millennium",millennium::new);
    public static final StaticModifier<zenith_first_fractal>ZENITH_FIRST_FRACTAL = MODIFIERS.register("zenith_first_fractal",zenith_first_fractal::new);
    public static final StaticModifier<oxidation>OXIDATION = MODIFIERS.register("oxidation",oxidation::new);
    public static final StaticModifier<tetanus>TETANUS_STATIC = MODIFIERS.register("tetanus",tetanus::new);
    public static final StaticModifier<persistence_of_nature> PERSISTENCE_OF_NATURE = MODIFIERS.register("persistence_of_nature",persistence_of_nature::new);
    public static final StaticModifier<CurioModifier> FORTUNE_CURIO = MODIFIERS.register("fortune_curio", FortuneCurio::new);
}