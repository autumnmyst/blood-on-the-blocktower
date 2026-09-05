package com.autumnwind.botb.networking;

import com.autumnwind.botb.BloodOnTheBlocktower;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Broadcast when the server resets game state (either normal or hard reset) to clear
 * ClientState.rolesRevealed. Paired with the scoreboard reveal-team wipe so AFTER_END
 * floating role icons disappear at the same moment nametag colors revert to botb_player.
 */
public record ResetRevealActiveS2CPayload() implements CustomPayload {
    public static final CustomPayload.Id<ResetRevealActiveS2CPayload> ID =
            new CustomPayload.Id<>(Identifier.of(BloodOnTheBlocktower.MOD_ID, "reset_reveal_active"));

    public static final PacketCodec<ByteBuf, ResetRevealActiveS2CPayload> CODEC = PacketCodec.of(
            (value, buf) -> {},
            buf -> new ResetRevealActiveS2CPayload()
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
