package com.autumnwind.botb.networking;

import com.autumnwind.botb.BloodOnTheBlocktower;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record ClearGrimoireS2CPayload() implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ClearGrimoireS2CPayload> ID =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(BloodOnTheBlocktower.MOD_ID, "clear_grimoire"));

    public static final StreamCodec<ByteBuf, ClearGrimoireS2CPayload> CODEC = StreamCodec.ofMember(
            (value, buf) -> {}, // No data to encode
            buf -> new ClearGrimoireS2CPayload() // No data to decode
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
