package com.autumnwind.botb.networking;

import com.autumnwind.botb.BloodOnTheBlocktower;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SyncDayNightS2CPayload(int night, int day, boolean executionToday) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SyncDayNightS2CPayload> ID =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(BloodOnTheBlocktower.MOD_ID, "sync_day_night"));

    public static final StreamCodec<ByteBuf, SyncDayNightS2CPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SyncDayNightS2CPayload::night,
            ByteBufCodecs.VAR_INT, SyncDayNightS2CPayload::day,
            ByteBufCodecs.BOOL, SyncDayNightS2CPayload::executionToday,
            SyncDayNightS2CPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
