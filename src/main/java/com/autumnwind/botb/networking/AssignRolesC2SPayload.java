package com.autumnwind.botb.networking;

import com.autumnwind.botb.BloodOnTheBlocktower;
import com.autumnwind.botb.util.PendingRoleAssignment;
import com.autumnwind.botb.util.Reminder;
import com.autumnwind.botb.util.Script;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import java.util.Optional;

/**
 * C2S payload for assigning roles to players.
 * Includes role assignments, seat numbers, active player count, optional script, and reminders
 * for madness detection.
 *
 * Note: Death status is handled separately via UpdateDeadPlayersC2SPayload.
 */
public record AssignRolesC2SPayload(
        Map<UUID, PendingRoleAssignment> roles,
        Map<UUID, Integer> seatNumbers,
        int activePlayerCount,
        Optional<Script> script,
        Map<UUID, List<Reminder>> reminders
) implements CustomPacketPayload {
    public static final ResourceLocation ASSIGN_ROLES_ID = ResourceLocation.fromNamespaceAndPath(BloodOnTheBlocktower.MOD_ID, "assign_roles");
    public static final CustomPacketPayload.Type<AssignRolesC2SPayload> ID = new CustomPacketPayload.Type<>(ASSIGN_ROLES_ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, AssignRolesC2SPayload> CODEC = StreamCodec.composite(
            PayloadCodecs.ROLE_MAP_CODEC, AssignRolesC2SPayload::roles,
            PayloadCodecs.SEAT_MAP_CODEC, AssignRolesC2SPayload::seatNumbers,
            ByteBufCodecs.VAR_INT, AssignRolesC2SPayload::activePlayerCount,
            Script.OPTIONAL_PACKET_CODEC, AssignRolesC2SPayload::script,
            PayloadCodecs.REMINDER_MAP_CODEC, AssignRolesC2SPayload::reminders,
            AssignRolesC2SPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
