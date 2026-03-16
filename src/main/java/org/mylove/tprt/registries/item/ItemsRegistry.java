package org.mylove.tprt.registries.item;

import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.eventbus.api.IEventBus;
import org.mylove.tprt.Tprt;
import org.mylove.tprt.registries.ModFoods;
import slimeknights.mantle.registration.object.EnumObject;
import slimeknights.mantle.registration.object.ItemObject;
import slimeknights.tconstruct.common.registration.CastItemObject;
import slimeknights.tconstruct.common.registration.ItemDeferredRegisterExtension;
import slimeknights.tconstruct.library.tools.helper.ToolBuildHandler;
import slimeknights.tconstruct.library.tools.item.IModifiable;
import slimeknights.tconstruct.library.tools.item.ModifiableItem;
import slimeknights.tconstruct.library.tools.part.IMaterialItem;
import slimeknights.tconstruct.tools.item.ModifiableSwordItem;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;


public class ItemsRegistry {
    private static final ItemDeferredRegisterExtension ITEMS = new ItemDeferredRegisterExtension(Tprt.MODID);
    /**
    public static final RegistryObject<Item> LRIRON=ITEMS.register("lriron",
            () -> new Item(new Item.Properties()));

    public static final  RegistryObject<Item>RAW_LRIRON=ITEMS.register("raw_lriron",
            ()->new Item(new Item.Properties()));
     public static final RegistryObject<Item> AWAIRON=ITEMS.register("awairon",
            () -> new Item(new Item.Properties()));

    public static final  RegistryObject<Item>RAW_AWAIRON=ITEMS.register("raw_awairon",
            ()->new Item(new Item.Properties()));

    public static final  RegistryObject<Item>TANPOWASD=ITEMS.register("tanpowasd",
            ()->new Item(new Item.Properties()));

    public static final RegistryObject<Item>LR_APPLE=ITEMS.register("lr_apple",
            ()->new Item(new Item.Properties().food(ModFoods.LR_APPLE)));
//挚爱锭前置
    public static final RegistryObject<Item>KING_OF_FOREST=ITEMS.register("king_of_forest",
            ()->new Item(new Item.Properties()));
    public static final RegistryObject<Item>KING_OF_FIREANDICE=ITEMS.register("king_of_fireandice",
            ()->new Item(new Item.Properties()));
    public static final RegistryObject<Item>KING_OF_CATACLYSM=ITEMS.register("king_of_cataclysm",
            ()->new Item(new Item.Properties()));
    public static final RegistryObject<Item>KING_OF_MAGIC=ITEMS.register("king_of_magic",
            ()->new Item(new Item.Properties()));
    public static final RegistryObject<Item>KING_OF_MC=ITEMS.register("king_of_mc",
            ()->new Item(new Item.Properties()));
    //精密合金
    public static final RegistryObject<Item>PRECISION_ALLOY=ITEMS.register("precision_alloy",
            ()->new Item(new Item.Properties()));
    //不锈钢
    public static final RegistryObject<Item>STAINLESS_STEEL=ITEMS.register("stainless_steel",
            ()->new Item(new Item.Properties()));
     */

    public static final  ItemObject<Item>TANPOWASD=ITEMS.register("tanpowasd",
            ()->new Item(new Item.Properties()));

