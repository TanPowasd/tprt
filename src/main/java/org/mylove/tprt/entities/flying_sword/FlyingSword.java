package org.mylove.tprt.entities.flying_sword;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;
import org.jetbrains.annotations.Nullable;
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
import java.util.UUID;

import static org.mylove.tprt.tags.modifier.flying_sword_tag.PERSISTENT_SLOT;
import static org.mylove.tprt.tags.modifier.flying_sword_tag.PERSISTENT_UUID_KEY;

/**
 * 古希腊掌管射剑的神
 *
 * @important: 由flying_sword_tag里的决策，此实体只运行于服务端上，望周知
 */
public class FlyingSword extends Entity implements GeoEntity, IEntityAdditionalSpawnData {
    // syncData
    // private static final EntityDataAccessor<String> DATA_MASTER_UUID = SynchedEntityData.defineId(FlyingSword.class, EntityDataSerializers.STRING);
    // private static final EntityDataAccessor<Integer> DATA_SLOT_NUMBER = SynchedEntityData.defineId(FlyingSword.class, EntityDataSerializers.INT);
    // private static final EntityDataAccessor<String> DATA_BEHAVIOR_MODE = SynchedEntityData.defineId(FlyingSword.class, EntityDataSerializers.STRING);
    private static final String MASTER_UUID = "MasterUUID";
    private static final String SLOT_NUMBER = "SlotNumber";
    // private static final String BEHAVIOR_MODE = "BehaviorMode";

    // initialization
    public static final int maxLifetime = 40;
    private static final double displayDensity = .3;
    private static final int maxLunchCooldown = 28; // 这也是魔法数, 设计上应当9把剑都存在时可以无缝交替发射, 参考九剑词条中的魔法数
    private static final int windowOfAttackTick = 8; // 决定了剑会多久抵达目标点
    private static final String[] BEHAVIOR_MODE_LIST = {"IDLE", "LAUNCH", "RECOUP"};

    // geckoLib
    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    // animation(respect sakuraTinker)
    private String animationState = "idle";
    private int animationTick = 0;

    // normalAttributes
    public boolean noPhysics = true;
    private Player master;
    private String masterUUID;
    private String behaviorMode = "IDLE";
    private int slotNumber;
    private int lifeTime = maxLifetime;
    private int lunchCooldown = 0;
    private int lunchTickRemaining = 0;
    private boolean isAboutToDiscard = false;
    @Deprecated
    private final int generalFlyingSpeed = 2; // we use "windowOfAttackTick" instead
    @Deprecated
    private Vec3 lunchingModeTarget; // we use getDeltaMovement() now


