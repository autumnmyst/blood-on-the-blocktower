package com.autumnwind.botb.networking;

import com.autumnwind.botb.BloodOnTheBlocktower;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;

import java.util.List;
import java.util.UUID;

public record TeleportPlayersToTownSquareSeatC2SPayload(int seatNumber, List<UUID> playerUuids) implements CustomPayload {
    public static final Identifier TELEPORT_PLAYERS_TOWN_SQUARE_SEAT_ID = Identifier.of(BloodOnTheBlocktower.MOD_ID, "teleport_players_town_square_seat");
    public static final CustomPayload.Id<TeleportPlayersToTownSquareSeatC2SPayload> ID = new CustomPayload.Id<>(TELEPORT_PLAYERS_TOWN_SQUARE_SEAT_ID);

    public static final PacketCodec<RegistryByteBuf, TeleportPlayersToTownSquareSeatC2SPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.VAR_INT, TeleportPlayersToTownSquareSeatC2SPayload::seatNumber,
            Uuids.PACKET_CODEC.collect(PacketCodecs.toList()), TeleportPlayersToTownSquareSeatC2SPayload::playerUuids,
            TeleportPlayersToTownSquareSeatC2SPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
