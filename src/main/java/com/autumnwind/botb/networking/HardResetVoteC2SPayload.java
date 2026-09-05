package com.autumnwind.botb.networking;

import com.autumnwind.botb.BloodOnTheBlocktower;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Client-to-Server payload for hard resetting the vote (clears nominations, votes, and MFE).
 */
public record HardResetVoteC2SPayload() implements CustomPayload {
    public static final CustomPayload.Id<HardResetVoteC2SPayload> ID =
            new CustomPayload.Id<>(Identifier.of(BloodOnTheBlocktower.MOD_ID, "hard_reset_vote"));

    public static final PacketCodec<RegistryByteBuf, HardResetVoteC2SPayload> CODEC = PacketCodec.of(
            (payload, buf) -> {}, // No data to write
            (buf) -> new HardResetVoteC2SPayload() // No data to read
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
