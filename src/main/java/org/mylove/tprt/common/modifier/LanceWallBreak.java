package org.mylove.tprt.common.modifier;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.mylove.tprt.registries.ModifierRegistry;
import slimeknights.tconstruct.library.modifiers.hook.build.ConditionalStatModifierHook;
import slimeknights.tconstruct.library.tools.context.ToolHarvestContext;
import slimeknights.tconstruct.library.tools.helper.ToolHarvestLogic;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.stat.ToolStats;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 摧壁: carves a 3 wide x 3 tall x 3 deep tunnel directly ahead while the lance charge is running.
 * Runs inside the existing charge tick, so it adds no hooks and no mixins.
 */
public final class LanceWallBreak {
    /** Hardness cap. Vanilla comparison: stone 1.5, deepslate 3.0, deepslate ores 4.5, iron block 5.0. */
    public static final double MAX_HARDNESS = 5.0D;
    /** Bounds the work and the number of dropped item entities produced per tick. */
    private static final int MAX_BLOCKS_PER_TICK = 9;
    private static final int LAYER_HEIGHT = 3;
    /** How many blocks deep the carved tunnel is. */
    private static final int LAYER_DEPTH = 3;

    private static final UUID SLOW_ID = UUID.fromString("2b7d64c1-8f2a-4b7e-9d31-6c5a0e4f8b12");
    private static final String SLOW_NAME = "tprt:wall_breaker_slow";
    /** Mining speed that maps to a neutral reference; slower lances pay more. */
    private static final double PENALTY_REFERENCE = 3.0D;
    private static final double MIN_PENALTY = 0.15D;
    private static final double MAX_PENALTY = 0.60D;

    private LanceWallBreak() {}

    public static void tick(IToolStackView tool, Player player, ItemStack stack, Vec3 look) {
        if (!(player.level() instanceof ServerLevel level) || !(player instanceof ServerPlayer serverPlayer)) return;
        if (tool.getModifierLevel(ModifierRegistry.WALL_BREAKER.getId()) <= 0 || tool.isBroken()) {
            clear(player);
            return;
        }
        // Crosshair yaw only: looking up/down never tilts the excavation volume.
        // Derive from yaw directly so looking straight up/down remains well-defined.
        double yaw = Math.toRadians(player.getYRot());
        Vec3 forward = new Vec3(-Math.sin(yaw), 0.0D, Math.cos(yaw));
        Vec3 origin = player.getEyePosition();
        Vec3 right = new Vec3(Math.cos(yaw), 0.0D, Math.sin(yaw));
        Vec3 up = new Vec3(0.0D, 1.0D, 0.0D);
        AABB body = player.getBoundingBox().inflate(0.05D);
        List<BlockPos> candidates = new ArrayList<>(27);
        // Test voxel centers in a crosshair-oriented 3x3 cross-section, three blocks deep.
        // A small bounded scan avoids diagonal gaps and also handles looking straight up/down.
        BlockPos anchor = BlockPos.containing(origin);
        for (BlockPos cursor : BlockPos.betweenClosed(anchor.offset(-5, -5, -5), anchor.offset(5, 5, 5))) {
            Vec3 offset = Vec3.atCenterOf(cursor).subtract(origin);
            double depth = offset.dot(forward);
            if (depth <= 0.0D || depth > LAYER_DEPTH
                    || Math.abs(offset.dot(right)) > 1.5D
                    || Math.abs(offset.dot(up)) > LAYER_HEIGHT * 0.5D) continue;
            BlockPos pos = cursor.immutable();
            if (canBreak(level, pos, body)) candidates.add(pos);
        }
        candidates.sort(java.util.Comparator.comparingDouble(pos ->
                Vec3.atCenterOf(pos).subtract(origin).dot(forward)));
        if (candidates.isEmpty()) {
            clear(player);
            return;
        }

        int broken = 0;
        for (BlockPos pos : candidates) {
            if (broken >= MAX_BLOCKS_PER_TICK) break;
            BlockState state = level.getBlockState(pos);
            if (state.isAir()) continue;
            // canHarvest/isEffective are forced true: the trait is what grants the ability here.
            ToolHarvestContext context = new ToolHarvestContext(level, serverPlayer, state, pos, Direction.UP, true, true);
            if (ToolHarvestLogic.breakExtraBlock(tool, stack, context)) broken++;
            if (tool.isBroken()) break;
        }

        if (broken > 0) {
            double miningSpeed = ConditionalStatModifierHook.getModifiedStat(tool, player, ToolStats.MINING_SPEED);
            double penalty = Mth.clamp(PENALTY_REFERENCE / Math.max(0.1D, miningSpeed), MIN_PENALTY, MAX_PENALTY);
            applySlow(player, penalty);
        } else {
            clear(player);
        }
    }

    private static boolean canBreak(ServerLevel level, BlockPos pos, AABB body) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir() || !state.getFluidState().isEmpty()) return false;
        if (state.hasBlockEntity()) return false; // never destroy containers or other tile entities
        float hardness = state.getDestroySpeed(level, pos);
        if (hardness < 0.0F || hardness > MAX_HARDNESS) return false;
        // Never break the block the player is currently standing in.
        var shape = state.getCollisionShape(level, pos);
        return shape.isEmpty() || !shape.bounds().move(pos).intersects(body);
    }

    /** Called when the charge ends, so the penalty can never outlive it. */
    public static void clear(Player player) {
        applySlow(player, 0.0D);
    }

    private static void applySlow(Player player, double penalty) {
        AttributeInstance attribute = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (attribute == null) return;
        attribute.removeModifier(SLOW_ID);
        if (penalty > 0.0D) {
            attribute.addTransientModifier(new AttributeModifier(SLOW_ID, SLOW_NAME, -penalty,
                    AttributeModifier.Operation.MULTIPLY_TOTAL));
        }
    }
}
