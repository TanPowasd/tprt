package org.mylove.tprt.client;

import com.github.L_Ender.cataclysm.client.particle.Options.TrackLightningParticleOptions;
import com.github.L_Ender.cataclysm.init.ModParticle;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.phys.Vec3;
import org.mylove.tprt.common.SoulArrowVisuals;
import org.mylove.tprt.compat.Cataclysm.entity.TprtPhantomArrow;

/** Client-only cosmetic particles; loaded only with Cataclysm present. */
public final class SoulArrowTrail {
    private SoulArrowTrail() {}

    public static void tick(ClientLevel level) {
        for (var entity : level.entitiesForRendering()) {
            if (!(entity instanceof AbstractArrow arrow) || !arrow.isAlive()
                    || (!(arrow instanceof TprtPhantomArrow) && !SoulArrowVisuals.isMarked(arrow))) continue;
            // Unlike the native phantom arrow, stop producing particles after ground impact.
            Vec3 velocity = arrow.getDeltaMovement();
            if (arrow.position().distanceToSqr(new Vec3(arrow.xo, arrow.yo, arrow.zo)) < 1.0E-8
                    || velocity.lengthSqr() < 1.0E-6) continue;
            Vec3 start = arrow.position().add(velocity);
            var random = level.random;
            Vec3 end = start.add(random.nextFloat() - 0.5F, random.nextFloat() - 0.5F, random.nextFloat() - 0.5F);
            level.addParticle(new TrackLightningParticleOptions(26, 107, 89),
                    start.x, start.y, start.z, end.x, end.y, end.z);
            for (int i = 0; i < 2; i++) {
                Vec3 point = arrow.position().add(velocity.scale(i / 4.0D));
                level.addParticle(ModParticle.CURSED_FLAME.get(), point.x, point.y, point.z, 0, 0, 0);
            }
        }
    }
}
