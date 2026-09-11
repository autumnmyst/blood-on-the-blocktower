package com.autumnwind.botb.networking;

import com.autumnwind.botb.BloodOnTheBlocktower;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * C2S payload for updating dead player status only.
 * Sent from storyteller client to server to update which players are dead.
 *
 * @param silent suppresses death-side UI effects that don't make sense for a batched
 *               update, currently the daytime cosmetic lightning strike. The dawn
 *               batch (deferred night kills) sends silent=true, and live storyteller
 *               actions during the day send silent=false.
 * @param voudonModeActive whether Voudon is alive with ability under the new death
 *                         status, so a kill or revival of the Voudon flips the mode
 *                         immediately rather than at the next nomination
 * @param voudonPlayerUuid the UUID of the Voudon player, if Voudon mode is active
 */
public record UpdateDeadPlayersC2SPayload(
        Map<UUID, Boolean> deadPlayers,
        Map<UUID, Integer> seatNumbers,
        boolean silent,
        boolean voudonModeActive,
        Optional<UUID> voudonPlayerUuid
) implements CustomPacketPayload {
    public static final ResourceLocation UPDATE_DEAD_PLAYERS_ID = ResourceLocation.fromNamespaceAndPath(BloodOnTheBlocktower.MOD_ID, "update_dead_players");
    public static final CustomPacketPayload.Type<UpdateDeadPlayersC2SPayload> ID = new CustomPacketPayload.Type<>(UPDATE_DEAD_PLAYERS_ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, UpdateDeadPlayersC2SPayload> CODEC = StreamCodec.composite(
            PayloadCodecs.DEATH_MAP_CODEC, UpdateDeadPlayersC2SPayload::deadPlayers,
            PayloadCodecs.SEAT_MAP_CODEC, UpdateDeadPlayersC2SPayload::seatNumbers,
            ByteBufCodecs.BOOL, UpdateDeadPlayersC2SPayload::silent,
            ByteBufCodecs.BOOL, UpdateDeadPlayersC2SPayload::voudonModeActive,
            ByteBufCodecs.optional(UUIDUtil.STREAM_CODEC), UpdateDeadPlayersC2SPayload::voudonPlayerUuid,
            UpdateDeadPlayersC2SPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
