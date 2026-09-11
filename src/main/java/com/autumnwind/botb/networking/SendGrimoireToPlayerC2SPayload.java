package com.autumnwind.botb.networking;

import com.autumnwind.botb.BloodOnTheBlocktower;
import com.autumnwind.botb.util.PendingRoleAssignment;
import com.autumnwind.botb.util.Reminder;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

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
) implements CustomPacketPayload {
    public static final ResourceLocation SEND_GRIMOIRE_TO_PLAYER_ID = ResourceLocation.fromNamespaceAndPath(BloodOnTheBlocktower.MOD_ID, "send_grimoire_to_player");
    public static final CustomPacketPayload.Type<SendGrimoireToPlayerC2SPayload> ID = new CustomPacketPayload.Type<>(SEND_GRIMOIRE_TO_PLAYER_ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, SendGrimoireToPlayerC2SPayload> CODEC = new StreamCodec<>() {
        @Override
        public SendGrimoireToPlayerC2SPayload decode(RegistryFriendlyByteBuf buf) {
            UUID targetPlayer = UUIDUtil.STREAM_CODEC.decode(buf);
            Map<UUID, PendingRoleAssignment> roles = PayloadCodecs.ROLE_MAP_CODEC.decode(buf);
            Map<UUID, Integer> seatNumbers = PayloadCodecs.SEAT_MAP_CODEC.decode(buf);
            Map<UUID, List<Reminder>> reminders = PayloadCodecs.REMINDER_MAP_CODEC.decode(buf);

            // Read demon bluffs (supports both official and custom roles)
            List<String> demonBluffs = PayloadCodecs.decodeBluffs(buf);

            return new SendGrimoireToPlayerC2SPayload(targetPlayer, roles, seatNumbers, reminders, demonBluffs);
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, SendGrimoireToPlayerC2SPayload payload) {
            UUIDUtil.STREAM_CODEC.encode(buf, payload.targetPlayer);
            PayloadCodecs.ROLE_MAP_CODEC.encode(buf, payload.roles);
            PayloadCodecs.SEAT_MAP_CODEC.encode(buf, payload.seatNumbers);
            PayloadCodecs.REMINDER_MAP_CODEC.encode(buf, payload.reminders);

            // Write demon bluffs (supports both official and custom roles)
            PayloadCodecs.encodeBluffs(buf, payload.demonBluffs);
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
