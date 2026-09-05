package com.autumnwind.botb.networking;

import com.autumnwind.botb.BloodOnTheBlocktower;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Every custom player name, sent on join and whenever one changes. */
public record CustomNamesS2CPayload(Map<UUID, String> names) implements CustomPayload {
    public static final CustomPayload.Id<CustomNamesS2CPayload> ID =
            new CustomPayload.Id<>(Identifier.of(BloodOnTheBlocktower.MOD_ID, "custom_names"));

    public static final PacketCodec<RegistryByteBuf, CustomNamesS2CPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.map(HashMap::new, Uuids.PACKET_CODEC, PacketCodecs.STRING), CustomNamesS2CPayload::names,
            CustomNamesS2CPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
