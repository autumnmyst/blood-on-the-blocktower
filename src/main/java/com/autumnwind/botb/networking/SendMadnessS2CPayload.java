package com.autumnwind.botb.networking;

import com.autumnwind.botb.BloodOnTheBlocktower;
import com.autumnwind.botb.util.Madness;
import com.autumnwind.botb.util.Role;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Payload for sending madness data to a player.
 * Contains a list of active madnesses for that player.
 */
public record SendMadnessS2CPayload(List<Madness> madnesses) implements CustomPacketPayload {
    public static final Identifier SEND_MADNESS_ID = Identifier.fromNamespaceAndPath(BloodOnTheBlocktower.MOD_ID, "send_madness");
    public static final CustomPacketPayload.Type<SendMadnessS2CPayload> ID = new CustomPacketPayload.Type<>(SEND_MADNESS_ID);

    // Custom codec for Madness list
    public static final StreamCodec<RegistryFriendlyByteBuf, List<Madness>> MADNESS_LIST_CODEC = new StreamCodec<>() {
        @Override
        public List<Madness> decode(RegistryFriendlyByteBuf buf) {
            int size = ByteBufCodecs.VAR_INT.decode(buf);
            List<Madness> list = new ArrayList<>(size);

            for (int i = 0; i < size; i++) {
                Madness.MadnessType type = buf.readEnum(Madness.MadnessType.class);

                Madness madness = switch (type) {
                    case PIXIE -> {
                        Role role = Role.PACKET_CODEC.decode(buf);
                        yield new Madness.PixieMadness(role);
                    }
                    case HARPY -> {
                        UUID playerUuid = UUIDUtil.STREAM_CODEC.decode(buf);
                        yield new Madness.HarpyMadness(playerUuid);
                    }
                    case CERENOVUS -> {
                        Role role = Role.PACKET_CODEC.decode(buf);
                        yield new Madness.CerenovusMadness(role);
                    }
                    case MUTANT -> new Madness.MutantMadness();
                };

                list.add(madness);
            }

            return list;
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, List<Madness> madnesses) {
            ByteBufCodecs.VAR_INT.encode(buf, madnesses.size());

            for (Madness madness : madnesses) {
                buf.writeEnum(madness.getType());

                switch (madness) {
                    case Madness.PixieMadness pixie -> Role.PACKET_CODEC.encode(buf, pixie.townsfolkRole());
                    case Madness.HarpyMadness harpy -> UUIDUtil.STREAM_CODEC.encode(buf, harpy.targetPlayerUuid());
                    case Madness.CerenovusMadness cerenovus -> Role.PACKET_CODEC.encode(buf, cerenovus.madRole());
                    case Madness.MutantMadness mutant -> {} // No additional data
                }
            }
        }
    };

    public static final StreamCodec<RegistryFriendlyByteBuf, SendMadnessS2CPayload> CODEC = StreamCodec.composite(
            MADNESS_LIST_CODEC, SendMadnessS2CPayload::madnesses,
            SendMadnessS2CPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
