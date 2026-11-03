package org.mylove.tprt.common.modifier;

import slimeknights.tconstruct.library.modifiers.impl.NoLevelsModifier;

/**
 * 摧壁 trait marker. Has no levels; the excavation itself is driven from the charge tick
 * via {@link LanceWallBreak}, so this class only supplies the level-less modifier identity.
 */
public class WallBreakerModifier extends NoLevelsModifier {
}
