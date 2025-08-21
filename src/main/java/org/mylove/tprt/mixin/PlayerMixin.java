package org.mylove.tprt.mixin;

import net.minecraft.world.entity.player.Player;
import org.mylove.tprt.entities.flying_sword.FlyingSword;
import org.mylove.tprt.mixin.api.PlayerMixinAPI;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = Player.class)
public class PlayerMixin implements PlayerMixinAPI {

    /** 玩家快捷栏的至多⑨把飞剑的引用，其顺序与快捷栏对应，且没有飞剑的位置为null */
    @Unique
    private FlyingSword[] nineSword$HotbarHolder = new FlyingSword[9];

    public FlyingSword[] getNineSword$HotbarHolder() {
        return nineSword$HotbarHolder;
    }
}