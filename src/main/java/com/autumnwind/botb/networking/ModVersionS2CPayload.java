package com.autumnwind.botb.networking;

import com.autumnwind.botb.BloodOnTheBlocktower;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** The server's mod version, sent on join so the client can warn when its own differs. */
public record ModVersionS2CPayload(String version) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ModVersionS2CPayload> ID =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(BloodOnTheBlocktower.MOD_ID, "mod_version"));

    public static final StreamCodec<ByteBuf, ModVersionS2CPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, ModVersionS2CPayload::version,
            ModVersionS2CPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
