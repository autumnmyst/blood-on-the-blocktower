package com.autumnwind.botb.networking;

import com.autumnwind.botb.BloodOnTheBlocktower;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * S2C payload to broadcast death status to all players.
 */
public record SendDeathStatusS2CPayload(Map<UUID, Boolean> deadPlayers) implements CustomPacketPayload {
    public static final Identifier SEND_DEATH_STATUS_ID = Identifier.fromNamespaceAndPath(BloodOnTheBlocktower.MOD_ID, "send_death_status");
    public static final CustomPacketPayload.Type<SendDeathStatusS2CPayload> ID = new CustomPacketPayload.Type<>(SEND_DEATH_STATUS_ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, SendDeathStatusS2CPayload> CODEC = StreamCodec.composite(
            PayloadCodecs.DEATH_MAP_CODEC, SendDeathStatusS2CPayload::deadPlayers,
            SendDeathStatusS2CPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
