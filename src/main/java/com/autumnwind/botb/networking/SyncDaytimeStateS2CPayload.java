package com.autumnwind.botb.networking;

import com.autumnwind.botb.BloodOnTheBlocktower;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Server-to-Client payload for synchronizing daytime state.
 */
public record SyncDaytimeStateS2CPayload(
        Map<UUID, Boolean> canNominate,
        Map<UUID, Boolean> canBeNominated,
        Map<UUID, Boolean> hasUsedGhostVote,
        Map<UUID, Integer> nominationsRemaining,
        UUID currentNominator,
        UUID currentNominee,
        UUID markedForExecution,
        int votesForMarkedPlayer,
        boolean nominationsOpen,
        boolean organGrinderModeActiveToday,
        UUID storytellerMFE,
        int storytellerMFEVotes,
        boolean storytellerCanBeNominated,
        // Exile state
        Map<UUID, Boolean> canBeExiled,
        UUID currentExileCaller,
        UUID currentExileTarget,
        boolean exileSupportInProgress,
        int exileSupportCount,
        // Voudon state (reversed voting eligibility while active)
        boolean voudonModeActive,
        UUID voudonPlayerUuid
) implements CustomPayload {
    public static final CustomPayload.Id<SyncDaytimeStateS2CPayload> ID =
            new CustomPayload.Id<>(Identifier.of(BloodOnTheBlocktower.MOD_ID, "sync_daytime_state"));

    public static final PacketCodec<RegistryByteBuf, SyncDaytimeStateS2CPayload> CODEC = PacketCodec.of(
            SyncDaytimeStateS2CPayload::write,
            SyncDaytimeStateS2CPayload::read
    );

    private static void write(SyncDaytimeStateS2CPayload payload, RegistryByteBuf buf) {
        // Write canNominate map
        buf.writeInt(payload.canNominate.size());
        for (Map.Entry<UUID, Boolean> entry : payload.canNominate.entrySet()) {
            Uuids.PACKET_CODEC.encode(buf, entry.getKey());
            buf.writeBoolean(entry.getValue());
        }

        // Write canBeNominated map
        buf.writeInt(payload.canBeNominated.size());
        for (Map.Entry<UUID, Boolean> entry : payload.canBeNominated.entrySet()) {
            Uuids.PACKET_CODEC.encode(buf, entry.getKey());
            buf.writeBoolean(entry.getValue());
        }

        // Write hasUsedGhostVote map
        buf.writeInt(payload.hasUsedGhostVote.size());
        for (Map.Entry<UUID, Boolean> entry : payload.hasUsedGhostVote.entrySet()) {
            Uuids.PACKET_CODEC.encode(buf, entry.getKey());
            buf.writeBoolean(entry.getValue());
        }

        // Write nominationsRemaining map (for Banshee double nominations)
        buf.writeInt(payload.nominationsRemaining.size());
        for (Map.Entry<UUID, Integer> entry : payload.nominationsRemaining.entrySet()) {
            Uuids.PACKET_CODEC.encode(buf, entry.getKey());
            buf.writeInt(entry.getValue());
        }

        // Write nullable UUIDs
        buf.writeBoolean(payload.currentNominator != null);
        if (payload.currentNominator != null) {
            Uuids.PACKET_CODEC.encode(buf, payload.currentNominator);
        }

        buf.writeBoolean(payload.currentNominee != null);
        if (payload.currentNominee != null) {
            Uuids.PACKET_CODEC.encode(buf, payload.currentNominee);
        }

        buf.writeBoolean(payload.markedForExecution != null);
        if (payload.markedForExecution != null) {
            Uuids.PACKET_CODEC.encode(buf, payload.markedForExecution);
        }

        // Write int and booleans
        buf.writeInt(payload.votesForMarkedPlayer);
        buf.writeBoolean(payload.nominationsOpen);
        buf.writeBoolean(payload.organGrinderModeActiveToday);

        // Write storyteller MFE (for Legion support)
        buf.writeBoolean(payload.storytellerMFE != null);
        if (payload.storytellerMFE != null) {
            Uuids.PACKET_CODEC.encode(buf, payload.storytellerMFE);
        }
        buf.writeInt(payload.storytellerMFEVotes);

        // Write storyteller nomination eligibility (for Atheist)
        buf.writeBoolean(payload.storytellerCanBeNominated);

        // Write exile state
        buf.writeInt(payload.canBeExiled.size());
        for (Map.Entry<UUID, Boolean> entry : payload.canBeExiled.entrySet()) {
            Uuids.PACKET_CODEC.encode(buf, entry.getKey());
            buf.writeBoolean(entry.getValue());
        }

        buf.writeBoolean(payload.currentExileCaller != null);
        if (payload.currentExileCaller != null) {
            Uuids.PACKET_CODEC.encode(buf, payload.currentExileCaller);
        }

        buf.writeBoolean(payload.currentExileTarget != null);
        if (payload.currentExileTarget != null) {
            Uuids.PACKET_CODEC.encode(buf, payload.currentExileTarget);
        }

        buf.writeBoolean(payload.exileSupportInProgress);
        buf.writeInt(payload.exileSupportCount);

        // Write Voudon state
        buf.writeBoolean(payload.voudonModeActive);
        buf.writeBoolean(payload.voudonPlayerUuid != null);
        if (payload.voudonPlayerUuid != null) {
            Uuids.PACKET_CODEC.encode(buf, payload.voudonPlayerUuid);
        }
    }

    private static SyncDaytimeStateS2CPayload read(RegistryByteBuf buf) {
        // Read canNominate map
        int canNominateSize = buf.readInt();
        Map<UUID, Boolean> canNominate = new HashMap<>();
        for (int i = 0; i < canNominateSize; i++) {
            UUID uuid = Uuids.PACKET_CODEC.decode(buf);
            boolean value = buf.readBoolean();
            canNominate.put(uuid, value);
        }

        // Read canBeNominated map
        int canBeNominatedSize = buf.readInt();
        Map<UUID, Boolean> canBeNominated = new HashMap<>();
        for (int i = 0; i < canBeNominatedSize; i++) {
            UUID uuid = Uuids.PACKET_CODEC.decode(buf);
            boolean value = buf.readBoolean();
            canBeNominated.put(uuid, value);
        }

        // Read hasUsedGhostVote map
        int ghostVoteSize = buf.readInt();
        Map<UUID, Boolean> hasUsedGhostVote = new HashMap<>();
        for (int i = 0; i < ghostVoteSize; i++) {
            UUID uuid = Uuids.PACKET_CODEC.decode(buf);
            boolean value = buf.readBoolean();
            hasUsedGhostVote.put(uuid, value);
        }

        // Read nominationsRemaining map (for Banshee double nominations)
        int nominationsRemainingSize = buf.readInt();
        Map<UUID, Integer> nominationsRemaining = new HashMap<>();
        for (int i = 0; i < nominationsRemainingSize; i++) {
            UUID uuid = Uuids.PACKET_CODEC.decode(buf);
            int value = buf.readInt();
            nominationsRemaining.put(uuid, value);
        }

        // Read nullable UUIDs
        UUID currentNominator = null;
        if (buf.readBoolean()) {
            currentNominator = Uuids.PACKET_CODEC.decode(buf);
        }

        UUID currentNominee = null;
        if (buf.readBoolean()) {
            currentNominee = Uuids.PACKET_CODEC.decode(buf);
        }

        UUID markedForExecution = null;
        if (buf.readBoolean()) {
            markedForExecution = Uuids.PACKET_CODEC.decode(buf);
        }

        // Read int and booleans
        int votesForMarkedPlayer = buf.readInt();
        boolean nominationsOpen = buf.readBoolean();
        boolean organGrinderModeActiveToday = buf.readBoolean();

        // Read storyteller MFE (for Legion support)
        UUID storytellerMFE = null;
        if (buf.readBoolean()) {
            storytellerMFE = Uuids.PACKET_CODEC.decode(buf);
        }
        int storytellerMFEVotes = buf.readInt();

        // Read storyteller nomination eligibility (for Atheist)
        boolean storytellerCanBeNominated = buf.readBoolean();

        // Read exile state
        int canBeExiledSize = buf.readInt();
        Map<UUID, Boolean> canBeExiled = new HashMap<>();
        for (int i = 0; i < canBeExiledSize; i++) {
            UUID uuid = Uuids.PACKET_CODEC.decode(buf);
            boolean value = buf.readBoolean();
            canBeExiled.put(uuid, value);
        }

        UUID currentExileCaller = null;
        if (buf.readBoolean()) {
            currentExileCaller = Uuids.PACKET_CODEC.decode(buf);
        }

        UUID currentExileTarget = null;
        if (buf.readBoolean()) {
            currentExileTarget = Uuids.PACKET_CODEC.decode(buf);
        }

        boolean exileSupportInProgress = buf.readBoolean();
        int exileSupportCount = buf.readInt();

        // Read Voudon state
        boolean voudonModeActive = buf.readBoolean();
        UUID voudonPlayerUuid = null;
        if (buf.readBoolean()) {
            voudonPlayerUuid = Uuids.PACKET_CODEC.decode(buf);
        }

        return new SyncDaytimeStateS2CPayload(
                canNominate,
                canBeNominated,
                hasUsedGhostVote,
                nominationsRemaining,
                currentNominator,
                currentNominee,
                markedForExecution,
                votesForMarkedPlayer,
                nominationsOpen,
                organGrinderModeActiveToday,
                storytellerMFE,
                storytellerMFEVotes,
                storytellerCanBeNominated,
                canBeExiled,
                currentExileCaller,
                currentExileTarget,
                exileSupportInProgress,
                exileSupportCount,
                voudonModeActive,
                voudonPlayerUuid
        );
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
