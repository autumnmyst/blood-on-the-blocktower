package com.autumnwind.botb.networking;

import com.autumnwind.botb.BloodOnTheBlocktower;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Server-to-Client payload for syncing timer state
 */
public record TimerStateS2CPayload(
        boolean isActive,
        boolean isPaused,
        int remainingSeconds,
        int totalSeconds
) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<TimerStateS2CPayload> ID =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(BloodOnTheBlocktower.MOD_ID, "timer_state"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TimerStateS2CPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, TimerStateS2CPayload::isActive,
            ByteBufCodecs.BOOL, TimerStateS2CPayload::isPaused,
            ByteBufCodecs.VAR_INT, TimerStateS2CPayload::remainingSeconds,
            ByteBufCodecs.VAR_INT, TimerStateS2CPayload::totalSeconds,
            TimerStateS2CPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
