package org.mylove.tprt.entities.flying_sword;

import com.mojang.logging.LogUtils;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;
import org.jetbrains.annotations.Nullable;
import org.mylove.tprt.registries.ModEntities;
import org.mylove.tprt.utilities.Abbr;
import org.mylove.tprt.utilities.DeBug;
import org.mylove.tprt.utilities.Math0;
import org.slf4j.Logger;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.Objects;
import java.util.UUID;

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
    private static final EntityDataAccessor<ItemStack> DATA_ITEM_STACK = SynchedEntityData.defineId(FlyingSword.class, EntityDataSerializers.ITEM_STACK);
    private static final String MASTER_UUID = "MasterUUID";
    private static final String SLOT_NUMBER = "SlotNumber";
    // private static final String BEHAVIOR_MODE = "BehaviorMode";
    private static final String ITEM_STACK = "ItemStack";

    // initialization
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final int maxLifetime = 40;
    private static final double displayDensity = .3;
    private static final int maxLunchCooldown = 18; // 这也是魔法数, 设计上应当9把剑都存在时可以无缝交替发射, 参考九剑词条中的魔法数
    private static final int windowOfAttackTick = 8; // 决定了剑会多久抵达目标点
    private static final String[] BEHAVIOR_MODE_LIST = {"IDLE", "LAUNCH", "RECOUP"};

    // geckoLib
    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    // normalAttributes
    public boolean noPhysics = true;
    private Player master;
    private String masterUUID;
    private String behaviorMode = "IDLE";
    private ItemStack itemStack;
    private int slotNumber;
    private int lifeTime = maxLifetime;
    private int lunchCooldown = 0;
    private int lunchTickRemaining = 0;
    private int recoupTickRemaining = 0;
    private boolean isAboutToDiscard = false;
    @Deprecated
    private final int generalFlyingSpeed = 2; // we use "windowOfAttackTick" instead
    private Vec3 lunchInitPosition;
    private Vec3 lunchModeTarget;
    private float lunchPitchRadius;
    private float lunchYawRadius;
    private float lunchVerticalRandom;
    /** 短轴 */
    private double lunchEllipseShort;
    /** 长轴 */
    private double lunchEllipseMajor;


    public FlyingSword(EntityType<FlyingSword> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }
    public FlyingSword(IToolStackView tool, Level pLevel, Player player, int slot, ItemStack stack){
        this(ModEntities.FLYING_SWORD.get(), pLevel);

        setMasterUUID(player.getUUID().toString());
        setSlotNumber(slot);
        setItemStack(stack);

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
    }

    private void tryFindMaster() {
        if(master==null && masterUUID!=null){
            Player p = level().getPlayerByUUID(UUID.fromString(masterUUID));
            if(p!=null) {
                master = p;
                lifeTime = maxLifetime;
            } else {
                lifeTime -= 10;
                if(lifeTime<=0) discard();
            }
        }
    }

    // 飞剑需要定期确定召唤者在身边，且对应slot的匠魂工具中有此剑的uuid
    private boolean checkToolStackValidate() {
        if(itemStack == null) return false;
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
        this.xRotO = this.getXRot();
        this.yRotO = this.getYRot();
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
            // todo: 设置一个合适的跟随速度, 当前会抽搐, 大概是单次移动距离过大的原因
            Vec3 motion = delta.normalize().scale(Math0.clamp(1.0e-7d + distance*distance*0.5, distance * 0.3, distance * 1));
            setDeltaMovement(motion);

            setPos(current.add(motion));
        } else {
            setDeltaMovement(Vec3.ZERO);
        }
        // lookAt(master);
    }

    private Vec3 calculateIdlePos() {
        // 快捷栏的每把飞剑都有自己对应的位置: 7 5 3 1 0 2 4 6 8
        Vec3 masterBack, back, perpendicular, verOffset, position0, lookingAngle;
        double slotCoefficient, horizontalOffset, verticalOffset;

        position0 = position();
        lookingAngle = master.getLookAngle();
        if(lookingAngle.x == 0 && lookingAngle.z == 0) return position0; // 避免抬头望天时缩成一团

        slotCoefficient = Math.ceil((double) slotNumber /2);
        horizontalOffset = Math.pow(-1, slotNumber) * slotCoefficient * displayDensity;
        verticalOffset = .3 - slotCoefficient * .1;

        masterBack = lookingAngle.scale(-1.2 + 0.1 * slotCoefficient); // 排列为弧形
        back = new Vec3(masterBack.x, 0, masterBack.z).normalize();
        // 构造垂直向量
        perpendicular = new Vec3(masterBack.z, 0, -1 * masterBack.x).normalize();
        verOffset = new Vec3(0, verticalOffset, 0);

        return master.position().add(back.add(perpendicular.scale(horizontalOffset)).add(verOffset));
    }

    private void LunchingMode() {
        // todo: 给模型添加旋转, 旋转可以等模型引用成功后再搞
        lunchTickRemaining--;
        if(lunchTickRemaining>=0){

            // 写一个简单的椭球, 定义长半轴为a, 两个短半轴均为b
            // pitch, yaw 两变量可参考此文
            // https://www.jianshu.com/p/a824e3cc4573
            double a, b, x, z, tmp;
            a = lunchEllipseMajor / 2;
            b = lunchEllipseShort / 2;

            int t = windowOfAttackTick - lunchTickRemaining;
            z = lunchEllipseMajor * t / windowOfAttackTick;
            tmp = b*b - b*b * (z-a)*(z-a) / (a*a);
//            boolean minus = tmp <= 0;
            // 左侧-1, 右侧+1
            float leftOrRight = (float) Math.pow(-1, slotNumber);
            x = leftOrRight * Math.sqrt(Math.abs(tmp));

            // 角度稍稍错开让轨迹不那么单调
            Vec3 posEllipse = new Vec3(x,0,z).zRot(-1 * leftOrRight * lunchVerticalRandom).xRot(lunchPitchRadius).yRot(lunchYawRadius);

            Vec3 pos0 = position();
            setPos(lunchInitPosition.add(posEllipse));
            setDeltaMovement(position().subtract(pos0));

        } else {
            setBehaviorMode(2);
            recoupTickRemaining = windowOfAttackTick;
            // 立刻调用一次, 不然在目标点有1tick停顿
            RecoupingMode();
        }
    }
    private boolean checkHitBoxCollide() {
        // todo 引用玩家攻击 参: @sakuraTinker 工匠箭矢
        // 碰撞和实体交互
        return false;
    }

    public boolean triggerLunch(Vec3 targetPoint, float pitch, float yaw) {
        if(!canLunch()) return false;
        setBehaviorMode(1);
        lunchCooldown = maxLunchCooldown;
        lunchTickRemaining = windowOfAttackTick;
        // 发射路径会在一开始就确定好
        lunchInitPosition = position();
        lunchModeTarget = targetPoint;
        lunchVerticalRandom = (float) Math.toRadians(Math.random() * 30);
        lunchEllipseMajor = position().distanceTo(lunchModeTarget);
        lunchEllipseShort = 2 * lunchEllipseMajor / 3;

        // 应当基于飞剑位置求出yaw和pitch, 用player传进来的会产生偏差
        Vec3 deltaV = position().vectorTo(targetPoint);
        lunchYawRadius = (float) Math.atan2(deltaV.x, deltaV.z);
        lunchPitchRadius = (float) Math.atan2(deltaV.y, Math.sqrt(deltaV.x * deltaV.x + deltaV.z * deltaV.z));

        return true;
    }

    private void RecoupingMode() {
        recoupTickRemaining--;
        if(recoupTickRemaining>=0){
            // 同发射时相同, 不过这边椭圆随玩家位置动态更新, 正如追踪导弹随目标位置调整自己轨迹, 飞剑也是如此
            double a, b, x, z, tmp, longAxis, dYaw, dPitch;
            Vec3 deltaV;
            deltaV = lunchModeTarget.vectorTo(calculateIdlePos());
            longAxis = lunchModeTarget.distanceTo(calculateIdlePos());
            // 回程的yaw & pitch通过向量夹角算出来
            dYaw = Math.atan2(deltaV.x, deltaV.z);
            dPitch = Math.atan2(deltaV.y, Math.sqrt(deltaV.x * deltaV.x + deltaV.z * deltaV.z));

            a = longAxis / 2;
            b = 2 * a / 3;

            int t = windowOfAttackTick - recoupTickRemaining;
            z = longAxis * t / windowOfAttackTick;
            tmp = b*b - b*b * (z-a)*(z-a) / (a*a);
            // 左侧-1, 右侧+1
            float leftOrRight = (float) Math.pow(-1, slotNumber);
            x = leftOrRight * Math.sqrt(Math.abs(tmp));

            // 随机z角同发射时角度一致, 方向相反
            Vec3 posEllipse = new Vec3(x,0,z).zRot(leftOrRight * lunchVerticalRandom).xRot((float) dPitch).yRot((float) dYaw);

            Vec3 pos0 = position();
            setPos(lunchModeTarget.add(posEllipse));
            setDeltaMovement(position().subtract(pos0));
        } else {
            setBehaviorMode(0);
        }
    }

    private void faceMaster() {
        facePoint(master.position());
    }
    private void facePoint(Vec3 point) {

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
        entityData.define(DATA_ITEM_STACK, ItemStack.EMPTY);
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

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
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
        // DeBug.Console(master, slotNumber+"号剑将销毁: "+message);
        isAboutToDiscard = true;
    }

    public int getSlotNumber() {
        return slotNumber;
    }

    @Nullable
    public ItemStack getItemStack() {
        return getEntityData().get(DATA_ITEM_STACK);
    }



    // 一些简写 ===================================
    public String getBehaviorMode() {
        return behaviorMode;
    }

    private void setMasterUUID(String uuid) {
        masterUUID = uuid;
        // entityData.set(DATA_MASTER_UUID, uuid);
    }
    private void setSlotNumber(int slot) {
        slotNumber = slot;
        //entityData.set(DATA_SLOT_NUMBER, slot);
    }
    /** @param mode: 0--IDLE; 1--LUNCH; 2--RECOUP */
    private void setBehaviorMode(int mode) {
        // todo: 把状态机换成enum类型
        // assume 0 <= mode <= 2
        String toMode = BEHAVIOR_MODE_LIST[mode];
        behaviorMode = toMode;
        //entityData.set(DATA_BEHAVIOR_MODE, toMode);
    }
    private void setItemStack(ItemStack stack) {
        itemStack = stack;
        // LOGGER.debug("setItemStack", stack.toString());
        getEntityData().set(DATA_ITEM_STACK, stack);
    }


    //
    static {
        enum ABCDEF{}
    }
}
