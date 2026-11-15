package org.mylove.tprt.common.chtholly;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.TraceableEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ThrownEnderpearl;
import net.minecraft.world.entity.projectile.ThrownExperienceBottle;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.entity.PartEntity;
import org.jetbrains.annotations.Nullable;
import org.mylove.tprt.registries.ModDamageSource;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.helper.ToolAttackUtil;
import slimeknights.tconstruct.library.tools.item.IModifiable;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 「挚爱」(chtholly) 体系跨词条战斗底层公共工具集:
 * 1) 多部件 Boss (PartEntity) 本体解析与纯粹真伤安全结算（保留正常死亡掉落/经验/事件触发，带全局防递归锁）
 * 2) 脱手/跨空间匠魂近战武器斩击（通过 toolAttributes 精确读取武器自身面板与词条属性，静默槽位互换不触发装备音效与双槽引用）
 * 3) 安全敌对弹射物拦截（严格保护联机队友、三叉戟、匠魂投掷武器/手里剑/悠悠球、末影珍珠与浮漂，绝不误删玩家装备）
 * 4) 跨模组攻击源玩家归属解析与友军/宠物/非 PVP 玩家过滤
 */
public final class ChthollyCombatHelper {

    private static final ThreadLocal<Set<UUID>> PURE_DAMAGE_TARGETS = ThreadLocal.withInitial(HashSet::new);
    private static final ThreadLocal<Integer> PURE_DAMAGE_DEPTH = ThreadLocal.withInitial(() -> 0);
    private static final ThreadLocal<Boolean> OFF_SLOT_ATTACKING = ThreadLocal.withInitial(() -> false);

    private ChthollyCombatHelper() {
    }

    public static boolean isDealingPureDamage() {
        return PURE_DAMAGE_DEPTH.get() > 0;
    }

    public static boolean isDealingPureDamage(@Nullable Entity entity) {
        if (PURE_DAMAGE_DEPTH.get() <= 0 || entity == null) return false;
        Set<UUID> set = PURE_DAMAGE_TARGETS.get();
        if (set.contains(entity.getUUID())) return true;
        LivingEntity root = resolveLivingTarget(entity);
        return root != null && set.contains(root.getUUID());
    }

    public static boolean isPerformingOffSlotAttack() {
        return OFF_SLOT_ATTACKING.get();
    }

    /**
     * 解析实体对应的实际 LivingEntity 本体（支持嵌套 PartEntity 多部件 Boss，如末影龙、灾变与冰火传说 Boss）
     */
    @Nullable
    public static LivingEntity resolveLivingTarget(@Nullable Entity entity) {
        Entity cursor = entity;
        for (int depth = 0; depth < 5 && cursor != null; depth++) {
            if (cursor instanceof LivingEntity living) {
                return living;
            }
            if (cursor instanceof PartEntity<?> part) {
                cursor = part.getParent();
            } else {
                break;
            }
        }
        return null;
    }

    /**
     * 清除目标及其多部件子实体的无敌帧
     */
    public static void resetInvulnerableFrames(@Nullable Entity entity) {
        if (entity == null) return;
        entity.invulnerableTime = 0;
        if (entity instanceof LivingEntity living) {
            living.hurtTime = 0;
        }
        if (entity.isMultipartEntity()) {
            PartEntity<?>[] parts = entity.getParts();
            if (parts != null) {
                for (PartEntity<?> part : parts) {
                    if (part != null) {
                        part.invulnerableTime = 0;
                    }
                }
            }
        }
    }

    public static DamageSource createPureDamageSource(LivingEntity attacker) {
        return new DamageSource(
                attacker.level().registryAccess()
                        .registryOrThrow(Registries.DAMAGE_TYPE)
                        .getHolderOrThrow(ModDamageSource.TPRT_PURE_DAMAGE_TYPE),
                attacker,
                attacker
        );
    }

