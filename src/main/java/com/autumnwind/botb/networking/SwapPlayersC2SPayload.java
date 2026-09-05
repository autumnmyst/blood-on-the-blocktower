package com.autumnwind.botb.networking;

import com.autumnwind.botb.BloodOnTheBlocktower;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;

import java.util.Map;
import java.util.UUID;

/**
 * Client-to-Server payload for swapping two players' seats.
 * Sent by storyteller when using Alt+Shift+Right-click seat swap feature.
 *
 * <p>Carries the storyteller's own seat map (PENDING_SEAT_NUMBERS) because server-side
 * {@code ServerState.PLAYER_SEAT_NUMBERS} is only populated by "Send Roles", and looking the
 * seats up there alone made every swap fail with "not seated" while the storyteller was
 * still arranging the table before the first send.
 */
public record SwapPlayersC2SPayload(UUID player1, UUID player2, Map<UUID, Integer> seatNumbers) implements CustomPayload {
    public static final CustomPayload.Id<SwapPlayersC2SPayload> ID =
            new CustomPayload.Id<>(Identifier.of(BloodOnTheBlocktower.MOD_ID, "swap_players"));

    public static final PacketCodec<RegistryByteBuf, SwapPlayersC2SPayload> CODEC = PacketCodec.tuple(
            Uuids.PACKET_CODEC, SwapPlayersC2SPayload::player1,
            Uuids.PACKET_CODEC, SwapPlayersC2SPayload::player2,
            PayloadCodecs.SEAT_MAP_CODEC, SwapPlayersC2SPayload::seatNumbers,
            SwapPlayersC2SPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
