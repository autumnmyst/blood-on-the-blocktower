package com.autumnwind.botb.networking;

import com.autumnwind.botb.BloodOnTheBlocktower;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record TeleportPlayersToTownSquareSeatC2SPayload(int seatNumber, List<UUID> playerUuids) implements CustomPacketPayload {
    public static final ResourceLocation TELEPORT_PLAYERS_TOWN_SQUARE_SEAT_ID = ResourceLocation.fromNamespaceAndPath(BloodOnTheBlocktower.MOD_ID, "teleport_players_town_square_seat");
    public static final CustomPacketPayload.Type<TeleportPlayersToTownSquareSeatC2SPayload> ID = new CustomPacketPayload.Type<>(TELEPORT_PLAYERS_TOWN_SQUARE_SEAT_ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, TeleportPlayersToTownSquareSeatC2SPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, TeleportPlayersToTownSquareSeatC2SPayload::seatNumber,
            UUIDUtil.STREAM_CODEC.apply(ByteBufCodecs.list()), TeleportPlayersToTownSquareSeatC2SPayload::playerUuids,
            TeleportPlayersToTownSquareSeatC2SPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