    /**
     * 跨模组纯粹真伤结算:
     * 仅通过标准 hurt(TPRT_PURE_DAMAGE_TYPE) 穿透护甲/魔抗/附魔并完整触发 LivingDeathEvent 与战利品掉落，
     * 严禁使用 setHealth(0) 吞没掉落物或绕过伤害吸收金心造成双倍伤害。
     */
    public static boolean dealPureDamage(LivingEntity attacker, @Nullable Entity rawTarget, float amount) {
        if (amount <= 0.0f || !Float.isFinite(amount)) return false;
        LivingEntity target = resolveLivingTarget(rawTarget);
        if (target == null || !target.isAlive() || target.isDeadOrDying()) return false;

        UUID targetId = target.getUUID();
        Set<UUID> guardSet = PURE_DAMAGE_TARGETS.get();
        if (!guardSet.add(targetId)) {
            return false;
        }

        int prevDepth = PURE_DAMAGE_DEPTH.get();
        PURE_DAMAGE_DEPTH.set(prevDepth + 1);
        try {
            int prevInvuln = target.invulnerableTime;
            int prevHurtTime = target.hurtTime;
            resetInvulnerableFrames(target);

            boolean hit = target.hurt(createPureDamageSource(attacker), amount);

            if (target.isAlive() && !target.isDeadOrDying()) {
                target.invulnerableTime = prevInvuln;
                target.hurtTime = prevHurtTime;
            }
            return hit;
        } finally {
            guardSet.remove(targetId);
            if (prevDepth == 0) {
                PURE_DAMAGE_DEPTH.remove();
                PURE_DAMAGE_TARGETS.remove();
            } else {
                PURE_DAMAGE_DEPTH.set(prevDepth);
            }
        }
    }

    /**
     * 脱手/跨空间执行单次或多次完整匠魂近战武器斩击:
     * 1) 使用 ToolAttackContext.Builder#toolAttributes(weaponTool) 精确计算该脱手武器的基础面板与词条属性修饰符（修复手持弓/法术书时只有 1.0 拳头伤害的问题）;
     * 2) 通过直接静默交换 Inventory 底层槽位引用，使外部主手钩子 (KillingHook / DamageRedirectHook / SakuraTinker) 正常识别该武器，
     *    同时不触发 setItemInHand 的装备音效/游戏振动事件，也不在两个槽位同时暴露同一 ItemStack 引用。
     */
    public static boolean performOffSlotAttacks(Player player, ItemStack weaponStack, IToolStackView weaponTool,
                                                LivingEntity target, int strikeCount, float minKnockback,
                                                @Nullable StrikeCallback afterEachStrike) {
        if (strikeCount <= 0 || weaponStack.isEmpty() || weaponTool.isBroken() || !target.isAlive()) {
            return false;
        }
        if (OFF_SLOT_ATTACKING.get()) {
            return false;
        }

        Inventory inv = player.getInventory();
        int selected = inv.selected;
        if (selected < 0 || selected >= inv.items.size()) {
            return false;
        }

        // 查找 weaponStack 所在的原始槽位 (-1 = 不在背包/副手, -2 = 副手, >=0 = items 槽位)
        int sourceSlot = -1;
        if (inv.offhand.get(0) == weaponStack) {
            sourceSlot = -2;
        } else {
            for (int i = 0; i < inv.items.size(); i++) {
                if (inv.items.get(i) == weaponStack) {
                    sourceSlot = i;
                    break;
                }
            }
        }

        boolean wasSprinting = player.isSprinting();
        Vec3 prevVelocity = player.getDeltaMovement();
        ItemStack prevMain = inv.items.get(selected);
        boolean needSwap = (prevMain != weaponStack);

        OFF_SLOT_ATTACKING.set(true);
        boolean anyHit = false;
        try {
            if (needSwap) {
                inv.items.set(selected, weaponStack);
                if (sourceSlot == -2) {
                    inv.offhand.set(0, prevMain);
                } else if (sourceSlot >= 0 && sourceSlot != selected) {
                    inv.items.set(sourceSlot, prevMain);
                }
            }

            for (int i = 0; i < strikeCount; i++) {
                if (!target.isAlive() || target.isDeadOrDying() || weaponTool.isBroken()) {
                    break;
                }
                resetInvulnerableFrames(target);

                float toolKnockback = ToolAttackUtil.getToolAttribute(
                        weaponTool, player, Attributes.ATTACK_KNOCKBACK, 0.0f);
                ToolAttackContext context = ToolAttackContext.attacker(player)
                        .target(target)
                        .hand(InteractionHand.MAIN_HAND)
                        .toolAttributes(weaponTool)
                        .baseKnockback(Math.max(minKnockback, toolKnockback))
                        .cooldown(1.0f)
                        .extraAttack()
                        .build();

                if (ToolAttackUtil.performAttack(weaponTool, context)) {
                    anyHit = true;
                }
                if (afterEachStrike != null) {
                    afterEachStrike.onStrike(i);
                }
            }
        } finally {
            if (needSwap) {
                if (sourceSlot == -2) {
                    inv.offhand.set(0, weaponStack);
                } else if (sourceSlot >= 0 && sourceSlot != selected) {
                    inv.items.set(sourceSlot, weaponStack);
                }
                inv.items.set(selected, prevMain);
            }
            OFF_SLOT_ATTACKING.remove();
            if (wasSprinting) {
                player.setSprinting(true);
            }
            player.setDeltaMovement(prevVelocity);
        }
        return anyHit;
    }

