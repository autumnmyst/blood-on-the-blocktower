package com.autumnwind.botb.networking;

import com.autumnwind.botb.BloodOnTheBlocktower;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Client-to-server request: please send me the current script, if any. Sent by clients on
 * JOIN so a (re)connecting player who missed the storyteller's last role-send can recover
 * it. Server replies with SendScriptS2CPayload, or nothing if no script is set yet.
 */
public record RequestScriptC2SPayload() implements CustomPacketPayload {
    public static final Identifier REQUEST_SCRIPT_ID = Identifier.fromNamespaceAndPath(BloodOnTheBlocktower.MOD_ID, "request_script");
    public static final CustomPacketPayload.Type<RequestScriptC2SPayload> ID = new CustomPacketPayload.Type<>(REQUEST_SCRIPT_ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, RequestScriptC2SPayload> CODEC =
            StreamCodec.unit(new RequestScriptC2SPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
