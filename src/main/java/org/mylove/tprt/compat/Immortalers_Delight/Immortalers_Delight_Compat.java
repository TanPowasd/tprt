package org.mylove.tprt.compat.Immortalers_Delight;

import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import org.mylove.tprt.Tprt;
import org.mylove.tprt.compat.Immortalers_Delight.Modifiers.flourishing;
import org.mylove.tprt.compat.Immortalers_Delight.Modifiers.gas_poison;
import org.mylove.tprt.compat.Immortalers_Delight.Modifiers.lingering_infusion;
import org.mylove.tprt.registries.ModFoods;
import slimeknights.mantle.registration.object.ItemObject;
import slimeknights.tconstruct.common.registration.ItemDeferredRegisterExtension;
import slimeknights.tconstruct.library.modifiers.util.ModifierDeferredRegister;
import slimeknights.tconstruct.library.modifiers.util.StaticModifier;

public class Immortalers_Delight_Compat {
    public static ModifierDeferredRegister Id_MODIFIERS = ModifierDeferredRegister.create(Tprt.MODID);

    /** 不朽乐事联动物品专用注册器: 只在装了不朽乐事时才会在 Tprt 里挂到事件总线上 */
    public static final ItemDeferredRegisterExtension ID_ITEMS = new ItemDeferredRegisterExtension(Tprt.MODID);
    //lr特供 瓦斯麦谷锭 / 溪竹板
    public static final ItemObject<Item> KWAT_WHEAT_GRAIN_INGOT = ID_ITEMS.register("kwat_wheat_grain_ingot", () -> new Item(new Item.Properties().food(ModFoods.KWAT_WHEAT_GRAIN_INGOT)));
    public static final ItemObject<Item> Leisamboo_Board = ID_ITEMS.register("leisamboo_board", () -> new Item(new Item.Properties()));

    /** 创造物品栏条目: 由 ItemsRegistry 在检测到不朽乐事时调用 */
    public static void addCommonTabItems(CreativeModeTab.Output tab) {
        tab.accept(KWAT_WHEAT_GRAIN_INGOT);
        tab.accept(Leisamboo_Board);
    }

    public static StaticModifier<flourishing> flourishing;
    public static StaticModifier<gas_poison> gas_poison;
    public static StaticModifier<lingering_infusion> lingering_infusion;
    static {
        flourishing = Id_MODIFIERS.register("flourishing", flourishing::new);
        gas_poison = Id_MODIFIERS.register("gas_poison", gas_poison::new);
        lingering_infusion = Id_MODIFIERS.register("lingering_infusion", lingering_infusion::new);
    }
}
