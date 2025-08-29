package org.mylove.tprt.registries;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.mylove.tprt.entities.flying_sword.FlyingSword;
import org.mylove.tprt.Tprt;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, Tprt.MODID);

    public static final RegistryObject<EntityType<FlyingSword>> FLYING_SWORD =
            ENTITY_TYPES.register("flying_sword", () -> EntityType.Builder
                    .<FlyingSword>of(FlyingSword::new, MobCategory.MISC)
                    .sized(0.5F, 1.0F)
                    .fireImmune()
                    .clientTrackingRange(16)
                    .updateInterval(1) // default: 3, 越快动画越流畅(?)，cpu负担越大 // 之前写的20，nmd修了2天 // 或许插值比实际算的流畅? 待测试
                    .build(ResourceLocation.fromNamespaceAndPath(Tprt.MODID, "flying_sword").toString())
            );

    public static void register(IEventBus eventBus){
        ENTITY_TYPES.register(eventBus);
    }
}
