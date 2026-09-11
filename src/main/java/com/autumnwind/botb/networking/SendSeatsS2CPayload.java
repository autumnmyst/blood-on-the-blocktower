package com.autumnwind.botb.networking;

import com.autumnwind.botb.BloodOnTheBlocktower;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * S2C payload to broadcast seat numbers to all players.
 */
public record SendSeatsS2CPayload(Map<UUID, Integer> seatNumbers) implements CustomPacketPayload {
    public static final ResourceLocation SEND_SEATS_ID = ResourceLocation.fromNamespaceAndPath(BloodOnTheBlocktower.MOD_ID, "send_seats");
    public static final CustomPacketPayload.Type<SendSeatsS2CPayload> ID = new CustomPacketPayload.Type<>(SEND_SEATS_ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, SendSeatsS2CPayload> CODEC = StreamCodec.composite(
            PayloadCodecs.SEAT_MAP_CODEC,
            SendSeatsS2CPayload::seatNumbers,
            SendSeatsS2CPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
