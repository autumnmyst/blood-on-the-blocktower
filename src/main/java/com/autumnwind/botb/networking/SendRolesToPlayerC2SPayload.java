package com.autumnwind.botb.networking;

import com.autumnwind.botb.BloodOnTheBlocktower;
import com.autumnwind.botb.util.PendingRoleAssignment;
import com.autumnwind.botb.util.Reminder;
import com.autumnwind.botb.util.Script;
import java.util.List;
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
) implements CustomPacketPayload {
    public static final ResourceLocation SEND_ROLES_TO_PLAYER_ID = ResourceLocation.fromNamespaceAndPath(BloodOnTheBlocktower.MOD_ID, "send_roles_to_player");
    public static final CustomPacketPayload.Type<SendRolesToPlayerC2SPayload> ID = new CustomPacketPayload.Type<>(SEND_ROLES_TO_PLAYER_ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, SendRolesToPlayerC2SPayload> CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, SendRolesToPlayerC2SPayload::targetPlayer,
            PayloadCodecs.ROLE_MAP_CODEC, SendRolesToPlayerC2SPayload::roles,
            PayloadCodecs.SEAT_MAP_CODEC, SendRolesToPlayerC2SPayload::seatNumbers,
            ByteBufCodecs.VAR_INT, SendRolesToPlayerC2SPayload::activePlayerCount,
            Script.OPTIONAL_PACKET_CODEC, SendRolesToPlayerC2SPayload::script,
            PayloadCodecs.REMINDER_MAP_CODEC, SendRolesToPlayerC2SPayload::reminders,
            SendRolesToPlayerC2SPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
