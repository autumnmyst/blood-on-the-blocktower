package com.autumnwind.botb.networking;

import com.autumnwind.botb.BloodOnTheBlocktower;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Broadcast when the server resets game state (either normal or hard reset) to clear
 * ClientState.rolesRevealed. Paired with the scoreboard reveal-team wipe so AFTER_END
 * floating role icons disappear at the same moment nametag colors revert to botb_player.
 */
public record ResetRevealActiveS2CPayload() implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ResetRevealActiveS2CPayload> ID =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(BloodOnTheBlocktower.MOD_ID, "reset_reveal_active"));

    public static final StreamCodec<ByteBuf, ResetRevealActiveS2CPayload> CODEC = StreamCodec.ofMember(
            (value, buf) -> {},
            buf -> new ResetRevealActiveS2CPayload()
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
