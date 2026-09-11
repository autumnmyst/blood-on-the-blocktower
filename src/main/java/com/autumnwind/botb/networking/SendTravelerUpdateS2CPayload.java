package com.autumnwind.botb.networking;

import com.autumnwind.botb.BloodOnTheBlocktower;
import com.autumnwind.botb.util.PendingRoleAssignment;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Server-to-Client payload for broadcasting traveler role updates to all players.
 * When a traveler is assigned, ALL players should see that player's traveler role
 * in their grimoire with "default" (unknown) alignment.
 * When someone is no longer a traveler, set them to "no role" in grimoires.
 */
public record SendTravelerUpdateS2CPayload(
        UUID travelerUuid,
        PendingRoleAssignment assignment
) implements CustomPacketPayload {

    public static final ResourceLocation ID_LOCATION = ResourceLocation.fromNamespaceAndPath(BloodOnTheBlocktower.MOD_ID, "send_traveler_update");
    public static final CustomPacketPayload.Type<SendTravelerUpdateS2CPayload> ID = new CustomPacketPayload.Type<>(ID_LOCATION);

    public static final StreamCodec<RegistryFriendlyByteBuf, SendTravelerUpdateS2CPayload> CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, SendTravelerUpdateS2CPayload::travelerUuid,
            PendingRoleAssignment.PACKET_CODEC, SendTravelerUpdateS2CPayload::assignment,
            SendTravelerUpdateS2CPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
