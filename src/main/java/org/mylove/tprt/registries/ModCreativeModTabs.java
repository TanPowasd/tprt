package org.mylove.tprt.registries;
//创造物品栏
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import org.mylove.tprt.ModSpells.thunder_att.MIAGIC_cs1;
import org.mylove.tprt.Tprt;
import org.mylove.tprt.registries.item.ModItems;

public class ModCreativeModTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS=
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Tprt.MODID);

    public  static final RegistryObject<CreativeModeTab> TPRT_TAB= CREATIVE_MODE_TABS.register("tprt",
            ()->CreativeModeTab.builder().icon(()->new ItemStack(ModItems.TANPOWASD.get()))
                    .title(Component.translatable("creativetab.tprt_tab"))
                    .withSearchBar()
                    .displayItems((pParameters, pOutput) -> {
                        //ModItem
                        pOutput.accept(ModItems.LRIRON.get());
                        pOutput.accept(ModItems.RAW_LRIRON.get());
                        pOutput.accept(ModItems.AWAIRON.get());
                        pOutput.accept(ModItems.RAW_AWAIRON.get());
                        pOutput.accept(ModItems.TANPOWASD.get());
                        pOutput.accept(Modblocks.LR_BLOCK_ITEM.get());
                        pOutput.accept(Modblocks.RAW_LR_BLOCK_ITEM.get());
                        pOutput.accept(ModItems.LR_APPLE.get());
                        pOutput.accept(ModItems.KING_OF_FOREST.get());
                        pOutput.accept(ModItems.KING_OF_FIREANDICE.get());
                        pOutput.accept(ModItems.KING_OF_CATACLYSM.get());
                        pOutput.accept(ModItems.KING_OF_MAGIC.get());
                        pOutput.accept(ModItems.KING_OF_MC.get());
                        pOutput.accept(ModItems.MIXEDDRAGON.get());//龙神锭
                        pOutput.accept(ModItems.FORGED_STEEL.get());
                        pOutput.accept(ModItems.PIG_BUN.get());
                        pOutput.accept(ModItems.SOURCE_ALLOY.get());
                        pOutput.accept(ModItems.ADVANCED_MANYULLYN.get());
                        pOutput.accept(ModItems.KWAT_WHEAT_GRAIN_INGOT.get());
                        pOutput.accept(ModItems.EVIL_BEAST_ALLOY_INGOT.get());
                        pOutput.accept(ModItems.BLUE_LANCER.get());
                        pOutput.accept(ModItems.RED_TURTLE.get());
                        pOutput.accept(ModItems.STAINLESS_STEEL.get());
                        pOutput.accept(ModItems.PRECISION_ALLOY.get());
                        pOutput.accept(ModItems.anchor_sword.get());
                        //magicitem
                        //pOutput.accept(MIAGIC_cs1.get());
                    })
                    .build()

    );
    public static void  register(IEventBus eventBus){
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
