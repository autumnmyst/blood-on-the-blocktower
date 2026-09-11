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
import net.minecraft.resources.Identifier;

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
) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<OpenNominationsC2SPayload> ID =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(BloodOnTheBlocktower.MOD_ID, "open_nominations"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenNominationsC2SPayload> CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC.apply(ByteBufCodecs.list()), OpenNominationsC2SPayload::bansheeHasAbilityPlayers,
            ByteBufCodecs.BOOL, OpenNominationsC2SPayload::voudonModeActive,
            ByteBufCodecs.optional(UUIDUtil.STREAM_CODEC), OpenNominationsC2SPayload::voudonPlayerUuid,
            UUIDUtil.STREAM_CODEC.apply(ByteBufCodecs.list()), OpenNominationsC2SPayload::mayNotNominatePlayers,
            OpenNominationsC2SPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
