package com.autumnwind.botb.networking;

import com.autumnwind.botb.BloodOnTheBlocktower;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record TeleportPlayersToSeatC2SPayload(int seatNumber, List<UUID> playerUuids) implements CustomPacketPayload {
    public static final Identifier TELEPORT_PLAYERS_SEAT_ID = Identifier.fromNamespaceAndPath(BloodOnTheBlocktower.MOD_ID, "teleport_players_seat");
    public static final CustomPacketPayload.Type<TeleportPlayersToSeatC2SPayload> ID = new CustomPacketPayload.Type<>(TELEPORT_PLAYERS_SEAT_ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, TeleportPlayersToSeatC2SPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, TeleportPlayersToSeatC2SPayload::seatNumber,
            UUIDUtil.STREAM_CODEC.apply(ByteBufCodecs.list()), TeleportPlayersToSeatC2SPayload::playerUuids,
            TeleportPlayersToSeatC2SPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
