package org.mylove.tprt.registries;

import net.minecraftforge.eventbus.api.IEventBus;
import org.mylove.tprt.Tprt;
import org.mylove.tprt.tags.*;
import org.mylove.tprt.tags.modifier.*;
import slimeknights.tconstruct.library.modifiers.util.ModifierDeferredRegister;
import slimeknights.tconstruct.library.modifiers.util.StaticModifier;

public class ModifierRegistry {
    private static final ModifierDeferredRegister MODIFIERS = ModifierDeferredRegister.create(Tprt.MODID);
    public static void register(IEventBus eventBus) {
        MODIFIERS.register(eventBus);
    }

    public static final StaticModifier<lastone> lastone = MODIFIERS.register("lastone", lastone::new);
    public static final StaticModifier<for_the_love> for_the_love = MODIFIERS.register("for_the_love", for_the_love::new);
    public static final StaticModifier<sacred_sacrifice> sacred_sacrifice = MODIFIERS.register("sacred_sacrifice", sacred_sacrifice::new);
    public static final StaticModifier<burning_flames> burning_flames = MODIFIERS.register("burning_flames", burning_flames::new);
    public static final StaticModifier<wrath_of_flames> wrath_of_flames = MODIFIERS.register("wrath_of_flames", wrath_of_flames::new);
    public static final StaticModifier<real_attack> real_attack = MODIFIERS.register("real_attack",real_attack::new);
    public static final StaticModifier<cursed_spirit> cursed_spirit = MODIFIERS.register("cursed_spirit",cursed_spirit::new);
    public static final StaticModifier<double_or_none> double_or_none = MODIFIERS.register("double_or_none",double_or_none::new);
    public static final StaticModifier<soul_dodge> soul_dodge = MODIFIERS.register("soul_dodge",soul_dodge::new);
    public static final StaticModifier<disruption_break> disruption_break = MODIFIERS.register("disruption_break",disruption_break::new);
    public static final StaticModifier<lucky_strike> luck_strike = MODIFIERS.register("luck_strike",lucky_strike::new);
    public static final StaticModifier<flying_sword_tag> flying_sword = MODIFIERS.register("flying_sword", flying_sword_tag::new);
    public static final StaticModifier<nine_sword_tag> nine_sword = MODIFIERS.register("nine_sword", nine_sword_tag::new);
    public static final StaticModifier<test_entity_gen> test_entity_gen = MODIFIERS.register("test_entity_gen", test_entity_gen::new);
    public static final StaticModifier<test_entity_uuid> test_entity_uuid = MODIFIERS.register("test_entity_uuid", test_entity_uuid::new);
    public static final StaticModifier<test_ellipse> test_ellipse = MODIFIERS.register("test_ellipse", test_ellipse::new);
    public static final StaticModifier<test_collide_hurt> test_collide_hurt = MODIFIERS.register("test_collide_hurt", test_collide_hurt::new);
}