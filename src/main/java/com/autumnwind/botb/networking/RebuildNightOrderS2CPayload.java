package com.autumnwind.botb.networking;

import com.autumnwind.botb.BloodOnTheBlocktower;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record RebuildNightOrderS2CPayload() implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<RebuildNightOrderS2CPayload> ID =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(BloodOnTheBlocktower.MOD_ID, "rebuild_night_order"));

    public static final StreamCodec<ByteBuf, RebuildNightOrderS2CPayload> CODEC = StreamCodec.ofMember(
            (value, buf) -> {}, // No data to encode
            buf -> new RebuildNightOrderS2CPayload() // No data to decode
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
