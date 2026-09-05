package com.autumnwind.botb.networking;

import com.autumnwind.botb.BloodOnTheBlocktower;
import com.autumnwind.botb.util.PendingRoleAssignment;
import com.autumnwind.botb.util.Reminder;
import com.autumnwind.botb.util.Script;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.Map;
import java.util.UUID;
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
) implements CustomPayload {
    public static final Identifier ASSIGN_ROLES_ID = Identifier.of(BloodOnTheBlocktower.MOD_ID, "assign_roles");
    public static final CustomPayload.Id<AssignRolesC2SPayload> ID = new CustomPayload.Id<>(ASSIGN_ROLES_ID);

    public static final PacketCodec<RegistryByteBuf, AssignRolesC2SPayload> CODEC = PacketCodec.tuple(
            PayloadCodecs.ROLE_MAP_CODEC, AssignRolesC2SPayload::roles,
            PayloadCodecs.SEAT_MAP_CODEC, AssignRolesC2SPayload::seatNumbers,
            PacketCodecs.VAR_INT, AssignRolesC2SPayload::activePlayerCount,
            Script.OPTIONAL_PACKET_CODEC, AssignRolesC2SPayload::script,
            PayloadCodecs.REMINDER_MAP_CODEC, AssignRolesC2SPayload::reminders,
            AssignRolesC2SPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
