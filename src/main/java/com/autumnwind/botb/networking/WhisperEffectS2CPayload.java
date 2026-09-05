package com.autumnwind.botb.networking;

import com.autumnwind.botb.BloodOnTheBlocktower;
import com.autumnwind.botb.config.WhisperSettings;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;

import java.util.UUID;

/**
 * Server → clients: render a whisper visual/audio effect arcing from sender to target.
 * Sent only on a successful (delivered) whisper, and only when at least one of visual
 * or audio is enabled in the active {@link WhisperSettings}.
 *
 * @param audio true if this client should play the whisper audio cue (server has
 *              already gated this per-recipient by VC group when {@code vcEnforced}
 *              is on), and always positional at the sender.
 * @param pitch small per-whisper pitch jitter (~0.92–1.08) for variety.
 */
public record WhisperEffectS2CPayload(
        UUID senderUuid,
        UUID targetUuid,
        int visualOrdinal,
        boolean audio,
        float pitch
) implements CustomPayload {
    public static final CustomPayload.Id<WhisperEffectS2CPayload> ID =
            new CustomPayload.Id<>(Identifier.of(BloodOnTheBlocktower.MOD_ID, "whisper_effect"));

    public static final PacketCodec<RegistryByteBuf, WhisperEffectS2CPayload> CODEC = PacketCodec.tuple(
            Uuids.PACKET_CODEC, WhisperEffectS2CPayload::senderUuid,
            Uuids.PACKET_CODEC, WhisperEffectS2CPayload::targetUuid,
            PacketCodecs.VAR_INT, WhisperEffectS2CPayload::visualOrdinal,
            PacketCodecs.BOOL, WhisperEffectS2CPayload::audio,
            PacketCodecs.FLOAT, WhisperEffectS2CPayload::pitch,
            WhisperEffectS2CPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    public WhisperSettings.VisualMode visual() {
        WhisperSettings.VisualMode[] values = WhisperSettings.VisualMode.values();
        return values[Math.floorMod(visualOrdinal, values.length)];
    }
}
