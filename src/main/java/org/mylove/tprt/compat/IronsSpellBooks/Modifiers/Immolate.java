package org.mylove.tprt.compat.IronsSpellBooks.Modifiers;

import io.redspace.ironsspellbooks.registries.MobEffectRegistry;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.mylove.tprt.registries.ModifierIds;
import org.mylove.tprt.utils.ModifierLEVEL;
import slimeknights.tconstruct.library.modifiers.impl.NoLevelsModifier;


import static io.redspace.ironsspellbooks.damage.ISSDamageTypes.FIRE_MAGIC;

public class Immolate extends NoLevelsModifier{

    public Immolate(){
        MinecraftForge.EVENT_BUS.addListener(this::onLivingDamage);
    }

    @SubscribeEvent
    public void onLivingDamage(LivingDamageEvent event) {
        DamageSource source = event.getSource();
        LivingEntity target = event.getEntity();
        if (!(source.getEntity() instanceof Player player)) return;
        int A = 0;
            if (ModifierLEVEL.getTotalArmorModifierlevel(player, ModifierIds.Immolate) > 0) {
                A = 1;
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
                    event.setAmount((float) (1.2 * event.getAmount()));
                }
            }
    }
}
