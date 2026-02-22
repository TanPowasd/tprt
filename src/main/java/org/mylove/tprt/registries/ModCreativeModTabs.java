package org.mylove.tprt.registries;
//创造物品栏
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import org.mylove.tprt.Tprt;
import org.mylove.tprt.registries.item.ModItems;


public class ModCreativeModTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS=
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Tprt.MODID);

    public  static final RegistryObject<CreativeModeTab> TPRT_TAB= CREATIVE_MODE_TABS.register("tprt",
            ()->CreativeModeTab.builder().icon(()->new ItemStack(ModItems.MIXEDDRAGON.get()))
                    .title(Component.translatable("creativetab.tprt_tab"))
                    .withSearchBar()
                    .displayItems((pParameters, pOutput) -> {
                        //ModItem
                        pOutput.accept(ModItems.MIXEDDRAGON.get());//龙神锭
                        pOutput.accept(ModItems.FORGED_STEEL.get());
                        pOutput.accept(ModItems.PIG_BUN.get());
                        pOutput.accept(ModItems.ADVANCED_MANYULLYN.get());
                        pOutput.accept(ModItems.KWAT_WHEAT_GRAIN_INGOT.get());
                        pOutput.accept(ModItems.EVIL_BEAST_ALLOY_INGOT.get());
                        pOutput.accept(ModItems.RED_TURTLE.get());
                        pOutput.accept(ModItems.Leisamboo_Board.get());
                        pOutput.accept(ModItems.golden_fox_mask.get());
                        pOutput.accept(ModItems.anchor_sword.get());
                        pOutput.accept(ModItems.SOURCE_ALLOY.get());
                        pOutput.accept(ModItems.DARK_KNIGHT_INGOT.get());
                        pOutput.accept(ModItems.magic_blade.get());
                        pOutput.accept(ModItems.Composite_dragon_scales.get());
                        //magicitem
                        //pOutput.accept(MIAGIC_cs1.get());
                    })
                    .build()

    );
    public static void  register(IEventBus eventBus){
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
