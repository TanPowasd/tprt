package org.mylove.tprt.registries.item;

import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.mylove.tprt.Tprt;
import org.mylove.tprt.registries.ModFoods;
import org.mylove.tprt.registries.item.curios.golden_fox_mask;
import slimeknights.tconstruct.tools.item.ModifiableSwordItem;

import static com.ssakura49.sakuratinker.register.STItems.registerCommonItem;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS,Tprt.MODID);

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


    public static final RegistryObject<Item>RED_TURTLE=ITEMS.register("red_turtle",
        ()->new Item(new Item.Properties().food(ModFoods.RED_TURTLE)));

//lr特供 龙神锭
    public static final RegistryObject<Item>MIXEDDRAGON=ITEMS.register("mixeddragon",
        ()->new Item(new Item.Properties()));
//lr特供 锻造钢
    public static final RegistryObject<Item>FORGED_STEEL=ITEMS.register("forged_steel",
        ()->new Item(new Item.Properties()));
//lr特供 源质合金
    public static final RegistryObject<Item>SOURCE_ALLOY=ITEMS.register("source_alloy",
        ()->new Item(new Item.Properties()));
//lr特供 超限玛玉灵
    public static final RegistryObject<Item>ADVANCED_MANYULLYN=ITEMS.register("advanced_manyullyn",
        ()->new Item(new Item.Properties()));
//lr特供 瓦斯麦谷锭
    public static final RegistryObject<Item>KWAT_WHEAT_GRAIN_INGOT=ITEMS.register("kwat_wheat_grain_ingot",
        ()->new Item(new Item.Properties().food(ModFoods.KWAT_WHEAT_GRAIN_INGOT)));
//lr特供 恶兽合金锭
    public static final RegistryObject<Item>EVIL_BEAST_ALLOY_INGOT=ITEMS.register("evil_beast_alloy_ingot",
        ()->new Item(new Item.Properties()));
//lr特供 濡湿骑士锭
public static final RegistryObject<Item>DARK_KNIGHT_INGOT=ITEMS.register("dark_knight_ingot",
        ()->new Item(new Item.Properties()));

//魂樱面具拓展：金质
    public static final RegistryObject<Item>golden_fox_mask = registerCommonItem(ITEMS, "golden_fox_mask",
        () -> new golden_fox_mask(), false);

//lr特供 锚剑
    public static final RegistryObject<Item>anchor_sword= ITEMS.register("anchor_sword",
        ()->new ModifiableSwordItem(TPRTItemUtils.UNSTACKABLE_PROPS, TPRTToolDefinitions.ANCHOR_SWORD));

//lr特供 注法者
    public static final RegistryObject<Item>magic_blade= ITEMS.register("magic_blade",
        ()->new ModifiableSwordItem(TPRTItemUtils.UNSTACKABLE_PROPS, TPRTToolDefinitions.MAGIC_BLADE));

//lr特供 🐖包
    public static final RegistryObject<Item>PIG_BUN=ITEMS.register("pig_bun",
        ()->new Item(new Item.Properties().food(ModFoods.PIG_BUN)));

//超级幸运E
    public static final RegistryObject<Item>CAPOO=ITEMS.register("capoo",
        () -> new Item(new Item.Properties()));

    //精密合金
    public static final RegistryObject<Item>PRECISION_ALLOY=ITEMS.register("precision_alloy",
            ()->new Item(new Item.Properties()));
    //不锈钢
    public static final RegistryObject<Item>STAINLESS_STEEL=ITEMS.register("stainless_steel",
            ()->new Item(new Item.Properties()));

    public static void register(IEventBus eventBus){
        ITEMS.register(eventBus);
    }
}
