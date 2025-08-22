package org.mylove.tprt.entities.flying_sword;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.mylove.tprt.registries.ModEntities;
import org.mylove.tprt.utilities.DeBug;
import org.mylove.tprt.utilities.Math0;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.Objects;


public class FlyingSword extends Entity implements GeoEntity {
    private static final String[] BEHAVIOR_MODE_LIST = {"IDLE", "LAUNCH", "RECOUP"};
    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private Player master;
    private String behaviorMode = "IDLE";
    private int slotNumber;

    public FlyingSword(EntityType<FlyingSword> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }
    public FlyingSword(IToolStackView tool, Level pLevel, Player player, int slot, ItemStack stack){
        this(ModEntities.FLYING_SWORD.get(), pLevel);
        master = player;
        noPhysics = true;
        slotNumber = slot;
        // master.nineSwordPlayerCapacity
    }


    public boolean isAttackable() {
        return false;
    }

    public void launchSword(Vec3 target){
        this.behaviorMode = BEHAVIOR_MODE_LIST[1];
        Vec3 local = this.position();
    }

    @Override
    public void tick() {
        super.tick();
        // idle
        if(Objects.equals(behaviorMode, BEHAVIOR_MODE_LIST[0])){
            IdleMode();
        }
    }

    @Override
    public void baseTick() {
        this.level().getProfiler().push("flyingSwordBaseTick");
        this.xRotO = this.getXRot();
        this.yRotO = this.getYRot();
        this.clearFire();
        this.checkBelowWorld();
        this.level().getProfiler().pop();
    }

    private void IdleMode() {
        // 无指令时：1.保持在玩家背后 2.跟随移动 3.跟随传送
        if (master == null) return;
        Vec3 ownerBack = calculateIdlePos();
        Vec3 current = this.position();
        Vec3 delta = ownerBack.subtract(current);
        double distance = delta.length();

        if(level().isClientSide) {
            // 大约一秒调用一次，不是每t吗？
            // DeBug.Console(master, distance+"");
        }

        if (distance > 0.1) {
            Vec3 motion = delta.normalize().scale(Math0.clamp(1d, distance * 0.25, distance * 1));
            this.setDeltaMovement(motion);

            // todo: 实体移动是跳跃的，有什么办法吗？
            this.setPos(current.add(motion));
        } else {
            this.setDeltaMovement(Vec3.ZERO);
        }
        // this.lookAt(master);
    }

    // 快捷栏的每把飞剑都有自己对应的位置: 7 5 3 1 0 2 4 6 8
    private Vec3 calculateIdlePos() {
        Vec3 masterBack = master.getLookAngle().scale(-1.5);
        // 构造垂直向量，简单的几何学：他们的点积为零 perpendicular.dot(masterBack) = 0
        Vec3 perpendicular = new Vec3(masterBack.z, 0, -1 * masterBack.x).normalize();
        // 最后的.3决定疏密程度
        double slotPosition = Math.pow(-1, slotNumber) * Math.ceil((double) slotNumber /2) * .3;
        return master.position().add(masterBack.add(perpendicular.scale(slotPosition)));
    }


    public String getBehaviorMode() {
        return behaviorMode;
    }

    @Override
    protected void defineSynchedData() {

    }

    @Override
    protected void readAdditionalSaveData(CompoundTag pCompound) {

    }

    @Override
    protected void addAdditionalSaveData(CompoundTag pCompound) {

    }

    @Override
    public void registerControllers(final AnimatableManager.ControllerRegistrar controllers) {
//        controllers.add(new AnimationController<>(this, "Flying", 5, this::flyAnimController));
    }

//    protected <E extends FlyingSword> PlayState flyAnimController(final AnimationState<E> event) {
//        if (event.isMoving())
//            return event.setAndContinue(FLY_ANIM);
//
//        return PlayState.STOP;
//    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }

}
