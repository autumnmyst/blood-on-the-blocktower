package com.autumnwind.botb.networking;

import com.autumnwind.botb.BloodOnTheBlocktower;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record ClearGrimoireS2CPayload() implements CustomPayload {
    public static final CustomPayload.Id<ClearGrimoireS2CPayload> ID =
            new CustomPayload.Id<>(Identifier.of(BloodOnTheBlocktower.MOD_ID, "clear_grimoire"));

    public static final PacketCodec<ByteBuf, ClearGrimoireS2CPayload> CODEC = PacketCodec.of(
            (value, buf) -> {}, // No data to encode
            buf -> new ClearGrimoireS2CPayload() // No data to decode
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
