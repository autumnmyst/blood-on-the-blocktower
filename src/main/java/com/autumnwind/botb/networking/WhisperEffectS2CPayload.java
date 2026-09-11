package com.autumnwind.botb.networking;

import com.autumnwind.botb.BloodOnTheBlocktower;
import com.autumnwind.botb.config.WhisperSettings;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

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
) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<WhisperEffectS2CPayload> ID =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(BloodOnTheBlocktower.MOD_ID, "whisper_effect"));

    public static final StreamCodec<RegistryFriendlyByteBuf, WhisperEffectS2CPayload> CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, WhisperEffectS2CPayload::senderUuid,
            UUIDUtil.STREAM_CODEC, WhisperEffectS2CPayload::targetUuid,
            ByteBufCodecs.VAR_INT, WhisperEffectS2CPayload::visualOrdinal,
            ByteBufCodecs.BOOL, WhisperEffectS2CPayload::audio,
            ByteBufCodecs.FLOAT, WhisperEffectS2CPayload::pitch,
            WhisperEffectS2CPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    public WhisperSettings.VisualMode visual() {
        WhisperSettings.VisualMode[] values = WhisperSettings.VisualMode.values();
        return values[Math.floorMod(visualOrdinal, values.length)];
    }
}
