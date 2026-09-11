package com.autumnwind.botb.networking;

import com.autumnwind.botb.BloodOnTheBlocktower;
import com.autumnwind.botb.util.PendingRoleAssignment;
import com.autumnwind.botb.util.Reminder;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

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
) implements CustomPacketPayload {
    public static final ResourceLocation SEND_GRIMOIRE_ID = ResourceLocation.fromNamespaceAndPath(BloodOnTheBlocktower.MOD_ID, "send_grimoire");
    public static final CustomPacketPayload.Type<SendGrimoireS2CPayload> ID = new CustomPacketPayload.Type<>(SEND_GRIMOIRE_ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, SendGrimoireS2CPayload> CODEC = new StreamCodec<>() {
        @Override
        public SendGrimoireS2CPayload decode(RegistryFriendlyByteBuf buf) {
            Map<UUID, PendingRoleAssignment> roles = PayloadCodecs.ROLE_MAP_CODEC.decode(buf);
            Map<UUID, Integer> seatNumbers = PayloadCodecs.SEAT_MAP_CODEC.decode(buf);
            Map<UUID, List<Reminder>> reminders = PayloadCodecs.REMINDER_MAP_CODEC.decode(buf);

            // Read demon bluffs (supports both official and custom roles)
            List<String> demonBluffs = PayloadCodecs.decodeBluffs(buf);

            boolean isTargetedSend = buf.readBoolean();

            return new SendGrimoireS2CPayload(roles, seatNumbers, reminders, demonBluffs, isTargetedSend);
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, SendGrimoireS2CPayload payload) {
            PayloadCodecs.ROLE_MAP_CODEC.encode(buf, payload.roles);
            PayloadCodecs.SEAT_MAP_CODEC.encode(buf, payload.seatNumbers);
            PayloadCodecs.REMINDER_MAP_CODEC.encode(buf, payload.reminders);

            // Write demon bluffs (supports both official and custom roles)
            PayloadCodecs.encodeBluffs(buf, payload.demonBluffs);

            buf.writeBoolean(payload.isTargetedSend);
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
