package org.mylove.tprt.registries;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.mylove.tprt.Tprt;

public class Modblocks {
    public static final DeferredRegister<Block> BLOCKS=DeferredRegister.create(ForgeRegistries.BLOCKS,Tprt.MODID);

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS,Tprt.MODID);


    public static final RegistryObject<Block>LR_BLOCK=BLOCKS.register("lr_block",
            ()->new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .instrument(NoteBlockInstrument.BELL)
                    .instabreak()
                    .pushReaction(PushReaction.PUSH_ONLY)
                    .lightLevel(param->15)//内发光
                    // .lootFrom(()-> Blocks.DIAMOND_ORE)
                    .emissiveRendering((pState, pLevel, pPos) -> true)
                    .sound(SoundType.METAL)
            ));
    public static final RegistryObject<Item>LR_BLOCK_ITEM=ITEMS.register("lr_block",
            ()->new BlockItem(LR_BLOCK.get(), new Item.Properties()));

    public static final RegistryObject<Block>RAW_LR_BLOCK=BLOCKS.register("raw_lr_block",
            ()->new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PINK)
                    .instrument((NoteBlockInstrument.BELL) )
                    .lightLevel(param->10)
                    .emissiveRendering((pState, pLevel, pPos) -> true)
                    .sound(SoundType.METAL)
                    .strength(3,3f)
            ));
    public static final RegistryObject<Item>RAW_LR_BLOCK_ITEM=ITEMS.register("raw_lr_block",
            ()-> new BlockItem(RAW_LR_BLOCK.get(),new Item.Properties()));


    public static  void register(IEventBus eventBus){
        ITEMS.register(eventBus);
        BLOCKS.register(eventBus);
    }
}
