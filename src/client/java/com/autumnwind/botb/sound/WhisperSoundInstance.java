package com.autumnwind.botb.sound;

import net.minecraft.client.sound.MovingSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.entity.Entity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;

/**
 * Whisper audio cue with a 1s fade-in / 1s sustain / 1s fade-out envelope. Total
 * runtime is ~3 seconds (60 client ticks). Always positional at the sender, so the
 * sound follows them each tick if they move during playback. Vanilla spatial
 * attenuation determines who hears it based on physical distance.
 *
 * <p>Subtle but important: the constructor seeds {@link #volume} to {@code baseVolume}
 * (full sustain) rather than 0. Vanilla's {@code SoundSystem.play()} short-circuits
 * sounds whose getVolume() returns 0 at submit time, never even creating an OpenAL
 * source, so the per-tick envelope would never get a chance to ramp them up. The
 * envelope is applied dynamically via {@link #getVolume()} on each frame instead.
 *
 * <p>Minecraft's vanilla sound engine cannot seek into a sound file, so this plays
 * {@code blood-on-the-blocktower:whisper} from its start. Adding multiple variants
 * to sounds.json is the supported way to get random per-play variety.
 */
public class WhisperSoundInstance extends MovingSoundInstance {
    /** Tick counts for the fade envelope. 20 ticks per second. */
    private static final int FADE_IN_TICKS = 20;
    private static final int SUSTAIN_TICKS = 20;
    private static final int FADE_OUT_TICKS = 20;
    private static final int TOTAL_TICKS = FADE_IN_TICKS + SUSTAIN_TICKS + FADE_OUT_TICKS;

    /** The sender, with the sound anchored to them as they move. */
    private final Entity follow;
    private final float baseVolume;

    private int age = 0;

    /**
     * @param sound      the registered whisper sound event
     * @param baseVolume target volume at full sustain (will be scaled by the envelope)
     * @param pitch      per-whisper pitch jitter, see MessageCommandMixin
     * @param follow     entity the sound follows each tick (the sender)
     */
    public WhisperSoundInstance(SoundEvent sound, float baseVolume, float pitch, Entity follow) {
        // MASTER category so the user's "Players" volume slider doesn't accidentally
        // hide whispers, since they're a gameplay signal, not ambient player noise.
        super(sound, SoundCategory.MASTER, SoundInstance.createRandom());
        this.baseVolume = baseVolume;
        this.pitch = pitch;
        // Seeded full, see class doc. The envelope kicks in via getVolume() on tick 0.
        this.volume = baseVolume;
        this.follow = follow;
        this.relative = false;
        this.repeat = false;
        this.attenuationType = AttenuationType.LINEAR;
        if (follow != null) {
            this.x = follow.getX();
            this.y = follow.getY() + follow.getStandingEyeHeight();
            this.z = follow.getZ();
        }
    }

    @Override
    public void tick() {
        age++;
        if (age >= TOTAL_TICKS) {
            this.setDone();
            return;
        }
        // Keep the sound anchored to the sender as they move during playback.
        if (follow != null && follow.isAlive()) {
            this.x = follow.getX();
            this.y = follow.getY() + follow.getStandingEyeHeight();
            this.z = follow.getZ();
        }
    }

    @Override
    public float getVolume() {
        // Dynamic envelope so the engine reads the correct volume each frame.
        // No upper clamp: OpenAL accepts gains > 1.0, and the caller may pass a
        // baseVolume > 1.0 to push above category baseline.
        return Math.max(0f, baseVolume * envelopeMultiplier(age));
    }

    /** 0..1 multiplier on top of {@link #baseVolume} given current age in ticks. */
    private static float envelopeMultiplier(int t) {
        if (t < FADE_IN_TICKS) {
            return t / (float) FADE_IN_TICKS;
        }
        if (t < FADE_IN_TICKS + SUSTAIN_TICKS) {
            return 1f;
        }
        int fadeOutT = t - FADE_IN_TICKS - SUSTAIN_TICKS;
        return 1f - (fadeOutT / (float) FADE_OUT_TICKS);
    }

    /**
     * Forces the sound engine to allocate an OpenAL source even though our
     * envelope returns 0 at age 0. Without this, vanilla {@code SoundSystem.play()}
     * short-circuits zero-volume sounds before the per-tick envelope can ever
     * ramp them up, and the whisper would be silently dropped at submit time.
     * The same pattern is used by {@code RepeatingSound} elsewhere in this mod.
     */
    @Override
    public boolean shouldAlwaysPlay() {
        return true;
    }

    @Override
    public boolean canPlay() {
        return true;
    }
}
