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

public record TeleportPlayersToSeatC2SPayload(int seatNumber, List<UUID> playerUuids) implements CustomPayload {
    public static final Identifier TELEPORT_PLAYERS_SEAT_ID = Identifier.of(BloodOnTheBlocktower.MOD_ID, "teleport_players_seat");
    public static final CustomPayload.Id<TeleportPlayersToSeatC2SPayload> ID = new CustomPayload.Id<>(TELEPORT_PLAYERS_SEAT_ID);

    public static final PacketCodec<RegistryByteBuf, TeleportPlayersToSeatC2SPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.VAR_INT, TeleportPlayersToSeatC2SPayload::seatNumber,
            Uuids.PACKET_CODEC.collect(PacketCodecs.toList()), TeleportPlayersToSeatC2SPayload::playerUuids,
            TeleportPlayersToSeatC2SPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
