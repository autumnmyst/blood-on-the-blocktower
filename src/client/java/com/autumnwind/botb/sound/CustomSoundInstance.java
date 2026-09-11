package com.autumnwind.botb.sound;

import com.autumnwind.botb.BloodOnTheBlocktower;
import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.valueproviders.ConstantFloat;
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
        super(event, SoundSource.MASTER, SoundInstance.createUnseededRandom());
        this.customPath = customPath;
        this.volume = volume;
        this.pitch = 1.0f;
        this.looping = repeat;
        this.delay = 0;
        this.relative = true;
    }

    @Override
    public WeighedSoundEvents resolve(SoundManager soundManager) {
        if (customPath == null) {
            return super.resolve(soundManager);
        }
        WeighedSoundEvents set = new WeighedSoundEvents(this.identifier, null);
        // Music is streamed rather than fully loaded, the way vanilla handles long tracks
        set.addSound(new Sound(Identifier.fromNamespaceAndPath(BloodOnTheBlocktower.MOD_ID, customPath),
                ConstantFloat.of(1.0f), ConstantFloat.of(1.0f), 1,
                Sound.Type.FILE, looping, false, 16));
        this.sound = set.getSound(this.random);
        return set;
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }

    @Override
    public boolean canPlaySound() {
        return true;
    }
}
