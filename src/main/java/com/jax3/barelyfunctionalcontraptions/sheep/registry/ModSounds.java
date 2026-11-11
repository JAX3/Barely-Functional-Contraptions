package com.jax3.barelyfunctionalcontraptions.sheep.registry;

import com.jax3.barelyfunctionalcontraptions.sheep.ResourcefulSheep;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Sound events of the Resourceful Sheep half. Files and subtitles are in assets/barelyfunctionalcontraptions/sounds.json. */
public final class ModSounds {
    public static final DeferredRegister<SoundEvent> REGISTER = DeferredRegister.create(Registries.SOUND_EVENT, ResourcefulSheep.MOD_ID);

    /** The Zero Point Energy Module's looping energy hum, played while it is linked. */
    public static final DeferredHolder<SoundEvent, SoundEvent> ZERO_POINT_HUM = REGISTER.register("block.zero_point_energy_module.hum",
            () -> SoundEvent.createVariableRangeEvent(ResourcefulSheep.id("block.zero_point_energy_module.hum")));

    private ModSounds() {}
}
