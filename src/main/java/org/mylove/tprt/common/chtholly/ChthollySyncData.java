package org.mylove.tprt.common.chtholly;

import org.jetbrains.annotations.NotNull;

/**
 * 「挚爱」(chtholly) 服务端 -> 客户端轻量状态快照。
 * 替代原先频繁修改护甲、Curios 饰品、法术书及手持弓弩 ItemStack NBT 的同步方式，
 * 彻底消除拉弓抖动、装备重绘及客户端每渲染帧重复反序列化 ToolStack 的开销。
 */
public record ChthollySyncData(
        // 1. 末日三问 (护甲)
        int weLevel,
        int wePhase,
        int wePoolTenths,
        int weBerserkSec,
        int weCdSec,
        // 2. 最幸福的女孩 (护符/战旗)
        boolean hgActive,
        int hgShieldTenths,
        int hgPeakTenths,
        int hgFinaleSec,
        int hgCdSec,
        // 3. 终末挽歌 (法术书/魔杖/激光枪/炼金手套)
        boolean elActive,
        int elActiveTargets,
        int elMaxStoredTenths,
        int elRemainSec,
        // 4. 纵使日薄西山 (远程弓弩)
        int ssHoverCount,
        int ssHoverSec,
        int ssWaymarkCount,
        int ssWaymarkSec,
        @NotNull String ssLinkedSword
) {

    public static final ChthollySyncData EMPTY = new ChthollySyncData(
            0, 1, 0, 0, 0,
            false, 0, 0, 0, 0,
            false, 0, 0, 0,
            0, 0, 0, 0, ""
    );

    private static volatile ChthollySyncData clientSnapshot = EMPTY;

    @NotNull
    public static ChthollySyncData getClient() {
        return clientSnapshot;
    }

    public static void setClient(@NotNull ChthollySyncData snapshot) {
        clientSnapshot = snapshot;
    }

    public static void clearClient() {
        clientSnapshot = EMPTY;
    }
}

