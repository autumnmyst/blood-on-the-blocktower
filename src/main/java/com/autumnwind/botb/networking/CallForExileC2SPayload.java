package com.autumnwind.botb.networking;

import com.autumnwind.botb.BloodOnTheBlocktower;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;

import java.util.UUID;

/**
 * Client-to-Server payload for calling for exile of a traveler.
 */
public record CallForExileC2SPayload(UUID caller, UUID traveler, boolean override) implements CustomPayload {
    public static final CustomPayload.Id<CallForExileC2SPayload> ID =
            new CustomPayload.Id<>(Identifier.of(BloodOnTheBlocktower.MOD_ID, "call_for_exile"));

    public static final PacketCodec<RegistryByteBuf, CallForExileC2SPayload> CODEC = PacketCodec.tuple(
            Uuids.PACKET_CODEC, CallForExileC2SPayload::caller,
            Uuids.PACKET_CODEC, CallForExileC2SPayload::traveler,
            PacketCodecs.BOOL, CallForExileC2SPayload::override,
            CallForExileC2SPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
