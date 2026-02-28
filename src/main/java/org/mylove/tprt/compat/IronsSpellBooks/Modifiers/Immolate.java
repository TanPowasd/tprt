package org.mylove.tprt.compat.IronsSpellBooks.Modifiers;

import com.ssakura49.sakuratinker.utils.tinker.ToolUtil;
import io.redspace.ironsspellbooks.registries.MobEffectRegistry;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.mylove.tprt.compat.IronsSpellBooks.IssCompat;
import slimeknights.tconstruct.library.modifiers.impl.NoLevelsModifier;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;


import static io.redspace.ironsspellbooks.damage.ISSDamageTypes.FIRE_MAGIC;

public class Immolate extends NoLevelsModifier{

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent event) {
        DamageSource source = event.getSource();
        LivingEntity target = event.getEntity();
        if (!(source.getEntity() instanceof Player player)) return;
        int A = 0;
        for (IToolStackView tool : ToolUtil.getAllEquippedToolStacks(player)) {
            if (tool.getModifierLevel(IssCompat.ImmoLate.get()) > 0) {
                A = 1;
                continue;
            }
            if (A == 1){
                if (source.is(FIRE_MAGIC)){
                    MobEffect effect = MobEffectRegistry.IMMOLATE.get();
                    MobEffectInstance instance = target.getEffect(effect);
                    if (instance != null) {
                        int x = instance.getAmplifier();
                        int X = x + 1;
                        target.addEffect(new MobEffectInstance(effect, 200, X));
                    }else {
                        target.addEffect(new MobEffectInstance(effect, 200, 0));
                    }
                }
            }else {return;}
        }
    }
}
