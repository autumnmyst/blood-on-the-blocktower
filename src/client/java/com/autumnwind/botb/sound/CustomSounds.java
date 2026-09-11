package com.autumnwind.botb.sound;

import com.autumnwind.botb.BloodOnTheBlocktower;
import com.autumnwind.botb.util.PendingRoleAssignment;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

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
    public static final String MADNESS_RECEIVE = "custom/madness_receive";

    /** Role id, then role type, then alignment. */
    public static List<String> roleReceiveCandidates(PendingRoleAssignment assignment) {
        return List.of(
                ROLE_RECEIVE_DIR + assignment.getRoleId(),
                ROLE_RECEIVE_DIR + assignment.getRoleType().name().toLowerCase(Locale.ROOT),
                ROLE_RECEIVE_DIR + (assignment.isFinalGood() ? "good" : "evil"));
    }

    /** A madness-specific file first, then the same chain as a role receive. */
    public static List<String> madnessReceiveCandidates(PendingRoleAssignment assignment) {
        List<String> candidates = new ArrayList<>();
        candidates.add(MADNESS_RECEIVE);
        if (assignment != null) candidates.addAll(roleReceiveCandidates(assignment));
        return candidates;
    }

    public static List<String> voteMusicCandidates(boolean organGrinder) {
        return organGrinder ? List.of(VOTE_MUSIC_DIR + "organ_grinder") : List.of();
    }

    public static List<String> gameEndCandidates(boolean won) {
        return List.of(GAME_END_DIR + (won ? "victory" : "defeat"));
    }

    /** Plays the first candidate a resource pack provides, else the built-in event. */
    public static void playOneShot(Minecraft client, List<String> candidates, SoundEvent fallback, float volume) {
        client.getSoundManager().play(instance(client, candidates, fallback, volume, false));
    }

    /** A looping instance for the first candidate a resource pack provides, else the built-in event. */
    public static SoundInstance loop(Minecraft client, List<String> candidates, SoundEvent fallback, float volume) {
        return instance(client, candidates, fallback, volume, true);
    }

    /**
     * Each candidate is checked two ways under the same name: a sounds.json event, which gets
     * the vanilla features like weighted variants, then a bare file at {@code sounds/<name>.ogg}.
     */
    private static SoundInstance instance(Minecraft client, List<String> candidates, SoundEvent fallback, float volume, boolean repeat) {
        for (String candidate : candidates) {
            Identifier name = Identifier.fromNamespaceAndPath(BloodOnTheBlocktower.MOD_ID, candidate);
            if (client.getSoundManager().getSoundEvent(name) != null) {
                return new CustomSoundInstance(SoundEvent.createVariableRangeEvent(name), null, volume, repeat);
            }
            Identifier file = Identifier.fromNamespaceAndPath(BloodOnTheBlocktower.MOD_ID, "sounds/" + candidate + ".ogg");
            if (client.getResourceManager().getResource(file).isPresent()) {
                return new CustomSoundInstance(fallback, candidate, volume, repeat);
            }
        }
        return new CustomSoundInstance(fallback, null, volume, repeat);
    }
}
