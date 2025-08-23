package org.mylove.tprt.entities.flying_sword;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;
import org.mylove.tprt.registries.ModEntities;
import org.mylove.tprt.utilities.Abbr;
import org.mylove.tprt.utilities.DeBug;
import org.mylove.tprt.utilities.Math0;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.text.DecimalFormat;
import java.util.Objects;
import java.util.UUID;


public class FlyingSword extends Entity implements GeoEntity, IEntityAdditionalSpawnData {
    // syncData
    // private static final EntityDataAccessor<String> DATA_MASTER_UUID = SynchedEntityData.defineId(FlyingSword.class, EntityDataSerializers.STRING);
    // private static final EntityDataAccessor<Integer> DATA_SLOT_NUMBER = SynchedEntityData.defineId(FlyingSword.class, EntityDataSerializers.INT);
    // private static final EntityDataAccessor<String> DATA_BEHAVIOR_MODE = SynchedEntityData.defineId(FlyingSword.class, EntityDataSerializers.STRING);
    private static final String MASTER_UUID = "MasterUUID";
    private static final String SLOT_NUMBER = "SlotNumber";
    // private static final String BEHAVIOR_MODE = "BehaviorMode";

    // initialization
    public static final int initLifetime = 40;
    private static final String[] BEHAVIOR_MODE_LIST = {"IDLE", "LAUNCH", "RECOUP"};

    // geckoLib
    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    // normalAttributes
    private Player master;
    private String masterUUID;
    // private IToolStackView tinkerTool;
    private String behaviorMode = "IDLE";
    private int slotNumber;
    private int lifeTime = initLifetime;
    private int lunchCooldown = 0;
    private final int generalFlyingSpeed = 2;
    private Vec3 lunchingModeTarget;
    private Vec3 lunchingModeDelta;
    public boolean noPhysics = true;


    public FlyingSword(EntityType<FlyingSword> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }
    public FlyingSword(IToolStackView tool, Level pLevel, Player player, int slot, ItemStack stack){
        this(ModEntities.FLYING_SWORD.get(), pLevel);

        setMasterUUID(player.getUUID().toString());
        setSlotNumber(slot);

        master = player;

        Abbr.setPlayerSwords(master, slot, this);
    }

    // 基础实体行为设定
    // public boolean isAlwaysTicking() { return true; }
    public boolean isAttackable() {
        return false;
    }
    public boolean skipAttackInteraction(Entity pEntity) {
        return true;
    }
    public boolean isIgnoringBlockTriggers() {
        return true;
    }
    // public float getLightLevelDependentMagicValue() { return 1.0F; }


    @Override
    public void tick() {
        super.tick();
//        this.baseTick();
        if (master == null) {
            // this.discard();
            if(tickCount % 20 == 0) tryFindMaster();
            return;
        }

        lifeTime--;
        if(lifeTime<=0){
            checkValidatedMaster();
        }
        if(Objects.equals(behaviorMode, BEHAVIOR_MODE_LIST[0])){
            // idle
            IdleMode();
        } else if (Objects.equals(behaviorMode, BEHAVIOR_MODE_LIST[1])) {
            // lunching
            LunchingMode();
        } else {
            // recoup
            RecoupingMode();
        }

        // DeBug.Console(this.master, slotNumber+"号飞剑: "+ DeBug.FlatXYZ(this.position()));
    }

    private void tryFindMaster() {
        if(master==null && masterUUID!=null){
            Player p = level().getPlayerByUUID(UUID.fromString(masterUUID));
            if(p!=null) {
                master = p;
                DeBug.Console(this.master, slotNumber+"号飞剑认领成功");
            }
        }
    }

    private void checkValidatedMaster() {
        // 飞剑需要定期确定召唤者在身边，且对应slot的匠魂工具中有此剑的uuid

        //ItemStack hotbarSlot = master.getInventory().getItem(slotNumber);
        //hotbarSlot.readShareTag();
        double distance = master.position().distanceTo(this.position());
        if(distance<=32) {
            lifeTime = initLifetime;
        } else {

            // 纯调试
            DeBug.Console(master, slotNumber+"号剑已销毁\n销毁飞剑位置: "+DeBug.FlatXYZ(this.position()));

            this.discard();
        }
    }



    //    @Override
//    public void baseTick() {
//        this.level().getProfiler().push("flyingSwordBaseTick");
////        this.xRotO = this.getXRot();
////        this.yRotO = this.getYRot();
//        this.clearFire();
//        this.checkBelowWorld();
//        this.level().getProfiler().pop();
//    }

