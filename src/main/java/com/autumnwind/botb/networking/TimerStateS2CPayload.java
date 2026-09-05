package com.autumnwind.botb.networking;

import com.autumnwind.botb.BloodOnTheBlocktower;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Server-to-Client payload for syncing timer state
 */
public record TimerStateS2CPayload(
        boolean isActive,
        boolean isPaused,
        int remainingSeconds,
        int totalSeconds
) implements CustomPayload {
    public static final CustomPayload.Id<TimerStateS2CPayload> ID =
            new CustomPayload.Id<>(Identifier.of(BloodOnTheBlocktower.MOD_ID, "timer_state"));

    public static final PacketCodec<RegistryByteBuf, TimerStateS2CPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.BOOL, TimerStateS2CPayload::isActive,
            PacketCodecs.BOOL, TimerStateS2CPayload::isPaused,
            PacketCodecs.VAR_INT, TimerStateS2CPayload::remainingSeconds,
            PacketCodecs.VAR_INT, TimerStateS2CPayload::totalSeconds,
            TimerStateS2CPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
