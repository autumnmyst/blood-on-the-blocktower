package com.autumnwind.botb.networking;

import com.autumnwind.botb.BloodOnTheBlocktower;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** Connected non-operator and operator (level 2+) counts, shown in the pre-game grimoire. */
public record LobbyCountsS2CPayload(int players, int storytellers) implements CustomPayload {
    public static final CustomPayload.Id<LobbyCountsS2CPayload> ID =
            new CustomPayload.Id<>(Identifier.of(BloodOnTheBlocktower.MOD_ID, "lobby_counts"));

    public static final PacketCodec<ByteBuf, LobbyCountsS2CPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.VAR_INT, LobbyCountsS2CPayload::players,
            PacketCodecs.VAR_INT, LobbyCountsS2CPayload::storytellers,
            LobbyCountsS2CPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
