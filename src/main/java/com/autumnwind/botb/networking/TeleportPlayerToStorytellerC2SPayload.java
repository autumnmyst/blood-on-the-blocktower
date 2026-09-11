package com.autumnwind.botb.networking;

import com.autumnwind.botb.BloodOnTheBlocktower;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record TeleportPlayerToStorytellerC2SPayload(UUID playerUuid) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<TeleportPlayerToStorytellerC2SPayload> ID =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(BloodOnTheBlocktower.MOD_ID, "teleport_player_to_storyteller"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TeleportPlayerToStorytellerC2SPayload> CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, TeleportPlayerToStorytellerC2SPayload::playerUuid,
            TeleportPlayerToStorytellerC2SPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
