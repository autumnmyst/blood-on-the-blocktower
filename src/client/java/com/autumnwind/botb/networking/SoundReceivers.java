package com.autumnwind.botb.networking;

import com.autumnwind.botb.sound.CustomSounds;
import com.autumnwind.botb.sound.ModSounds;
import com.autumnwind.botb.states.ClientState;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.sound.SoundEvent;

/**
 * Client-bound packet receivers for sounds. One-shot sounds come from a table of sound,
 * base volume, and the player's volume multiplier; the vote music and clock ticking loops
 * are started and stopped explicitly so a new start replaces a running loop.
 */
final class SoundReceivers {

    private SoundReceivers() {}

    private record OneShot(SoundEvent sound, float baseVolume, Supplier<Float> multiplier) {
        float volume() {
            return baseVolume * multiplier.get();
        }
    }

    private static final Map<String, OneShot> ONE_SHOTS = Map.ofEntries(
            Map.entry(PlaySoundS2CPayload.DUSK, new OneShot(ModSounds.DUSK, ClientState.BASE_VOLUME_DUSK, () -> ClientState.volumeDawnDusk)),
            Map.entry(PlaySoundS2CPayload.DAWN, new OneShot(ModSounds.DAWN, ClientState.BASE_VOLUME_DAWN, () -> ClientState.volumeDawnDusk)),
            Map.entry(PlaySoundS2CPayload.CALL_BACK, new OneShot(ModSounds.CALL_BACK, ClientState.BASE_VOLUME_CALL_BACK, () -> ClientState.volumeNominations)),
            Map.entry(PlaySoundS2CPayload.NOMINATION, new OneShot(ModSounds.NOMINATION, ClientState.BASE_VOLUME_NOMINATION, () -> ClientState.volumeNominations)),
            Map.entry(PlaySoundS2CPayload.VOTE_START, new OneShot(ModSounds.VOTE_START, ClientState.BASE_VOLUME_VOTE_START, () -> ClientState.volumeNominations)),
            Map.entry(PlaySoundS2CPayload.NOT_ENOUGH_VOTES, new OneShot(ModSounds.NOT_ENOUGH_VOTES, ClientState.BASE_VOLUME_NOT_ENOUGH_VOTES, () -> ClientState.volumeNominations)),
            Map.entry(PlaySoundS2CPayload.TIE, new OneShot(ModSounds.TIE, ClientState.BASE_VOLUME_TIE, () -> ClientState.volumeNominations)),
            Map.entry(PlaySoundS2CPayload.MARKED, new OneShot(ModSounds.MARKED, ClientState.BASE_VOLUME_MARKED, () -> ClientState.volumeNominations)),
            Map.entry(PlaySoundS2CPayload.EXECUTION, new OneShot(ModSounds.EXECUTION, ClientState.BASE_VOLUME_EXECUTION, () -> ClientState.volumeNominations)),
            // The survived sting shares the execution base volume
            Map.entry(PlaySoundS2CPayload.EXECUTION_SURVIVED, new OneShot(ModSounds.EXECUTION_SURVIVED, ClientState.BASE_VOLUME_EXECUTION, () -> ClientState.volumeNominations)),
            Map.entry(PlaySoundS2CPayload.GAME_END, new OneShot(ModSounds.GAME_END, ClientState.BASE_VOLUME_GAME_END, () -> ClientState.volumeFinalReveal))
    );

    // Looping sounds are kept so they can be stopped later
    private static SoundInstance voteMusicInstance = null;
    private static SoundInstance clockTickingInstance = null;

    static void register() {
        ClientPlayNetworking.registerGlobalReceiver(PlaySoundS2CPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                MinecraftClient client = context.client();
                if (client.player == null || client.world == null) return;
                String type = payload.soundType();

                switch (type) {
                    case PlaySoundS2CPayload.DOORBELL, PlaySoundS2CPayload.DOORKNOCK -> {
                        // Play whichever sound the storyteller chose
                        float volume = ClientState.BASE_VOLUME_DOORBELL * ClientState.volumeDoorbell;
                        if (volume > 0) client.player.playSound(ModSounds.of(type), volume, 1.0f);
                    }
                    case PlaySoundS2CPayload.VOTE_MUSIC, PlaySoundS2CPayload.VOTE_MUSIC_ORGAN_GRINDER -> {
                        boolean organGrinder = type.equals(PlaySoundS2CPayload.VOTE_MUSIC_ORGAN_GRINDER);
                        voteMusicInstance = restartLoop(client, voteMusicInstance,
                                CustomSounds.voteMusicCandidates(organGrinder), ModSounds.VOTE_MUSIC,
                                ClientState.BASE_VOLUME_VOTE_MUSIC * ClientState.volumeNominations);
                    }
                    case PlaySoundS2CPayload.VOTE_MUSIC_STOP -> voteMusicInstance = stopLoop(client, voteMusicInstance);
                    case PlaySoundS2CPayload.CLOCK_TICKING -> {
                        clockTickingInstance = restartLoop(client, clockTickingInstance, List.of(), ModSounds.CLOCK_TICKING,
                                ClientState.BASE_VOLUME_CLOCK_TICKING * ClientState.volumeNominations);
                    }
                    case PlaySoundS2CPayload.CLOCK_TICKING_STOP -> clockTickingInstance = stopLoop(client, clockTickingInstance);
                    default -> {
                        OneShot oneShot = ONE_SHOTS.get(type);
                        if (oneShot != null) {
                            float volume = oneShot.volume();
                            if (volume > 0) client.player.playSound(oneShot.sound(), volume, 1.0f);
                        }
                    }
                }
            });
        });
    }

    /** Stops the running loop if any, then starts a new one when the volume is audible. */
    private static SoundInstance restartLoop(MinecraftClient client, SoundInstance running, List<String> customCandidates,
                                             SoundEvent sound, float volume) {
        stopLoop(client, running);
        if (volume <= 0) return null;
        SoundInstance loop = CustomSounds.loop(client, customCandidates, sound, volume);
        client.getSoundManager().play(loop);
        return loop;
    }

    private static SoundInstance stopLoop(MinecraftClient client, SoundInstance running) {
        if (running != null) client.getSoundManager().stop(running);
        return null;
    }
}
