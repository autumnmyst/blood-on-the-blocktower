package com.autumnwind.botb.networking;

import com.autumnwind.botb.BloodOnTheBlocktower;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/**
 * Server-to-Client payload for updating clock hands state during nominations, voting, and exile.
 *
 * Mode values:
 * - 0 = HIDDEN - No hands visible
 * - 1 = NOMINATION - Both hands visible, pointing at nominator (hour) and nominee (minute)
 * - 2 = VOTING - Only minute hand visible, ticking around to each voter
 * - 3 = EXILE - Only exile minute hand visible (different texture), pointing at exile target
 */
public record ClockHandsStateS2CPayload(
        int mode,                    // 0=HIDDEN, 1=NOMINATION, 2=VOTING, 3=EXILE
        BlockPos clockCenter,        // Center position for clock hands (nullable if not set)
        float scale,                 // Scale multiplier for clock hands
        Vec3 hourHandTargetPos,     // Target position for hour hand (nullable when hidden or voting)
        Vec3 minuteHandTargetPos,   // Target position for minute hand (nullable when hidden)
        boolean fadeIn,              // True if hands should fade in (new nomination/vote start)
        boolean swivel               // True if hands should do swivel animation (nomination only)
) implements CustomPacketPayload {

    public static final int MODE_HIDDEN = 0;
    public static final int MODE_NOMINATION = 1;
    public static final int MODE_VOTING = 2;
    public static final int MODE_EXILE = 3;

    public static final CustomPacketPayload.Type<ClockHandsStateS2CPayload> ID =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(BloodOnTheBlocktower.MOD_ID, "clock_hands_state"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ClockHandsStateS2CPayload> CODEC = StreamCodec.ofMember(
            ClockHandsStateS2CPayload::write,
            ClockHandsStateS2CPayload::read
    );

    private static void write(ClockHandsStateS2CPayload payload, RegistryFriendlyByteBuf buf) {
        buf.writeInt(payload.mode);

        // Write clock center (nullable)
        boolean hasClockCenter = payload.clockCenter != null;
        buf.writeBoolean(hasClockCenter);
        if (hasClockCenter) {
            buf.writeLong(payload.clockCenter.asLong());
        }

        buf.writeFloat(payload.scale);

        // Write hour hand target position (nullable)
        boolean hasHourHand = payload.hourHandTargetPos != null;
        buf.writeBoolean(hasHourHand);
        if (hasHourHand) {
            buf.writeDouble(payload.hourHandTargetPos.x);
            buf.writeDouble(payload.hourHandTargetPos.y);
            buf.writeDouble(payload.hourHandTargetPos.z);
        }

        // Write minute hand target position (nullable)
        boolean hasMinuteHand = payload.minuteHandTargetPos != null;
        buf.writeBoolean(hasMinuteHand);
        if (hasMinuteHand) {
            buf.writeDouble(payload.minuteHandTargetPos.x);
            buf.writeDouble(payload.minuteHandTargetPos.y);
            buf.writeDouble(payload.minuteHandTargetPos.z);
        }

        buf.writeBoolean(payload.fadeIn);
        buf.writeBoolean(payload.swivel);
    }

    private static ClockHandsStateS2CPayload read(RegistryFriendlyByteBuf buf) {
        int mode = buf.readInt();

        // Read clock center (nullable)
        BlockPos clockCenter = null;
        if (buf.readBoolean()) {
            clockCenter = BlockPos.of(buf.readLong());
        }

        float scale = buf.readFloat();

        // Read hour hand target position (nullable)
        Vec3 hourHandTargetPos = null;
        if (buf.readBoolean()) {
            hourHandTargetPos = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        }

        // Read minute hand target position (nullable)
        Vec3 minuteHandTargetPos = null;
        if (buf.readBoolean()) {
            minuteHandTargetPos = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        }

        boolean fadeIn = buf.readBoolean();
        boolean swivel = buf.readBoolean();

        return new ClockHandsStateS2CPayload(mode, clockCenter, scale, hourHandTargetPos, minuteHandTargetPos, fadeIn, swivel);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
