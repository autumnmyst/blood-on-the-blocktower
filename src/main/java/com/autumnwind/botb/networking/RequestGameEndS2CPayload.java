package com.autumnwind.botb.networking;

import com.autumnwind.botb.BloodOnTheBlocktower;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * S2C payload requesting the storyteller to send grimoire data for game end.
 */
public record RequestGameEndS2CPayload(boolean goodWins) implements CustomPacketPayload {
    public static final Identifier REQUEST_GAME_END_ID = Identifier.fromNamespaceAndPath(BloodOnTheBlocktower.MOD_ID, "request_game_end");
    public static final CustomPacketPayload.Type<RequestGameEndS2CPayload> ID = new CustomPacketPayload.Type<>(REQUEST_GAME_END_ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, RequestGameEndS2CPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, RequestGameEndS2CPayload::goodWins,
            RequestGameEndS2CPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
