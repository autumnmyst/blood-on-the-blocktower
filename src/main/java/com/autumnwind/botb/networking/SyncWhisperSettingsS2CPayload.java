package com.autumnwind.botb.networking;

import com.autumnwind.botb.BloodOnTheBlocktower;
import com.autumnwind.botb.config.WhisperSettings;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Server → all clients: the current whisper settings. Sent on player join and
 * whenever a storyteller updates them.
 */
public record SyncWhisperSettingsS2CPayload(WhisperSettings settings) implements CustomPayload {
    public static final CustomPayload.Id<SyncWhisperSettingsS2CPayload> ID =
            new CustomPayload.Id<>(Identifier.of(BloodOnTheBlocktower.MOD_ID, "sync_whisper_settings"));

    public static final PacketCodec<ByteBuf, SyncWhisperSettingsS2CPayload> CODEC = PacketCodec.tuple(
            WhisperSettings.CODEC, SyncWhisperSettingsS2CPayload::settings,
            SyncWhisperSettingsS2CPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