    public FlyingSword(EntityType<FlyingSword> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }
    public FlyingSword(IToolStackView tool, Level pLevel, Player player, int slot, ItemStack stack){
        this(ModEntities.FLYING_SWORD.get(), pLevel);

        setMasterUUID(player.getUUID().toString());
        setSlotNumber(slot);

        master = player;
        setPos(master.position().add(0,2,0));
        generateSmokeParticle();
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
    public boolean isSilent() {
        return true;
    }
    // public float getLightLevelDependentMagicValue() { return 1.0F; }


    @Override
    public void tick() {
        baseTick();
        if (master == null) {
            if(tickCount % 20 == 0) tryFindMaster();
            return;
        }

        // if(tickCount % 40 == 0) DeBug.Console(master, slotNumber+"号--1"+behaviorMode);

        if(!Objects.equals(behaviorMode, BEHAVIOR_MODE_LIST[1]) && isAboutToDiscard) discard();
        // 飞剑合法性检测: 距离, 维度, 对应物品栏工具
        if(tickCount % 20 == 0) {
            if(!checkToolStackValidate() || !checkDimensionValidate() || !checkDistanceValidate()) discard();
        }

        if(lunchCooldown>0 && !Objects.equals(behaviorMode, BEHAVIOR_MODE_LIST[1])) lunchCooldown--;

        if(Objects.equals(behaviorMode, BEHAVIOR_MODE_LIST[0])){
            // idle
            IdleMode();
        } else if (Objects.equals(behaviorMode, BEHAVIOR_MODE_LIST[1])) {
            // lunching
            LunchingMode();
            checkHitBoxCollide();
        } else {
            // recoup
            RecoupingMode();
        }

        // if(tickCount % 40 == 0) DeBug.Console(master, slotNumber+"号飞剑客户端"+level().isClientSide);
        // if(tickCount % 40 == 0) DeBug.Console(master, slotNumber+"号飞剑: "+ DeBug.FlatXYZ(position()));
    }

    private void tryFindMaster() {
        if(master==null && masterUUID!=null){
            Player p = level().getPlayerByUUID(UUID.fromString(masterUUID));
            if(p!=null) {
                master = p;
                lifeTime = maxLifetime;
                // DeBug.Console(master, slotNumber+"号飞剑认领成功");
            } else {
                lifeTime -= 10;
                if(lifeTime<=0) discard();
            }
        }
    }

    // 飞剑需要定期确定召唤者在身边，且对应slot的匠魂工具中有此剑的uuid
    private boolean checkToolStackValidate() {
        // tinkerTool只有正常用构造器调用时才不为null且该值不写入硬盘, 故重建存档时销毁所有现存飞剑  (飞剑已经渐渐变成消耗品了, 悲
        // 若后续遇到性能问题再考虑优化实现 (例如对tinker工具的引用占用太多内存)
        // 破案了, IToolStackView是工具栈堆快照, 不动态更新
        // todo 基于工具检测的飞剑销毁还有问题
//        if(tinkerTool == null) return false;
//        String uuid =  tinkerTool.getPersistentData().getString(PERSISTENT_UUID_KEY);
//        int slot =  tinkerTool.getPersistentData().getInt(PERSISTENT_SLOT);
//
//        DeBug.Console(master, slotNumber+"号--2\n"+getStringUUID()+"\n"+uuid+"\n"+slot);
//        return !uuid.isEmpty() && uuid.equals(getStringUUID()) && slot == slotNumber;

        // ItemStack stack = master.getInventory().getItem(slotNumber).getItem();
        return true;
    }
    private boolean checkDimensionValidate() {
        // todo 不同维度直接删除
        // getDimensions()
        return true;
    }
    private boolean checkDistanceValidate() {
        double distance = master.position().distanceTo(position());
        return distance <= 64;
    }



    @Override
    public void baseTick() {
        level().getProfiler().push("flyingSwordBaseTick");
        clearFire();
        checkBelowWorld();
        level().getProfiler().pop();
    }

    private void IdleMode() {
        Vec3 delta0 = getDeltaMovement();
        // 无指令时：1.保持在玩家背后 2.跟随移动 3.跟随传送
        Vec3 ownerBack = calculateIdlePos();
        Vec3 current = position();
        Vec3 delta = ownerBack.subtract(current);
        double distance = delta.length();

        // if(level().isClientSide) {}

        // 08/22: 1.tick中始终为服务端 2.setPos函数雀食每t更新，发包（或许）不是，本地和服务端数据1s左右同步一次，故本地渲染是跳跃的
        // 08/23: 并非，tick中始终为服务端因为实体只在服务端创建了（看flying_sword_tag的逻辑），这是个bug
        // 08/23: 并非并非, 见注册类ModEntity

        if (distance > 1.0e-7d) {
            Vec3 motion = delta.normalize().scale(Math0.clamp(.01 + distance*distance*0.1, distance * 0.1, distance * 1));
            setDeltaMovement(motion);

            setPos(current.add(motion));
        } else {
            setDeltaMovement(Vec3.ZERO);
        }
        // lookAt(master);
    }

    private Vec3 calculateIdlePos() {
        // 快捷栏的每把飞剑都有自己对应的位置: 7 5 3 1 0 2 4 6 8
        Vec3 masterBack = master.getLookAngle().scale(-1.5);
        // 构造垂直向量，简单的几何学：他们的点积为零 即perpendicular.dot(masterBack) = 0
        Vec3 perpendicular = new Vec3(masterBack.z, 0, -1 * masterBack.x).normalize();
        double slotPosition = Math.pow(-1, slotNumber) * Math.ceil((double) slotNumber /2) * displayDensity;
        return master.position().add(masterBack.add(perpendicular.scale(slotPosition)));
    }

    private void LunchingMode() {
        // todo: 写两个轨迹函数, 给模型添加旋转, 旋转可以等模型引用成功后再搞
        lunchTickRemaining--;
        Vec3 delta0 = getDeltaMovement();

        // 两种判断哪个好?
        // if(position().distanceTo(lunchingModeTarget) >= delta0.scale(0.6).length()){
        if(lunchTickRemaining>=0){
            setPos(position().add(delta0));
            // setDeltaMovement(delta0);
        } else {
            setBehaviorMode(2);
        }
    }
    private boolean checkHitBoxCollide() {
        return false;
    }

    public boolean triggerLunch(Vec3 targetPoint) {
        if(!canLunch()) return false;
        DeBug.Console(master, slotNumber+"号剑发射");
        setBehaviorMode(1);
        lunchCooldown = maxLunchCooldown;
        lunchTickRemaining = windowOfAttackTick + 1;
        // 发射路径会在一开始就确定好
        Vec3 startingPoint = position();

        lunchingModeTarget = targetPoint;
        Vec3 lunchingModeDelta = targetPoint.subtract(startingPoint).scale((double) 1 / windowOfAttackTick);
        setDeltaMovement(lunchingModeDelta);

        // todo: 应当画弧，但现在先用直线
        // double distance = startingPoint.distanceTo(targetPoint);

        return true;
    }

    private void RecoupingMode() {
        // 返程的弧线是动态的, 不好画先空着
        setBehaviorMode(0);
    }

    /** 获取就绪状态 */
    private boolean canLunch() {
        return lunchCooldown <= 0 && Objects.equals(behaviorMode, BEHAVIOR_MODE_LIST[0]);
    }

    @Override
    public void kill() {
        generateSmokeParticle();
        super.kill();
    }

    // 数据管理 ========================================
    /** 设置与服务器同步的数据 */
    @Override
    protected void defineSynchedData() {
        //改用生成时一次性同步的数据，因为masterUUID不会改变
        //entityData.define(DATA_MASTER_UUID, "");
        //entityData.define(DATA_SLOT_NUMBER, 0);
        //entityData.define(DATA_BEHAVIOR_MODE, "IDLE");
    }

    /** 实体生成时与服务器同步一次，随后撒手 */
    @Override
    public void writeSpawnData(FriendlyByteBuf buffer) {
        buffer.writeUUID(getUUID());
        buffer.writeUtf(masterUUID);
    }

    /** 实体生成时与服务器同步一次，随后撒手 */
    @Override
    public void readSpawnData(FriendlyByteBuf buffer) {
        setUUID(buffer.readUUID());
        setMasterUUID(buffer.readUtf());
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
        return geoCache;
    }

    // use vanilla renderer @sakuraTinker
    public void setAnimationState(String state) {
        if (!animationState.equals(state)) {
            animationState = state;
            animationTick = 0;
        }
    }
    public String getAnimationState() {
        return animationState;
    }
    public float getAnimationProgress(float partialTicks) {
        return (animationTick + partialTicks) / 20f; // 一次动画20 tick
    }
    public Vec3 getSmoothedPosition(float partialTick) {
        return position().add(getDeltaMovement().scale(partialTick));
    }

    /** 在当前位置生成一些烟雾, 在飞剑生成和销毁时调用 */
    public void generateSmokeParticle() {
        for(int i = 0; i < 16; ++i) {
            // 服务端实体用sendParticle
            // level().addParticle(ParticleTypes.CLOUD, getX(), getY() + random.nextDouble() * 2.0D, getZ(), random.nextGaussian(), 0.0D, random.nextGaussian());
            ((ServerLevel) level()).sendParticles(
                    ParticleTypes.CLOUD,
                    getX() + random.nextDouble(),
                    getY() + random.nextDouble() * 2.0D,
                    getZ() + random.nextDouble(),
                    1,
                    random.nextGaussian(),
                    0.0D,
                    random.nextGaussian(),
                    1.0D
            );
        }
    }

    // 对外方法 ===================================
    /** 外部调用的销毁函数, 会在一个攻击周期后自动销毁 */
    public void setToDiscard(@Nullable String message) {
        DeBug.Console(master, slotNumber+"号剑将销毁: "+message);
        isAboutToDiscard = true;
    }

    public int getSlotNumber() {
        return slotNumber;
    }



    // 一些简写 ===================================
    public String getBehaviorMode() {
        return behaviorMode;
    }

    private void setMasterUUID(String uuid) {
        masterUUID = uuid;
        // entityData.set(DATA_MASTER_UUID, uuid);
        // 世界加载之初玩家尚未加入
//        Player p = level().getPlayerByUUID(UUID.fromString(uuid));
//        if(p!=null){
//            master = p;
//        }
    }

    private void setSlotNumber(int slot) {
        slotNumber = slot;
        //entityData.set(DATA_SLOT_NUMBER, slot);
    }

    /** @param mode: 0--IDLE; 1--LUNCH; 2--RECOUP */
    private void setBehaviorMode(int mode) {
        // assume 0 <= mode <= 2
        String toMode = BEHAVIOR_MODE_LIST[mode];
        behaviorMode = toMode;
        //entityData.set(DATA_BEHAVIOR_MODE, toMode);
        //DeBug.Console(master, slotNumber+"号剑切换模式: "+toMode);
    }

}
