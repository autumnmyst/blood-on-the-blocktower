package com.autumnwind.botb.networking;

import com.autumnwind.botb.BloodOnTheBlocktower;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Client-to-Server payload for nominating a player.
 * @param nominator UUID of the player making the nomination
 * @param nominee UUID of the player being nominated
 * @param override If true, skip eligibility checks
 * @param voudonModeActive Whether Voudon is alive with ability (reversed voting eligibility)
 * @param voudonPlayerUuid The UUID of the Voudon player, if Voudon mode is active
 */
public record NominatePlayerC2SPayload(
        UUID nominator,
        UUID nominee,
        boolean override,
        boolean voudonModeActive,
        Optional<UUID> voudonPlayerUuid
) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<NominatePlayerC2SPayload> ID =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(BloodOnTheBlocktower.MOD_ID, "nominate_player"));

    public static final StreamCodec<RegistryFriendlyByteBuf, NominatePlayerC2SPayload> CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, NominatePlayerC2SPayload::nominator,
            UUIDUtil.STREAM_CODEC, NominatePlayerC2SPayload::nominee,
            ByteBufCodecs.BOOL, NominatePlayerC2SPayload::override,
            ByteBufCodecs.BOOL, NominatePlayerC2SPayload::voudonModeActive,
            ByteBufCodecs.optional(UUIDUtil.STREAM_CODEC), NominatePlayerC2SPayload::voudonPlayerUuid,
            NominatePlayerC2SPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
