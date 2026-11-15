package org.mylove.tprt.Modifiers.melee;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

/**
 * Shared state for the blazeforged steel melee traits.
 *
 * <p>焚毁点数 lives in each tool's persistent data, so two tools never share stacks.
 * Points are earned only by positive actual fire damage in the shared Forge bridge,
 * and drop to zero after {@link #FADE_TICKS} ticks without actual fire damage.
 */
public final class BlazeforgedTraitData {
    public static final int MAX_POINTS = 20;
    public static final int FADE_TICKS = 200;
    public static final float BONUS_PER_POINT = 0.05F;

    private static final ResourceLocation POINTS = new ResourceLocation("tprt", "kindled_points");
    private static final ResourceLocation LAST_FIRE = new ResourceLocation("tprt", "kindled_last_fire");

    private BlazeforgedTraitData() {
    }

    /** Current points, dropping the stack if the ten second window has expired. */
    public static int points(IToolStackView tool, long now) {
        var data = tool.getPersistentData();
        int points = data.getInt(POINTS);
        if (points <= 0) {
            return 0;
        }
        // Modular int subtraction remains correct across the game-time int rollover.
        int elapsed = (int) now - data.getInt(LAST_FIRE);
        if (elapsed < 0 || elapsed >= FADE_TICKS) {
            data.putInt(POINTS, 0);
            return 0;
        }
        return Math.min(MAX_POINTS, points);
    }

    /** Read-only expiry check for client tooltips; only server hooks mutate persistent data. */
    public static int displayPoints(IToolStackView tool, long now) {
        int elapsed = (int) now - tool.getPersistentData().getInt(LAST_FIRE);
        return elapsed < 0 || elapsed >= FADE_TICKS ? 0 : rawPoints(tool);
    }

    /** Stored points without applying expiry, for display when no player context exists. */
    public static int rawPoints(IToolStackView tool) {
        return Math.min(MAX_POINTS, Math.max(0, tool.getPersistentData().getInt(POINTS)));
    }

    /** Awards one point; an expired stack is cleared first, then refilled. */
    public static void addPoint(IToolStackView tool, long now) {
        var data = tool.getPersistentData();
        int next = Math.min(MAX_POINTS, points(tool, now) + 1);
        data.putInt(POINTS, next);
        data.putInt(LAST_FIRE, (int) now);
    }

    /** Refreshes the ten second window without changing points. */
    public static void markFire(IToolStackView tool, long now) {
        tool.getPersistentData().putInt(LAST_FIRE, (int) now);
    }

    public static float damageBonus(IToolStackView tool, long now) {
        return points(tool, now) * BONUS_PER_POINT;
    }

    /** Fire damage credited to, and caused by, the given entity. */
    public static DamageSource fireFrom(LivingEntity source) {
        return new DamageSource(
                source.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                        .getHolderOrThrow(DamageTypes.ON_FIRE),
                source, source);
    }
}
