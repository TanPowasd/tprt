package org.mylove.tprt.common.modifier;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.phys.Vec3;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.interaction.GeneralInteractionModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.InteractionSource;
import slimeknights.tconstruct.library.modifiers.impl.NoLevelsModifier;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.helper.ToolAttackUtil;
import slimeknights.tconstruct.library.tools.helper.ToolDamageUtil;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.UUID;

/** Active interaction trait: one hit per target, with a fixed window after the first hit. */
public class LanceChargeModifier extends NoLevelsModifier implements GeneralInteractionModifierHook {
    public static final int HIT_WINDOW_TICKS = 40;
    private static final double ATTACK_LENGTH = 3.5D;
    private static final double ATTACK_HALF_WIDTH = 0.75D;
    // Weak player keys avoid retaining disconnected players; state is never shared between sides.
    private final Map<Player, Charge> charges = Collections.synchronizedMap(new WeakHashMap<>());

    private static final class Charge {
        final InteractionHand hand;
        final Set<UUID> targets = new HashSet<>();
        long deadline = Long.MAX_VALUE;
        double previousX, previousZ;
        Charge(InteractionHand hand, Player player) {
            this.hand = hand;
            previousX = player.getX();
            previousZ = player.getZ();
        }
    }

    @Override
    protected void registerHooks(ModuleHookMap.Builder builder) {
        super.registerHooks(builder);
        builder.addHook(this, ModifierHooks.GENERAL_INTERACT);
    }

    @Override
    public InteractionResult onToolUse(IToolStackView tool, ModifierEntry modifier, Player player,
                                       InteractionHand hand, InteractionSource source) {
        if (source != InteractionSource.RIGHT_CLICK || !ToolAttackUtil.canPerformAttack(tool)) return InteractionResult.PASS;
        if (!player.level().isClientSide) charges.put(player, new Charge(hand, player));
        GeneralInteractionModifierHook.startUsing(tool, modifier.getId(), player, hand);
        return InteractionResult.CONSUME;
    }

    @Override public int getUseDuration(IToolStackView tool, ModifierEntry modifier) { return 72000; }
    @Override public UseAnim getUseAction(IToolStackView tool, ModifierEntry modifier) { return UseAnim.NONE; }

