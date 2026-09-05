package com.autumnwind.botb.networking;

import com.autumnwind.botb.BloodOnTheBlocktower;
import com.autumnwind.botb.config.WhisperSettings;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Storyteller → server: replace the active whisper settings. Server validates the
 * sender is an operator (permission level &ge; 2) before applying.
 */
public record UpdateWhisperSettingsC2SPayload(WhisperSettings settings) implements CustomPayload {
    public static final CustomPayload.Id<UpdateWhisperSettingsC2SPayload> ID =
            new CustomPayload.Id<>(Identifier.of(BloodOnTheBlocktower.MOD_ID, "update_whisper_settings"));

    public static final PacketCodec<ByteBuf, UpdateWhisperSettingsC2SPayload> CODEC = PacketCodec.tuple(
            WhisperSettings.CODEC, UpdateWhisperSettingsC2SPayload::settings,
            UpdateWhisperSettingsC2SPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
