package com.autumnwind.botb.networking;

import com.autumnwind.botb.BloodOnTheBlocktower;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * S2C payload to sync storytellers' night visit index to Dawn or Dusk.
 * Sent to all OTHER operators when one activates Dawn or Dusk, to keep them in sync.
 */
public record SyncNightVisitS2CPayload(String visitType) implements CustomPayload {
    public static final CustomPayload.Id<SyncNightVisitS2CPayload> ID =
            new CustomPayload.Id<>(Identifier.of(BloodOnTheBlocktower.MOD_ID, "sync_night_visit"));

    public static final PacketCodec<ByteBuf, SyncNightVisitS2CPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.STRING, SyncNightVisitS2CPayload::visitType,
            SyncNightVisitS2CPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    public static final String DUSK = "dusk";
    public static final String DAWN = "dawn";
}
