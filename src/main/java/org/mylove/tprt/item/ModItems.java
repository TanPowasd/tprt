package org.mylove.tprt.item;

import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.mylove.tprt.Tprt;

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

//lr特供 龙神锭
    public static final RegistryObject<Item>MIXEDDRAGON=ITEMS.register("mixeddragon",
        ()->new Item(new Item.Properties()));




    public static void register(IEventBus eventBus){
        ITEMS.register(eventBus);
    }
}
