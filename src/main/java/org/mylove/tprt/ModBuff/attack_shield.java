package org.mylove.tprt.ModBuff;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * 次数盾的标记效果: 本身不产生任何属性/行为, 只作为"还能免疫几次攻击"的同步载体。
 * 应用时带上 showIcon=false / visible=false, 所以不会出现在状态栏, 也不会有粒子。
 */
public class attack_shield extends MobEffect {
    public attack_shield() {
        super(MobEffectCategory.BENEFICIAL, 0x55FFFF);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return false;
    }
}