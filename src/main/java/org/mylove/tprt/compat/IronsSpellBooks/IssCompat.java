package org.mylove.tprt.compat.IronsSpellBooks;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraftforge.common.SoundActions;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.ForgeFlowingFluid;
import org.mylove.tprt.Tprt;
import org.mylove.tprt.compat.IronsSpellBooks.Modifiers.*;
import org.mylove.tprt.compat.IronsSpellBooks.Modifiers.Curios.*;
import slimeknights.mantle.registration.deferred.FluidDeferredRegister;
import slimeknights.mantle.registration.object.FluidObject;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.modifiers.util.ModifierDeferredRegister;
import slimeknights.tconstruct.library.modifiers.util.StaticModifier;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static slimeknights.tconstruct.fluids.block.BurningLiquidBlock.createBurning;

public class IssCompat {

    public static final FluidDeferredRegister ISS_FLUIDS=new FluidDeferredRegister(Tprt.MODID);
    protected static Map<FluidObject<ForgeFlowingFluid>,Boolean> FLUID_MAP = new HashMap<>();
    public static Set<FluidObject<ForgeFlowingFluid>> getFluids(){
        return FLUID_MAP.keySet();
    }
    public static Map<FluidObject<ForgeFlowingFluid>,Boolean> getFluidMap(){
        return FLUID_MAP;
    }
    private static FluidObject<ForgeFlowingFluid> registerHotFluid(FluidDeferredRegister register,String name,int temp,int lightLevel,int burnTime,float damage,boolean gas){
        FluidObject<ForgeFlowingFluid> object = register.register(name).type(hot(name,temp,gas)).bucket().block(createBurning(MapColor.COLOR_GRAY,lightLevel,burnTime,damage)).commonTag().flowing();
        FLUID_MAP.put(object,gas);
        return object;
    }
    public static final FluidObject<ForgeFlowingFluid>molten_pyrium=registerHotFluid(ISS_FLUIDS,"molten_pyrium",2000,8,5,5,false);

    private static FluidType.Properties hot(String name, int Temp, boolean gas) {
        return FluidType.Properties.create().density(gas?-2000:2000).viscosity(10000).temperature(Temp)
                .descriptionId(TConstruct.makeDescriptionId("fluid", name))
                .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL_LAVA)
                .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY_LAVA)
                .canSwim(false).canDrown(false)
                .pathType(BlockPathTypes.LAVA).adjacentPathType(null);
    }

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

