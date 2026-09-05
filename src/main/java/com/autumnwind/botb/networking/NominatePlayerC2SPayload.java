package com.autumnwind.botb.networking;

import com.autumnwind.botb.BloodOnTheBlocktower;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;

import java.util.Optional;
import java.util.UUID;

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
) implements CustomPayload {
    public static final CustomPayload.Id<NominatePlayerC2SPayload> ID =
            new CustomPayload.Id<>(Identifier.of(BloodOnTheBlocktower.MOD_ID, "nominate_player"));

    public static final PacketCodec<RegistryByteBuf, NominatePlayerC2SPayload> CODEC = PacketCodec.tuple(
            Uuids.PACKET_CODEC, NominatePlayerC2SPayload::nominator,
            Uuids.PACKET_CODEC, NominatePlayerC2SPayload::nominee,
            PacketCodecs.BOOL, NominatePlayerC2SPayload::override,
            PacketCodecs.BOOL, NominatePlayerC2SPayload::voudonModeActive,
            PacketCodecs.optional(Uuids.PACKET_CODEC), NominatePlayerC2SPayload::voudonPlayerUuid,
            NominatePlayerC2SPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
