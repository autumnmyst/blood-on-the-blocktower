package com.autumnwind.botb.networking;

import com.autumnwind.botb.BloodOnTheBlocktower;
import com.autumnwind.botb.util.PendingRoleAssignment;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;

import java.util.UUID;

/**
 * Server-to-Client payload for broadcasting traveler role updates to all players.
 * When a traveler is assigned, ALL players should see that player's traveler role
 * in their grimoire with "default" (unknown) alignment.
 * When someone is no longer a traveler, set them to "no role" in grimoires.
 */
public record SendTravelerUpdateS2CPayload(
        UUID travelerUuid,
        PendingRoleAssignment assignment
) implements CustomPayload {

    public static final Identifier ID_LOCATION = Identifier.of(BloodOnTheBlocktower.MOD_ID, "send_traveler_update");
    public static final CustomPayload.Id<SendTravelerUpdateS2CPayload> ID = new CustomPayload.Id<>(ID_LOCATION);

    public static final PacketCodec<RegistryByteBuf, SendTravelerUpdateS2CPayload> CODEC = PacketCodec.tuple(
            Uuids.PACKET_CODEC, SendTravelerUpdateS2CPayload::travelerUuid,
            PendingRoleAssignment.PACKET_CODEC, SendTravelerUpdateS2CPayload::assignment,
            SendTravelerUpdateS2CPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
