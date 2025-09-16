package org.mylove.tprt.entity.tec;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import org.mylove.tprt.registries.ModEntities;

/// 因服务端不能定向发出粒子, 而匠魂的攻击钩子又只执行在服务端, 我们引入经典的“技术性实体”
public class CinderParticle extends TecEntity {
    private ParticleProvider particleProvider;

    public CinderParticle(EntityType<?> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }
    public CinderParticle(Level level, int tickRemain, ParticleProvider lambda) {
        this(ModEntities.CINDER_SLASH_PARTICLE.get(), level);

        setDataRemainTick(tickRemain);
        particleProvider = lambda;
    }

    @Override
    public void tick() {
        System.out.println("CinderParticle" + tickCount + "   client?:" + level().isClientSide);
        particleProvider.runPerTick(level(), tickCount, entityData.get(DATA_REMAIN_TICK));
        super.tick();
    }
}
