package com.autumnwind.botb.networking;

import com.autumnwind.botb.BloodOnTheBlocktower;
import com.autumnwind.botb.util.Script;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SendScriptS2CPayload(Script script) implements CustomPacketPayload {
    public static final ResourceLocation SEND_SCRIPT_ID = ResourceLocation.fromNamespaceAndPath(BloodOnTheBlocktower.MOD_ID, "send_script");
    public static final CustomPacketPayload.Type<SendScriptS2CPayload> ID = new CustomPacketPayload.Type<>(SEND_SCRIPT_ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, SendScriptS2CPayload> CODEC = StreamCodec.composite(
            Script.PACKET_CODEC, SendScriptS2CPayload::script,
            SendScriptS2CPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}