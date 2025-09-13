package org.mylove.tprt.hooks;

import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import java.util.Collection;

public interface KillingHook {
    default void onKillLivingTarget(IToolStackView tool, LivingDeathEvent event, LivingEntity attacker, LivingEntity target, int level) {
    }

    record AllMerge(Collection<KillingHook> modules) implements KillingHook {
        @Override
        public void onKillLivingTarget(IToolStackView tool, LivingDeathEvent event, LivingEntity attacker, LivingEntity target, int level) {
            for(KillingHook module : this.modules) {
                module.onKillLivingTarget(tool, event, attacker, target, level);
            }

        }
    }
}
