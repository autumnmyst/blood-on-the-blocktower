package com.autumnwind.botb.networking;

import com.autumnwind.botb.BloodOnTheBlocktower;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * S2C payload to sync storytellers' night visit index to Dawn or Dusk.
 * Sent to all OTHER operators when one activates Dawn or Dusk, to keep them in sync.
 */
public record SyncNightVisitS2CPayload(String visitType) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SyncNightVisitS2CPayload> ID =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(BloodOnTheBlocktower.MOD_ID, "sync_night_visit"));

    public static final StreamCodec<ByteBuf, SyncNightVisitS2CPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, SyncNightVisitS2CPayload::visitType,
            SyncNightVisitS2CPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    public static final String DUSK = "dusk";
    public static final String DAWN = "dawn";
}
