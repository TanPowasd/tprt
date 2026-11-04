package org.mylove.tprt.client;

import com.github.L_Ender.cataclysm.client.particle.Options.StormParticleOptions;
import org.mylove.tprt.compat.Cataclysm.entity.TprtCursedSandstormArrow;

/** Cataclysm 3.31's actual orbiting ribbon particles, once per client tick, not per render. */
public final class CursedSandstormVisuals {
    private CursedSandstormVisuals() {}

    public static void tick(TprtCursedSandstormArrow arrow) {
        var random = arrow.level().random;
        // Native StormParticle tracks any Entity ID (no native projectile cast), including during despawn.
        arrow.level().addParticle(new StormParticleOptions(
                        0.09F + random.nextFloat() * 0.04F,
                        0.42F + random.nextFloat() * 0.04F,
                        0.35F + random.nextFloat() * 0.04F * 1.5F,
                        0.25F + random.nextFloat() * 0.45F,
                        0.35F + random.nextFloat() * 0.45F, arrow.getId()),
                arrow.getX(), arrow.getY(), arrow.getZ(), 0, 0, 0);
    }
}
