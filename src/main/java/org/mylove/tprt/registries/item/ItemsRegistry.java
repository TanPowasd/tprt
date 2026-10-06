package org.mylove.tprt.registries.item;

import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.eventbus.api.IEventBus;
import org.mylove.tprt.Tprt;
import org.mylove.tprt.compat.Cataclysm.CataclysmCompat;
import org.mylove.tprt.compat.Iceandfire.Iceandfire_Compat;
import org.mylove.tprt.compat.Immortalers_Delight.Immortalers_Delight_Compat;
import org.mylove.tprt.compat.IronsSpellBooks.IssCompat;
/*import org.mylove.tprt.common.item.curios.Goldenfoxmask;*/
import org.mylove.tprt.registries.ModFoods;
import org.mylove.tprt.utils.ModListUtil;
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
/*
//lr特供 金质狐狸面具
    public static final ItemObject<Item>Goldenfoxmask = ITEMS.register("golden_fox_mask",
        () -> new Goldenfoxmask(new Item.Properties(), "fox_mask", ImmutableMultimap.of()));*/
//lr特供 龙神锭 -> 已移到 Iceandfire_Compat.MIXEDDRAGON (冰与火联动材料)
//lr特供 锻造钢
    public static final ItemObject<Item>FORGED_STEEL=ITEMS.register("forged_steel",
        ()->new Item(new Item.Properties()));
//lr特供 源质合金 -> 已移到 IssCompat.SOURCE_ALLOY (铁魔法联动材料)
//lr特供 超限玛玉灵
    public static final ItemObject<Item>ADVANCED_MANYULLYN=ITEMS.register("advanced_manyullyn",
        ()->new Item(new Item.Properties()));
//lr特供 耀光骑士金属锭
    public static final ItemObject<Item>RADIANT_KNIGHTMETAL_INGOT=ITEMS.register("radiant_knightmetal_ingot",
            ()->new Item(new Item.Properties()));
//联动模组物品已按所属模组移到各自的兼容类: 装了对应模组才会注册
//  灾变     -> CataclysmCompat.CATACLYSM_ITEMS            (tears_of_the_storm / abyss_fragment / void_power_alloy / sandstorm_skeleton / evil_beast_alloy_ingot)
//  冰与火   -> Iceandfire_Compat.ICEANDFIRE_ITEMS         (composite_dragon_scales / mixeddragon)
//  不朽乐事 -> Immortalers_Delight_Compat.ID_ITEMS        (kwat_wheat_grain_ingot / leisamboo_board)
//  铁魔法   -> IssCompat.ISS_ITEMS                        (magic_blade / source_alloy / dark_knight_ingot)


    //lr特供 锚剑
    public static final ItemObject<ModifiableItem>anchor_sword= ITEMS.register("anchor_sword",
        ()->new ModifiableSwordItem(TPRTItemUtils.UNSTACKABLE_PROPS, TPRTToolDefinitions.ANCHOR_SWORD));
    //注法者 (铁魔法联动) 已移到 IssCompat.magic_blade: 装了铁魔法时才会注册

//lr特供 🐖包
    public static final ItemObject<Item>PIG_BUN=ITEMS.register("pig_bun",
        ()->new Item(new Item.Properties().food(ModFoods.PIG_BUN)));
//lr特供 红温🐢
    public static final ItemObject<Item> RED_TURTLE = ITEMS.register("red_turtle", () -> new Item(new Item.Properties().food(ModFoods.RED_TURTLE)));

    public static void register(IEventBus eventBus){
        ITEMS.register(eventBus);
    }


    public static void addCommonTabItems(CreativeModeTab.ItemDisplayParameters itemDisplayParameters, CreativeModeTab.Output tab) {
        //自有材料
        tab.accept(FORGED_STEEL);
        tab.accept(ADVANCED_MANYULLYN);
        tab.accept(RADIANT_KNIGHTMETAL_INGOT);
        //联动模组物品: 没装对应模组就没有注册, 也就不往创造栏里放
        if (ModListUtil.ISSLoaded) {
            IssCompat.addCommonTabItems(tab);
        }
        if (ModListUtil.CALoaded) {
            CataclysmCompat.addCommonTabItems(tab);
        }
        if (ModListUtil.IceandfireLoaded) {
            Iceandfire_Compat.addCommonTabItems(tab);
        }
        if (ModListUtil.IDLoaded) {
            Immortalers_Delight_Compat.addCommonTabItems(tab);
        }
        /*tab.accept(Goldenfoxmask);*/
    }

    public static void addToolTabItems(CreativeModeTab.ItemDisplayParameters itemDisplayParameters, CreativeModeTab.Output tab) {
        Consumer<ItemStack> output = tab::accept;
        acceptTool(output, anchor_sword);
        // 铁魔法联动物品 (注法者) 只有装了铁魔法才会注册, 没装就不往创造栏里放
        if (ModListUtil.ISSLoaded) {
            IssCompat.addToolTabItems(tab);
        }
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
