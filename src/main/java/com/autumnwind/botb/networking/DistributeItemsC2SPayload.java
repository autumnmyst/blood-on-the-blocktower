package com.autumnwind.botb.networking;

import com.autumnwind.botb.BloodOnTheBlocktower;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Storyteller → server: hand out the Script and Grimoire items to every seated player.
 *
 * <p>Carries the client's authoritative seat map (storyteller's PENDING_SEAT_NUMBERS)
 * because server-side {@code ServerState.PLAYER_SEAT_NUMBERS} is only populated after
 * "Send Roles", and using it as the source of truth would make Distribute Items silently
 * no-op when used before the first send. Server still validates op permission.
 */
public record DistributeItemsC2SPayload(Map<UUID, Integer> seatNumbers) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<DistributeItemsC2SPayload> ID =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(BloodOnTheBlocktower.MOD_ID, "distribute_items"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DistributeItemsC2SPayload> CODEC = StreamCodec.composite(
            PayloadCodecs.SEAT_MAP_CODEC, DistributeItemsC2SPayload::seatNumbers,
            DistributeItemsC2SPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
