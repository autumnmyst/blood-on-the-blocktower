package com.autumnwind.botb.networking;

import com.autumnwind.botb.BloodOnTheBlocktower;
import com.autumnwind.botb.util.PendingRoleAssignment;
import com.autumnwind.botb.util.Reminder;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * C2S payload to send the complete grimoire to a specific player.
 * Used for roles like the Spy that need to see the full grimoire.
 * Storyteller Ctrl+Shift+Alt+Clicks a player to send them the grimoire.
 */
public record SendGrimoireToPlayerC2SPayload(
        UUID targetPlayer,
        Map<UUID, PendingRoleAssignment> roles,
        Map<UUID, Integer> seatNumbers,
        Map<UUID, List<Reminder>> reminders,
        List<String> demonBluffs // String format: "" = empty, "ROLE_NAME" = official, "custom:id" = custom
) implements CustomPayload {
    public static final Identifier SEND_GRIMOIRE_TO_PLAYER_ID = Identifier.of(BloodOnTheBlocktower.MOD_ID, "send_grimoire_to_player");
    public static final CustomPayload.Id<SendGrimoireToPlayerC2SPayload> ID = new CustomPayload.Id<>(SEND_GRIMOIRE_TO_PLAYER_ID);

    public static final PacketCodec<RegistryByteBuf, SendGrimoireToPlayerC2SPayload> CODEC = new PacketCodec<>() {
        @Override
        public SendGrimoireToPlayerC2SPayload decode(RegistryByteBuf buf) {
            UUID targetPlayer = Uuids.PACKET_CODEC.decode(buf);
            Map<UUID, PendingRoleAssignment> roles = PayloadCodecs.ROLE_MAP_CODEC.decode(buf);
            Map<UUID, Integer> seatNumbers = PayloadCodecs.SEAT_MAP_CODEC.decode(buf);
            Map<UUID, List<Reminder>> reminders = PayloadCodecs.REMINDER_MAP_CODEC.decode(buf);

            // Read demon bluffs (supports both official and custom roles)
            List<String> demonBluffs = PayloadCodecs.decodeBluffs(buf);

            return new SendGrimoireToPlayerC2SPayload(targetPlayer, roles, seatNumbers, reminders, demonBluffs);
        }

        @Override
        public void encode(RegistryByteBuf buf, SendGrimoireToPlayerC2SPayload payload) {
            Uuids.PACKET_CODEC.encode(buf, payload.targetPlayer);
            PayloadCodecs.ROLE_MAP_CODEC.encode(buf, payload.roles);
            PayloadCodecs.SEAT_MAP_CODEC.encode(buf, payload.seatNumbers);
            PayloadCodecs.REMINDER_MAP_CODEC.encode(buf, payload.reminders);

            // Write demon bluffs (supports both official and custom roles)
            PayloadCodecs.encodeBluffs(buf, payload.demonBluffs);
        }
    };

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
