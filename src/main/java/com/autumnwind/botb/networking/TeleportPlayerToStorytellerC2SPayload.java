package com.autumnwind.botb.networking;

import com.autumnwind.botb.BloodOnTheBlocktower;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;

import java.util.UUID;

public record TeleportPlayerToStorytellerC2SPayload(UUID playerUuid) implements CustomPayload {
    public static final CustomPayload.Id<TeleportPlayerToStorytellerC2SPayload> ID =
            new CustomPayload.Id<>(Identifier.of(BloodOnTheBlocktower.MOD_ID, "teleport_player_to_storyteller"));

    public static final PacketCodec<RegistryByteBuf, TeleportPlayerToStorytellerC2SPayload> CODEC = PacketCodec.tuple(
            Uuids.PACKET_CODEC, TeleportPlayerToStorytellerC2SPayload::playerUuid,
            TeleportPlayerToStorytellerC2SPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
