package com.autumnwind.botb.networking;

import com.autumnwind.botb.BloodOnTheBlocktower;
import com.autumnwind.botb.util.Script;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record SendScriptS2CPayload(Script script) implements CustomPayload {
    public static final Identifier SEND_SCRIPT_ID = Identifier.of(BloodOnTheBlocktower.MOD_ID, "send_script");
    public static final CustomPayload.Id<SendScriptS2CPayload> ID = new CustomPayload.Id<>(SEND_SCRIPT_ID);

    public static final PacketCodec<RegistryByteBuf, SendScriptS2CPayload> CODEC = PacketCodec.tuple(
            Script.PACKET_CODEC, SendScriptS2CPayload::script,
            SendScriptS2CPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}