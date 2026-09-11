package com.autumnwind.botb.networking;

import com.autumnwind.botb.BloodOnTheBlocktower;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

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
                                 Optional<List<UUID>> evilsForLegion) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<RunVoteC2SPayload> ID =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(BloodOnTheBlocktower.MOD_ID, "run_vote"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RunVoteC2SPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, RunVoteC2SPayload::organGrinderMode,
            UUIDUtil.STREAM_CODEC.apply(ByteBufCodecs.list()), RunVoteC2SPayload::bansheeHasAbilityPlayers,
            ByteBufCodecs.BOOL, RunVoteC2SPayload::voudonModeActive,
            UUIDUtil.STREAM_CODEC.apply(ByteBufCodecs::optional), RunVoteC2SPayload::voudonPlayerUuid,
            VoteMultiplierLists.CODEC, RunVoteC2SPayload::voteMultipliers,
            UUIDUtil.STREAM_CODEC.apply(ByteBufCodecs.list()).apply(ByteBufCodecs::optional),
            RunVoteC2SPayload::evilsForLegion,
            RunVoteC2SPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
