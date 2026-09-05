package com.autumnwind.botb.networking;

import com.autumnwind.botb.BloodOnTheBlocktower;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Client-to-Server payload for running an exile support vote.
 * No special modes apply - all players vote normally without consuming ghost votes.
 */
public record RunExileSupportC2SPayload() implements CustomPayload {
    public static final CustomPayload.Id<RunExileSupportC2SPayload> ID =
            new CustomPayload.Id<>(Identifier.of(BloodOnTheBlocktower.MOD_ID, "run_exile_support"));

    public static final PacketCodec<RegistryByteBuf, RunExileSupportC2SPayload> CODEC = PacketCodec.of(
            (payload, buf) -> {},  // Nothing to write
            buf -> new RunExileSupportC2SPayload()
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
