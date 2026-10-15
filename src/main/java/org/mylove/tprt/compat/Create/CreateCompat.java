package org.mylove.tprt.compat.Create;

import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraftforge.fluids.ForgeFlowingFluid;
import org.mylove.tprt.Modifiers.curio.be_thoroughly_tempered_curios;
import org.mylove.tprt.Tprt;
import org.mylove.tprt.common.modifier.modifierModule.CuriosModifier;
import org.mylove.tprt.registries.ModFluids;
import slimeknights.mantle.registration.deferred.FluidDeferredRegister;
import slimeknights.mantle.registration.object.FlowingFluidObject;
import slimeknights.mantle.registration.object.ItemObject;
import slimeknights.tconstruct.common.registration.ItemDeferredRegisterExtension;
import slimeknights.tconstruct.library.modifiers.util.ModifierDeferredRegister;
import slimeknights.tconstruct.library.modifiers.util.StaticModifier;

/**
 * 机械动力 (Create) 联动: 锻造钢的物品 / 流体 / 专属特性。
 * 与灾变等联动材料一样, 只有装了 Create 时才会在 Tprt 里挂到事件总线上
 * (没装就不注册, 也不会出现在创造栏里)。
 */
public class CreateCompat {

    public static final ModifierDeferredRegister CREATE_MODIFIERS = ModifierDeferredRegister.create(Tprt.MODID);
    public static final ItemDeferredRegisterExtension CREATE_ITEMS = new ItemDeferredRegisterExtension(Tprt.MODID);
    public static final FluidDeferredRegister CREATE_FLUIDS = new FluidDeferredRegister(Tprt.MODID);

    //锻造钢
    public static final ItemObject<Item> FORGED_STEEL = CREATE_ITEMS.register("forged_steel", () -> new Item(new Item.Properties()));
    public static final FlowingFluidObject<ForgeFlowingFluid> FORGED_STEEL_FLUID = ModFluids.registerHotFluid(CREATE_FLUIDS, "forged_steel_fluid", 1475, 4, 5, 2, false);
    /** 锻造钢专属特性 (调质处理): 材料本身依赖 Create, 所以特性也一起条件注册 */
    public static final StaticModifier<CuriosModifier> be_thoroughly_tempered_curios = CREATE_MODIFIERS.register("be_thoroughly_tempered_curios", be_thoroughly_tempered_curios::new);

    /** 创造物品栏条目: 由 ItemsRegistry 在检测到机械动力时调用 */
    public static void addCommonTabItems(CreativeModeTab.Output tab) {
        tab.accept(FORGED_STEEL);
    }
}