    private void IdleMode() {
        if(this.lunchCooldown>0) this.lunchCooldown--;
        Vec3 delta0 = getDeltaMovement();
        // 无指令时：1.保持在玩家背后 2.跟随移动 3.跟随传送
        Vec3 ownerBack = calculateIdlePos();
        Vec3 current = this.position();
        Vec3 delta = ownerBack.subtract(current);
        double distance = delta.length();

        // if(level().isClientSide) {}

        // 08/22: 1.tick中始终为服务端 2.setPos函数雀食每t更新，发包（或许）不是，本地和服务端数据1s左右同步一次，故本地渲染是跳跃的
        // 08/23: 并非，tick中始终为服务端因为实体只在服务端创建了（看flying_sword_tag的逻辑），这是个bug
        if(this.tickCount % 40 == 0) DeBug.Console(master, slotNumber+"号飞剑客户端"+level().isClientSide);


        if (distance > 0.01) {
            Vec3 motion = delta.normalize().scale(Math0.clamp(1d, distance * 0.25, distance * 1));
            this.setDeltaMovement(motion);

            // todo: 实体移动是跳跃的，有什么办法吗？
            this.setPos(current.add(motion));

            this.move(MoverType.SELF, motion);
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
        Vec3 delta0 = getDeltaMovement();
        Vec3 deltaStatic = this.lunchingModeDelta.scale(this.generalFlyingSpeed);
        if(this.position().distanceTo(this.lunchingModeTarget) >= deltaStatic.scale(0.6).length()){
            this.setPos(this.position().add(deltaStatic));
            this.setDeltaMovement(deltaStatic);
        } else {
            this.setBehaviorMode(2);
        }
    }

    public boolean triggerLunch(Vec3 targetPoint) {
        boolean canLunch = this.lunchCooldown == 0;
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

    private void RecoupingMode() {
        setBehaviorMode(0);
    }


    public String getBehaviorMode() {
        return this.behaviorMode;
    }



    // 数据管理 ========================================
    /** 设置与服务器同步的数据 */
    @Override
    protected void defineSynchedData() {
        //改用生成时一次性同步的数据，因为masterUUID不会改变
        //this.entityData.define(DATA_MASTER_UUID, "");
        //this.entityData.define(DATA_SLOT_NUMBER, 0);
        //this.entityData.define(DATA_BEHAVIOR_MODE, "IDLE");

    }

    /** 实体生成时与服务器同步一次，随后撒手 */
    @Override
    public void writeSpawnData(FriendlyByteBuf buffer) {
        buffer.writeUUID(this.getUUID());
        buffer.writeUtf(this.masterUUID);
    }

    /** 实体生成时与服务器同步一次，随后撒手 */
    @Override
    public void readSpawnData(FriendlyByteBuf buffer) {
        this.setUUID(buffer.readUUID());
        this.setMasterUUID(buffer.readUtf());
    }

    /** 从存档中读取长久保存的数据 */
    @Override
    protected void readAdditionalSaveData(CompoundTag pCompound) {
        if (pCompound.contains(MASTER_UUID, Tag.TAG_STRING)) {
            setMasterUUID(pCompound.getString(MASTER_UUID));
        }

        if (pCompound.contains(SLOT_NUMBER, Tag.TAG_INT)) {
            setSlotNumber(pCompound.getInt(SLOT_NUMBER));
        }
    }

    /** 将数据长久保存到存档 */
    @Override
    protected void addAdditionalSaveData(CompoundTag pCompound) {
        pCompound.putString(MASTER_UUID, masterUUID);
        pCompound.putInt(SLOT_NUMBER, slotNumber);
    }



    // 动画相关 ===================================
    @Override
    public void lerpMotion(double pX, double pY, double pZ) {
        super.lerpMotion(pX, pY, pZ);
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


    // 一些简写 ===================================
    private void setMasterUUID(String uuid) {
        this.masterUUID = uuid;
        // this.entityData.set(DATA_MASTER_UUID, uuid);
        // 世界加载之初玩家尚未加入
//        Player p = level().getPlayerByUUID(UUID.fromString(uuid));
//        if(p!=null){
//            this.master = p;
//            DeBug.Console(this.master, "飞剑认领成功");
//        }
    }

    private void setSlotNumber(int slot) {
        this.slotNumber = slot;
        //this.entityData.set(DATA_SLOT_NUMBER, slot);
    }

    private void setBehaviorMode(int mode) {
        // assume 0 <= mode <= 2
        String toMode = BEHAVIOR_MODE_LIST[mode];
        this.behaviorMode = toMode;
        //this.entityData.set(DATA_BEHAVIOR_MODE, toMode);
        DeBug.Console(master, "号剑切换模式: "+toMode);
    }

}
