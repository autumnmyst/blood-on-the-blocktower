package com.autumnwind.botb.networking;

import com.autumnwind.botb.BloodOnTheBlocktower;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * What the map-setup wizard's on-screen box shows: the step being set, its current value
 * (empty when unset), a short description (empty when the title says it all), and the label
 * of the Shift + MB2 control ("Homes" during the seat loop, "Finish" otherwise). Sent on every
 * step change; {@code active} false hides the box.
 */
public record SetupHudS2CPayload(boolean active, Component title, String current, Component description,
                                 Component finishLabel) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SetupHudS2CPayload> ID =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(BloodOnTheBlocktower.MOD_ID, "setup_hud"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SetupHudS2CPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, SetupHudS2CPayload::active,
            ComponentSerialization.STREAM_CODEC, SetupHudS2CPayload::title,
            ByteBufCodecs.STRING_UTF8, SetupHudS2CPayload::current,
            ComponentSerialization.STREAM_CODEC, SetupHudS2CPayload::description,
            ComponentSerialization.STREAM_CODEC, SetupHudS2CPayload::finishLabel,
            SetupHudS2CPayload::new
    );

    public static SetupHudS2CPayload hidden() {
        return new SetupHudS2CPayload(false, Component.empty(), "", Component.empty(), Component.empty());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
