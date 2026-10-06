package org.mylove.tprt.compat.Iceandfire;

import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraftforge.fluids.ForgeFlowingFluid;
import org.mylove.tprt.Tprt;
import org.mylove.tprt.compat.Iceandfire.Modifires.The_dragon_lord;
import org.mylove.tprt.compat.Iceandfire.Modifires.Tide_Guardian;
import org.mylove.tprt.compat.Iceandfire.Modifires.the_dragon_power;
import org.mylove.tprt.registries.ModFluids;
import slimeknights.mantle.registration.deferred.FluidDeferredRegister;
import slimeknights.mantle.registration.object.FluidObject;
import slimeknights.mantle.registration.object.ItemObject;
import slimeknights.tconstruct.common.registration.ItemDeferredRegisterExtension;
import slimeknights.tconstruct.library.modifiers.util.ModifierDeferredRegister;
import slimeknights.tconstruct.library.modifiers.util.StaticModifier;

public class Iceandfire_Compat {
    public static ModifierDeferredRegister Iceandfire_MODIFIERS = ModifierDeferredRegister.create(Tprt.MODID);

    /** 冰与火联动物品专用注册器: 只在装了冰与火时才会在 Tprt 里挂到事件总线上 */
    public static final ItemDeferredRegisterExtension ICEANDFIRE_ITEMS = new ItemDeferredRegisterExtension(Tprt.MODID);
    //lr特供 复合龙鳞 / 龙神钢锭 (冰与火联动材料, 材料定义里已带 iceandfire 条件)
    public static final ItemObject<Item> Composite_dragon_scales = ICEANDFIRE_ITEMS.register("composite_dragon_scales", () -> new Item(new Item.Properties()));
    public static final ItemObject<Item> MIXEDDRAGON = ICEANDFIRE_ITEMS.register("mixeddragon", () -> new Item(new Item.Properties()));

    /** 冰与火联动流体专用注册器 (熔融龙神钢) */
    public static final FluidDeferredRegister ICEANDFIRE_FLUIDS = new FluidDeferredRegister(Tprt.MODID);
    public static final FluidObject<ForgeFlowingFluid> MIXEDDRAGON_FLUID = ModFluids.registerHotFluid(ICEANDFIRE_FLUIDS,"mixeddragon_fluid",4000,15,5,200,false);

    /** 创造物品栏条目: 由 ItemsRegistry 在检测到冰与火时调用 */
    public static void addCommonTabItems(CreativeModeTab.Output tab) {
        tab.accept(Composite_dragon_scales);
        tab.accept(MIXEDDRAGON);
    }

    public static StaticModifier<the_dragon_power> the_dragon_power;
    public static StaticModifier<The_dragon_lord> The_dragon_lord;
    public static StaticModifier<Tide_Guardian> Tide_Guardian;
    static {
        the_dragon_power = Iceandfire_MODIFIERS.register("the_dragon_power", the_dragon_power::new);
        The_dragon_lord = Iceandfire_MODIFIERS.register("the_dragon_lord", The_dragon_lord::new);
        Tide_Guardian = Iceandfire_MODIFIERS.register("tide_guardian", Tide_Guardian::new);
    }
}
