package com.autumnwind.botb.networking;

import com.autumnwind.botb.BloodOnTheBlocktower;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.Map;
import java.util.UUID;

/**
 * S2C payload to broadcast death status to all players.
 */
public record SendDeathStatusS2CPayload(Map<UUID, Boolean> deadPlayers) implements CustomPayload {
    public static final Identifier SEND_DEATH_STATUS_ID = Identifier.of(BloodOnTheBlocktower.MOD_ID, "send_death_status");
    public static final CustomPayload.Id<SendDeathStatusS2CPayload> ID = new CustomPayload.Id<>(SEND_DEATH_STATUS_ID);

    public static final PacketCodec<RegistryByteBuf, SendDeathStatusS2CPayload> CODEC = PacketCodec.tuple(
            PayloadCodecs.DEATH_MAP_CODEC, SendDeathStatusS2CPayload::deadPlayers,
            SendDeathStatusS2CPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
