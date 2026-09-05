package com.autumnwind.botb.networking;

import com.autumnwind.botb.BloodOnTheBlocktower;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record SyncDayNightS2CPayload(int night, int day, boolean executionToday) implements CustomPayload {
    public static final CustomPayload.Id<SyncDayNightS2CPayload> ID =
            new CustomPayload.Id<>(Identifier.of(BloodOnTheBlocktower.MOD_ID, "sync_day_night"));

    public static final PacketCodec<ByteBuf, SyncDayNightS2CPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.VAR_INT, SyncDayNightS2CPayload::night,
            PacketCodecs.VAR_INT, SyncDayNightS2CPayload::day,
            PacketCodecs.BOOL, SyncDayNightS2CPayload::executionToday,
            SyncDayNightS2CPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
