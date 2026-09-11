package com.autumnwind.botb.networking;

import com.autumnwind.botb.BloodOnTheBlocktower;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record TeleportToSeatC2SPayload(int seatNumber, boolean useDoorknock) implements CustomPacketPayload {
    public static final ResourceLocation TELEPORT_SEAT_ID = ResourceLocation.fromNamespaceAndPath(BloodOnTheBlocktower.MOD_ID, "teleport_seat");
    public static final CustomPacketPayload.Type<TeleportToSeatC2SPayload> ID = new CustomPacketPayload.Type<>(TELEPORT_SEAT_ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, TeleportToSeatC2SPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, TeleportToSeatC2SPayload::seatNumber,
            ByteBufCodecs.BOOL, TeleportToSeatC2SPayload::useDoorknock,
            TeleportToSeatC2SPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}