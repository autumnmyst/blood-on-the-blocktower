package com.autumnwind.botb.sound;

import com.autumnwind.botb.BloodOnTheBlocktower;
import net.minecraft.client.sound.AbstractSoundInstance;
import net.minecraft.client.sound.Sound;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.client.sound.WeightedSoundSet;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.floatprovider.ConstantFloatProvider;
import org.jetbrains.annotations.Nullable;

/**
 * A non-positional sound for the local player that can play a file straight from a resource
 * pack path instead of a sounds.json event. With no custom path it behaves like the event.
 */
public class CustomSoundInstance extends AbstractSoundInstance {

    @Nullable
    private final String customPath;

    /** @param customPath a path under {@code sounds/} without the extension, or null for the event's own sounds */
    public CustomSoundInstance(SoundEvent event, @Nullable String customPath, float volume, boolean repeat) {
        super(event, SoundCategory.MASTER, SoundInstance.createRandom());
        this.customPath = customPath;
        this.volume = volume;
        this.pitch = 1.0f;
        this.repeat = repeat;
        this.repeatDelay = 0;
        this.relative = true;
    }

    @Override
    public WeightedSoundSet getSoundSet(SoundManager soundManager) {
        if (customPath == null) {
            return super.getSoundSet(soundManager);
        }
        WeightedSoundSet set = new WeightedSoundSet(this.id, null);
        // Music is streamed rather than fully loaded, the way vanilla handles long tracks
        set.add(new Sound(Identifier.of(BloodOnTheBlocktower.MOD_ID, customPath),
                ConstantFloatProvider.create(1.0f), ConstantFloatProvider.create(1.0f), 1,
                Sound.RegistrationType.FILE, repeat, false, 16));
        this.sound = set.getSound(this.random);
        return set;
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
