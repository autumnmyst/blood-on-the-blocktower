package com.autumnwind.botb.sound;

import net.minecraft.client.sound.AbstractSoundInstance;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.client.sound.SoundInstance;

/**
 * A sound instance that repeats/loops continuously until stopped.
 */
public class RepeatingSound extends AbstractSoundInstance {

    public RepeatingSound(SoundEvent sound, float volume, float pitch) {
        super(sound, SoundCategory.MASTER, SoundInstance.createRandom());
        this.volume = volume;
        this.pitch = pitch;
        this.repeat = true;
        this.repeatDelay = 0; // No delay between loops - seamless
        this.relative = true; // Not positional, plays for the player
    }

    @Override
    public boolean shouldAlwaysPlay() {
        return true;
    }

    @Override
    public boolean canPlay() {
        return true;
    }
}
