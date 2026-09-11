package com.autumnwind.botb.networking;

import com.autumnwind.botb.BloodOnTheBlocktower;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Client-to-Server payload for swapping two players' seats.
 * Sent by storyteller when using Alt+Shift+Right-click seat swap feature.
 *
 * <p>Carries the storyteller's own seat map (PENDING_SEAT_NUMBERS) because server-side
 * {@code ServerState.PLAYER_SEAT_NUMBERS} is only populated by "Send Roles", and looking the
 * seats up there alone made every swap fail with "not seated" while the storyteller was
 * still arranging the table before the first send.
 */
public record SwapPlayersC2SPayload(UUID player1, UUID player2, Map<UUID, Integer> seatNumbers) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SwapPlayersC2SPayload> ID =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(BloodOnTheBlocktower.MOD_ID, "swap_players"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SwapPlayersC2SPayload> CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, SwapPlayersC2SPayload::player1,
            UUIDUtil.STREAM_CODEC, SwapPlayersC2SPayload::player2,
            PayloadCodecs.SEAT_MAP_CODEC, SwapPlayersC2SPayload::seatNumbers,
            SwapPlayersC2SPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
