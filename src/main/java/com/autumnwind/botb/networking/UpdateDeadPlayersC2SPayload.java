package com.autumnwind.botb.networking;

import com.autumnwind.botb.BloodOnTheBlocktower;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import net.minecraft.util.Uuids;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * C2S payload for updating dead player status only.
 * Sent from storyteller client to server to update which players are dead.
 *
 * @param silent suppresses death-side UI effects that don't make sense for a batched
 *               update, currently the daytime cosmetic lightning strike. The dawn
 *               batch (deferred night kills) sends silent=true, and live storyteller
 *               actions during the day send silent=false.
 * @param voudonModeActive whether Voudon is alive with ability under the new death
 *                         status, so a kill or revival of the Voudon flips the mode
 *                         immediately rather than at the next nomination
 * @param voudonPlayerUuid the UUID of the Voudon player, if Voudon mode is active
 */
public record UpdateDeadPlayersC2SPayload(
        Map<UUID, Boolean> deadPlayers,
        Map<UUID, Integer> seatNumbers,
        boolean silent,
        boolean voudonModeActive,
        Optional<UUID> voudonPlayerUuid
) implements CustomPayload {
    public static final Identifier UPDATE_DEAD_PLAYERS_ID = Identifier.of(BloodOnTheBlocktower.MOD_ID, "update_dead_players");
    public static final CustomPayload.Id<UpdateDeadPlayersC2SPayload> ID = new CustomPayload.Id<>(UPDATE_DEAD_PLAYERS_ID);

    public static final PacketCodec<RegistryByteBuf, UpdateDeadPlayersC2SPayload> CODEC = PacketCodec.tuple(
            PayloadCodecs.DEATH_MAP_CODEC, UpdateDeadPlayersC2SPayload::deadPlayers,
            PayloadCodecs.SEAT_MAP_CODEC, UpdateDeadPlayersC2SPayload::seatNumbers,
            PacketCodecs.BOOL, UpdateDeadPlayersC2SPayload::silent,
            PacketCodecs.BOOL, UpdateDeadPlayersC2SPayload::voudonModeActive,
            PacketCodecs.optional(Uuids.PACKET_CODEC), UpdateDeadPlayersC2SPayload::voudonPlayerUuid,
            UpdateDeadPlayersC2SPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
