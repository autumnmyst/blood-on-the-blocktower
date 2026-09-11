package com.autumnwind.botb.config;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

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

        public String translationKey() {
            return switch (this) {
                case OFF -> "gui.blood-on-the-blocktower.whisper_settings.off";
                case RUNES -> "gui.blood-on-the-blocktower.whisper_settings.on";
            };
        }
    }

    public static final WhisperSettings DEFAULT = new WhisperSettings(
            true,             // allowWhispering
            false,            // broadcast
            VisualMode.RUNES, // visual
            true,             // audio
            7.0,              // range (0 = unlimited)
            true              // vcEnforced
    );

    /** True when {@link #range} is the sentinel for unlimited. */
    public boolean rangeUnlimited() {
        return range <= 0.0;
    }

    public static final StreamCodec<ByteBuf, WhisperSettings> CODEC = new StreamCodec<>() {
        @Override
        public WhisperSettings decode(ByteBuf buf) {
            boolean allow = ByteBufCodecs.BOOL.decode(buf);
            boolean broadcast = ByteBufCodecs.BOOL.decode(buf);
            int visualOrd = ByteBufCodecs.VAR_INT.decode(buf);
            boolean audio = ByteBufCodecs.BOOL.decode(buf);
            double range = ByteBufCodecs.DOUBLE.decode(buf);
            boolean vc = ByteBufCodecs.BOOL.decode(buf);
            VisualMode v = VisualMode.values()[Math.floorMod(visualOrd, VisualMode.values().length)];
            return new WhisperSettings(allow, broadcast, v, audio, range, vc);
        }

        @Override
        public void encode(ByteBuf buf, WhisperSettings v) {
            ByteBufCodecs.BOOL.encode(buf, v.allowWhispering);
            ByteBufCodecs.BOOL.encode(buf, v.broadcast);
            ByteBufCodecs.VAR_INT.encode(buf, v.visual.ordinal());
            ByteBufCodecs.BOOL.encode(buf, v.audio);
            ByteBufCodecs.DOUBLE.encode(buf, v.range);
            ByteBufCodecs.BOOL.encode(buf, v.vcEnforced);
        }
    };
}
