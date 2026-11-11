package com.jax3.barelyfunctionalcontraptions.sheep.client;

import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.ZeroPointEnergyModuleBlockEntity;
import com.jax3.barelyfunctionalcontraptions.sheep.registry.ModSounds;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/**
 * The Zero Point Energy Module's energy hum: a seamless 4-second loop (sounds/block/zero_point_hum.ogg) that plays at
 * each linked module for as long as it stays linked, fading in when the beam connects and out when it breaks or the
 * module is removed or unloaded, when it also winds down in pitch. Bigger gaps hum a little higher. Uses the Blocks
 * volume slider.
 */
public final class ZeroPointHumSound extends AbstractTickableSoundInstance {
    private static final Map<BlockPos, ZeroPointHumSound> PLAYING = new HashMap<>();
    private static final float VOLUME = 0.6F;
    private static final float FADE_PER_TICK = 0.05F;

    private final ZeroPointEnergyModuleBlockEntity module;
    private float fade;

    private ZeroPointHumSound(ZeroPointEnergyModuleBlockEntity module) {
        super(ModSounds.ZERO_POINT_HUM.get(), SoundSource.BLOCKS, RandomSource.create());
        this.module = module;
        BlockPos pos = module.getBlockPos();
        this.x = pos.getX() + 0.5;
        this.y = pos.getY() + 0.5;
        this.z = pos.getZ() + 0.5;
        this.looping = true;
        this.delay = 0;
        this.volume = 0.01F; // starts silent and fades in
        this.pitch = pitchFor(module.getLinkDistance());
    }

    /** 0.85 for the smallest gap, rising to 1.15 for a 32-block gap or more. */
    public static float pitchFor(int distance) {
        return 0.85F + 0.3F * Mth.clamp((distance - 2) / 30F, 0F, 1F);
    }

    /** Client tick of a module: start its hum when it is linked and none is playing. */
    public static void tick(ZeroPointEnergyModuleBlockEntity module) {
        if (module.getLinkDistance() <= 0) return;
        BlockPos pos = module.getBlockPos().immutable();
        ZeroPointHumSound current = PLAYING.get(pos);
        if (current != null && !current.isStopped() && current.module == module) return;
        ZeroPointHumSound sound = new ZeroPointHumSound(module);
        PLAYING.put(pos, sound);
        Minecraft.getInstance().getSoundManager().play(sound);
    }

    @Override
    public void tick() {
        boolean on = !module.isRemoved() && module.getLinkDistance() > 0;
        fade = Mth.clamp(fade + (on ? FADE_PER_TICK : -FADE_PER_TICK * 2), 0F, 1F);
        if (!on && fade <= 0) {
            PLAYING.remove(module.getBlockPos(), this);
            stop();
            return;
        }
        volume = Math.max(0.01F, VOLUME * fade);
        if (on) {
            // Glide towards the gap's pitch (a new gap, or the power-up after a fade).
            pitch += (pitchFor(module.getLinkDistance()) - pitch) * 0.2F;
        } else {
            pitch = Math.max(0.5F, pitch - 0.02F); // powering down: the hum winds down as it fades
        }
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }
}
