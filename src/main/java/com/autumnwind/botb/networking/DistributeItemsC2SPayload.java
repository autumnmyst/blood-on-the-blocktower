package com.autumnwind.botb.networking;

import com.autumnwind.botb.BloodOnTheBlocktower;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.Map;
import java.util.UUID;

/**
 * Storyteller → server: hand out the Script and Grimoire items to every seated player.
 *
 * <p>Carries the client's authoritative seat map (storyteller's PENDING_SEAT_NUMBERS)
 * because server-side {@code ServerState.PLAYER_SEAT_NUMBERS} is only populated after
 * "Send Roles", and using it as the source of truth would make Distribute Items silently
 * no-op when used before the first send. Server still validates op permission.
 */
public record DistributeItemsC2SPayload(Map<UUID, Integer> seatNumbers) implements CustomPayload {
    public static final CustomPayload.Id<DistributeItemsC2SPayload> ID =
            new CustomPayload.Id<>(Identifier.of(BloodOnTheBlocktower.MOD_ID, "distribute_items"));

    public static final PacketCodec<RegistryByteBuf, DistributeItemsC2SPayload> CODEC = PacketCodec.tuple(
            PayloadCodecs.SEAT_MAP_CODEC, DistributeItemsC2SPayload::seatNumbers,
            DistributeItemsC2SPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
