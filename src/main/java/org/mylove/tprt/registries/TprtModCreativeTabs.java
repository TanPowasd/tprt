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
import org.mylove.tprt.registries.item.ItemsRegistry;
import slimeknights.tconstruct.library.utils.Util;

import static slimeknights.tconstruct.tools.TinkerToolParts.tabToolParts;


public class TprtModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS=
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Tprt.MODID);

    public static final RegistryObject<CreativeModeTab> tabGeneral = CREATIVE_MODE_TABS.register(
            "general", () -> CreativeModeTab.builder().title(Component.translatable(Util.makeTranslationKey("creative_tab", Tprt.getResource("common"))))
                    .icon(() -> new ItemStack(ItemsRegistry.TANPOWASD))
                    .displayItems(ItemsRegistry::addCommonTabItems)
                    .build());
    public static final RegistryObject<CreativeModeTab> tabTools = CREATIVE_MODE_TABS.register(
            "tools", () -> CreativeModeTab.builder().title(Component.translatable(Util.makeTranslationKey("creative_tab", Tprt.getResource( "tools"))))
                    .icon(() -> ItemsRegistry.anchor_sword.get().getRenderTool())
                    .displayItems(ItemsRegistry::addToolTabItems)
                    .withTabsBefore(tabToolParts.getId())
                    .withSearchBar()
                    .build());
    public static void register(IEventBus eventBus)
    {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
