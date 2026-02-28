package org.mylove.tprt.registries;

import slimeknights.tconstruct.library.modifiers.ModifierId;

public class ModifierIds {

    public static final ModifierId STRONG_BUT_PLIABLE = id("strong_but_pliable");
    public static final ModifierId ANCIENT_SANDSTORM = id("ancient_sandstorm");
    public static final ModifierId mechanical_star_arm = id("mechanical_star_arm");
    public static final ModifierId Arrogant = id("arrogant");
    public static final ModifierId Tide_Guardian = id("tide_guardian");
    public static final ModifierId Storm_incarnation_arm = id("storm_incarnation_arm");
    public static final ModifierId soul_dodge = id("soul_dodge");

    private ModifierIds() {
    }

    public static ModifierId id(String name) {
        return new ModifierId("tprt", name);
    }
}
