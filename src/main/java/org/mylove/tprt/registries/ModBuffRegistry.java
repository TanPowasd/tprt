package org.mylove.tprt.registries;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.mylove.tprt.Tprt;
import org.mylove.tprt.item.ModBuff.*;
import java.rmi.registry.Registry;

public class ModBuffRegistry {
    public static final DeferredRegister<MobEffect>EFFECT=DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, Tprt.MODID);
    public static void register(IEventBus eventBus) {
        EFFECT.register(eventBus);
    }
    public static final RegistryObject<MobEffect> MOREDAMAGE=EFFECT.register("moredamage",moredamage::new);
}
