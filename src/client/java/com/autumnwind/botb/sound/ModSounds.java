package com.autumnwind.botb.sound;

import com.autumnwind.botb.BloodOnTheBlocktower;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

/**
 * The mod's sound events, one per entry in sounds.json. Kept as constants so a typo is a
 * compile error rather than a silently missing sound.
 */
public final class ModSounds {

    private ModSounds() {}

    public static final SoundEvent DOORBELL = of("doorbell");
    public static final SoundEvent DOORKNOCK = of("doorknock");
    public static final SoundEvent DUSK = of("dusk");
    public static final SoundEvent DAWN = of("dawn");
    public static final SoundEvent CALL_BACK = of("call_back");
    public static final SoundEvent ROLE_RECEIVE = of("role_receive");
    public static final SoundEvent NOMINATION = of("nomination");
    public static final SoundEvent VOTE_START = of("vote_start");
    public static final SoundEvent VOTE_MUSIC = of("vote_music");
    public static final SoundEvent CLOCK_TICKING = of("clock_ticking");
    public static final SoundEvent NOT_ENOUGH_VOTES = of("not_enough_votes");
    public static final SoundEvent TIE = of("tie");
    public static final SoundEvent MARKED = of("marked");
    public static final SoundEvent EXECUTION = of("execution");
    public static final SoundEvent EXECUTION_SURVIVED = of("execution_survived");
    public static final SoundEvent GAME_END = of("game_end");
    public static final SoundEvent WHISPER = of("whisper");

    /** A sound by its sounds.json name, for payloads that carry the name (e.g. the doorbell choice). */
    public static SoundEvent of(String name) {
        return SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(BloodOnTheBlocktower.MOD_ID, name));
    }
}
