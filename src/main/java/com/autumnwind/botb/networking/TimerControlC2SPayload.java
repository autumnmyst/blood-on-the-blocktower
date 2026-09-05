package com.autumnwind.botb.networking;

import com.autumnwind.botb.BloodOnTheBlocktower;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Client-to-Server payload for timer control actions (start, pause, resume, stop)
 */
public record TimerControlC2SPayload(Action action, int durationSeconds, boolean syncDaylight) implements CustomPayload {
    public static final CustomPayload.Id<TimerControlC2SPayload> ID =
            new CustomPayload.Id<>(Identifier.of(BloodOnTheBlocktower.MOD_ID, "timer_control"));

    public static final PacketCodec<RegistryByteBuf, TimerControlC2SPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.STRING.xmap(Action::valueOf, Action::name), TimerControlC2SPayload::action,
            PacketCodecs.VAR_INT, TimerControlC2SPayload::durationSeconds,
            PacketCodecs.BOOL, TimerControlC2SPayload::syncDaylight,
            TimerControlC2SPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    public enum Action {
        START,   // Start new timer with duration
        PAUSE,   // Pause current timer
        RESUME,  // Resume paused timer
        STOP     // Stop and clear timer
    }
}
