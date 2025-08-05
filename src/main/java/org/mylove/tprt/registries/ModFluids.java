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
    public static final FluidObject<ForgeFlowingFluid>LRIRON_FLUID=registerHotFluid(FLUIDS,"lriron_fluid",3999,4,5,2f,false);

    public static final FluidObject<ForgeFlowingFluid>KING_OF_FOREST_FLUID=registerHotFluid(FLUIDS,"king_of_forest_fluid",2001,4,5,2f,false);
    public static final FluidObject<ForgeFlowingFluid>KING_OF_FIREANDICE_FLUID=registerHotFluid(FLUIDS,"king_of_fireandice_fluid",2001,4,5,2f,false);
    public static final FluidObject<ForgeFlowingFluid>KING_OF_MAGIC_FLUID=registerHotFluid(FLUIDS,"king_of_magic_fluid",2001,4,5,2f,false);
    public static final FluidObject<ForgeFlowingFluid>KING_OF_MC_FLUID=registerHotFluid(FLUIDS,"king_of_mc_fluid",2001,4,5,2f,false);
    public static final FluidObject<ForgeFlowingFluid>KING_OF_CATACLYSM_FLUID=registerHotFluid(FLUIDS,"king_of_cataclysm_fluid",2001,4,5,2f,false);
    public static final FluidObject<ForgeFlowingFluid>TANPOWASD_FLUID=registerHotFluid(FLUIDS,"tanpowasd_fluid",4000,4,5,2f,false);
    public static final FluidObject<ForgeFlowingFluid>MIXEDDRAGON_FLUID=registerHotFluid(FLUIDS,"mixeddragon_fluid",4000,4,5,2,false);
    public static final FluidObject<ForgeFlowingFluid>FORGED_STEEL_FLUID=registerHotFluid(FLUIDS,"forged_steel_fluid",1475,4,5,2,false);
    public static final FluidObject<ForgeFlowingFluid>SOURCE_ALLOY_FLUID=registerHotFluid(FLUIDS,"source_alloy_fluid",3500,4,5,2,false);
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
