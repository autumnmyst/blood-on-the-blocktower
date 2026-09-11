package com.autumnwind.botb.networking;

import com.autumnwind.botb.BloodOnTheBlocktower;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Server-to-Client payload for updating vote state during an active vote.
 * Also used for exile support votes with isExileSupport=true.
 */
public record VoteStateUpdateS2CPayload(
        int lockedVoteCount,
        int secondsUntilMyVoteLocks,
        int myVotePosition,
        Map<UUID, Boolean> lockedVotes,
        Map<UUID, Boolean> leverStates,
        boolean organGrinderMode,
        boolean hasSecretlyUsedGhostVote, // Player secretly used ghost vote in OG mode
        boolean isExileSupport, // True if this is for exile support, not regular voting
        boolean isVoudonBlocked, // Player is alive non-Voudon in Voudon mode (can't vote)
        Set<UUID> bansheePlayers, // Banshees with their ability (vote like living players)
        Set<UUID> bansheeDoubleActivePlayers // Banshees whose double vote is currently active
) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<VoteStateUpdateS2CPayload> ID =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(BloodOnTheBlocktower.MOD_ID, "vote_state_update"));

    public static final StreamCodec<RegistryFriendlyByteBuf, VoteStateUpdateS2CPayload> CODEC = StreamCodec.ofMember(
            VoteStateUpdateS2CPayload::write,
            VoteStateUpdateS2CPayload::read
    );

    private static void write(VoteStateUpdateS2CPayload payload, RegistryFriendlyByteBuf buf) {
        buf.writeInt(payload.lockedVoteCount);
        buf.writeInt(payload.secondsUntilMyVoteLocks);
        buf.writeInt(payload.myVotePosition);

        // Write locked votes map
        buf.writeInt(payload.lockedVotes.size());
        for (Map.Entry<UUID, Boolean> entry : payload.lockedVotes.entrySet()) {
            UUIDUtil.STREAM_CODEC.encode(buf, entry.getKey());
            buf.writeBoolean(entry.getValue());
        }

        // Write lever states map
        buf.writeInt(payload.leverStates.size());
        for (Map.Entry<UUID, Boolean> entry : payload.leverStates.entrySet()) {
            UUIDUtil.STREAM_CODEC.encode(buf, entry.getKey());
            buf.writeBoolean(entry.getValue());
        }

        // Write Organ Grinder mode
        buf.writeBoolean(payload.organGrinderMode);

        // Write secretly used ghost vote flag
        buf.writeBoolean(payload.hasSecretlyUsedGhostVote);

        // Write exile support flag
        buf.writeBoolean(payload.isExileSupport);

        // Write Voudon blocked flag
        buf.writeBoolean(payload.isVoudonBlocked);

        LeverStateUpdateS2CPayload.writeUuidSet(buf, payload.bansheePlayers);
        LeverStateUpdateS2CPayload.writeUuidSet(buf, payload.bansheeDoubleActivePlayers);
    }

    private static VoteStateUpdateS2CPayload read(RegistryFriendlyByteBuf buf) {
        int lockedVoteCount = buf.readInt();
        int secondsUntilMyVoteLocks = buf.readInt();
        int myVotePosition = buf.readInt();

        // Read locked votes map
        int lockedVotesSize = buf.readInt();
        Map<UUID, Boolean> lockedVotes = new HashMap<>();
        for (int i = 0; i < lockedVotesSize; i++) {
            UUID uuid = UUIDUtil.STREAM_CODEC.decode(buf);
            boolean value = buf.readBoolean();
            lockedVotes.put(uuid, value);
        }

        // Read lever states map
        int leverStatesSize = buf.readInt();
        Map<UUID, Boolean> leverStates = new HashMap<>();
        for (int i = 0; i < leverStatesSize; i++) {
            UUID uuid = UUIDUtil.STREAM_CODEC.decode(buf);
            boolean value = buf.readBoolean();
            leverStates.put(uuid, value);
        }

        // Read Organ Grinder mode
        boolean organGrinderMode = buf.readBoolean();

        // Read secretly used ghost vote flag
        boolean hasSecretlyUsedGhostVote = buf.readBoolean();

        // Read exile support flag
        boolean isExileSupport = buf.readBoolean();

        // Read Voudon blocked flag
        boolean isVoudonBlocked = buf.readBoolean();

        Set<UUID> bansheePlayers = LeverStateUpdateS2CPayload.readUuidSet(buf);
        Set<UUID> bansheeDoubleActivePlayers = LeverStateUpdateS2CPayload.readUuidSet(buf);

        return new VoteStateUpdateS2CPayload(lockedVoteCount, secondsUntilMyVoteLocks, myVotePosition, lockedVotes, leverStates, organGrinderMode, hasSecretlyUsedGhostVote, isExileSupport, isVoudonBlocked, bansheePlayers, bansheeDoubleActivePlayers);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
