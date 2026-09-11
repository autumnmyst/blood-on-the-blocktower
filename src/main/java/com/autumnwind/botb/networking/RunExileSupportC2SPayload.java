package com.autumnwind.botb.networking;

import com.autumnwind.botb.BloodOnTheBlocktower;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Client-to-Server payload for running an exile support vote.
 * No special modes apply - all players vote normally without consuming ghost votes.
 */
public record RunExileSupportC2SPayload() implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<RunExileSupportC2SPayload> ID =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(BloodOnTheBlocktower.MOD_ID, "run_exile_support"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RunExileSupportC2SPayload> CODEC = StreamCodec.ofMember(
            (payload, buf) -> {},  // Nothing to write
            buf -> new RunExileSupportC2SPayload()
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
