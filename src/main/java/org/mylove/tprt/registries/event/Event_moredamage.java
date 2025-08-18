package org.mylove.tprt.registries.event;


import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.mylove.tprt.registries.ModBuffRegistry;
import org.mylove.tprt.Tprt;

@Mod.EventBusSubscriber(modid=Tprt.MODID,bus=Mod.EventBusSubscriber.Bus.FORGE)
public class Event_moredamage {
    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event){
        LivingEntity entity = event.getEntity();
        //MobEffect effect=entity.getEffect(ModBuffRegistry.MOREDAMAGE.get());
        MobEffectInstance instance = entity.getEffect(ModBuffRegistry.MOREDAMAGE.get());
        if(instance!=null){
            int bufnum=instance.getAmplifier()+1;
            //float attE=1.0f+0.2f*bufnum;
            float attE= (float) Math.pow(1.2f,bufnum);
            event.setAmount(event.getAmount()*attE);
        }
    }
}
