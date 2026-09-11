package com.autumnwind.botb.networking;

import com.autumnwind.botb.BloodOnTheBlocktower;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Client-to-Server payload for resetting the current vote/nomination.
 */
public record ResetVoteC2SPayload() implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ResetVoteC2SPayload> ID =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(BloodOnTheBlocktower.MOD_ID, "reset_vote"));

    public static final StreamCodec<ByteBuf, ResetVoteC2SPayload> CODEC = StreamCodec.ofMember(
            (value, buf) -> {}, // No data to write
            buf -> new ResetVoteC2SPayload() // No data to read
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
