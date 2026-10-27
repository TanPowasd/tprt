package org.mylove.tprt.compat.Create;

import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraftforge.fluids.ForgeFlowingFluid;
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
 * 鏈烘鍔ㄥ姏 (Create) 鑱斿姩: 閿婚€犻挗鐨勭墿鍝?/ 娴佷綋 / 涓撳睘鐗规€с€? * 涓庣伨鍙樼瓑鑱斿姩鏉愭枡涓€鏍? 鍙湁瑁呬簡 Create 鏃舵墠浼氬湪 Tprt 閲屾寕鍒颁簨浠舵€荤嚎涓? * (娌¤灏变笉娉ㄥ唽, 涔熶笉浼氬嚭鐜板湪鍒涢€犳爮閲?銆? */
public class CreateCompat {

    public static final ModifierDeferredRegister CREATE_MODIFIERS = ModifierDeferredRegister.create(Tprt.MODID);
    public static final ItemDeferredRegisterExtension CREATE_ITEMS = new ItemDeferredRegisterExtension(Tprt.MODID);
    public static final FluidDeferredRegister CREATE_FLUIDS = new FluidDeferredRegister(Tprt.MODID);

    //閿婚€犻挗
    public static final ItemObject<Item> FORGED_STEEL = CREATE_ITEMS.register("forged_steel", () -> new Item(new Item.Properties()));
    public static final FlowingFluidObject<ForgeFlowingFluid> FORGED_STEEL_FLUID = ModFluids.registerHotFluid(CREATE_FLUIDS, "forged_steel_fluid", 1475, 4, 5, 2, false);
    /** 閿婚€犻挗涓撳睘鐗规€?(璋冭川澶勭悊): 鏉愭枡鏈韩渚濊禆 Create, 鎵€浠ョ壒鎬т篃涓€璧锋潯浠舵敞鍐?*/

    /** 鍒涢€犵墿鍝佹爮鏉＄洰: 鐢?ItemsRegistry 鍦ㄦ娴嬪埌鏈烘鍔ㄥ姏鏃惰皟鐢?*/
    public static void addCommonTabItems(CreativeModeTab.Output tab) {
        tab.accept(FORGED_STEEL);
    }
}
