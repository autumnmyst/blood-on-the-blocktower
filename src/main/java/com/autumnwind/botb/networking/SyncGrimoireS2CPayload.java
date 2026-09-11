package com.autumnwind.botb.networking;

import com.autumnwind.botb.BloodOnTheBlocktower;
import com.autumnwind.botb.util.PendingRoleAssignment;
import com.autumnwind.botb.util.Reminder;
import com.autumnwind.botb.util.Script;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;

import java.util.*;

/**
 * S2C payload for syncing grimoire state from server to other storytellers.
 * Sent when a storyteller updates their grimoire and there are multiple storytellers online.
 */
public record SyncGrimoireS2CPayload(
        Map<UUID, PendingRoleAssignment> roles,
        Map<UUID, Integer> seatNumbers,
        Map<UUID, List<Reminder>> reminders,
        Optional<Script> script,
        Set<UUID> markedPlayers,
        List<String> demonBluffs, // String format: "" = empty, "ROLE_NAME" = official, "custom:id" = custom
        int setupOutsiderCount
) implements CustomPayload {
    public static final CustomPayload.Id<SyncGrimoireS2CPayload> ID =
            new CustomPayload.Id<>(Identifier.of(BloodOnTheBlocktower.MOD_ID, "sync_grimoire_s2c"));

    public static final PacketCodec<RegistryByteBuf, SyncGrimoireS2CPayload> CODEC = new PacketCodec<>() {
        @Override
        public SyncGrimoireS2CPayload decode(RegistryByteBuf buf) {
            Map<UUID, PendingRoleAssignment> roles = PayloadCodecs.ROLE_MAP_CODEC.decode(buf);
            Map<UUID, Integer> seatNumbers = PayloadCodecs.SEAT_MAP_CODEC.decode(buf);
            Map<UUID, List<Reminder>> reminders = PayloadCodecs.REMINDER_MAP_CODEC.decode(buf);

            // Read optional script
            boolean hasScript = buf.readBoolean();
            Optional<Script> script = hasScript ? Optional.of(Script.PACKET_CODEC.decode(buf)) : Optional.empty();

            // Read marked players set
            int markedCount = buf.readVarInt();
            Set<UUID> markedPlayers = new HashSet<>();
            for (int i = 0; i < markedCount; i++) {
                markedPlayers.add(Uuids.PACKET_CODEC.decode(buf));
            }

            // Read demon bluffs (supports both official and custom roles)
            List<String> demonBluffs = PayloadCodecs.decodeBluffs(buf);

            int setupOutsiderCount = buf.readVarInt();

            return new SyncGrimoireS2CPayload(roles, seatNumbers, reminders, script, markedPlayers, demonBluffs, setupOutsiderCount);
        }

        @Override
        public void encode(RegistryByteBuf buf, SyncGrimoireS2CPayload payload) {
            PayloadCodecs.ROLE_MAP_CODEC.encode(buf, payload.roles);
            PayloadCodecs.SEAT_MAP_CODEC.encode(buf, payload.seatNumbers);
            PayloadCodecs.REMINDER_MAP_CODEC.encode(buf, payload.reminders);

            // Write optional script
            buf.writeBoolean(payload.script.isPresent());
            payload.script.ifPresent(s -> Script.PACKET_CODEC.encode(buf, s));

            // Write marked players set
            buf.writeVarInt(payload.markedPlayers.size());
            for (UUID uuid : payload.markedPlayers) {
                Uuids.PACKET_CODEC.encode(buf, uuid);
            }

            // Write demon bluffs (supports both official and custom roles)
            PayloadCodecs.encodeBluffs(buf, payload.demonBluffs);

            buf.writeVarInt(payload.setupOutsiderCount);
        }
    };

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
