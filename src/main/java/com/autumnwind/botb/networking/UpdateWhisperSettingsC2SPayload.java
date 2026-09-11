package com.autumnwind.botb.networking;

import com.autumnwind.botb.BloodOnTheBlocktower;
import com.autumnwind.botb.config.WhisperSettings;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Storyteller → server: replace the active whisper settings. Server validates the
 * sender is an operator (permission level &ge; 2) before applying.
 */
public record UpdateWhisperSettingsC2SPayload(WhisperSettings settings) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<UpdateWhisperSettingsC2SPayload> ID =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(BloodOnTheBlocktower.MOD_ID, "update_whisper_settings"));

    public static final StreamCodec<ByteBuf, UpdateWhisperSettingsC2SPayload> CODEC = StreamCodec.composite(
            WhisperSettings.CODEC, UpdateWhisperSettingsC2SPayload::settings,
            UpdateWhisperSettingsC2SPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
