package com.autumnwind.botb.networking;

import com.autumnwind.botb.BloodOnTheBlocktower;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Client-to-Server payload for opening nominations (triggered by storyteller clicking NOMINATIONS static action).
 * @param bansheeHasAbilityPlayers list of player UUIDs who have the Banshee "Has Ability" reminder
 *                                  (these players get double voting and double nominations, can nominate while dead)
 * @param voudonModeActive whether Voudon is alive with ability (reversed voting eligibility)
 * @param voudonPlayerUuid the UUID of the Voudon player, if Voudon mode is active
 * @param mayNotNominatePlayers list of player UUIDs who have a "May Not Nominate" reminder (e.g., Golem)
 *                               (these players cannot nominate even if otherwise eligible)
 */
public record OpenNominationsC2SPayload(
        List<UUID> bansheeHasAbilityPlayers,
        boolean voudonModeActive,
        Optional<UUID> voudonPlayerUuid,
        List<UUID> mayNotNominatePlayers
) implements CustomPayload {
    public static final CustomPayload.Id<OpenNominationsC2SPayload> ID =
            new CustomPayload.Id<>(Identifier.of(BloodOnTheBlocktower.MOD_ID, "open_nominations"));

    public static final PacketCodec<RegistryByteBuf, OpenNominationsC2SPayload> CODEC = PacketCodec.tuple(
            Uuids.PACKET_CODEC.collect(PacketCodecs.toList()), OpenNominationsC2SPayload::bansheeHasAbilityPlayers,
            PacketCodecs.BOOL, OpenNominationsC2SPayload::voudonModeActive,
            PacketCodecs.optional(Uuids.PACKET_CODEC), OpenNominationsC2SPayload::voudonPlayerUuid,
            Uuids.PACKET_CODEC.collect(PacketCodecs.toList()), OpenNominationsC2SPayload::mayNotNominatePlayers,
            OpenNominationsC2SPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
