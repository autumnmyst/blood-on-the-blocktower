package com.autumnwind.botb.networking;

import com.autumnwind.botb.BloodOnTheBlocktower;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Connected non-operator and operator (level 2+) counts, shown in the pre-game grimoire. */
public record LobbyCountsS2CPayload(int players, int storytellers) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<LobbyCountsS2CPayload> ID =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(BloodOnTheBlocktower.MOD_ID, "lobby_counts"));

    public static final StreamCodec<ByteBuf, LobbyCountsS2CPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, LobbyCountsS2CPayload::players,
            ByteBufCodecs.VAR_INT, LobbyCountsS2CPayload::storytellers,
            LobbyCountsS2CPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
