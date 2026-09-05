package com.autumnwind.botb.networking;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.util.Uuids;

import java.util.List;
import java.util.UUID;

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

    public static final PacketCodec<RegistryByteBuf, VoteMultiplierLists> CODEC = PacketCodec.tuple(
            Uuids.PACKET_CODEC.collect(PacketCodecs.toList()), VoteMultiplierLists::ugHatPlayers,
            Uuids.PACKET_CODEC.collect(PacketCodecs.toList()), VoteMultiplierLists::bureaucrat3VotePlayers,
            Uuids.PACKET_CODEC.collect(PacketCodecs.toList()), VoteMultiplierLists::thiefNegativeVotePlayers,
            VoteMultiplierLists::new
    );

    public static VoteMultiplierLists empty() {
        return new VoteMultiplierLists(List.of(), List.of(), List.of());
    }
}
