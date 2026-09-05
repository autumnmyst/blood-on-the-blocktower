package com.autumnwind.botb.networking;

import com.autumnwind.botb.BloodOnTheBlocktower;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Client-to-Server payload for executing dusk or dawn transition.
 * For DAWN, includes lists of players with/without Banshee ability for indicator updates.
 * @param type "dusk" or "dawn"
 * @param bansheeHasAbilityPlayers For DAWN only: players who currently have the Banshee "Has Ability" reminder
 * @param bansheeLostAbilityPlayers For DAWN only: players who previously had Banshee ability but now lost it
 * @param voudonModeActive For DAWN only: if Voudon traveler is alive with ability
 * @param voudonPlayerUuid For DAWN only: UUID of the Voudon traveler (if active)
 */
public record ExecuteDuskDawnC2SPayload(
        String type,
        List<UUID> bansheeHasAbilityPlayers,
        List<UUID> bansheeLostAbilityPlayers,
        boolean voudonModeActive,
        Optional<UUID> voudonPlayerUuid
) implements CustomPayload {
    public static final CustomPayload.Id<ExecuteDuskDawnC2SPayload> ID =
            new CustomPayload.Id<>(Identifier.of(BloodOnTheBlocktower.MOD_ID, "execute_dusk_dawn"));

    public static final PacketCodec<RegistryByteBuf, ExecuteDuskDawnC2SPayload> CODEC = PacketCodec.of(
            (payload, buf) -> {
                PacketCodecs.STRING.encode(buf, payload.type);
                buf.writeInt(payload.bansheeHasAbilityPlayers.size());
                for (UUID uuid : payload.bansheeHasAbilityPlayers) {
                    Uuids.PACKET_CODEC.encode(buf, uuid);
                }
                buf.writeInt(payload.bansheeLostAbilityPlayers.size());
                for (UUID uuid : payload.bansheeLostAbilityPlayers) {
                    Uuids.PACKET_CODEC.encode(buf, uuid);
                }
                buf.writeBoolean(payload.voudonModeActive);
                buf.writeBoolean(payload.voudonPlayerUuid.isPresent());
                payload.voudonPlayerUuid.ifPresent(uuid -> Uuids.PACKET_CODEC.encode(buf, uuid));
            },
            buf -> {
                String type = PacketCodecs.STRING.decode(buf);
                int hasAbilitySize = buf.readInt();
                List<UUID> hasAbilityPlayers = new ArrayList<>();
                for (int i = 0; i < hasAbilitySize; i++) {
                    hasAbilityPlayers.add(Uuids.PACKET_CODEC.decode(buf));
                }
                int lostAbilitySize = buf.readInt();
                List<UUID> lostAbilityPlayers = new ArrayList<>();
                for (int i = 0; i < lostAbilitySize; i++) {
                    lostAbilityPlayers.add(Uuids.PACKET_CODEC.decode(buf));
                }
                boolean voudonModeActive = buf.readBoolean();
                Optional<UUID> voudonPlayerUuid = buf.readBoolean() ? Optional.of(Uuids.PACKET_CODEC.decode(buf)) : Optional.empty();
                return new ExecuteDuskDawnC2SPayload(type, hasAbilityPlayers, lostAbilityPlayers, voudonModeActive, voudonPlayerUuid);
            }
    );

    // Convenience constructors
    public ExecuteDuskDawnC2SPayload(String type) {
        this(type, List.of(), List.of(), false, Optional.empty());
    }

    public ExecuteDuskDawnC2SPayload(String type, List<UUID> bansheeHasAbilityPlayers, List<UUID> bansheeLostAbilityPlayers) {
        this(type, bansheeHasAbilityPlayers, bansheeLostAbilityPlayers, false, Optional.empty());
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    public static final String DUSK = "dusk";
    public static final String DAWN = "dawn";
}
