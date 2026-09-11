package com.autumnwind.botb.networking;

import com.autumnwind.botb.BloodOnTheBlocktower;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Client-to-Server payload for timer control actions (start, pause, resume, stop)
 */
public record TimerControlC2SPayload(Action action, int durationSeconds, boolean syncDaylight) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<TimerControlC2SPayload> ID =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(BloodOnTheBlocktower.MOD_ID, "timer_control"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TimerControlC2SPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8.map(Action::valueOf, Action::name), TimerControlC2SPayload::action,
            ByteBufCodecs.VAR_INT, TimerControlC2SPayload::durationSeconds,
            ByteBufCodecs.BOOL, TimerControlC2SPayload::syncDaylight,
            TimerControlC2SPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    public enum Action {
        START,   // Start new timer with duration
        PAUSE,   // Pause current timer
        RESUME,  // Resume paused timer
        STOP     // Stop and clear timer
    }
}
