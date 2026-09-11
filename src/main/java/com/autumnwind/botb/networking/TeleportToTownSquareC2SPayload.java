package com.autumnwind.botb.networking;

import com.autumnwind.botb.BloodOnTheBlocktower;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record TeleportToTownSquareC2SPayload() implements CustomPacketPayload {
    public static final Identifier TELEPORT_TOWN_SQUARE_ID = Identifier.fromNamespaceAndPath(BloodOnTheBlocktower.MOD_ID, "teleport_town_square");
    public static final CustomPacketPayload.Type<TeleportToTownSquareC2SPayload> ID = new CustomPacketPayload.Type<>(TELEPORT_TOWN_SQUARE_ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, TeleportToTownSquareC2SPayload> CODEC = StreamCodec.unit(new TeleportToTownSquareC2SPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
