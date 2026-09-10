package com.autumnwind.botb.sound;

import com.autumnwind.botb.BloodOnTheBlocktower;
import com.autumnwind.botb.util.PendingRoleAssignment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.Locale;

/**
 * Resource pack sound overrides that fall back through a chain of file paths under
 * {@code sounds/custom/} and finally to the mod's built-in event. Packs just drop files in;
 * nothing needs declaring in sounds.json.
 */
public final class CustomSounds {

    private CustomSounds() {}

    public static final String ROLE_RECEIVE_DIR = "custom/role_receive/";
    public static final String VOTE_MUSIC_DIR = "custom/vote_music/";
    public static final String GAME_END_DIR = "custom/game_end/";

    /** Role id, then role type, then alignment. */
    public static List<String> roleReceiveCandidates(PendingRoleAssignment assignment) {
        return List.of(
                ROLE_RECEIVE_DIR + assignment.getRoleId(),
                ROLE_RECEIVE_DIR + assignment.getRoleType().name().toLowerCase(Locale.ROOT),
                ROLE_RECEIVE_DIR + (assignment.isFinalGood() ? "good" : "evil"));
    }

    public static List<String> voteMusicCandidates(boolean organGrinder) {
        return organGrinder ? List.of(VOTE_MUSIC_DIR + "organ_grinder") : List.of();
    }

    public static List<String> gameEndCandidates(boolean won) {
        return List.of(GAME_END_DIR + (won ? "victory" : "defeat"));
    }

    /** Plays the first candidate a resource pack provides, else the built-in event. */
    public static void playOneShot(MinecraftClient client, List<String> candidates, SoundEvent fallback, float volume) {
        client.getSoundManager().play(instance(client, candidates, fallback, volume, false));
    }

    /** A looping instance for the first candidate a resource pack provides, else the built-in event. */
    public static SoundInstance loop(MinecraftClient client, List<String> candidates, SoundEvent fallback, float volume) {
        return instance(client, candidates, fallback, volume, true);
    }

    /**
     * Each candidate is checked two ways under the same name: a sounds.json event, which gets
     * the vanilla features like weighted variants, then a bare file at {@code sounds/<name>.ogg}.
     */
    private static SoundInstance instance(MinecraftClient client, List<String> candidates, SoundEvent fallback, float volume, boolean repeat) {
        for (String candidate : candidates) {
            Identifier name = Identifier.of(BloodOnTheBlocktower.MOD_ID, candidate);
            if (client.getSoundManager().get(name) != null) {
                return new CustomSoundInstance(SoundEvent.of(name), null, volume, repeat);
            }
            Identifier file = Identifier.of(BloodOnTheBlocktower.MOD_ID, "sounds/" + candidate + ".ogg");
            if (client.getResourceManager().getResource(file).isPresent()) {
                return new CustomSoundInstance(fallback, candidate, volume, repeat);
            }
        }
        return new CustomSoundInstance(fallback, null, volume, repeat);
    }
}
