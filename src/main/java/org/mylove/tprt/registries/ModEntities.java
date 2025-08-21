package org.mylove.tprt.registries;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import org.mylove.tprt.Tprt;
import slimeknights.mantle.registration.deferred.EntityTypeDeferredRegister;

import java.util.function.BiConsumer;


public class ModEntities {
    public static final EntityTypeDeferredRegister ENTITIES = new EntityTypeDeferredRegister(Tprt.MODID);
}