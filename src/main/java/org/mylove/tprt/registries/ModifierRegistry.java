package org.mylove.tprt.registries;

import net.minecraftforge.eventbus.api.IEventBus;
import org.mylove.tprt.Modifiers.armor.Arrogant;
import org.mylove.tprt.Modifiers.armor.Glow_armor;
import org.mylove.tprt.Modifiers.common.Poisonous_explosion;
import org.mylove.tprt.Modifiers.common.Quenching_poison;
import org.mylove.tprt.Modifiers.curio.*;
import org.mylove.tprt.Modifiers.exclusive.Break_the_path;
import org.mylove.tprt.Modifiers.exclusive.Precision_stab;
import org.mylove.tprt.Modifiers.exclusive.persistence_of_nature;
import org.mylove.tprt.Modifiers.exclusive.reaper;
import org.mylove.tprt.Modifiers.melee.Shine_at_night;
import org.mylove.tprt.Modifiers.melee.advanced_greed;
import org.mylove.tprt.Modifiers.melee.all_permitted;
import org.mylove.tprt.Modifiers.melee.combat_echo;
import org.mylove.tprt.Modifiers.range.Automation;
import org.mylove.tprt.Modifiers.special.*;
import org.mylove.tprt.Modifiers.re.before_i_vanish;
import org.mylove.tprt.Modifiers.re.elegy_of_world_end;
import org.mylove.tprt.Modifiers.re.even_as_the_sun_sets;
import org.mylove.tprt.Modifiers.re.happiest_girl_in_the_world;
import org.mylove.tprt.Modifiers.re.seniorious;
import org.mylove.tprt.Modifiers.re.what_do_you_do_at_the_end_of_the_world;
import org.mylove.tprt.Tprt;
import org.mylove.tprt.common.modifier.modifierModule.CuriosModifier;
import slimeknights.tconstruct.library.modifiers.util.ModifierDeferredRegister;
import slimeknights.tconstruct.library.modifiers.util.StaticModifier;

public class ModifierRegistry {
    private static final ModifierDeferredRegister MODIFIERS = ModifierDeferredRegister.create(Tprt.MODID);
    public static final StaticModifier<org.mylove.tprt.common.modifier.LanceChargeModifier> LANCE_CHARGE =
            MODIFIERS.register("lance_charge", org.mylove.tprt.common.modifier.LanceChargeModifier::new);
    public static final StaticModifier<org.mylove.tprt.common.modifier.ThunderousMomentumModifier> THUNDEROUS_MOMENTUM =
            MODIFIERS.register("thunderous_momentum", org.mylove.tprt.common.modifier.ThunderousMomentumModifier::new);
    /** Marker only: the stacking damage bonus is applied by {@link org.mylove.tprt.common.modifier.LanceChargeModifier}. */
    public static final StaticModifier<slimeknights.tconstruct.library.modifiers.Modifier> BREAKING_THROUGH =
            MODIFIERS.register("breaking_through", slimeknights.tconstruct.library.modifiers.Modifier::new);
    /** Marker only: block breaking runs inside the charge tick, see {@link org.mylove.tprt.common.modifier.LanceWallBreak}. */
    public static final StaticModifier<org.mylove.tprt.common.modifier.WallBreakerModifier> WALL_BREAKER =
            MODIFIERS.register("wall_breaker", org.mylove.tprt.common.modifier.WallBreakerModifier::new);
    public static void register(IEventBus eventBus) {
        MODIFIERS.register(eventBus);
    }

