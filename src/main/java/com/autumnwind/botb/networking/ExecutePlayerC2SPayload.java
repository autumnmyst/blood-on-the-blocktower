package com.autumnwind.botb.networking;

import com.autumnwind.botb.BloodOnTheBlocktower;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Client-to-Server payload for executing a player.
 * Can be forced execution or execution of the marked player.
 * @param butcherAliveWithAbility If true, Butcher traveler is alive with ability,
 *        so nominations should continue after execution with only Butcher able to nominate.
 * @param butcherUuid The UUID of the Butcher player (required if butcherAliveWithAbility is true)
 */
public record ExecutePlayerC2SPayload(UUID player, boolean forced, boolean butcherAliveWithAbility, Optional<UUID> butcherUuid) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ExecutePlayerC2SPayload> ID =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(BloodOnTheBlocktower.MOD_ID, "execute_player"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ExecutePlayerC2SPayload> CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, ExecutePlayerC2SPayload::player,
            ByteBufCodecs.BOOL, ExecutePlayerC2SPayload::forced,
            ByteBufCodecs.BOOL, ExecutePlayerC2SPayload::butcherAliveWithAbility,
            UUIDUtil.STREAM_CODEC.apply(ByteBufCodecs::optional), ExecutePlayerC2SPayload::butcherUuid,
            ExecutePlayerC2SPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
