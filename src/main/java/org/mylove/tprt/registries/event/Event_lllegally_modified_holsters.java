package org.mylove.tprt.registries.event;


import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.mylove.tprt.ModUsed.ModMaths.OiMaths;
import org.mylove.tprt.Tprt;
import org.mylove.tprt.registries.ModBuffRegistry;

@Mod.EventBusSubscriber(modid= Tprt.MODID,bus=Mod.EventBusSubscriber.Bus.FORGE)
public class Event_lllegally_modified_holsters {
    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event){
        LivingEntity hurtevent= event.getEntity();
        LivingEntity attacker = hurtevent.getLastAttacker();
        MobEffectInstance instance=attacker.getEffect(ModBuffRegistry.LLLEGALLY_MODIFIED_HOLSTERS.get());
        if(instance!=null&&attacker!=null&&hurtevent!=null){
            int bufnum=instance.getAmplifier()+1;
            //float attE=1.0f+0.2f*bufnum;
            int bl= OiMaths.PRIME.getPrime(bufnum);
            if(bl==-1){
                bl=1;
            }
            event.setAmount(event.getAmount()*bl);
            int duration = instance.getDuration();
            boolean ambient = instance.isAmbient();
            boolean visible = instance.isVisible();
            boolean showIcon = instance.showIcon();
            if(bufnum<=7){
                MobEffectInstance newEffect = new MobEffectInstance(
                        ModBuffRegistry.MOREDAMAGE.get(),
                        duration,
                        bufnum,
                        ambient,
                        visible,
                        showIcon);
                attacker.addEffect(newEffect);
            }
        }
    }
}
