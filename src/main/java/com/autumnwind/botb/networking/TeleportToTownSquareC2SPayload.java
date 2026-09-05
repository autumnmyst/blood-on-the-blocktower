package com.autumnwind.botb.networking;

import com.autumnwind.botb.BloodOnTheBlocktower;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record TeleportToTownSquareC2SPayload() implements CustomPayload {
    public static final Identifier TELEPORT_TOWN_SQUARE_ID = Identifier.of(BloodOnTheBlocktower.MOD_ID, "teleport_town_square");
    public static final CustomPayload.Id<TeleportToTownSquareC2SPayload> ID = new CustomPayload.Id<>(TELEPORT_TOWN_SQUARE_ID);

    public static final PacketCodec<RegistryByteBuf, TeleportToTownSquareC2SPayload> CODEC = PacketCodec.unit(new TeleportToTownSquareC2SPayload());

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
