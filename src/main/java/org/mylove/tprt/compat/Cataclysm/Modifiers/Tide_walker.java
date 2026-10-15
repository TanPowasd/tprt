package org.mylove.tprt.compat.Cataclysm.Modifiers;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import org.mylove.tprt.registries.ModifierIds;
import org.mylove.tprt.utils.ModifierLEVEL;
import slimeknights.tconstruct.library.modifiers.impl.NoLevelsModifier;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 潮汐行者: 风暴之泪的护甲特性。
 * 免疫摔落伤害; 可以在水面上行走; 在水里可以自由飞行;
 * 不处于水中但正在下雨时也能短暂飞行 (每次落地后重置, 单次最多 10 秒)。
 */
public class Tide_walker extends NoLevelsModifier {

    /** 雨中短暂飞行的上限: 10 秒 = 200 tick */
    public static final int RAIN_FLIGHT_TICKS = 200;
    /** 水面行走只在离水面这么近时生效, 免得把深水里的玩家顶上来 */
    public static final double WATER_WALK_DEPTH = 1.0D;

    /** 由本特性授予飞行状态的玩家 */
    private static final Set<UUID> FLIGHT_GRANTED = new HashSet<>();
    /** 雨中飞行已经用掉的 tick */
    private static final Map<UUID, Integer> RAIN_FLIGHT_USED = new HashMap<>();

    static {
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, LivingFallEvent.class, Tide_walker::onFall);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, TickEvent.PlayerTickEvent.class, Tide_walker::onPlayerTick);
    }

    /** 免疫摔落伤害 */
    private static void onFall(LivingFallEvent event) {
        if (event.getEntity().level().isClientSide) {
            return;
        }
        if (ModifierLEVEL.getTotalArmorModifierlevel(event.getEntity(), ModifierIds.TIDE_WALKER) > 0) {
            event.setCanceled(true);
        }
    }

    /** 每 tick: 水面行走 / 水中飞行 / 雨中短暂飞行 */
    private static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Player player = event.player;
        if (player.level().isClientSide || player.isCreative() || player.isSpectator()) {
            return;
        }
        // 没穿 (或不完整) 就直接收回飞行
        if (ModifierLEVEL.getTotalArmorModifierlevel(player, ModifierIds.TIDE_WALKER) <= 0) {
            revokeFlight(player);
            RAIN_FLIGHT_USED.remove(player.getUUID());
            return;
        }
        boolean inWater = player.isInWater();
        boolean raining = !inWater && player.level().isRainingAt(player.blockPosition());
        // 每次落地就把雨中飞行时间重置; 只有真正在飞的时候才消耗这 10 秒
        if (player.onGround()) {
            RAIN_FLIGHT_USED.remove(player.getUUID());
        } else if (raining && player.getAbilities().flying) {
            RAIN_FLIGHT_USED.merge(player.getUUID(), 1, Integer::sum);
        }
        boolean rainFlight = raining && RAIN_FLIGHT_USED.getOrDefault(player.getUUID(), 0) <= RAIN_FLIGHT_TICKS;
        if (inWater || rainFlight) {
            if (!player.getAbilities().mayfly) {
                player.getAbilities().mayfly = true;
                FLIGHT_GRANTED.add(player.getUUID());
                player.onUpdateAbilities();
            }
        } else {
            revokeFlight(player);
        }
        if (!player.getAbilities().flying) {
            walkOnWater(player);
        }
    }

    /** 收回本特性授予的飞行状态 */
    private static void revokeFlight(Player player) {
        if (FLIGHT_GRANTED.remove(player.getUUID())) {
            player.getAbilities().mayfly = false;
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
        }
    }

    /** 水面行走: 贴着水面时把玩家托在水面上 (潜行则主动下水) */
    private static void walkOnWater(Player player) {
        if (player.isShiftKeyDown()) {
            return;
        }
        Level level = player.level();
        BlockPos feet = player.blockPosition();
        FluidState fluid = level.getFluidState(feet);
        if (!fluid.is(FluidTags.WATER)) {
            return;
        }
        double surface = feet.getY() + fluid.getOwnHeight();
        if (player.getY() >= surface || player.getY() < surface - WATER_WALK_DEPTH) {
            return;
        }
        player.setPos(player.getX(), surface, player.getZ());
        Vec3 motion = player.getDeltaMovement();
        player.setDeltaMovement(motion.x, Math.max(motion.y, 0.0D), motion.z);
        player.setOnGround(true);
        player.fallDistance = 0.0F;
    }
}