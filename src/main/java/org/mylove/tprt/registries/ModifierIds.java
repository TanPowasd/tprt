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
    public static final ModifierId Undead_Revive = id("undead_revive");
    public static final ModifierId double_or_none = id("double_or_none");
    public static final ModifierId Monstrous = id("monstrous");
    public static final ModifierId amethyst_of_the_earth = id("amethyst_of_the_earth");
    public static final ModifierId Ender_Armor = id("ender_armor");
    public static final ModifierId Glow_armor = id("glow_armor");
    public static final ModifierId Immolate = id("immolate");
    public static final ModifierId The_dragon_lord = id("the_dragon_lord");

    public static final ModifierId netherite_spell_book = id("netherite_spell_book");
    public static final ModifierId gold_spell_book = id("gold_spell_book");
    public static final ModifierId evoker_spell_book = id("evoker_spell_book");
    public static final ModifierId blaze_spell_book = id("blaze_spell_book");
    public static final ModifierId necronomicon_spell_book = id("necronomicon_spell_book");
    public static final ModifierId dragonskin_spell_book = id("dragonskin_spell_book");
    public static final ModifierId villager_spell_book = id("villager_spell_book");
    public static final ModifierId druidic_spell_book = id("druidic_spell_book");
    public static final ModifierId cursed_doll_spell_book = id("cursed_doll_spell_book");
    public static final ModifierId ice_spell_book = id("ice_spell_book");
    public static final ModifierId the_source_of_all_spells = id("the_source_of_all_spells");

    private ModifierIds() {
    }

    public static ModifierId id(String name) {
        return new ModifierId("tprt", name);
    }
}