    @Override
    public void onUsingTick(IToolStackView tool, ModifierEntry modifier, LivingEntity living, int remaining) {
        if (living.level().isClientSide || !(living instanceof Player player)) return;
        Charge charge = charges.get(player);
        if (charge == null) { player.stopUsingItem(); return; }
        long now = player.level().getGameTime();
        // Deadline is checked even when stationary, and is never extended by later hits.
        if (now >= charge.deadline || !ToolAttackUtil.canPerformAttack(tool)) {
            finish(player);
            return;
        }
        double dx = player.getX() - charge.previousX;
        double dz = player.getZ() - charge.previousZ;
        charge.previousX = player.getX();
        charge.previousZ = player.getZ();
        double speed = Math.sqrt(dx * dx + dz * dz);
        if (speed <= 0.05D) return;
        double yaw = Math.toRadians(player.yBodyRot);
        Vec3 look = new Vec3(-Math.sin(yaw), 0.0D, Math.cos(yaw));
        // Oriented 1x3 rectangle: centered on the body, from its front edge
        // to three blocks ahead. AABB below is broad-phase only.
        Vec3 side = new Vec3(-look.z, 0.0D, look.x);
        // Project the rider and every vehicle in the riding chain onto the
        // attack direction. The foremost AABB support plane is the start plane,
        // including off-center seats and non-square modded mounts.
        double front = frontDistance(player.getBoundingBox(), player.position(), look);
        double minY = player.getBoundingBox().minY;
        for (var vehicle = player.getVehicle(); vehicle != null; vehicle = vehicle.getVehicle()) {
            front = Math.max(front, frontDistance(vehicle.getBoundingBox(), player.position(), look));
            minY = Math.min(minY, vehicle.getBoundingBox().minY);
        }
        Vec3 start = player.position().add(look.scale(front));
        Vec3 center = start.add(look.scale(ATTACK_LENGTH * 0.5D));
        double extentX = Math.abs(look.x) * ATTACK_LENGTH * 0.5D + Math.abs(side.x) * ATTACK_HALF_WIDTH;
        double extentZ = Math.abs(look.z) * ATTACK_LENGTH * 0.5D + Math.abs(side.z) * ATTACK_HALF_WIDTH;
        var body = player.getBoundingBox();
        var box = new net.minecraft.world.phys.AABB(center.x - extentX, minY, center.z - extentZ,
                center.x + extentX, body.maxY, center.z + extentZ);
        EquipmentSlot slot = charge.hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
        for (LivingEntity target : player.level().getEntitiesOfClass(LivingEntity.class, box,
                e -> e != player && e.isAlive() && !player.isPassengerOfSameVehicle(e) && !player.isAlliedTo(e))) {
            if (charge.targets.contains(target.getUUID()) || !player.hasLineOfSight(target)
                    || !intersectsAttackRectangle(target.getBoundingBox(), center, look, side)
                    || !ToolAttackUtil.isAttackable(player, target)) continue;
            double bonus = Math.max(0.0D, (speed - 0.13D) * 3.0D);
            float damage = ToolAttackUtil.getAttributeAttackDamage(tool, player, slot);
            float knockback = ToolAttackUtil.getSlotAttribute(tool, player, slot, Attributes.ATTACK_KNOCKBACK, 0.0F) / 2.0F;
            ToolAttackContext context = ToolAttackContext.attacker(player).target(target).hand(charge.hand)
                    .baseDamage((float) (damage * (1.0D + bonus))).baseKnockback(knockback).cooldown(1.0F).build();
            boolean sprinting = player.isSprinting();
            Vec3 movement = player.getDeltaMovement();
            boolean hit;
            try {
                hit = ToolAttackUtil.performAttack(tool, context);
            } finally {
                // TC sprint knockback normally cancels sprint and damps movement after an attack.
                if (sprinting) {
                    player.setSprinting(true);
                    player.setDeltaMovement(movement);
                }
            }
            if (!hit) continue;
            charge.targets.add(target.getUUID());
            if (charge.deadline == Long.MAX_VALUE) charge.deadline = now + HIT_WINDOW_TICKS;
            ToolDamageUtil.damageAnimated(tool, 10, player, charge.hand);
            if (!ToolAttackUtil.canPerformAttack(tool)) { finish(player); return; }
        }
    }

    @Override
    public void onStoppedUsing(IToolStackView tool, ModifierEntry modifier, LivingEntity living, int remaining) {
        if (living instanceof Player player) charges.remove(player);
    }

    private static double frontDistance(net.minecraft.world.phys.AABB box, Vec3 origin, Vec3 forward) {
        double x = forward.x >= 0.0D ? box.maxX : box.minX;
        double z = forward.z >= 0.0D ? box.maxZ : box.minZ;
        return (x - origin.x) * forward.x + (z - origin.z) * forward.z;
    }

    /** Separating-axis test on the rectangle's two axes (world X/Z checked by broad phase). */
    private static boolean intersectsAttackRectangle(net.minecraft.world.phys.AABB target,
                                                     Vec3 center, Vec3 forward, Vec3 side) {
        double dx = (target.minX + target.maxX) * 0.5D - center.x;
        double dz = (target.minZ + target.maxZ) * 0.5D - center.z;
        double halfX = target.getXsize() * 0.5D;
        double halfZ = target.getZsize() * 0.5D;
        double along = Math.abs(dx * forward.x + dz * forward.z);
        double across = Math.abs(dx * side.x + dz * side.z);
        return along <= ATTACK_LENGTH * 0.5D + halfX * Math.abs(forward.x) + halfZ * Math.abs(forward.z)
                && across <= ATTACK_HALF_WIDTH + halfX * Math.abs(side.x) + halfZ * Math.abs(side.z);
    }

    private void finish(Player player) {
        charges.remove(player);
        player.stopUsingItem();
    }
}
