package com.autumnwind.botb.networking;

import com.autumnwind.botb.BloodOnTheBlocktower;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;

import java.util.Optional;
import java.util.UUID;

/**
 * Client-to-Server payload for failed execution (no death marking).
 * Executes a player but does not mark them as dead.
 * @param butcherAliveWithAbility If true, Butcher traveler is alive with ability,
 *        so nominations should continue after execution with only Butcher able to nominate.
 * @param butcherUuid The UUID of the Butcher player (required if butcherAliveWithAbility is true)
 */
public record ExecutePlayerFailC2SPayload(UUID player, boolean forced, boolean butcherAliveWithAbility, Optional<UUID> butcherUuid) implements CustomPayload {
    public static final CustomPayload.Id<ExecutePlayerFailC2SPayload> ID =
            new CustomPayload.Id<>(Identifier.of(BloodOnTheBlocktower.MOD_ID, "execute_player_fail"));

    public static final PacketCodec<RegistryByteBuf, ExecutePlayerFailC2SPayload> CODEC = PacketCodec.tuple(
            Uuids.PACKET_CODEC, ExecutePlayerFailC2SPayload::player,
            PacketCodecs.BOOL, ExecutePlayerFailC2SPayload::forced,
            PacketCodecs.BOOL, ExecutePlayerFailC2SPayload::butcherAliveWithAbility,
            Uuids.PACKET_CODEC.collect(PacketCodecs::optional), ExecutePlayerFailC2SPayload::butcherUuid,
            ExecutePlayerFailC2SPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
