package com.autumnwind.botb.networking;

import com.autumnwind.botb.BloodOnTheBlocktower;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** The server's mod version, sent on join so the client can warn when its own differs. */
public record ModVersionS2CPayload(String version) implements CustomPayload {
    public static final CustomPayload.Id<ModVersionS2CPayload> ID =
            new CustomPayload.Id<>(Identifier.of(BloodOnTheBlocktower.MOD_ID, "mod_version"));

    public static final PacketCodec<ByteBuf, ModVersionS2CPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.STRING, ModVersionS2CPayload::version,
            ModVersionS2CPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
