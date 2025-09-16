package org.mylove.tprt.registries;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.mylove.tprt.Tprt;
import org.mylove.tprt.entity.tec.CinderParticle;


public class ModEntities {
    //public static final EntityTypeDeferredRegister ENTITIES = new EntityTypeDeferredRegister(Tprt.MODID);
    // 上面那是匠魂里的

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, Tprt.MODID);

    public static void register(IEventBus eventBus){
        ENTITY_TYPES.register(eventBus);
    }

    public static final RegistryObject<EntityType<CinderParticle>> CINDER_SLASH_PARTICLE =
            ENTITY_TYPES.register("particle_provider_cinder_slash", () -> EntityType.Builder
                    .<CinderParticle>of(CinderParticle::new, MobCategory.MISC)
                    .sized(.1F, .1F)
                    .build(ResourceLocation.fromNamespaceAndPath(Tprt.MODID, "particle_provider_cinder_slash").toString())
            );
}