package com.autumnwind.botb.networking;

import com.autumnwind.botb.BloodOnTheBlocktower;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Client-to-Server payload for resetting the current exile call.
 * Restores exile eligibility for the traveler.
 */
public record ResetExileC2SPayload() implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ResetExileC2SPayload> ID =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(BloodOnTheBlocktower.MOD_ID, "reset_exile"));

    public static final StreamCodec<ByteBuf, ResetExileC2SPayload> CODEC = StreamCodec.ofMember(
            (value, buf) -> {}, // No data to write
            buf -> new ResetExileC2SPayload() // No data to read
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
