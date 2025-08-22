package org.mylove.tprt.registries;

import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.mylove.tprt.Tprt;
import org.mylove.tprt.registries.ModBuff.*;

public class ModBuffRegistry {
    public static final DeferredRegister<MobEffect>EFFECT=DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, Tprt.MODID);
    public static void register(IEventBus eventBus) {
        EFFECT.register(eventBus);
    }
    public static final RegistryObject<MobEffect> MOREDAMAGE=EFFECT.register("moredamage",moredamage::new);
    public static final RegistryObject<MobEffect>LLLEGALLY_MODIFIED_HOLSTERS=EFFECT.register("lllegally_modified_holsters", lllegally_modified_holsters::new);
    public static final RegistryObject<MobEffect>MYL_GIRIG_PRO=EFFECT.register("myl_girig_pro", myl_girig_pro::new);
    public static final RegistryObject<MobEffect>EAGER_EDGE=EFFECT.register("eager_edge", eager_edge::new);
    public static final RegistryObject<MobEffect>DE_EAGER_EDGE=EFFECT.register("de_eager_edge", de_eager_edge::new);
}
