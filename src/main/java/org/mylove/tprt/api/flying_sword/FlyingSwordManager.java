package org.mylove.tprt.api.flying_sword;

import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mylove.tprt.entities.flying_sword.FlyingSword;

/**
 * 思路来自:
 * <a href="https://github.com/FrozenRainDev/Hardcore-Survival/blob/1.20.1/src/main/java/biz/coolpage/hcs/status/manager/ThirstManager.java#L6">@Github</a>
 * */
public class FlyingSwordManager {
    private FlyingSword[] nineSword$HotbarHolder = new FlyingSword[9];

    public FlyingSword[] getBar() {
        return this.nineSword$HotbarHolder;
    }

    @Nullable
    public FlyingSword get(int slot) {
        return this.nineSword$HotbarHolder[slot];
    }

    public void set(int slot, @Nullable FlyingSword sword) {
        this.nineSword$HotbarHolder[slot] = sword;
    }

    /** 发射飞剑，已作空判 */
    public boolean lunch(int slot, Vec3 target) {
        if(this.nineSword$HotbarHolder[slot] == null) return false;
        return this.nineSword$HotbarHolder[slot].triggerLunch(target);
    }

}
