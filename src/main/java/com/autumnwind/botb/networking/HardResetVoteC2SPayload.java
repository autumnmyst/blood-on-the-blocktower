package com.autumnwind.botb.networking;

import com.autumnwind.botb.BloodOnTheBlocktower;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Client-to-Server payload for hard resetting the vote (clears nominations, votes, and MFE).
 */
public record HardResetVoteC2SPayload() implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<HardResetVoteC2SPayload> ID =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(BloodOnTheBlocktower.MOD_ID, "hard_reset_vote"));

    public static final StreamCodec<RegistryFriendlyByteBuf, HardResetVoteC2SPayload> CODEC = StreamCodec.ofMember(
            (payload, buf) -> {}, // No data to write
            (buf) -> new HardResetVoteC2SPayload() // No data to read
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
