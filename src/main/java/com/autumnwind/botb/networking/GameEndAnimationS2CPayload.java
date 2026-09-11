package com.autumnwind.botb.networking;

import com.autumnwind.botb.BloodOnTheBlocktower;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * S2C payload to trigger the game end animation and sound.
 */
public record GameEndAnimationS2CPayload(boolean goodWins) implements CustomPacketPayload {
    public static final Identifier GAME_END_ANIMATION_ID = Identifier.fromNamespaceAndPath(BloodOnTheBlocktower.MOD_ID, "game_end_animation");
    public static final CustomPacketPayload.Type<GameEndAnimationS2CPayload> ID = new CustomPacketPayload.Type<>(GAME_END_ANIMATION_ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, GameEndAnimationS2CPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, GameEndAnimationS2CPayload::goodWins,
            GameEndAnimationS2CPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
