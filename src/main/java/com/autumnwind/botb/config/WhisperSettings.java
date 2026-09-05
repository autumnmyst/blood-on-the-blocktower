package com.autumnwind.botb.config;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;

/**
 * Server-authoritative whisper rules. Sent from server to all clients on join and on
 * storyteller-driven changes. Restrictions only apply to non-operator → non-operator
 * whispers. Whispers involving an operator (storyteller↔player or op↔op) bypass all
 * gating.
 *
 * <p>{@code range <= 0} means unlimited. Using a sentinel rather than {@code Optional}
 * keeps the JSON/network codecs trivial.
 */
public record WhisperSettings(
        boolean allowWhispering,
        boolean broadcast,
        VisualMode visual,
        boolean audio,
        double range,
        boolean vcEnforced
) {
    public enum VisualMode {
        OFF,
        RUNES;

        public VisualMode cycle() {
            return values()[(ordinal() + 1) % values().length];
        }

        public String displayName() {
            return switch (this) {
                case OFF -> "OFF";
                case RUNES -> "ON";
            };
        }
    }

    public static final WhisperSettings DEFAULT = new WhisperSettings(
            true,             // allowWhispering
            true,             // broadcast
            VisualMode.OFF,   // visual
            false,            // audio
            0.0,              // range (0 = unlimited)
            true              // vcEnforced
    );

    /** True when {@link #range} is the sentinel for unlimited. */
    public boolean rangeUnlimited() {
        return range <= 0.0;
    }

    public static final PacketCodec<ByteBuf, WhisperSettings> CODEC = new PacketCodec<>() {
        @Override
        public WhisperSettings decode(ByteBuf buf) {
            boolean allow = PacketCodecs.BOOL.decode(buf);
            boolean broadcast = PacketCodecs.BOOL.decode(buf);
            int visualOrd = PacketCodecs.VAR_INT.decode(buf);
            boolean audio = PacketCodecs.BOOL.decode(buf);
            double range = PacketCodecs.DOUBLE.decode(buf);
            boolean vc = PacketCodecs.BOOL.decode(buf);
            VisualMode v = VisualMode.values()[Math.floorMod(visualOrd, VisualMode.values().length)];
            return new WhisperSettings(allow, broadcast, v, audio, range, vc);
        }

        @Override
        public void encode(ByteBuf buf, WhisperSettings v) {
            PacketCodecs.BOOL.encode(buf, v.allowWhispering);
            PacketCodecs.BOOL.encode(buf, v.broadcast);
            PacketCodecs.VAR_INT.encode(buf, v.visual.ordinal());
            PacketCodecs.BOOL.encode(buf, v.audio);
            PacketCodecs.DOUBLE.encode(buf, v.range);
            PacketCodecs.BOOL.encode(buf, v.vcEnforced);
        }
    };
}
