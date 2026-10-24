package org.mylove.tprt.compat.Cataclysm.entity;

import com.github.L_Ender.cataclysm.entity.projectile.Death_Laser_Beam_Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

/**
 * 跟随施法者的死亡激光。
 *
 * 灾变原版只在 _Harbinger_ / _Prowler_ 施法时才更新光束位置与朝向
 * (updateWithHarbinger / updateWithProwler), 玩家施法时光束会僵在原地。
 * 这里在 super.tick() 之前把位置/朝向改成施法者的眼睛, 让任意施法者都能跟随。
 */
public class TprtDeathLaser extends Death_Laser_Beam_Entity {

    public TprtDeathLaser(EntityType<? extends Death_Laser_Beam_Entity> type, Level level) {
        super(type, level);
    }

    public TprtDeathLaser(EntityType<? extends Death_Laser_Beam_Entity> type, Level level, LivingEntity caster,
                          double x, double y, double z, float yaw, float pitch, int duration,
                          float damage, float hpDamage) {
        super(type, level, caster, x, y, z, yaw, pitch, duration, damage, hpDamage);
    }

    @Override
    public void tick() {
        LivingEntity owner = this.caster;
        if (owner != null && owner.isAlive()) {
            // 与灾变一致: 内部朝向使用弧度
            float yaw = (float) ((owner.yBodyRot + 90.0F) * Math.PI / 180.0);
            float pitch = (float) (-owner.getXRot() * Math.PI / 180.0);
            this.setYaw(yaw);
            this.setPitch(pitch);
            this.renderYaw = yaw;
            this.renderPitch = pitch;
            this.setPos(owner.getX(), owner.getY() + owner.getEyeHeight(), owner.getZ());
        }
        super.tick();
    }
}