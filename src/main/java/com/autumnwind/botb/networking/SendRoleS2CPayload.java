package com.autumnwind.botb.networking;

import com.autumnwind.botb.BloodOnTheBlocktower;
import com.autumnwind.botb.util.PendingRoleAssignment;
import com.autumnwind.botb.util.Role;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import com.autumnwind.botb.util.AlignmentOverride;

/**
 * S2C payload for sending role assignments to players.
 * Supports both official roles and custom roles via PendingRoleAssignment.
 */
public record SendRoleS2CPayload(PendingRoleAssignment assignment, int activePlayerCount, int travelerCount, boolean silent) implements CustomPacketPayload {
    public static final ResourceLocation SEND_ROLE_ID = ResourceLocation.fromNamespaceAndPath(BloodOnTheBlocktower.MOD_ID, "send_role");
    public static final CustomPacketPayload.Type<SendRoleS2CPayload> ID = new CustomPacketPayload.Type<>(SEND_ROLE_ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, SendRoleS2CPayload> CODEC = StreamCodec.composite(
            PendingRoleAssignment.PACKET_CODEC, SendRoleS2CPayload::assignment,
            ByteBufCodecs.VAR_INT, SendRoleS2CPayload::activePlayerCount,
            ByteBufCodecs.VAR_INT, SendRoleS2CPayload::travelerCount,
            ByteBufCodecs.BOOL, SendRoleS2CPayload::silent,
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
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}