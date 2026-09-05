package com.autumnwind.botb.networking;

import com.autumnwind.botb.BloodOnTheBlocktower;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.Map;
import java.util.UUID;

/**
 * S2C payload to broadcast seat numbers to all players.
 */
public record SendSeatsS2CPayload(Map<UUID, Integer> seatNumbers) implements CustomPayload {
    public static final Identifier SEND_SEATS_ID = Identifier.of(BloodOnTheBlocktower.MOD_ID, "send_seats");
    public static final CustomPayload.Id<SendSeatsS2CPayload> ID = new CustomPayload.Id<>(SEND_SEATS_ID);

    public static final PacketCodec<RegistryByteBuf, SendSeatsS2CPayload> CODEC = PacketCodec.tuple(
            PayloadCodecs.SEAT_MAP_CODEC,
            SendSeatsS2CPayload::seatNumbers,
            SendSeatsS2CPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
