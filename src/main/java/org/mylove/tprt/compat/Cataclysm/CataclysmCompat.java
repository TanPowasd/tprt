package org.mylove.tprt.compat.Cataclysm;

import org.mylove.tprt.Tprt;
import org.mylove.tprt.compat.Cataclysm.Modifiers.*;
import slimeknights.tconstruct.library.modifiers.util.ModifierDeferredRegister;
import slimeknights.tconstruct.library.modifiers.util.StaticModifier;

public class CataclysmCompat {
    public static ModifierDeferredRegister Cataclysm_MODIFIERS = ModifierDeferredRegister.create(Tprt.MODID);

    public static final StaticModifier<ancient_sandstorm> ancient_sandstorm;
    public static final StaticModifier<burning_flames> burning_flames;
    public static final StaticModifier<wrath_of_flames> wrath_of_flames;
    public static final StaticModifier<cinder_slash> cinder_slash;
    public static final StaticModifier<Monstrous> Monstrous;
    public static final StaticModifier<cursed_spirit> cursed_spirit;
    public static final StaticModifier<machine_iron_casting> machine_iron_casting;
    public static final StaticModifier<mechanical_star_arm> mechanical_star_arm;
    public static final StaticModifier<mechanical_star_att> mechanical_star_att;
    public static final StaticModifier<soul_dodge> soul_dodge;
    public static final StaticModifier<Soul_puncture> Soul_puncture;
    public static final StaticModifier<strong_but_pliable> strong_but_pliable;
    public static final StaticModifier<Storm_incarnation_att> Storm_incarnation_att;
    public static final StaticModifier<Storm_incarnation_arm> Storm_incarnation_arm;
    public static final StaticModifier<Eye_of_Storm> Eye_of_Storm;
    public static final StaticModifier<Thunder> Thunder;
    public static final StaticModifier<Soul_render> Soul_render;
    public static final StaticModifier<Meat_Shredder> Meat_Shredder;
    public static final StaticModifier<Infernal_Forge> Infernal_Forge;
    public static final StaticModifier<The_Incinerator> The_Incinerator;
    public static final StaticModifier<Bulwark_of_The_Flame> Bulwark_of_The_Flame;
    public static final StaticModifier<Ancient_Spear> Ancient_Spear;
    public static final StaticModifier<Gauntlet_of_Guard> Gauntlet_of_Guard;
    public static final StaticModifier<Breath_of_the_Abyss> Breath_of_the_Abyss;
    public static final StaticModifier<Dimension_Fragmentation> Dimension_Fragmentation;
    public static final StaticModifier<Void_Scatter> Void_Scatter;
    static {
        ancient_sandstorm = Cataclysm_MODIFIERS.register("ancient_sandstorm", ancient_sandstorm::new);
        burning_flames = Cataclysm_MODIFIERS.register("burning_flames", burning_flames::new);
        wrath_of_flames = Cataclysm_MODIFIERS.register("wrath_of_flames", wrath_of_flames::new);
        cinder_slash = Cataclysm_MODIFIERS.register("cinder_slash", cinder_slash::new);
        Monstrous = Cataclysm_MODIFIERS.register("monstrous", Monstrous::new);
        cursed_spirit = Cataclysm_MODIFIERS.register("cursed_spirit", cursed_spirit::new);
        machine_iron_casting = Cataclysm_MODIFIERS.register("machine_iron_casting", machine_iron_casting::new);
        mechanical_star_arm = Cataclysm_MODIFIERS.register("mechanical_star_arm", mechanical_star_arm::new);
        mechanical_star_att = Cataclysm_MODIFIERS.register("mechanical_star_att", mechanical_star_att::new);
        soul_dodge = Cataclysm_MODIFIERS.register("soul_dodge", soul_dodge::new);
        Soul_puncture = Cataclysm_MODIFIERS.register("soul_puncture", Soul_puncture::new);
        strong_but_pliable = Cataclysm_MODIFIERS.register("strong_but_pliable", strong_but_pliable::new);
        Storm_incarnation_att = Cataclysm_MODIFIERS.register("storm_incarnation_att", Storm_incarnation_att::new);
        Storm_incarnation_arm = Cataclysm_MODIFIERS.register("storm_incarnation_arm", Storm_incarnation_arm::new);
        Eye_of_Storm = Cataclysm_MODIFIERS.register("eye_of_storm", Eye_of_Storm::new);
        Thunder = Cataclysm_MODIFIERS.register("thunder", Thunder::new);
        Soul_render = Cataclysm_MODIFIERS.register("soul_render", Soul_render::new);
        Meat_Shredder = Cataclysm_MODIFIERS.register("meat_shredder", Meat_Shredder::new);
        Infernal_Forge = Cataclysm_MODIFIERS.register("infernal_forge", Infernal_Forge::new);
        The_Incinerator = Cataclysm_MODIFIERS.register("the_incinerator", The_Incinerator::new);
        Bulwark_of_The_Flame = Cataclysm_MODIFIERS.register("bulwark_of_the_flame", Bulwark_of_The_Flame::new);
        Ancient_Spear = Cataclysm_MODIFIERS.register("ancient_spear", Ancient_Spear::new);
        Gauntlet_of_Guard = Cataclysm_MODIFIERS.register("gauntlet_of_guard", Gauntlet_of_Guard::new);
        Breath_of_the_Abyss = Cataclysm_MODIFIERS.register("breath_of_the_abyss", Breath_of_the_Abyss::new);
        Dimension_Fragmentation = Cataclysm_MODIFIERS.register("dimension_fragmentation", Dimension_Fragmentation::new);
        Void_Scatter = Cataclysm_MODIFIERS.register("void_scatter", Void_Scatter::new);
    }
}
