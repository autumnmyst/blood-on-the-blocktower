package com.autumnwind.botb.networking;

import java.util.List;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Reminder-based vote multiplier lists sent from client to server.
 * The three lists are disjoint per role but a single player could appear in multiple
 * (e.g. Bureaucrat-chosen Ug-hat wearer → × 3 × 2).
 *
 * @param ugHatPlayers God of Ug "Ug hat" reminder → vote × 2
 * @param bureaucrat3VotePlayers Bureaucrat "3 Votes" reminder → vote × 3
 * @param thiefNegativeVotePlayers Thief "Negative Vote" reminder → vote × -1
 */
public record VoteMultiplierLists(List<UUID> ugHatPlayers,
                                   List<UUID> bureaucrat3VotePlayers,
                                   List<UUID> thiefNegativeVotePlayers) {

    public static final StreamCodec<RegistryFriendlyByteBuf, VoteMultiplierLists> CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC.apply(ByteBufCodecs.list()), VoteMultiplierLists::ugHatPlayers,
            UUIDUtil.STREAM_CODEC.apply(ByteBufCodecs.list()), VoteMultiplierLists::bureaucrat3VotePlayers,
            UUIDUtil.STREAM_CODEC.apply(ByteBufCodecs.list()), VoteMultiplierLists::thiefNegativeVotePlayers,
            VoteMultiplierLists::new
    );

    public static VoteMultiplierLists empty() {
        return new VoteMultiplierLists(List.of(), List.of(), List.of());
    }
}
