package org.mylove.tprt.utilities;

import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;
import org.mylove.tprt.entities.flying_sword.FlyingSword;
import org.mylove.tprt.api.PlayerMixinAPI;

/** 各种缩写和语法糖 */
public class Abbr {
    public static FlyingSword[] getSwordBar(Player player){
        return ((PlayerMixinAPI) player).getFlyingSwordManager().getBar();
    }

    @Nullable
    public static FlyingSword getSword(Player player, int slot){
        return ((PlayerMixinAPI) player).getFlyingSwordManager().get(slot);
    }

    /** 使得玩家实体可以访问飞剑 */
    public static void setPlayerSwords(Player player, int slot, FlyingSword sword){
        ((PlayerMixinAPI) player).getFlyingSwordManager().set(slot, sword);
    }
}
