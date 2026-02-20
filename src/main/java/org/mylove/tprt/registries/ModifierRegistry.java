package org.mylove.tprt.registries;

import com.ssakura49.sakuratinker.generic.CurioModifier;
import net.minecraftforge.eventbus.api.IEventBus;
import org.mylove.tprt.Modifiers.curio.*;
import org.mylove.tprt.Tprt;
import org.mylove.tprt.Modifiers.*;
import slimeknights.tconstruct.library.modifiers.util.ModifierDeferredRegister;
import slimeknights.tconstruct.library.modifiers.util.StaticModifier;

public class ModifierRegistry {
    private static final ModifierDeferredRegister MODIFIERS = ModifierDeferredRegister.create(Tprt.MODID);
    public static void register(IEventBus eventBus) {
        MODIFIERS.register(eventBus);
    }

    public static final StaticModifier<lastone> LASTONE = MODIFIERS.register("lastone", lastone::new);
    public static final StaticModifier<for_the_love> FOR_THE_LOVE = MODIFIERS.register("for_the_love", for_the_love::new);
    public static final StaticModifier<sacred_sacrifice> SACRED_SACRIFICE = MODIFIERS.register("sacred_sacrifice", sacred_sacrifice::new);
    public static final StaticModifier<wrath_of_flames> WRATH_OF_FLAMES = MODIFIERS.register("wrath_of_flames", wrath_of_flames::new);
    public static final StaticModifier<real_attack> REAL_ATTACK = MODIFIERS.register("real_attack",real_attack::new);
    public static final StaticModifier<double_or_none> DOUBLE_OR_NONE = MODIFIERS.register("double_or_none",double_or_none::new);
    public static final StaticModifier<disruption_break> DISRUPTION_BREAK = MODIFIERS.register("disruption_break",disruption_break::new);
    public static final StaticModifier<lucky_strike> LUCK_STRIKE = MODIFIERS.register("luck_strike",lucky_strike::new);
    public static final StaticModifier<the_dragon_power> THE_DRAGON_POWER = MODIFIERS.register("the_dragon_power",the_dragon_power::new);
    public static final StaticModifier<The_dragon_lord> The_dragon_lord = MODIFIERS.register("the_dragon_lord",The_dragon_lord::new);
    public static final StaticModifier<advanced_greed> ADVANCED_GREED = MODIFIERS.register("advanced_greed",advanced_greed::new);
    public static final StaticModifier<Arrogant> Arrogant = MODIFIERS.register("arrogant",Arrogant::new);
    public static final StaticModifier<reaper> REAPER = MODIFIERS.register("reaper",reaper::new);
    public static final StaticModifier<Poisonous_explosion> Poisonous_explosion = MODIFIERS.register("poisonous_explosion",Poisonous_explosion::new);
    public static final StaticModifier<Quenching_poison> Quenching_poison = MODIFIERS.register("quenching_poison",Quenching_poison::new);
    public static final StaticModifier<Tide_Guardian> Tide_Guardian = MODIFIERS.register("tide_guardian",Tide_Guardian::new);
    public static final StaticModifier<CurioModifier> blood_of_hydra = MODIFIERS.register("blood_of_hydra", Blood_of_Hydra::new);
    public static final StaticModifier<CurioModifier> dwarf_power = MODIFIERS.register("dwarf_power", Dwarf_Power::new);
    public static final StaticModifier<CurioModifier> down_to_the_bone = MODIFIERS.register("down_to_the_bone", Down_to_the_Bone::new);
    public static final StaticModifier<CurioModifier> unreal_image = MODIFIERS.register("unreal_image", Unreal_Image::new);
    public static final StaticModifier<CurioModifier> body_like_glass = MODIFIERS.register("body_like_glass", Body_like_glass::new);
    public static final StaticModifier<CurioModifier> strengthen_for_charm_endurance = MODIFIERS.register("strengthen_for_charm_endurance", Strengthen_for_charm_endurance::new);
    public static final StaticModifier<CurioModifier> endless_life = MODIFIERS.register("endless_life", Endless_Life::new);
    public static final StaticModifier<d2_half_truths> D2_HALF_TRUTHS = MODIFIERS.register("d2_half_truths",d2_half_truths::new);
    public static final StaticModifier<d2_lucky_pants> D2_LUCKY_PANTS = MODIFIERS.register("d2_lucky_pants",d2_lucky_pants::new);
    public static final StaticModifier<dynamic_attack> DYNAMIC_ATTACK = MODIFIERS.register("dynamic_attack",dynamic_attack::new);
    public static final StaticModifier<dynamic_defense> DYNAMIC_DEFENSE = MODIFIERS.register("dynamic_defense",dynamic_defense::new);
    public static final StaticModifier<infinite_dropping> INFINITE_DROPPING = MODIFIERS.register("infinite_dropping",infinite_dropping::new);
    public static final StaticModifier<millennium> MILLENNIUM = MODIFIERS.register("millennium",millennium::new);
    public static final StaticModifier<zenith_first_fractal>ZENITH_FIRST_FRACTAL = MODIFIERS.register("zenith_first_fractal",zenith_first_fractal::new);
    public static final StaticModifier<oxidation>OXIDATION = MODIFIERS.register("oxidation",oxidation::new);
    public static final StaticModifier<tetanus>TETANUS_STATIC = MODIFIERS.register("tetanus",tetanus::new);
    public static final StaticModifier<persistence_of_nature> PERSISTENCE_OF_NATURE = MODIFIERS.register("persistence_of_nature",persistence_of_nature::new);
    public static final StaticModifier<Automation> Automation = MODIFIERS.register("automation",Automation::new);
    //public static final StaticModifier<CurioModifier> FORTUNE_CURIO = MODIFIERS.register("fortune_curio", FortuneCurio::new);//
}