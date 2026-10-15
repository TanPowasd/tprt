package org.mylove.tprt.registries;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraftforge.common.SoundActions;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.ForgeFlowingFluid;
import org.mylove.tprt.Tprt;
import slimeknights.mantle.registration.deferred.FluidDeferredRegister;
import slimeknights.mantle.registration.object.FlowingFluidObject;
import slimeknights.tconstruct.TConstruct;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ModFluids {
    public static final FluidDeferredRegister FLUIDS=new FluidDeferredRegister(Tprt.MODID);
    protected static Map<FlowingFluidObject<ForgeFlowingFluid>,Boolean> FLUID_MAP = new HashMap<>();
    public static Set<FlowingFluidObject<ForgeFlowingFluid>> getFluids(){
        return FLUID_MAP.keySet();
    }
    public static Map<FlowingFluidObject<ForgeFlowingFluid>,Boolean> getFluidMap(){
        return FLUID_MAP;
    }
    /** 供各联动兼容类复用: 用 Mantle 的 FluidBuilder 注册一个高温流体 (地幔的 burningBlock 生成燃烧液块; 同时登记进 FLUID_MAP, 注册器由调用方提供) */
    public static FlowingFluidObject<ForgeFlowingFluid> registerHotFluid(FluidDeferredRegister register,String name,int temp,int lightLevel,int burnTime,float damage,boolean gas){
        FlowingFluidObject<ForgeFlowingFluid> object = register.register(name).type(hot(name,temp,gas)).bucket().burningBlock(MapColor.COLOR_GRAY,lightLevel,burnTime,damage).commonTag().flowing();
        FLUID_MAP.put(object,gas);
        return object;
    }
    //锻造钢流体 -> 已移到 CreateCompat.FORGED_STEEL_FLUID (机械动力联动材料)
    public static final FlowingFluidObject<ForgeFlowingFluid>MOLTEN_ADVANCED_MANYULLYN=registerHotFluid(FLUIDS,"molten_advanced_manyullyn",2000,4,5,2,false);
    public static final FlowingFluidObject<ForgeFlowingFluid>molten_radiant_knightmetal=registerHotFluid(FLUIDS,"molten_radiant_knightmetal",2000,12,5,15,false);
    //联动流体已移到各自兼容类的注册器:
    //  黑钢 / 远古金属                -> CataclysmCompat.CATACLYSM_FLUIDS
    //  奥术金属 / 源质合金 / 濡湿骑士 -> IssCompat.ISS_FLUIDS
    //  龙神钢                        -> Iceandfire_Compat.ICEANDFIRE_FLUIDS
    private static FluidType.Properties hot(String name, int Temp, boolean gas) {
        return FluidType.Properties.create().density(gas?-2000:2000).viscosity(10000).temperature(Temp)
                .descriptionId(TConstruct.makeDescriptionId("fluid", name))
                .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL_LAVA)
                .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY_LAVA)
                .canSwim(false).canDrown(false)
                .pathType(BlockPathTypes.LAVA).adjacentPathType(null);
    }
    public static void register(IEventBus eventBus){
        FLUIDS.register(eventBus);
    }
}