    public static final StaticModifier<lastone> LASTONE = MODIFIERS.register("lastone", lastone::new);
    public static final StaticModifier<lucky_strike> LUCK_STRIKE = MODIFIERS.register("luck_strike",lucky_strike::new);
    public static final StaticModifier<advanced_greed> ADVANCED_GREED = MODIFIERS.register("advanced_greed",advanced_greed::new);
    public static final StaticModifier<org.mylove.tprt.Modifiers.ranged.RangedGreedModifier> RANGED_GREED =
            MODIFIERS.register("ranged_greed", org.mylove.tprt.Modifiers.ranged.RangedGreedModifier::new);
    public static final StaticModifier<org.mylove.tprt.Modifiers.curio.Overlimit> OVERLIMIT =
            MODIFIERS.register("overlimit", org.mylove.tprt.Modifiers.curio.Overlimit::new);
    public static final StaticModifier<Arrogant> Arrogant = MODIFIERS.register("arrogant",Arrogant::new);
    public static final StaticModifier<Shine_at_night> Shine_at_night = MODIFIERS.register("shine_at_night",Shine_at_night::new);
    public static final StaticModifier<Glow_armor> Glow_armor = MODIFIERS.register("glow_armor",Glow_armor::new);
    public static final StaticModifier<CuriosModifier> The_shining_sun = MODIFIERS.register("the_shining_sun",The_shining_sun::new);
    public static final StaticModifier<reaper> REAPER = MODIFIERS.register("reaper",reaper::new);
    public static final StaticModifier<Break_the_path> Break_the_path = MODIFIERS.register("break_the_path",Break_the_path::new);
    public static final StaticModifier<Poisonous_explosion> Poisonous_explosion = MODIFIERS.register("poisonous_explosion",Poisonous_explosion::new);
    public static final StaticModifier<Quenching_poison> Quenching_poison = MODIFIERS.register("quenching_poison",Quenching_poison::new);
    public static final StaticModifier<CuriosModifier> blood_of_hydra = MODIFIERS.register("blood_of_hydra", Blood_of_Hydra::new);
    public static final StaticModifier<CuriosModifier> dwarf_power = MODIFIERS.register("dwarf_power", Dwarf_Power::new);
    public static final StaticModifier<CuriosModifier> down_to_the_bone = MODIFIERS.register("down_to_the_bone", Down_to_the_Bone::new);
    public static final StaticModifier<CuriosModifier> strengthen_for_charm_endurance = MODIFIERS.register("strengthen_for_charm_endurance", Strengthen_for_charm_endurance::new);
    public static final StaticModifier<CuriosModifier> endless_life = MODIFIERS.register("endless_life", Endless_Life::new);
    public static final StaticModifier<CuriosModifier> Strengthen_for_charm_ranged = MODIFIERS.register("strengthen_for_charm_ranged", Strengthen_for_charm_ranged::new);
    public static final StaticModifier<CuriosModifier> Fortune_curio = MODIFIERS.register("fortune_curio", Fortune_curio::new);
    //閿婚€犻挗涓撳睘鐗规€?(璋冭川澶勭悊) 宸茬Щ鍒?CreateCompat: 鍙湁瑁呬簡鏈烘鍔ㄥ姏鎵嶄細娉ㄥ唽
    public static final StaticModifier<infinite_dropping> INFINITE_DROPPING = MODIFIERS.register("infinite_dropping",infinite_dropping::new);
    public static final StaticModifier<millennium> MILLENNIUM = MODIFIERS.register("millennium",millennium::new);
    public static final StaticModifier<oxidation>OXIDATION = MODIFIERS.register("oxidation",oxidation::new);
    public static final StaticModifier<tetanus>TETANUS_STATIC = MODIFIERS.register("tetanus",tetanus::new);
    public static final StaticModifier<persistence_of_nature> PERSISTENCE_OF_NATURE = MODIFIERS.register("persistence_of_nature",persistence_of_nature::new);
    public static final StaticModifier<Automation> Automation = MODIFIERS.register("automation",Automation::new);
    public static final StaticModifier<Precision_stab> Precision_stab = MODIFIERS.register("precision_stab",Precision_stab::new);
    public static final StaticModifier<all_permitted> ALL_PERMITTED = MODIFIERS.register("all_permitted",all_permitted::new);
    public static final StaticModifier<combat_echo> COMBAT_ECHO = MODIFIERS.register("combat_echo",combat_echo::new);
    public static final StaticModifier<what_do_you_do_at_the_end_of_the_world> WHAT_DO_YOU_DO_AT_THE_END_OF_THE_WORLD =
            MODIFIERS.register("what_do_you_do_at_the_end_of_the_world", what_do_you_do_at_the_end_of_the_world::new);
    public static final StaticModifier<seniorious> SENIORIOUS =
            MODIFIERS.register("seniorious", seniorious::new);
    public static final StaticModifier<happiest_girl_in_the_world> HAPPIEST_GIRL_IN_THE_WORLD =
            MODIFIERS.register("happiest_girl_in_the_world", happiest_girl_in_the_world::new);
    public static final StaticModifier<elegy_of_world_end> ELEGY_OF_WORLD_END =
            MODIFIERS.register("elegy_of_world_end", elegy_of_world_end::new);
    public static final StaticModifier<before_i_vanish> BEFORE_I_VANISH =
            MODIFIERS.register("before_i_vanish", before_i_vanish::new);
    public static final StaticModifier<even_as_the_sun_sets> EVEN_AS_THE_SUN_SETS =
            MODIFIERS.register("even_as_the_sun_sets", even_as_the_sun_sets::new);
}
