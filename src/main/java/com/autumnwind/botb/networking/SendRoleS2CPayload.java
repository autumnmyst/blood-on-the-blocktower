package com.autumnwind.botb.networking;

import com.autumnwind.botb.BloodOnTheBlocktower;
import com.autumnwind.botb.util.PendingRoleAssignment;
import com.autumnwind.botb.util.Role;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import com.autumnwind.botb.util.AlignmentOverride;

/**
 * S2C payload for sending role assignments to players.
 * Supports both official roles and custom roles via PendingRoleAssignment.
 */
public record SendRoleS2CPayload(PendingRoleAssignment assignment, int activePlayerCount, int travelerCount, boolean silent) implements CustomPayload {
    public static final Identifier SEND_ROLE_ID = Identifier.of(BloodOnTheBlocktower.MOD_ID, "send_role");
    public static final CustomPayload.Id<SendRoleS2CPayload> ID = new CustomPayload.Id<>(SEND_ROLE_ID);

    public static final PacketCodec<RegistryByteBuf, SendRoleS2CPayload> CODEC = PacketCodec.tuple(
            PendingRoleAssignment.PACKET_CODEC, SendRoleS2CPayload::assignment,
            PacketCodecs.VAR_INT, SendRoleS2CPayload::activePlayerCount,
            PacketCodecs.VAR_INT, SendRoleS2CPayload::travelerCount,
            PacketCodecs.BOOL, SendRoleS2CPayload::silent,
            SendRoleS2CPayload::new
    );

    // Convenience accessors for backwards compatibility
    public Role role() {
        return assignment.role();
    }

    // Factory for official roles (backwards compatibility)
    public static SendRoleS2CPayload ofRole(Role role, boolean isGood, int activePlayerCount, int travelerCount, boolean silent) {
        return new SendRoleS2CPayload(
                new PendingRoleAssignment(role, isGood ? AlignmentOverride.FORCE_GOOD : AlignmentOverride.FORCE_BAD),
                activePlayerCount,
                travelerCount,
                silent
        );
    }

    // Factory for PendingRoleAssignment
    public static SendRoleS2CPayload ofAssignment(PendingRoleAssignment assignment, int activePlayerCount, int travelerCount, boolean silent) {
        return new SendRoleS2CPayload(assignment, activePlayerCount, travelerCount, silent);
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}