package org.mylove.tprt.entity.tec;

import net.minecraft.world.level.Level;

public interface ParticleProvider {
    void runPerTick(Level level, int tickCount, int tickRemain);
}
