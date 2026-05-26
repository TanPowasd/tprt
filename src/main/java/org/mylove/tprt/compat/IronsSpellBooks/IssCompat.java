package org.mylove.tprt.compat.IronsSpellBooks;

import org.mylove.tprt.Tprt;
import org.mylove.tprt.compat.IronsSpellBooks.Modifiers.*;
import org.mylove.tprt.compat.IronsSpellBooks.Modifiers.Curios.*;
import slimeknights.tconstruct.library.modifiers.util.ModifierDeferredRegister;
import slimeknights.tconstruct.library.modifiers.util.StaticModifier;

public class IssCompat {
    public static ModifierDeferredRegister Iss_MODIFIERS = ModifierDeferredRegister.create(Tprt.MODID);
/*
    public static final RegistryObject<Item> magic_blade= Iss_ITEMS.register("magic_blade",
            ()->new ModifiableSwordItem(TPRTItemUtils.UNSTACKABLE_PROPS, TPRTToolDefinitions.MAGIC_BLADE));
    public static final RegistryObject<Item>SOURCE_ALLOY=Iss_ITEMS.register("source_alloy",
            ()->new Item(new Item.Properties()));
    public static final RegistryObject<Item>DARK_KNIGHT_INGOT=Iss_ITEMS.register("dark_knight_ingot",
            ()->new Item(new Item.Properties()));
*/
    public static final StaticModifier<enchanting> enchanting;
    public static StaticModifier<Magic_Sublimation_Range> Magic_Sublimation_Range;
    public static StaticModifier<Magic_sublimation> Magic_sublimation;
    public static StaticModifier<human_turpentine> Human_turpentine;
    public static StaticModifier<Release_spells> Release_spells;
    public static StaticModifier<Immolate> ImmoLate;
    public static StaticModifier<Lightning_upgrade> Lightning_upgrade;
    public static StaticModifier<Imbued> Imbued;

    public static final StaticModifier<BreakPhantom> BreakPhantom;
    public static final StaticModifier<Thorough> Thorough;
    public static StaticModifier<netherite_spell_book> netherite_spell_book;
    public static StaticModifier<gold_spell_book> gold_spell_book;
    public static StaticModifier<evoker_spell_book> evoker_spell_book;
    public static StaticModifier<blaze_spell_book> blaze_spell_book;
    public static StaticModifier<necronomicon_spell_book> necronomicon_spell_book;
    public static StaticModifier<dragonskin_spell_book> dragonskin_spell_book;
    public static StaticModifier<villager_spell_book> villager_spell_book;
    public static StaticModifier<druidic_spell_book> druidic_spell_book;
    public static StaticModifier<cursed_doll_spell_book> cursed_doll_spell_book;
    public static StaticModifier<ice_spell_book> ice_spell_book;
    public static StaticModifier<the_source_of_all_spells> the_source_of_all_spells;
    public static StaticModifier<strengthen_for_charm_spell_power> strengthen_for_charm_spell_power;
    public static StaticModifier<spell_power_improved_curios> spell_power_improved_curios;
    public static StaticModifier<lightweight_spell> lightweight_spell;
    public static StaticModifier<mana_regeneration> mana_regeneration;
    public static StaticModifier<ruthless_and_tyrannical_spell> ruthless_and_tyrannical_spell;
    public static StaticModifier<magician> magician;
    public static StaticModifier<fountain_magic> fountain_magic;
    public static StaticModifier<elemental_mastery> elemental_mastery;
    public static StaticModifier<source_alloy_charm> source_alloy_charm;
    public static StaticModifier<source_alloy_spell_cloth> source_alloy_spell_cloth;
    static {
        enchanting = Iss_MODIFIERS.register("enchanting", enchanting::new);
        Magic_Sublimation_Range = Iss_MODIFIERS.register("magic_sublimation_range", Magic_Sublimation_Range::new);
        Magic_sublimation = Iss_MODIFIERS.register("magic_sublimation", Magic_sublimation::new);
        Human_turpentine = Iss_MODIFIERS.register("human_turpentine", human_turpentine::new);
        Release_spells = Iss_MODIFIERS.register("release_spells", Release_spells::new);
        ImmoLate = Iss_MODIFIERS.register("immolate", Immolate::new);
        Lightning_upgrade = Iss_MODIFIERS.register("lightning_upgrade", Lightning_upgrade::new);
        Imbued = Iss_MODIFIERS.register("imbued", Imbued::new);

        BreakPhantom = Iss_MODIFIERS.register("breakphantom", BreakPhantom::new);
        Thorough = Iss_MODIFIERS.register("thorough", Thorough::new);
        netherite_spell_book = Iss_MODIFIERS.register("netherite_spell_book", netherite_spell_book::new);
        gold_spell_book = Iss_MODIFIERS.register("gold_spell_book", gold_spell_book::new);
        evoker_spell_book = Iss_MODIFIERS.register("evoker_spell_book", evoker_spell_book::new);
        blaze_spell_book = Iss_MODIFIERS.register("blaze_spell_book", blaze_spell_book::new);
        necronomicon_spell_book = Iss_MODIFIERS.register("necronomicon_spell_book", necronomicon_spell_book::new);
        dragonskin_spell_book = Iss_MODIFIERS.register("dragonskin_spell_book", dragonskin_spell_book::new);
        villager_spell_book = Iss_MODIFIERS.register("villager_spell_book", villager_spell_book::new);
        druidic_spell_book = Iss_MODIFIERS.register("druidic_spell_book", druidic_spell_book::new);
        cursed_doll_spell_book = Iss_MODIFIERS.register("cursed_doll_spell_book", cursed_doll_spell_book::new);
        ice_spell_book = Iss_MODIFIERS.register("ice_spell_book", ice_spell_book::new);
        the_source_of_all_spells = Iss_MODIFIERS.register("the_source_of_all_spells", the_source_of_all_spells::new);
        strengthen_for_charm_spell_power = Iss_MODIFIERS.register("strengthen_for_charm_spell_power", strengthen_for_charm_spell_power::new);
        spell_power_improved_curios = Iss_MODIFIERS.register("spell_power_improved_curios", spell_power_improved_curios::new);
        lightweight_spell = Iss_MODIFIERS.register("lightweight_spell", lightweight_spell::new);
        mana_regeneration = Iss_MODIFIERS.register("mana_regeneration", mana_regeneration::new);
        ruthless_and_tyrannical_spell = Iss_MODIFIERS.register("ruthless_and_tyrannical_spell", ruthless_and_tyrannical_spell::new);
        magician = Iss_MODIFIERS.register("magician", magician::new);
        fountain_magic = Iss_MODIFIERS.register("fountain_magic", fountain_magic::new);
        elemental_mastery = Iss_MODIFIERS.register("elemental_mastery", elemental_mastery::new);
        source_alloy_charm = Iss_MODIFIERS.register("source_alloy_charm", source_alloy_charm::new);
        source_alloy_spell_cloth = Iss_MODIFIERS.register("source_alloy_spell_cloth", source_alloy_spell_cloth::new);
    }
}

