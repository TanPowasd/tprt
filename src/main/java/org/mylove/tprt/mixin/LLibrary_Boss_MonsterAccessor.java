package org.mylove.tprt.mixin;


import com.github.L_Ender.cataclysm.entity.AnimationMonster.BossMonsters.LLibrary_Boss_Monster;
import com.github.L_Ender.cataclysm.init.ModTag;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({LLibrary_Boss_Monster.class})
public interface LLibrary_Boss_MonsterAccessor {
    @Accessor(
            value = "reducedDamageTicks",
            remap = false
    )
    int getreducedDamageTicks();

    @Accessor(
            value = "reducedDamageTicks",
            remap = false
    )
    void setreducedDamageTicks(int var1);
}

