package com.autumnwind.botb.networking;

import com.autumnwind.botb.BloodOnTheBlocktower;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Client-to-Server payload for executing dusk or dawn transition.
 * For DAWN, includes lists of players with/without Banshee ability for indicator updates.
 * @param transition "dusk" or "dawn"
 * @param bansheeHasAbilityPlayers For DAWN only: players who currently have the Banshee "Has Ability" reminder
 * @param bansheeLostAbilityPlayers For DAWN only: players who previously had Banshee ability but now lost it
 * @param voudonModeActive For DAWN only: if Voudon traveler is alive with ability
 * @param voudonPlayerUuid For DAWN only: UUID of the Voudon traveler (if active)
 */
public record ExecuteDuskDawnC2SPayload(
        String transition,        List<UUID> bansheeHasAbilityPlayers,
        List<UUID> bansheeLostAbilityPlayers,
        boolean voudonModeActive,
        Optional<UUID> voudonPlayerUuid
) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ExecuteDuskDawnC2SPayload> ID =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(BloodOnTheBlocktower.MOD_ID, "execute_dusk_dawn"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ExecuteDuskDawnC2SPayload> CODEC = StreamCodec.ofMember(
            (payload, buf) -> {
                ByteBufCodecs.STRING_UTF8.encode(buf, payload.transition);
                buf.writeInt(payload.bansheeHasAbilityPlayers.size());
                for (UUID uuid : payload.bansheeHasAbilityPlayers) {
                    UUIDUtil.STREAM_CODEC.encode(buf, uuid);
                }
                buf.writeInt(payload.bansheeLostAbilityPlayers.size());
                for (UUID uuid : payload.bansheeLostAbilityPlayers) {
                    UUIDUtil.STREAM_CODEC.encode(buf, uuid);
                }
                buf.writeBoolean(payload.voudonModeActive);
                buf.writeBoolean(payload.voudonPlayerUuid.isPresent());
                payload.voudonPlayerUuid.ifPresent(uuid -> UUIDUtil.STREAM_CODEC.encode(buf, uuid));
            },
            buf -> {
                String transition = ByteBufCodecs.STRING_UTF8.decode(buf);
                int hasAbilitySize = buf.readInt();
                List<UUID> hasAbilityPlayers = new ArrayList<>();
                for (int i = 0; i < hasAbilitySize; i++) {
                    hasAbilityPlayers.add(UUIDUtil.STREAM_CODEC.decode(buf));
                }
                int lostAbilitySize = buf.readInt();
                List<UUID> lostAbilityPlayers = new ArrayList<>();
                for (int i = 0; i < lostAbilitySize; i++) {
                    lostAbilityPlayers.add(UUIDUtil.STREAM_CODEC.decode(buf));
                }
                boolean voudonModeActive = buf.readBoolean();
                Optional<UUID> voudonPlayerUuid = buf.readBoolean() ? Optional.of(UUIDUtil.STREAM_CODEC.decode(buf)) : Optional.empty();
                return new ExecuteDuskDawnC2SPayload(transition, hasAbilityPlayers, lostAbilityPlayers, voudonModeActive, voudonPlayerUuid);
            }
    );

    // Convenience constructors
    public ExecuteDuskDawnC2SPayload(String transition) {
        this(transition, List.of(), List.of(), false, Optional.empty());
    }

    public ExecuteDuskDawnC2SPayload(String transition, List<UUID> bansheeHasAbilityPlayers, List<UUID> bansheeLostAbilityPlayers) {
        this(transition, bansheeHasAbilityPlayers, bansheeLostAbilityPlayers, false, Optional.empty());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    public static final String DUSK = "dusk";
    public static final String DAWN = "dawn";
}
