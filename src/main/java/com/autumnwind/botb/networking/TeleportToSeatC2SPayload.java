package com.autumnwind.botb.networking;

import com.autumnwind.botb.BloodOnTheBlocktower;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record TeleportToSeatC2SPayload(int seatNumber, boolean useDoorknock) implements CustomPayload {
    public static final Identifier TELEPORT_SEAT_ID = Identifier.of(BloodOnTheBlocktower.MOD_ID, "teleport_seat");
    public static final CustomPayload.Id<TeleportToSeatC2SPayload> ID = new CustomPayload.Id<>(TELEPORT_SEAT_ID);

    public static final PacketCodec<RegistryByteBuf, TeleportToSeatC2SPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.VAR_INT, TeleportToSeatC2SPayload::seatNumber,
            PacketCodecs.BOOL, TeleportToSeatC2SPayload::useDoorknock,
            TeleportToSeatC2SPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}