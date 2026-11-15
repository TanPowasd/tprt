package org.mylove.tprt.compat.Create;

import net.minecraft.world.item.CreativeModeTab;
import org.mylove.tprt.Tprt;
import slimeknights.mantle.registration.deferred.FluidDeferredRegister;
import slimeknights.tconstruct.common.registration.ItemDeferredRegisterExtension;
import slimeknights.tconstruct.library.modifiers.util.ModifierDeferredRegister;

/**
 * 机械动力 (Create) 联动。
 *
 * <p>锻造钢的物品与流体已移到核心注册 (ItemsRegistry / ModFluids)，因此不再依赖 Create。
 * 这里只保留 Create 专属的装配线配方钩子，装了 Create 时才挂事件总线。
 */
public class CreateCompat {

    public static final ModifierDeferredRegister CREATE_MODIFIERS = ModifierDeferredRegister.create(Tprt.MODID);
    public static final ItemDeferredRegisterExtension CREATE_ITEMS = new ItemDeferredRegisterExtension(Tprt.MODID);
    public static final FluidDeferredRegister CREATE_FLUIDS = new FluidDeferredRegister(Tprt.MODID);

    /** 创造物品栏条目: 锻造钢已由 ItemsRegistry 统一注册 */
    public static void addCommonTabItems(CreativeModeTab.Output tab) {
        // 无 Create 专属物品
    }
}
