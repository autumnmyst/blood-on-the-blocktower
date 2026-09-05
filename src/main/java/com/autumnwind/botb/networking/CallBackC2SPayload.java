package com.autumnwind.botb.networking;

import com.autumnwind.botb.BloodOnTheBlocktower;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record CallBackC2SPayload() implements CustomPayload {
    public static final CustomPayload.Id<CallBackC2SPayload> ID =
            new CustomPayload.Id<>(Identifier.of(BloodOnTheBlocktower.MOD_ID, "call_back"));

    public static final PacketCodec<ByteBuf, CallBackC2SPayload> CODEC = PacketCodec.unit(new CallBackC2SPayload());

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
