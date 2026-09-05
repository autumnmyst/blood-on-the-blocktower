package com.autumnwind.botb.networking;

import com.autumnwind.botb.BloodOnTheBlocktower;
import com.autumnwind.botb.util.Madness;
import com.autumnwind.botb.util.Role;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Payload for sending madness data to a player.
 * Contains a list of active madnesses for that player.
 */
public record SendMadnessS2CPayload(List<Madness> madnesses) implements CustomPayload {
    public static final Identifier SEND_MADNESS_ID = Identifier.of(BloodOnTheBlocktower.MOD_ID, "send_madness");
    public static final CustomPayload.Id<SendMadnessS2CPayload> ID = new CustomPayload.Id<>(SEND_MADNESS_ID);

    // Custom codec for Madness list
    public static final PacketCodec<RegistryByteBuf, List<Madness>> MADNESS_LIST_CODEC = new PacketCodec<>() {
        @Override
        public List<Madness> decode(RegistryByteBuf buf) {
            int size = PacketCodecs.VAR_INT.decode(buf);
            List<Madness> list = new ArrayList<>(size);

            for (int i = 0; i < size; i++) {
                Madness.MadnessType type = buf.readEnumConstant(Madness.MadnessType.class);

                Madness madness = switch (type) {
                    case PIXIE -> {
                        Role role = Role.PACKET_CODEC.decode(buf);
                        yield new Madness.PixieMadness(role);
                    }
                    case HARPY -> {
                        UUID playerUuid = Uuids.PACKET_CODEC.decode(buf);
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
        public void encode(RegistryByteBuf buf, List<Madness> madnesses) {
            PacketCodecs.VAR_INT.encode(buf, madnesses.size());

            for (Madness madness : madnesses) {
                buf.writeEnumConstant(madness.getType());

                switch (madness) {
                    case Madness.PixieMadness pixie -> Role.PACKET_CODEC.encode(buf, pixie.townsfolkRole());
                    case Madness.HarpyMadness harpy -> Uuids.PACKET_CODEC.encode(buf, harpy.targetPlayerUuid());
                    case Madness.CerenovusMadness cerenovus -> Role.PACKET_CODEC.encode(buf, cerenovus.madRole());
                    case Madness.MutantMadness mutant -> {} // No additional data
                }
            }
        }
    };

    public static final PacketCodec<RegistryByteBuf, SendMadnessS2CPayload> CODEC = PacketCodec.tuple(
            MADNESS_LIST_CODEC, SendMadnessS2CPayload::madnesses,
            SendMadnessS2CPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
