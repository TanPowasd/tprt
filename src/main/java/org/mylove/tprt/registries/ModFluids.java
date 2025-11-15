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
import slimeknights.mantle.registration.object.FluidObject;
import slimeknights.tconstruct.TConstruct;
import static slimeknights.tconstruct.fluids.block.BurningLiquidBlock.createBurning;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ModFluids {
    public static final FluidDeferredRegister FLUIDS=new FluidDeferredRegister(Tprt.MODID);
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
    public static final FluidObject<ForgeFlowingFluid>MIXEDDRAGON_FLUID=registerHotFluid(FLUIDS,"mixeddragon_fluid",4000,15,5,200,false);
    public static final FluidObject<ForgeFlowingFluid>FORGED_STEEL_FLUID=registerHotFluid(FLUIDS,"forged_steel_fluid",1475,4,5,2,false);
    public static final FluidObject<ForgeFlowingFluid>SOURCE_ALLOY_FLUID=registerHotFluid(FLUIDS,"source_alloy_fluid",3500,4,5,2,false);
    public static final FluidObject<ForgeFlowingFluid>MOLTEN_ADVANCED_MANYULLYN=registerHotFluid(FLUIDS,"molten_advanced_manyullyn",2000,4,5,2,false);
    public static final FluidObject<ForgeFlowingFluid>MOLTEN_BLACK_STEEL=registerHotFluid(FLUIDS,"molten_black_steel",1000,4,5,2,false);
    public static final FluidObject<ForgeFlowingFluid>MOLTEN_ANCIENT_METAL=registerHotFluid(FLUIDS,"molten_ancient_metal",1250,4,5,2,false);
    public static final FluidObject<ForgeFlowingFluid>MOLTEN_ARCANE_METAL=registerHotFluid(FLUIDS,"molten_arcane_metal",1000,4,5,2,false);
    public static final FluidObject<ForgeFlowingFluid>MOLTEN_DARK_KNIGHT=registerHotFluid(FLUIDS,"molten_dark_knight",2255,4,5,2,false);
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
