package com.autumnwind.botb.networking;

import com.autumnwind.botb.BloodOnTheBlocktower;
import com.autumnwind.botb.config.WhisperSettings;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Server → all clients: the current whisper settings. Sent on player join and
 * whenever a storyteller updates them.
 */
public record SyncWhisperSettingsS2CPayload(WhisperSettings settings) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SyncWhisperSettingsS2CPayload> ID =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(BloodOnTheBlocktower.MOD_ID, "sync_whisper_settings"));

    public static final StreamCodec<ByteBuf, SyncWhisperSettingsS2CPayload> CODEC = StreamCodec.composite(
            WhisperSettings.CODEC, SyncWhisperSettingsS2CPayload::settings,
            SyncWhisperSettingsS2CPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
