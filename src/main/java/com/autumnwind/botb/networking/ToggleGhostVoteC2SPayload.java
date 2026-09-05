package com.autumnwind.botb.networking;

import com.autumnwind.botb.BloodOnTheBlocktower;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;

import java.util.UUID;

/**
 * Client-to-Server payload for toggling a player's ghost vote used status.
 * Only valid for dead players. Sets or clears their intrinsic ghost vote used state.
 * @param playerUuid The UUID of the player whose ghost vote to toggle
 * @param setUsed true to mark ghost vote as used, false to restore it
 */
public record ToggleGhostVoteC2SPayload(UUID playerUuid, boolean setUsed) implements CustomPayload {
    public static final CustomPayload.Id<ToggleGhostVoteC2SPayload> ID =
            new CustomPayload.Id<>(Identifier.of(BloodOnTheBlocktower.MOD_ID, "toggle_ghost_vote"));

    public static final PacketCodec<RegistryByteBuf, ToggleGhostVoteC2SPayload> CODEC = PacketCodec.tuple(
            Uuids.PACKET_CODEC, ToggleGhostVoteC2SPayload::playerUuid,
            PacketCodecs.BOOL, ToggleGhostVoteC2SPayload::setUsed,
            ToggleGhostVoteC2SPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
