package org.mylove.tprt.client;

import net.minecraft.client.Minecraft;

/** A forced stop requires a physical release, not a timed item cooldown. */
public final class LanceUseInput {
    private static boolean waitForRelease;
    private LanceUseInput() {}
    public static void ended() {
        waitForRelease = Minecraft.getInstance().options.keyUse.isDown();
    }
    public static boolean blocked() {
        if (!Minecraft.getInstance().options.keyUse.isDown()) waitForRelease = false;
        return waitForRelease;
    }
}
