package org.mylove.tprt.common.chtholly;

import net.minecraft.resources.ResourceLocation;
import org.mylove.tprt.Tprt;
import slimeknights.tconstruct.library.tools.nbt.IModDataView;
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;

/**
 * 「挚爱」(chtholly) 体系持久化工具数据键与时间戳工具集。
 * 统一采用服务端/客户端共享的 gameTime 截止时间戳代替每 tick 递减写 NBT，
 * 避免频繁刷新物品栈导致挖掘进度重置、拉弓抖动与冗余同步包。
 */
public final class ChthollyDataKeys {

    private ChthollyDataKeys() {
    }

    // ===== 1. 瑟尼欧里斯 (seniorious) =====
    public static final ResourceLocation SN_HEARTFIRE = Tprt.getResource("sn_heartfire");
    public static final ResourceLocation SN_MODE = Tprt.getResource("sn_mode");
    public static final ResourceLocation SN_SWITCH_UNTIL = Tprt.getResource("sn_switch_until");
    public static final ResourceLocation SN_STEAL_STACKS = Tprt.getResource("sn_steal_stacks");
    public static final ResourceLocation SN_STEAL_UNTIL = Tprt.getResource("sn_steal_until");
    public static final ResourceLocation SN_STEAL_TARGET_ARMOR = Tprt.getResource("sn_steal_target_armor");
    public static final ResourceLocation SN_STEAL_QUALITY = Tprt.getResource("sn_steal_quality");
    public static final ResourceLocation SN_CONSUME_STEP = Tprt.getResource("sn_consume_step");
    public static final ResourceLocation SN_COMBO = Tprt.getResource("sn_combo");
    public static final ResourceLocation SN_COMBO_TARGET_ID = Tprt.getResource("sn_combo_target_id");
    public static final ResourceLocation SN_REST_UNTIL = Tprt.getResource("sn_rest_until");
    public static final ResourceLocation SN_TUNE_COUNT = Tprt.getResource("sn_tune_count");

    // ===== 2. 在消失之前 (before_i_vanish) =====
    public static final ResourceLocation BV_VANISH_UNTIL = Tprt.getResource("vanish_grace_until");
    public static final ResourceLocation BV_CD_UNTIL = Tprt.getResource("vanish_cd_until");
    public static final ResourceLocation BV_VANISHING_HITS = Tprt.getResource("vanish_reforge_hits");

    // 旧存档兼容迁移键 (仅在读取到旧工具时一次性迁移并移除)
    public static final ResourceLocation LEGACY_BV_VANISHING_TICKS = Tprt.getResource("vanish_grace_ticks");
    public static final ResourceLocation LEGACY_BV_VANISHING_CD = Tprt.getResource("vanish_cooldown_ticks");
    public static final ResourceLocation LEGACY_BV_GUARDIAN_ACTIVE = Tprt.getResource("vanish_guardian_active");

    public static long getLong(IModDataView data, ResourceLocation key) {
        String raw = data.getString(key);
        if (raw.isEmpty()) return 0L;
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    public static void putLong(ModDataNBT data, ResourceLocation key, long value) {
        if (value <= 0L) {
            data.remove(key);
        } else {
            data.putString(key, Long.toString(value));
        }
    }

    public static boolean isTimerActive(long now, long until, int maxDurationTicks) {
        return until > now && (until - now) <= maxDurationTicks + 20L;
    }

    public static int getRemainingTicks(long now, long until, int maxDurationTicks) {
        if (!isTimerActive(now, until, maxDurationTicks)) return 0;
        return (int) (until - now);
    }

    public static int getRemainingSeconds(long now, long until, int maxDurationTicks) {
        int ticks = getRemainingTicks(now, until, maxDurationTicks);
        return ticks > 0 ? (ticks + 19) / 20 : 0;
    }
}

