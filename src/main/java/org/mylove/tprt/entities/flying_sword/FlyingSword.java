package org.mylove.tprt.entities.flying_sword;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.mylove.tprt.registries.ModEntities;
import org.mylove.tprt.utilities.Abbr;
import org.mylove.tprt.utilities.DeBug;
import org.mylove.tprt.utilities.Math0;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.Objects;


public class FlyingSword extends Entity implements GeoEntity {
    private static final String[] BEHAVIOR_MODE_LIST = {"IDLE", "LAUNCH", "RESET"};
    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private Player master;
    // private IToolStackView tinkerTool;
    private String behaviorMode;
    private int slotNumber;
    private final int initLifetime = 40;
    private int lifeTime;
    private int lunchCooldown;
    private final int generalFlyingSpeed = 2;
    private Vec3 lunchingModeTarget;
    private Vec3 lunchingModeDelta;

    public FlyingSword(EntityType<FlyingSword> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }
    public FlyingSword(IToolStackView tool, Level pLevel, Player player, int slot, ItemStack stack){
        this(ModEntities.FLYING_SWORD.get(), pLevel);
        noPhysics = true;
        master = player;
        slotNumber = slot;
        behaviorMode = "IDLE";
        lifeTime = initLifetime;
        lunchCooldown = 0;

        Abbr.setPlayerSwords(master, slot, this);
    }

    public boolean isAttackable() {
        return false;
    }
    public boolean skipAttackInteraction(Entity pEntity) {
        return true;
    }


    @Override
    public void tick() {
        super.tick();
        if (master == null) {
            // this.discard();
            return;
        }

        lifeTime--;
        if(lifeTime<=0){
            checkMasterPos();
        }
        if(Objects.equals(behaviorMode, BEHAVIOR_MODE_LIST[0])){
            // idle
            IdleMode();
        } else if (Objects.equals(behaviorMode, BEHAVIOR_MODE_LIST[1])) {
            // lunching
            LunchingMode();
        } else {
            // resetting
            ResettingMode();
        }
    }

    private void checkMasterPos() {
        // 飞剑需要定期确定召唤者在身边，且对应slot的匠魂工具中有此剑的uuid

        //ItemStack hotbarSlot = master.getInventory().getItem(slotNumber);
        //hotbarSlot.readShareTag();
        double distance = master.position().distanceTo(this.position());
        if(distance<=32) {
            lifeTime = initLifetime;
        } else {
            DeBug.Console(master, slotNumber+"号剑已销毁");
            this.discard();
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
        if(this.lunchCooldown>0) this.lunchCooldown--;
        // 无指令时：1.保持在玩家背后 2.跟随移动 3.跟随传送
        Vec3 ownerBack = calculateIdlePos();
        Vec3 current = this.position();
        Vec3 delta = ownerBack.subtract(current);
        double distance = delta.length();

        if(level().isClientSide) {
            // 大约一秒调用一次，不是每t吗？
            // DeBug.Console(master, distance+"distance");
        }
        // 08/22: 1.tick中始终为服务端 2.setPos函数雀食每t更新，发包（或许）不是，本地和服务端数据1s左右同步一次，故本地渲染是跳跃的
        // DeBug.Console(master, distance+"distance"+level().isClientSide);

        if (distance > 0.01) {
            Vec3 motion = delta.normalize().scale(Math0.clamp(1d, distance * 0.25, distance * 1));
            this.setDeltaMovement(motion);

            // todo: 实体移动是跳跃的，有什么办法吗？
            this.setPos(current.add(motion));
        } else {
            this.setDeltaMovement(Vec3.ZERO);
        }
        // this.lookAt(master);
    }

    private Vec3 calculateIdlePos() {
        // 快捷栏的每把飞剑都有自己对应的位置: 7 5 3 1 0 2 4 6 8
        Vec3 masterBack = master.getLookAngle().scale(-1.5);
        // 构造垂直向量，简单的几何学：他们的点积为零 perpendicular.dot(masterBack) = 0
        Vec3 perpendicular = new Vec3(masterBack.z, 0, -1 * masterBack.x).normalize();
        // 最后的.3决定疏密程度
        double slotPosition = Math.pow(-1, slotNumber) * Math.ceil((double) slotNumber /2) * .6;
        return master.position().add(masterBack.add(perpendicular.scale(slotPosition)));
    }

    private void LunchingMode() {
        if(this.position().distanceTo(this.lunchingModeTarget) > 0.01){
            this.setPos(this.position().add(this.lunchingModeDelta.scale(this.generalFlyingSpeed)));
        } else {
            this.setBehaviorMode(2);
        }
    }

    public boolean triggerLunch(Vec3 targetPoint) {
        boolean canLunch = this.lunchCooldown > 0;
        if(!canLunch) return false;
        DeBug.Console(master, slotNumber+"号剑发射");
        this.lunchCooldown = 30;
        // 发射路径会在一开始就确定好
        Vec3 startingPoint = this.position();

        this.lunchingModeTarget = targetPoint;
        this.lunchingModeDelta = targetPoint.subtract(startingPoint).scale(0.01);

        // 应当画弧，但现在先用直线
        // double distance = startingPoint.distanceTo(targetPoint);

        if(Objects.equals(this.behaviorMode, BEHAVIOR_MODE_LIST[0])){
            this.setBehaviorMode(1);
        }

        return true;
    }

    private void ResettingMode() {

    }


    public String getBehaviorMode() {
        return this.behaviorMode;
    }
    private void setBehaviorMode(int mode) {
        // assume 0 <= mode <= 2
        String toMode = BEHAVIOR_MODE_LIST[mode];
        this.behaviorMode = toMode;

        DeBug.Console(master, "号剑切换模式: "+toMode);
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
