package org.mylove.tprt.compat.IronsSpellBooks;

import com.ssakura49.sakuratinker.compat.IronSpellBooks.modifiers.*;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.mylove.tprt.Tprt;
import org.mylove.tprt.compat.IronsSpellBooks.Modifiers.Magic_Sublimation_Range;
import org.mylove.tprt.compat.IronsSpellBooks.Modifiers.Magic_sublimation;
import org.mylove.tprt.compat.IronsSpellBooks.Modifiers.enchanting;
import org.mylove.tprt.compat.IronsSpellBooks.Modifiers.human_turpentine;
import org.mylove.tprt.registries.item.TPRTItemUtils;
import org.mylove.tprt.registries.item.TPRTToolDefinitions;
import slimeknights.tconstruct.library.modifiers.util.ModifierDeferredRegister;
import slimeknights.tconstruct.library.modifiers.util.StaticModifier;
import slimeknights.tconstruct.tools.item.ModifiableSwordItem;

public class IssCompat {
    public static ModifierDeferredRegister Iss_MODIFIERS = ModifierDeferredRegister.create(Tprt.MODID);
    public static final DeferredRegister<Item> Iss_ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Tprt.MODID);
/*
    public static final RegistryObject<Item> magic_blade= Iss_ITEMS.register("magic_blade",
            ()->new ModifiableSwordItem(TPRTItemUtils.UNSTACKABLE_PROPS, TPRTToolDefinitions.MAGIC_BLADE));
    public static final RegistryObject<Item>SOURCE_ALLOY=Iss_ITEMS.register("source_alloy",
            ()->new Item(new Item.Properties()));
    public static final RegistryObject<Item>DARK_KNIGHT_INGOT=Iss_ITEMS.register("dark_knight_ingot",
            ()->new Item(new Item.Properties()));
*/
    public static final StaticModifier<enchanting> enchanting;
    public static StaticModifier<Magic_Sublimation_Range> Magic_Sublimation_Range;
    public static StaticModifier<Magic_sublimation> Magic_sublimation;
    public static StaticModifier<human_turpentine> Human_turpentine;
    static {
        enchanting = Iss_MODIFIERS.register("enchanting", enchanting::new);
        Magic_Sublimation_Range = Iss_MODIFIERS.register("magic_sublimation_range", Magic_Sublimation_Range::new);
        Magic_sublimation = Iss_MODIFIERS.register("magic_sublimation", Magic_sublimation::new);
        Human_turpentine = Iss_MODIFIERS.register("human_turpentine", human_turpentine::new);
    }
}

