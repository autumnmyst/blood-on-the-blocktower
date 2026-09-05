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
 * C2S payload to trigger game end with a winning team and grimoire data.
 */
public record EndGameC2SPayload(
        boolean goodWins,
        Map<UUID, PendingRoleAssignment> roles,
        Map<UUID, Integer> seatNumbers,
        Map<UUID, List<Reminder>> reminders,
        List<String> demonBluffs // String format: "" = empty, "ROLE_NAME" = official, "custom:id" = custom
) implements CustomPayload {
    public static final Identifier END_GAME_ID = Identifier.of(BloodOnTheBlocktower.MOD_ID, "end_game");
    public static final CustomPayload.Id<EndGameC2SPayload> ID = new CustomPayload.Id<>(END_GAME_ID);

    public static final PacketCodec<RegistryByteBuf, EndGameC2SPayload> CODEC = new PacketCodec<>() {
        @Override
        public EndGameC2SPayload decode(RegistryByteBuf buf) {
            boolean goodWins = buf.readBoolean();
            Map<UUID, PendingRoleAssignment> roles = PayloadCodecs.ROLE_MAP_CODEC.decode(buf);
            Map<UUID, Integer> seatNumbers = PayloadCodecs.SEAT_MAP_CODEC.decode(buf);
            Map<UUID, List<Reminder>> reminders = PayloadCodecs.REMINDER_MAP_CODEC.decode(buf);

            // Read demon bluffs (supports both official and custom roles)
            List<String> demonBluffs = PayloadCodecs.decodeBluffs(buf);

            return new EndGameC2SPayload(goodWins, roles, seatNumbers, reminders, demonBluffs);
        }

        @Override
        public void encode(RegistryByteBuf buf, EndGameC2SPayload payload) {
            buf.writeBoolean(payload.goodWins());
            PayloadCodecs.ROLE_MAP_CODEC.encode(buf, payload.roles());
            PayloadCodecs.SEAT_MAP_CODEC.encode(buf, payload.seatNumbers());
            PayloadCodecs.REMINDER_MAP_CODEC.encode(buf, payload.reminders());

            // Write demon bluffs (supports both official and custom roles)
            PayloadCodecs.encodeBluffs(buf, payload.demonBluffs);
        }
    };

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
