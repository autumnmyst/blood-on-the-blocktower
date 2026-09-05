package com.autumnwind.botb.networking;

import com.autumnwind.botb.BloodOnTheBlocktower;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Client-to-server request: please send me the current script, if any. Sent by clients on
 * JOIN so a (re)connecting player who missed the storyteller's last role-send can recover
 * it. Server replies with SendScriptS2CPayload, or nothing if no script is set yet.
 */
public record RequestScriptC2SPayload() implements CustomPayload {
    public static final Identifier REQUEST_SCRIPT_ID = Identifier.of(BloodOnTheBlocktower.MOD_ID, "request_script");
    public static final CustomPayload.Id<RequestScriptC2SPayload> ID = new CustomPayload.Id<>(REQUEST_SCRIPT_ID);

    public static final PacketCodec<RegistryByteBuf, RequestScriptC2SPayload> CODEC =
            PacketCodec.unit(new RequestScriptC2SPayload());

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
