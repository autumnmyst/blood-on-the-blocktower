package com.autumnwind.botb.networking;

import com.autumnwind.botb.BloodOnTheBlocktower;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record CallBackC2SPayload() implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<CallBackC2SPayload> ID =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(BloodOnTheBlocktower.MOD_ID, "call_back"));

    public static final StreamCodec<ByteBuf, CallBackC2SPayload> CODEC = StreamCodec.unit(new CallBackC2SPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
