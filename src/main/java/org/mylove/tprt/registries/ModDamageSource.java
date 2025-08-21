package org.mylove.tprt.registries;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import org.mylove.tprt.Tprt;

import java.rmi.registry.Registry;

public class ModDamageSource {
    private static ResourceKey<DamageType>create(String string) {
        return ResourceKey.create(Registries.DAMAGE_TYPE,new ResourceLocation(Tprt.MODID,string));
    }
    public static final ResourceKey<DamageType> LANCER_RICHER_DAMAGE_TYPE=create("lancer_richer_damage_type");
    public static final ResourceKey<DamageType> TPRT_FIRE_DAMAGE_TYPE=create("tprt_fire_damage_type");
    public static final ResourceKey<DamageType> TPRT_MAGIC_DAMAGE_TYPE=create("tprt_magic_damage_type");

}
