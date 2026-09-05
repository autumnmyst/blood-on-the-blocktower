package com.autumnwind.botb.networking;

import com.autumnwind.botb.BloodOnTheBlocktower;
import com.autumnwind.botb.util.PendingRoleAssignment;
import com.autumnwind.botb.util.Reminder;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * S2C payload to send the complete grimoire (all roles and reminders) to players.
 * Used for game end animation display and for roles like the Spy that can see the grimoire.
 * NOTE: This updates StorytellerState for display purposes, NOT ClientState.
 *
 * @param roles Map of player UUIDs to their role assignments
 * @param seatNumbers Map of player UUIDs to their seat numbers
 * @param reminders Map of player UUIDs to their reminders
 * @param demonBluffs List of demon bluff strings (format: "" = empty, "ROLE_NAME" = official, "custom:id" = custom)
 * @param isTargetedSend true if this is a targeted send to a specific player (e.g., Spy),
 *                       false if this is a broadcast to all players (e.g., game end)
 */
public record SendGrimoireS2CPayload(
        Map<UUID, PendingRoleAssignment> roles,
        Map<UUID, Integer> seatNumbers,
        Map<UUID, List<Reminder>> reminders,
        List<String> demonBluffs,
        boolean isTargetedSend
) implements CustomPayload {
    public static final Identifier SEND_GRIMOIRE_ID = Identifier.of(BloodOnTheBlocktower.MOD_ID, "send_grimoire");
    public static final CustomPayload.Id<SendGrimoireS2CPayload> ID = new CustomPayload.Id<>(SEND_GRIMOIRE_ID);

    public static final PacketCodec<RegistryByteBuf, SendGrimoireS2CPayload> CODEC = new PacketCodec<>() {
        @Override
        public SendGrimoireS2CPayload decode(RegistryByteBuf buf) {
            Map<UUID, PendingRoleAssignment> roles = PayloadCodecs.ROLE_MAP_CODEC.decode(buf);
            Map<UUID, Integer> seatNumbers = PayloadCodecs.SEAT_MAP_CODEC.decode(buf);
            Map<UUID, List<Reminder>> reminders = PayloadCodecs.REMINDER_MAP_CODEC.decode(buf);

            // Read demon bluffs (supports both official and custom roles)
            List<String> demonBluffs = PayloadCodecs.decodeBluffs(buf);

            boolean isTargetedSend = buf.readBoolean();

            return new SendGrimoireS2CPayload(roles, seatNumbers, reminders, demonBluffs, isTargetedSend);
        }

        @Override
        public void encode(RegistryByteBuf buf, SendGrimoireS2CPayload payload) {
            PayloadCodecs.ROLE_MAP_CODEC.encode(buf, payload.roles);
            PayloadCodecs.SEAT_MAP_CODEC.encode(buf, payload.seatNumbers);
            PayloadCodecs.REMINDER_MAP_CODEC.encode(buf, payload.reminders);

            // Write demon bluffs (supports both official and custom roles)
            PayloadCodecs.encodeBluffs(buf, payload.demonBluffs);

            buf.writeBoolean(payload.isTargetedSend);
        }
    };

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
