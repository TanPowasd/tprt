package org.mylove.tprt.hooks;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import java.util.Collection;

public interface DamageRedirectHook {
    /**  */
    @Nullable
    DamageSource redirectDamageSource(IToolStackView tool, LivingEntity attacker, LivingEntity target, int level, DamageSource source);

    record AllMerge(Collection<DamageRedirectHook> modules) implements DamageRedirectHook {
        @Override
        public DamageSource redirectDamageSource(IToolStackView tool, LivingEntity attacker, LivingEntity target, int level, DamageSource source) {
            for(DamageRedirectHook module : this.modules) {
                source = module.redirectDamageSource(tool, attacker, target, level, source);
            }
            return source;
        }

    }
}
