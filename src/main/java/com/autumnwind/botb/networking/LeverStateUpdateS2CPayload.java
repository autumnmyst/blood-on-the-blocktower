package com.autumnwind.botb.networking;

import com.autumnwind.botb.BloodOnTheBlocktower;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Server-to-Client payload for updating lever states during nomination phase (before vote starts).
 * This is separate from VoteStateUpdateS2CPayload to avoid setting voteInProgress=true on the client.
 */
public record LeverStateUpdateS2CPayload(
        Map<UUID, Boolean> leverStates,
        Set<UUID> bansheePlayers,
        Set<UUID> bansheeDoubleActivePlayers
) implements CustomPayload {
    public static final CustomPayload.Id<LeverStateUpdateS2CPayload> ID =
            new CustomPayload.Id<>(Identifier.of(BloodOnTheBlocktower.MOD_ID, "lever_state_update"));

    public static final PacketCodec<RegistryByteBuf, LeverStateUpdateS2CPayload> CODEC = PacketCodec.of(
            LeverStateUpdateS2CPayload::write,
            LeverStateUpdateS2CPayload::read
    );

    private static void write(LeverStateUpdateS2CPayload payload, RegistryByteBuf buf) {
        // Write lever states map
        buf.writeInt(payload.leverStates.size());
        for (Map.Entry<UUID, Boolean> entry : payload.leverStates.entrySet()) {
            Uuids.PACKET_CODEC.encode(buf, entry.getKey());
            buf.writeBoolean(entry.getValue());
        }

        writeUuidSet(buf, payload.bansheePlayers);
        writeUuidSet(buf, payload.bansheeDoubleActivePlayers);
    }

    private static LeverStateUpdateS2CPayload read(RegistryByteBuf buf) {
        // Read lever states map
        int leverStatesSize = buf.readInt();
        Map<UUID, Boolean> leverStates = new HashMap<>();
        for (int i = 0; i < leverStatesSize; i++) {
            UUID uuid = Uuids.PACKET_CODEC.decode(buf);
            boolean value = buf.readBoolean();
            leverStates.put(uuid, value);
        }

        Set<UUID> bansheePlayers = readUuidSet(buf);
        Set<UUID> bansheeDoubleActivePlayers = readUuidSet(buf);

        return new LeverStateUpdateS2CPayload(leverStates, bansheePlayers, bansheeDoubleActivePlayers);
    }

    static void writeUuidSet(RegistryByteBuf buf, Set<UUID> set) {
        buf.writeInt(set.size());
        for (UUID uuid : set) {
            Uuids.PACKET_CODEC.encode(buf, uuid);
        }
    }

    static Set<UUID> readUuidSet(RegistryByteBuf buf) {
        int size = buf.readInt();
        Set<UUID> set = new HashSet<>();
        for (int i = 0; i < size; i++) {
            set.add(Uuids.PACKET_CODEC.decode(buf));
        }
        return set;
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
