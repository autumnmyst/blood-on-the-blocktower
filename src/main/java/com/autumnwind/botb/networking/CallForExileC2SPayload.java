package com.autumnwind.botb.networking;

import com.autumnwind.botb.BloodOnTheBlocktower;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Client-to-Server payload for calling for exile of a traveler.
 */
public record CallForExileC2SPayload(UUID caller, UUID traveler, boolean override) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<CallForExileC2SPayload> ID =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(BloodOnTheBlocktower.MOD_ID, "call_for_exile"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CallForExileC2SPayload> CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, CallForExileC2SPayload::caller,
            UUIDUtil.STREAM_CODEC, CallForExileC2SPayload::traveler,
            ByteBufCodecs.BOOL, CallForExileC2SPayload::override,
            CallForExileC2SPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
