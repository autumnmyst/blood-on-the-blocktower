package com.autumnwind.botb.networking;

import com.autumnwind.botb.BloodOnTheBlocktower;
import com.autumnwind.botb.daytime.VotingManager;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Server-to-Client payload for the final vote result.
 *
 * For Legion games: when only evil players vote, the storyteller should see 0 votes.
 * This means the storyteller may see a different result (e.g., NOT_ENOUGH instead of MARKED/TIE).
 * The storytellerResult/storytellerMFE/storytellerMFEVotes fields provide the storyteller's view.
 */
public record VoteResultS2CPayload(
        VotingManager.VoteResult result,
        String nomineeName,
        int voteCount,
        List<UUID> voters,
        boolean organGrinderMode,
        boolean legionProtectedVote,
        VotingManager.VoteResult storytellerResult,
        UUID storytellerMFE,
        int storytellerMFEVotes
) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<VoteResultS2CPayload> ID =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(BloodOnTheBlocktower.MOD_ID, "vote_result"));

    public static final StreamCodec<RegistryFriendlyByteBuf, VoteResultS2CPayload> CODEC = StreamCodec.ofMember(
            VoteResultS2CPayload::write,
            VoteResultS2CPayload::read
    );

    private static void write(VoteResultS2CPayload payload, RegistryFriendlyByteBuf buf) {
        // Write result as ordinal
        buf.writeInt(payload.result.ordinal());

        // Write nominee name
        ByteBufCodecs.STRING_UTF8.encode(buf, payload.nomineeName);

        // Write vote count
        buf.writeInt(payload.voteCount);

        // Write voters list
        buf.writeInt(payload.voters.size());
        for (UUID voter : payload.voters) {
            UUIDUtil.STREAM_CODEC.encode(buf, voter);
        }

        // Write Organ Grinder mode
        buf.writeBoolean(payload.organGrinderMode);

        // Write Legion protected vote flag
        buf.writeBoolean(payload.legionProtectedVote);

        // Write storyteller-specific fields for Legion
        buf.writeInt(payload.storytellerResult.ordinal());
        buf.writeBoolean(payload.storytellerMFE != null);
        if (payload.storytellerMFE != null) {
            UUIDUtil.STREAM_CODEC.encode(buf, payload.storytellerMFE);
        }
        buf.writeInt(payload.storytellerMFEVotes);
    }

    private static VoteResultS2CPayload read(RegistryFriendlyByteBuf buf) {
        // Read result
        int resultOrdinal = buf.readInt();
        VotingManager.VoteResult result = VotingManager.VoteResult.values()[resultOrdinal];

        // Read nominee name
        String nomineeName = ByteBufCodecs.STRING_UTF8.decode(buf);

        // Read vote count
        int voteCount = buf.readInt();

        // Read voters list
        int votersSize = buf.readInt();
        List<UUID> voters = new ArrayList<>();
        for (int i = 0; i < votersSize; i++) {
            voters.add(UUIDUtil.STREAM_CODEC.decode(buf));
        }

        // Read Organ Grinder mode
        boolean organGrinderMode = buf.readBoolean();

        // Read Legion protected vote flag
        boolean legionProtectedVote = buf.readBoolean();

        // Read storyteller-specific fields for Legion
        int storytellerResultOrdinal = buf.readInt();
        VotingManager.VoteResult storytellerResult = VotingManager.VoteResult.values()[storytellerResultOrdinal];
        boolean hasStorytellerMFE = buf.readBoolean();
        UUID storytellerMFE = hasStorytellerMFE ? UUIDUtil.STREAM_CODEC.decode(buf) : null;
        int storytellerMFEVotes = buf.readInt();

        return new VoteResultS2CPayload(result, nomineeName, voteCount, voters, organGrinderMode, legionProtectedVote, storytellerResult, storytellerMFE, storytellerMFEVotes);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
