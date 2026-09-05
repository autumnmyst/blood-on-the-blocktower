package com.autumnwind.botb.networking;

import com.autumnwind.botb.BloodOnTheBlocktower;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * S2C payload to trigger the game end animation and sound.
 */
public record GameEndAnimationS2CPayload(boolean goodWins) implements CustomPayload {
    public static final Identifier GAME_END_ANIMATION_ID = Identifier.of(BloodOnTheBlocktower.MOD_ID, "game_end_animation");
    public static final CustomPayload.Id<GameEndAnimationS2CPayload> ID = new CustomPayload.Id<>(GAME_END_ANIMATION_ID);

    public static final PacketCodec<RegistryByteBuf, GameEndAnimationS2CPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.BOOL, GameEndAnimationS2CPayload::goodWins,
            GameEndAnimationS2CPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
