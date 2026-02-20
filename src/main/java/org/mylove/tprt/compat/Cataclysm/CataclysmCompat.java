package org.mylove.tprt.compat.Cataclysm;

import org.mylove.tprt.Tprt;
import org.mylove.tprt.compat.Cataclysm.Modifiers.*;
import slimeknights.tconstruct.library.modifiers.util.ModifierDeferredRegister;
import slimeknights.tconstruct.library.modifiers.util.StaticModifier;

public class CataclysmCompat {
    public static ModifierDeferredRegister Cataclysm_MODIFIERS = ModifierDeferredRegister.create(Tprt.MODID);

    public static final StaticModifier<ancient_sandstorm> ancient_sandstorm;
    public static final StaticModifier<burning_flames> burning_flames;
    public static final StaticModifier<cinder_slash> cinder_slash;
    public static final StaticModifier<cursed_spirit> cursed_spirit;
    public static final StaticModifier<machine_iron_casting> machine_iron_casting;
    public static final StaticModifier<mechanical_star_arm> mechanical_star_arm;
    public static final StaticModifier<mechanical_star_att> mechanical_star_att;
    public static final StaticModifier<soul_dodge> soul_dodge;
    public static final StaticModifier<Soul_puncture> Soul_puncture;
    public static final StaticModifier<strong_but_pliable> strong_but_pliable;
    static {
        ancient_sandstorm = Cataclysm_MODIFIERS.register("ancient_sandstorm", ancient_sandstorm::new);
        burning_flames = Cataclysm_MODIFIERS.register("burning_flames", burning_flames::new);
        cinder_slash = Cataclysm_MODIFIERS.register("cinder_slash", cinder_slash::new);
        cursed_spirit = Cataclysm_MODIFIERS.register("cursed_spirit", cursed_spirit::new);
        machine_iron_casting = Cataclysm_MODIFIERS.register("machine_iron_casting", machine_iron_casting::new);
        mechanical_star_arm = Cataclysm_MODIFIERS.register("mechanical_star_arm", mechanical_star_arm::new);
        mechanical_star_att = Cataclysm_MODIFIERS.register("mechanical_star_att", mechanical_star_att::new);
        soul_dodge = Cataclysm_MODIFIERS.register("soul_dodge", soul_dodge::new);
        Soul_puncture = Cataclysm_MODIFIERS.register("soul_puncture", Soul_puncture::new);
        strong_but_pliable = Cataclysm_MODIFIERS.register("strong_but_pliable", strong_but_pliable::new);
    }
}
