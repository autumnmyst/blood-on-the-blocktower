package com.autumnwind.botb.networking;

import com.autumnwind.botb.BloodOnTheBlocktower;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * S2C payload requesting the storyteller to send grimoire data for game end.
 */
public record RequestGameEndS2CPayload(boolean goodWins) implements CustomPayload {
    public static final Identifier REQUEST_GAME_END_ID = Identifier.of(BloodOnTheBlocktower.MOD_ID, "request_game_end");
    public static final CustomPayload.Id<RequestGameEndS2CPayload> ID = new CustomPayload.Id<>(REQUEST_GAME_END_ID);

    public static final PacketCodec<RegistryByteBuf, RequestGameEndS2CPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.BOOL, RequestGameEndS2CPayload::goodWins,
            RequestGameEndS2CPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
