package org.mylove.tprt.registries;

import slimeknights.tconstruct.library.modifiers.ModifierId;

public class ModifierIds {

    public static final ModifierId STRONG_BUT_PLIABLE = id("strong_but_pliable");
    public static final ModifierId ANCIENT_SANDSTORM = id("ancient_sandstorm");

    private ModifierIds() {
    }

    private static ModifierId id(String name) {
        return new ModifierId("tprt", name);
    }
}
