package org.mylove.tprt.compat.IronsSpellBooks;

import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraftforge.fluids.ForgeFlowingFluid;
import org.mylove.tprt.Tprt;
import org.mylove.tprt.compat.IronsSpellBooks.Modifiers.*;
import org.mylove.tprt.compat.IronsSpellBooks.Modifiers.Curios.*;
import org.mylove.tprt.registries.ModFluids;
import org.mylove.tprt.registries.item.TPRTItemUtils;
import slimeknights.mantle.registration.deferred.FluidDeferredRegister;
import slimeknights.mantle.registration.object.FluidObject;
import slimeknights.mantle.registration.object.ItemObject;
import slimeknights.tconstruct.common.registration.ItemDeferredRegisterExtension;
import slimeknights.tconstruct.library.modifiers.util.ModifierDeferredRegister;
import slimeknights.tconstruct.library.modifiers.util.StaticModifier;
import slimeknights.tconstruct.library.tools.definition.ToolDefinition;
import slimeknights.tconstruct.library.tools.helper.ToolBuildHandler;
import slimeknights.tconstruct.library.tools.item.ModifiableItem;
import slimeknights.tconstruct.tools.item.ModifiableSwordItem;

public class IssCompat {

    public static final FluidDeferredRegister ISS_FLUIDS=new FluidDeferredRegister(Tprt.MODID);
    //lr特供 铁魔法联动流体 (熔融派瑞姆 / 熔融奥术金属 / 熔融源质合金 / 熔融濡湿骑士)
    public static final FluidObject<ForgeFlowingFluid> molten_pyrium=ModFluids.registerHotFluid(ISS_FLUIDS,"molten_pyrium",2000,8,5,5,false);
    public static final FluidObject<ForgeFlowingFluid> molten_arcane_metal=ModFluids.registerHotFluid(ISS_FLUIDS,"molten_arcane_metal",1000,4,5,2,false);
    public static final FluidObject<ForgeFlowingFluid> SOURCE_ALLOY_FLUID=ModFluids.registerHotFluid(ISS_FLUIDS,"source_alloy_fluid",3500,4,5,2,false);
    public static final FluidObject<ForgeFlowingFluid> MOLTEN_DARK_KNIGHT=ModFluids.registerHotFluid(ISS_FLUIDS,"molten_dark_knight",2255,4,5,2,false);

    public static ModifierDeferredRegister Iss_MODIFIERS = ModifierDeferredRegister.create(Tprt.MODID);
    /**
     * 铁魔法联动物品专用注册器: 和词条 ({@link #Iss_MODIFIERS}) 与流体 ({@link #ISS_FLUIDS}) 一样,
     * 只有检测到铁魔法时才会在 Tprt 里挂到事件总线上 —— 没装就完全不注册,
     * 所以注法者既不在注册表里, 也不会出现在创造物品栏。
     */
    public static final ItemDeferredRegisterExtension ISS_ITEMS = new ItemDeferredRegisterExtension(Tprt.MODID);
    /** 注法者的工具定义 (跟物品一起, 只在装了铁魔法时创建) */
    public static final ToolDefinition MAGIC_BLADE = ToolDefinition.create(Tprt.getResource("magic_blade"));
    //lr特供 注法者 (铁魔法联动)
    public static final ItemObject<ModifiableItem> magic_blade = ISS_ITEMS.register("magic_blade",
            ()->new ModifiableSwordItem(TPRTItemUtils.UNSTACKABLE_PROPS, MAGIC_BLADE));

    //lr特供 源质合金 / 濡湿骑士锭 (铁魔法联动材料, 材料定义里已带 irons_spellbooks 条件)
    public static final ItemObject<Item> SOURCE_ALLOY = ISS_ITEMS.register("source_alloy",
            ()->new Item(new Item.Properties()));
    public static final ItemObject<Item> DARK_KNIGHT_INGOT = ISS_ITEMS.register("dark_knight_ingot",
            ()->new Item(new Item.Properties()));

    /** 创造物品栏条目: 由 ItemsRegistry 在检测到铁魔法时调用 */
    public static void addToolTabItems(CreativeModeTab.Output tab) {
        ToolBuildHandler.addVariants(tab::accept, magic_blade.get(), "");
    }
    public static void addCommonTabItems(CreativeModeTab.Output tab) {
        tab.accept(SOURCE_ALLOY);
        tab.accept(DARK_KNIGHT_INGOT);
    }

    public static final StaticModifier<enchanting> enchanting;
    public static StaticModifier<Magic_Sublimation_Range> Magic_Sublimation_Range;
    public static StaticModifier<Magic_sublimation> Magic_sublimation;
    public static StaticModifier<human_turpentine> Human_turpentine;
    public static StaticModifier<Release_spells> Release_spells;
    public static StaticModifier<Immolate> ImmoLate;
    public static StaticModifier<Lightning_upgrade> Lightning_upgrade;
    public static StaticModifier<Imbued> Imbued;
    public static StaticModifier<light_the_fire> light_the_fire;
    public static StaticModifier<flaming_torus> flaming_torus;
    public static StaticModifier<fire_flow> fire_flow;
    public static StaticModifier<flame_paradise> flame_paradise;

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
    public static StaticModifier<bloodstain> bloodstain;
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
        light_the_fire = Iss_MODIFIERS.register("light_the_fire", light_the_fire::new);
        flaming_torus = Iss_MODIFIERS.register("flaming_torus", flaming_torus::new);
        fire_flow = Iss_MODIFIERS.register("fire_flow", fire_flow::new);
        flame_paradise = Iss_MODIFIERS.register("flame_paradise", flame_paradise::new);

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
        bloodstain = Iss_MODIFIERS.register("bloodstain", bloodstain::new);
        source_alloy_charm = Iss_MODIFIERS.register("source_alloy_charm", source_alloy_charm::new);
        source_alloy_spell_cloth = Iss_MODIFIERS.register("source_alloy_spell_cloth", source_alloy_spell_cloth::new);
    }
}

