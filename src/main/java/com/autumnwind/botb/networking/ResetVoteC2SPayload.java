package com.autumnwind.botb.networking;

import com.autumnwind.botb.BloodOnTheBlocktower;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Client-to-Server payload for resetting the current vote/nomination.
 */
public record ResetVoteC2SPayload() implements CustomPayload {
    public static final CustomPayload.Id<ResetVoteC2SPayload> ID =
            new CustomPayload.Id<>(Identifier.of(BloodOnTheBlocktower.MOD_ID, "reset_vote"));

    public static final PacketCodec<ByteBuf, ResetVoteC2SPayload> CODEC = PacketCodec.of(
            (value, buf) -> {}, // No data to write
            buf -> new ResetVoteC2SPayload() // No data to read
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
