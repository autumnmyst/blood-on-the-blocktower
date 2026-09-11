package com.autumnwind.botb.networking;

import com.autumnwind.botb.BloodOnTheBlocktower;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Every custom player name, sent on join and whenever one changes. */
public record CustomNamesS2CPayload(Map<UUID, String> names) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<CustomNamesS2CPayload> ID =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(BloodOnTheBlocktower.MOD_ID, "custom_names"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CustomNamesS2CPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.map(HashMap::new, UUIDUtil.STREAM_CODEC, ByteBufCodecs.STRING_UTF8), CustomNamesS2CPayload::names,
            CustomNamesS2CPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
