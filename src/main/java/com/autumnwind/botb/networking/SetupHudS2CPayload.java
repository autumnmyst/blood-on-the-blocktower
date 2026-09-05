package com.autumnwind.botb.networking;

import com.autumnwind.botb.BloodOnTheBlocktower;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * What the map-setup wizard's on-screen box shows: the step being set, its current value
 * (empty when unset), a short description (empty when the title says it all), and the label
 * of the Shift + MB2 control ("Homes" during the seat loop, "Finish" otherwise). Sent on every
 * step change; {@code active} false hides the box.
 */
public record SetupHudS2CPayload(boolean active, String title, String current, String description,
                                 String finishLabel) implements CustomPayload {
    public static final CustomPayload.Id<SetupHudS2CPayload> ID =
            new CustomPayload.Id<>(Identifier.of(BloodOnTheBlocktower.MOD_ID, "setup_hud"));

    public static final PacketCodec<RegistryByteBuf, SetupHudS2CPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.BOOL, SetupHudS2CPayload::active,
            PacketCodecs.STRING, SetupHudS2CPayload::title,
            PacketCodecs.STRING, SetupHudS2CPayload::current,
            PacketCodecs.STRING, SetupHudS2CPayload::description,
            PacketCodecs.STRING, SetupHudS2CPayload::finishLabel,
            SetupHudS2CPayload::new
    );

    public static SetupHudS2CPayload hidden() {
        return new SetupHudS2CPayload(false, "", "", "", "");
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
