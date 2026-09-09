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
import net.minecraft.util.Uuids;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Targeted Send Roles: same data as {@link AssignRolesC2SPayload}, but only the named player's client
 * is updated. Full maps are carried so the server derives the same counts as the full send.
 */
public record SendRolesToPlayerC2SPayload(
        UUID targetPlayer,
        Map<UUID, PendingRoleAssignment> roles,
        Map<UUID, Integer> seatNumbers,
        int activePlayerCount,
        Optional<Script> script,
        Map<UUID, List<Reminder>> reminders
) implements CustomPayload {
    public static final Identifier SEND_ROLES_TO_PLAYER_ID = Identifier.of(BloodOnTheBlocktower.MOD_ID, "send_roles_to_player");
    public static final CustomPayload.Id<SendRolesToPlayerC2SPayload> ID = new CustomPayload.Id<>(SEND_ROLES_TO_PLAYER_ID);

    public static final PacketCodec<RegistryByteBuf, SendRolesToPlayerC2SPayload> CODEC = PacketCodec.tuple(
            Uuids.PACKET_CODEC, SendRolesToPlayerC2SPayload::targetPlayer,
            PayloadCodecs.ROLE_MAP_CODEC, SendRolesToPlayerC2SPayload::roles,
            PayloadCodecs.SEAT_MAP_CODEC, SendRolesToPlayerC2SPayload::seatNumbers,
            PacketCodecs.VAR_INT, SendRolesToPlayerC2SPayload::activePlayerCount,
            Script.OPTIONAL_PACKET_CODEC, SendRolesToPlayerC2SPayload::script,
            PayloadCodecs.REMINDER_MAP_CODEC, SendRolesToPlayerC2SPayload::reminders,
            SendRolesToPlayerC2SPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
