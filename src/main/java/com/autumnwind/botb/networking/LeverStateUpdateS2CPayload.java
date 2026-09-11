package com.autumnwind.botb.networking;

import com.autumnwind.botb.BloodOnTheBlocktower;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Server-to-Client payload for updating lever states during nomination phase (before vote starts).
 * This is separate from VoteStateUpdateS2CPayload to avoid setting voteInProgress=true on the client.
 */
public record LeverStateUpdateS2CPayload(
        Map<UUID, Boolean> leverStates,
        Set<UUID> bansheePlayers,
        Set<UUID> bansheeDoubleActivePlayers
) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<LeverStateUpdateS2CPayload> ID =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(BloodOnTheBlocktower.MOD_ID, "lever_state_update"));

    public static final StreamCodec<RegistryFriendlyByteBuf, LeverStateUpdateS2CPayload> CODEC = StreamCodec.ofMember(
            LeverStateUpdateS2CPayload::write,
            LeverStateUpdateS2CPayload::read
    );

    private static void write(LeverStateUpdateS2CPayload payload, RegistryFriendlyByteBuf buf) {
        // Write lever states map
        buf.writeInt(payload.leverStates.size());
        for (Map.Entry<UUID, Boolean> entry : payload.leverStates.entrySet()) {
            UUIDUtil.STREAM_CODEC.encode(buf, entry.getKey());
            buf.writeBoolean(entry.getValue());
        }

        writeUuidSet(buf, payload.bansheePlayers);
        writeUuidSet(buf, payload.bansheeDoubleActivePlayers);
    }

    private static LeverStateUpdateS2CPayload read(RegistryFriendlyByteBuf buf) {
        // Read lever states map
        int leverStatesSize = buf.readInt();
        Map<UUID, Boolean> leverStates = new HashMap<>();
        for (int i = 0; i < leverStatesSize; i++) {
            UUID uuid = UUIDUtil.STREAM_CODEC.decode(buf);
            boolean value = buf.readBoolean();
            leverStates.put(uuid, value);
        }

        Set<UUID> bansheePlayers = readUuidSet(buf);
        Set<UUID> bansheeDoubleActivePlayers = readUuidSet(buf);

        return new LeverStateUpdateS2CPayload(leverStates, bansheePlayers, bansheeDoubleActivePlayers);
    }

    static void writeUuidSet(RegistryFriendlyByteBuf buf, Set<UUID> set) {
        buf.writeInt(set.size());
        for (UUID uuid : set) {
            UUIDUtil.STREAM_CODEC.encode(buf, uuid);
        }
    }

    static Set<UUID> readUuidSet(RegistryFriendlyByteBuf buf) {
        int size = buf.readInt();
        Set<UUID> set = new HashSet<>();
        for (int i = 0; i < size; i++) {
            set.add(UUIDUtil.STREAM_CODEC.decode(buf));
        }
        return set;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
