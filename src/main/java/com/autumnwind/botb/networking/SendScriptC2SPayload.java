package com.autumnwind.botb.networking;

import com.autumnwind.botb.BloodOnTheBlocktower;
import com.autumnwind.botb.util.Script;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Storyteller sends only the script, leaving roles, seats, and reminders untouched.
 * Server caches it and broadcasts SendScriptS2CPayload to everyone online.
 */
public record SendScriptC2SPayload(Script script) implements CustomPayload {
    public static final Identifier SEND_SCRIPT_ONLY_ID = Identifier.of(BloodOnTheBlocktower.MOD_ID, "send_script_only");
    public static final CustomPayload.Id<SendScriptC2SPayload> ID = new CustomPayload.Id<>(SEND_SCRIPT_ONLY_ID);

    public static final PacketCodec<RegistryByteBuf, SendScriptC2SPayload> CODEC = PacketCodec.tuple(
            Script.PACKET_CODEC, SendScriptC2SPayload::script,
            SendScriptC2SPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
