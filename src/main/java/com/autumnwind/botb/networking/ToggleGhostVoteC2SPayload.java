package com.autumnwind.botb.networking;

import com.autumnwind.botb.BloodOnTheBlocktower;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Client-to-Server payload for toggling a player's ghost vote used status.
 * Only valid for dead players. Sets or clears their intrinsic ghost vote used state.
 * @param playerUuid The UUID of the player whose ghost vote to toggle
 * @param setUsed true to mark ghost vote as used, false to restore it
 */
public record ToggleGhostVoteC2SPayload(UUID playerUuid, boolean setUsed) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ToggleGhostVoteC2SPayload> ID =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(BloodOnTheBlocktower.MOD_ID, "toggle_ghost_vote"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ToggleGhostVoteC2SPayload> CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, ToggleGhostVoteC2SPayload::playerUuid,
            ByteBufCodecs.BOOL, ToggleGhostVoteC2SPayload::setUsed,
            ToggleGhostVoteC2SPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