    @FunctionalInterface
    public interface StrikeCallback {
        void onStrike(int strikeIndex);
    }

    /**
     * 解析跨模组伤害来源中的玩家归属（支持直接攻击、投射物、法术与召唤物/从属实体）
     */
    @Nullable
    public static Player resolvePlayerAttacker(@Nullable DamageSource source) {
        if (source == null) return null;
        Entity direct = source.getDirectEntity();
        if (direct instanceof Player p) return p;
        Entity causer = source.getEntity();
        if (causer instanceof Player p) return p;
        if (causer instanceof OwnableEntity ownable && ownable.getOwner() instanceof Player p) return p;
        if (causer instanceof TraceableEntity traceable && traceable.getOwner() instanceof Player p) return p;
        if (direct instanceof OwnableEntity ownable && ownable.getOwner() instanceof Player p) return p;
        if (direct instanceof TraceableEntity traceable && traceable.getOwner() instanceof Player p) return p;
        return null;
    }

    /**
     * 统一敌对目标合法性校验（过滤自身、死亡/旁观、盔甲架、同队友军、非 PVP 联机玩家、自身及友方宠物/召唤物、同载具乘客）
     */
    public static boolean isValidHostileTarget(LivingEntity attacker, @Nullable Entity rawCandidate) {
        LivingEntity candidate = resolveLivingTarget(rawCandidate);
        if (candidate == null || candidate == attacker || !candidate.isAlive() || candidate.isDeadOrDying()) {
            return false;
        }
        if (candidate.isSpectator() || candidate instanceof ArmorStand) {
            return false;
        }
        if (attacker.isAlliedTo(candidate) || attacker.isPassengerOfSameVehicle(candidate)) {
            return false;
        }
        if (attacker instanceof Player atkPlayer && candidate instanceof Player targetPlayer) {
            if (!atkPlayer.canHarmPlayer(targetPlayer)) {
                return false;
            }
        }
        if (candidate instanceof OwnableEntity ownable) {
            UUID ownerId = ownable.getOwnerUUID();
            if (ownerId != null) {
                if (attacker.getUUID().equals(ownerId)) return false;
                Entity owner = ownable.getOwner();
                if (owner != null && (attacker.isAlliedTo(owner)
                        || (attacker instanceof Player atkPlayer && owner instanceof Player ownerPlayer
                        && !atkPlayer.canHarmPlayer(ownerPlayer)))) {
                    return false;
                }
            }
        }
        if (candidate instanceof TraceableEntity traceable) {
            Entity owner = traceable.getOwner();
            if (owner != null) {
                if (owner == attacker || attacker.isAlliedTo(owner)) return false;
                if (attacker instanceof Player atkPlayer && owner instanceof Player ownerPlayer
                        && !atkPlayer.canHarmPlayer(ownerPlayer)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * 安全拦截周围飞向玩家的敌对弹射物:
     * - 绝不拦截自身、同队、非 PVP 联机队友及其召唤物发射的弹射物;
     * - 绝不销毁钓鱼浮漂、末影珍珠、末影之眼、三叉戟或携带可拾取玩家武器的投掷实体（对敌对玩家投掷的三叉戟/武器仅做弹开偏转，不删物品）。
     */
    public static int parryIncomingProjectiles(ServerLevel serverLevel, Player player, double radius) {
        AABB box = player.getBoundingBox().inflate(radius);
        List<Projectile> projectiles = serverLevel.getEntitiesOfClass(Projectile.class, box,
                proj -> isHostileIncomingProjectile(player, proj));
        if (projectiles.isEmpty()) return 0;

        int parried = 0;
        for (Projectile proj : projectiles) {
            double px = proj.getX();
            double py = proj.getY() + proj.getBbHeight() * 0.5D;
            double pz = proj.getZ();

            serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK, px, py, pz, 1, 0.0, 0.0, 0.0, 0.0);
            serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, px, py, pz, 6, 0.15, 0.15, 0.15, 0.03);
            serverLevel.sendParticles(ParticleTypes.END_ROD, px, py, pz, 4, 0.1, 0.1, 0.1, 0.02);

            if (isValuableOrIndestructibleProjectile(proj)) {
                // 对玩家投掷的三叉戟、匠魂投枪/飞镖等实体武器仅做剑气格挡弹开，绝不 discard 删除武器
                Vec3 away = proj.position().subtract(player.getEyePosition());
                Vec3 bounce = away.lengthSqr() > 1.0E-4D ? away.normalize().scale(0.45D) : proj.getDeltaMovement().scale(-0.35D);
                proj.setDeltaMovement(bounce.x, Math.max(0.1D, bounce.y), bounce.z);
                proj.hurtMarked = true;
            } else {
                proj.discard();
            }
            parried++;
        }

        if (parried > 0) {
            serverLevel.playSound(null, player.getX(), player.getY() + 1.0, player.getZ(),
                    SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 0.75f, 1.45f);
            serverLevel.playSound(null, player.getX(), player.getY() + 1.0, player.getZ(),
                    SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8f, 1.35f);
        }
        return parried;
    }

    private static boolean isHostileIncomingProjectile(Player player, Projectile proj) {
        if (proj == null || !proj.isAlive() || proj.onGround()) return false;
        // 永不拦截功能性非伤害投掷物
        if (proj instanceof FishingHook || proj instanceof ThrownEnderpearl
                || proj instanceof ThrownExperienceBottle) {
            return false;
        }
        String projClass = proj.getClass().getName();
        if (projClass.contains("YoYo") || projClass.contains("Yoyo") || projClass.contains("Hook")
                || projClass.contains("Grapple") || projClass.contains("Bobber")) {
            return false;
        }

        Entity owner = proj.getOwner();
        if (owner == null) {
            // 无主且为玩家可拾取武器（如插地弹起的三叉戟/投枪）绝不触碰
            if (isValuableOrIndestructibleProjectile(proj)) {
                return false;
            }
        } else {
            if (owner == player || player.isAlliedTo(owner)) return false;
            if (owner instanceof Player otherPlayer && !player.canHarmPlayer(otherPlayer)) {
                return false;
            }
            if (owner instanceof OwnableEntity ownable) {
                if (player.getUUID().equals(ownable.getOwnerUUID())) return false;
                if (ownable.getOwner() instanceof Player ownerPlayer && !player.canHarmPlayer(ownerPlayer)) {
                    return false;
                }
            }
            if (owner instanceof TraceableEntity traceable) {
                Entity rootOwner = traceable.getOwner();
                if (rootOwner == player) return false;
                if (rootOwner instanceof Player ownerPlayer && !player.canHarmPlayer(ownerPlayer)) {
                    return false;
                }
            }
        }

        Vec3 toPlayer = player.getEyePosition().subtract(proj.position());
        Vec3 motion = proj.getDeltaMovement();
        if (motion.lengthSqr() < 1.0E-4D) return false;
        // 必须正在朝玩家方向逼近 (点积 > 0)，避免把刚被弹开或背向飞离的投射物重复拦截
        return motion.dot(toPlayer) > 0.05D;
    }

    private static boolean isValuableOrIndestructibleProjectile(Projectile proj) {
        if (proj instanceof ThrownTrident) {
            return true;
        }
        if (proj instanceof AbstractArrow arrow) {
            if (arrow.pickup == AbstractArrow.Pickup.ALLOWED && proj.getOwner() instanceof Player) {
                return true;
            }
        }
        if (proj instanceof ItemSupplier supplier) {
            ItemStack item = supplier.getItem();
            if (!item.isEmpty() && (item.isDamageableItem() || item.getItem() instanceof IModifiable)) {
                return true;
            }
        }
        String className = proj.getClass().getName();
        return className.contains("Trident") || className.contains("Javelin")
                || className.contains("Boomerang") || className.contains("Returning")
                || className.contains("Shuriken") || className.contains("ThrowingAxe")
                || className.contains("Spear");
    }
}
