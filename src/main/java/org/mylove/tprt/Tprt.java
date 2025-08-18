package org.mylove.tprt;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.mylove.tprt.block.Modblocks;
import org.mylove.tprt.item.ModCreativeModTabs;
import org.mylove.tprt.item.ModItems;
import org.mylove.tprt.registries.ModBuffRegistry;
import org.mylove.tprt.registries.ModFluids;
import org.mylove.tprt.registries.ModifierRegistry;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(Tprt.MODID)
public class Tprt {

    // Define mod id in a common place for everything to reference
    public static final String MODID = "tprt";
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();

    public Tprt() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModFluids.register(modEventBus);
        ModCreativeModTabs.register(modEventBus);
        ModItems.register(modEventBus);
        Modblocks.register(modEventBus);
        ModBuffRegistry.register(modEventBus);
        modEventBus.addListener(this::commonSetup);

        ModifierRegistry.register(modEventBus);

        MinecraftForge.EVENT_BUS.register(this);
        modEventBus.addListener(this::addCreative);
    }
    private void commonSetup(final FMLCommonSetupEvent event) {

    }

    // Add the example block item to the building blocks tab
    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if(event.getTabKey()==CreativeModeTabs.INGREDIENTS){
            event.accept(ModItems.LRIRON);
            event.accept(ModItems.RAW_LRIRON);
            event.accept(ModItems.AWAIRON);
            event.accept(ModItems.RAW_AWAIRON);
            event.accept(ModItems.TANPOWASD);
            event.accept(ModItems.KING_OF_FOREST);
        }
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {

    }

    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {

        }
    }
}