    public static final ItemObject<Item> RED_TURTLE = ITEMS.register("red_turtle", () -> new Item(new Item.Properties().food(ModFoods.RED_TURTLE)));

//lr特供 龙神锭
    public static final ItemObject<Item>MIXEDDRAGON=ITEMS.register("mixeddragon",
        ()->new Item(new Item.Properties()));
//lr特供 锻造钢
    public static final ItemObject<Item>FORGED_STEEL=ITEMS.register("forged_steel",
        ()->new Item(new Item.Properties()));
//lr特供 源质合金
    public static final ItemObject<Item>SOURCE_ALLOY=ITEMS.register("source_alloy",
        ()->new Item(new Item.Properties()));
//lr特供 超限玛玉灵
    public static final ItemObject<Item>ADVANCED_MANYULLYN=ITEMS.register("advanced_manyullyn",
        ()->new Item(new Item.Properties()));
//lr特供 瓦斯麦谷锭
    public static final ItemObject<Item>KWAT_WHEAT_GRAIN_INGOT=ITEMS.register("kwat_wheat_grain_ingot",
        ()->new Item(new Item.Properties().food(ModFoods.KWAT_WHEAT_GRAIN_INGOT)));
//lr特供 恶兽合金锭
    public static final ItemObject<Item>EVIL_BEAST_ALLOY_INGOT=ITEMS.register("evil_beast_alloy_ingot",
        ()->new Item(new Item.Properties()));
//lr特供 风暴之泪
    public static final ItemObject<Item>Tears_of_the_storm=ITEMS.register("tears_of_the_storm",
            ()->new Item(new Item.Properties()));
//lr特供 深渊残片
    public static final ItemObject<Item>Abyss_fragment=ITEMS.register("abyss_fragment",
            ()->new Item(new Item.Properties()));
//lr特供 濡湿骑士锭
    public static final ItemObject<Item>DARK_KNIGHT_INGOT=ITEMS.register("dark_knight_ingot",
        ()->new Item(new Item.Properties()));
//lr特供 溪竹板
    public static final ItemObject<Item>Leisamboo_Board=ITEMS.register("leisamboo_board",
        ()->new Item(new Item.Properties()));
//lr特供 复合龙鳞
    public static final ItemObject<Item>Composite_dragon_scales=ITEMS.register("composite_dragon_scales",
        ()->new Item(new Item.Properties()));


    //lr特供 锚剑
    public static final ItemObject<ModifiableItem>anchor_sword= ITEMS.register("anchor_sword",
        ()->new ModifiableSwordItem(TPRTItemUtils.UNSTACKABLE_PROPS, TPRTToolDefinitions.ANCHOR_SWORD));
    //lr特供 注法者
    public static final ItemObject<ModifiableItem>magic_blade= ITEMS.register("magic_blade",
        ()->new ModifiableSwordItem(TPRTItemUtils.UNSTACKABLE_PROPS, TPRTToolDefinitions.MAGIC_BLADE));

//lr特供 🐖包
    public static final ItemObject<Item>PIG_BUN=ITEMS.register("pig_bun",
        ()->new Item(new Item.Properties().food(ModFoods.PIG_BUN)));

    public static void register(IEventBus eventBus){
        ITEMS.register(eventBus);
    }


    public static void addCommonTabItems(CreativeModeTab.ItemDisplayParameters itemDisplayParameters, CreativeModeTab.Output tab) {
        tab.accept(MIXEDDRAGON);
        tab.accept(FORGED_STEEL);
        tab.accept(SOURCE_ALLOY);
        tab.accept(ADVANCED_MANYULLYN);
        tab.accept(KWAT_WHEAT_GRAIN_INGOT);
        tab.accept(EVIL_BEAST_ALLOY_INGOT);
        tab.accept(Tears_of_the_storm);
        tab.accept(Abyss_fragment);
        tab.accept(DARK_KNIGHT_INGOT);
        tab.accept(Leisamboo_Board);
        tab.accept(Composite_dragon_scales);
    }

    public static void addToolTabItems(CreativeModeTab.ItemDisplayParameters itemDisplayParameters, CreativeModeTab.Output tab) {
        Consumer<ItemStack> output = tab::accept;
        acceptTool(output, anchor_sword);
        acceptTool(output, magic_blade);
    }

    private static void acceptTool(Consumer<ItemStack> output, Supplier<? extends IModifiable> tool) {
        ToolBuildHandler.addVariants(output, (IModifiable)tool.get(), "");
    }
    private static void acceptTools(Consumer<ItemStack> output, EnumObject<?, ? extends IModifiable> tools) {
        tools.forEach((tool) -> ToolBuildHandler.addVariants(output, tool, ""));
    }
    private static void accept(Consumer<ItemStack> output, Supplier<? extends IMaterialItem> item) {
        item.get().addVariants(output, "");
    }
    private static void accept(CreativeModeTab.Output output, Function<CastItemObject, ItemLike> getter, CastItemObject cast) {
        output.accept(getter.apply(cast));
    }
}
