package org.mylove.tprt.registries;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.mylove.tprt.Tprt;

/** 本 mod 的自定义音效 */
public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, Tprt.MODID);

    /** 激光持续音 */
    public static final RegistryObject<SoundEvent> LASER_LOOP = SOUNDS.register("laser_loop",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(Tprt.MODID, "laser_loop")));

    private ModSounds() {
    }
}