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
 * Client-to-Server payload for running a vote on the current nominee.
 * @param organGrinderMode true if the vote should be run in Organ Grinder mode
 *                         (blindness for non-operators, hidden vote counts, etc.)
 * @param bansheeHasAbilityPlayers list of player UUIDs who have the Banshee "Has Ability" reminder
 *                                  (these players can double vote by toggling their lever)
 * @param voudonModeActive true if Voudon is alive with ability (reverses who can vote)
 * @param voudonPlayerUuid UUID of the Voudon player if voudonModeActive is true
 * @param voteMultipliers reminder-based vote multipliers (Ug hat × 2, Bureaucrat × 3, Thief × -1)
 * @param evilsForLegion present only in a Legion game, and then holding every player the
 *                       storyteller considers evil. Its presence is what tells the server a
 *                       Legion is in play. The server can't work that out for itself, because
 *                       the role map it holds is the doctored one the players were sent.
 */
public record RunVoteC2SPayload(boolean organGrinderMode, List<UUID> bansheeHasAbilityPlayers,
                                 boolean voudonModeActive, Optional<UUID> voudonPlayerUuid,
                                 VoteMultiplierLists voteMultipliers,
                                 Optional<List<UUID>> evilsForLegion) implements CustomPayload {
    public static final CustomPayload.Id<RunVoteC2SPayload> ID =
            new CustomPayload.Id<>(Identifier.of(BloodOnTheBlocktower.MOD_ID, "run_vote"));

    public static final PacketCodec<RegistryByteBuf, RunVoteC2SPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.BOOL, RunVoteC2SPayload::organGrinderMode,
            Uuids.PACKET_CODEC.collect(PacketCodecs.toList()), RunVoteC2SPayload::bansheeHasAbilityPlayers,
            PacketCodecs.BOOL, RunVoteC2SPayload::voudonModeActive,
            Uuids.PACKET_CODEC.collect(PacketCodecs::optional), RunVoteC2SPayload::voudonPlayerUuid,
            VoteMultiplierLists.CODEC, RunVoteC2SPayload::voteMultipliers,
            Uuids.PACKET_CODEC.collect(PacketCodecs.toList()).collect(PacketCodecs::optional),
            RunVoteC2SPayload::evilsForLegion,
            RunVoteC2SPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
