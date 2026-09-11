package com.autumnwind.botb.networking;

import com.autumnwind.botb.BloodOnTheBlocktower;
import com.autumnwind.botb.util.Script;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Storyteller sends only the script, leaving roles, seats, and reminders untouched.
 * Server caches it and broadcasts SendScriptS2CPayload to everyone online.
 */
public record SendScriptC2SPayload(Script script) implements CustomPacketPayload {
    public static final ResourceLocation SEND_SCRIPT_ONLY_ID = ResourceLocation.fromNamespaceAndPath(BloodOnTheBlocktower.MOD_ID, "send_script_only");
    public static final CustomPacketPayload.Type<SendScriptC2SPayload> ID = new CustomPacketPayload.Type<>(SEND_SCRIPT_ONLY_ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, SendScriptC2SPayload> CODEC = StreamCodec.composite(
            Script.PACKET_CODEC, SendScriptC2SPayload::script,
            SendScriptC2SPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
