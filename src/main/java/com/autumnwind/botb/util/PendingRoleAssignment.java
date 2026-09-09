package com.autumnwind.botb.util;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;

import java.util.Optional;
import java.util.List;

/**
 * Represents a role assignment for a player, which can be either an official Role or a CustomRole.
 */
public record PendingRoleAssignment(
    Role role,                      // Official role (Role.NO_ROLE if custom)
    Optional<CustomRole> customRole, // Custom role (empty if official)
    AlignmentOverride override
) {

    // Constructor for official roles (backwards compatible)
    public PendingRoleAssignment(Role role, AlignmentOverride override) {
        this(role, Optional.empty(), override);
    }

    // Constructor for custom roles
    public PendingRoleAssignment(CustomRole customRole, AlignmentOverride override) {
        this(Role.NO_ROLE, Optional.of(customRole), override);
    }

    // Packet codec to send this object over the network
    // For custom roles, we send the custom role ID and rely on the script to resolve it
    public static final PacketCodec<RegistryByteBuf, PendingRoleAssignment> PACKET_CODEC = new PacketCodec<>() {
        @Override
        public PendingRoleAssignment decode(RegistryByteBuf buf) {
            boolean isCustom = PacketCodecs.BOOL.decode(buf);
            AlignmentOverride override = AlignmentOverride.PACKET_CODEC.decode(buf);

            if (isCustom) {
                String customRoleId = PacketCodecs.STRING.decode(buf);
                // Only the id crosses the wire, and the client resolves it against the current
                // script, so a placeholder carries it until then.
                return new PendingRoleAssignment(Role.NO_ROLE, Optional.empty(), override)
                    .withCustomRoleId(customRoleId);
            } else {
                Role role = Role.PACKET_CODEC.decode(buf);
                return new PendingRoleAssignment(role, override);
            }
        }

        @Override
        public void encode(RegistryByteBuf buf, PendingRoleAssignment assignment) {
            boolean isCustom = assignment.isCustomRole();
            PacketCodecs.BOOL.encode(buf, isCustom);
            AlignmentOverride.PACKET_CODEC.encode(buf, assignment.override());

            if (isCustom) {
                PacketCodecs.STRING.encode(buf, assignment.customRole().get().id());
            } else {
                Role.PACKET_CODEC.encode(buf, assignment.role());
            }
        }
    };

    /**
     * Check if this is a custom role assignment.
     */
    public boolean isCustomRole() {
        return customRole.isPresent();
    }

    /**
     * Check if this is an official role assignment.
     */
    public boolean isOfficialRole() {
        return !isCustomRole() && role != Role.NO_ROLE;
    }

    /**
     * Get the role ID (works for both official and custom roles).
     */
    public String getRoleId() {
        if (isCustomRole()) {
            return customRole.get().id();
        }
        return role.getId();
    }

    /**
     * Get the display name (works for both official and custom roles).
     */
    public String getDisplayName() {
        if (isCustomRole()) {
            return customRole.get().getDisplayName();
        }
        return role.getDisplayName();
    }

    /**
     * Get the role type (works for both official and custom roles).
     */
    public RoleType getRoleType() {
        if (isCustomRole()) {
            return customRole.get().team();
        }
        return role.getType();
    }

    /**
     * Check if this role is good by default (works for both official and custom roles).
     */
    public boolean isRoleDefaultGood() {
        if (isCustomRole()) {
            return customRole.get().isDefaultGood();
        }
        return role.isDefaultGood();
    }

    /**
     * Get this assignment as a ScriptRole (works for both official and custom roles).
     * Returns null if no role is assigned.
     */
    public ScriptRole getScriptRole() {
        if (isCustomRole()) {
            return new ScriptRole.Custom(customRole.get());
        }
        if (role != Role.NO_ROLE) {
            return new ScriptRole.Official(role);
        }
        return null;
    }

    /**
     * Calculate the final alignment based on override.
     */
    public boolean isFinalGood() {
        return switch (override) {
            case FORCE_GOOD -> true;
            case FORCE_BAD -> false;
            case DEFAULT -> isRoleDefaultGood();
        };
    }

    /**
     * Create a copy with a custom role ID placeholder.
     * Used during network deserialization - the actual CustomRole is resolved later.
     */
    private PendingRoleAssignment withCustomRoleId(String customRoleId) {
        // Holds the id until the client resolves it against the current script.
        return new PendingRoleAssignment(
            Role.NO_ROLE,
            Optional.of(new CustomRole(
                customRoleId, customRoleId, RoleType.TOWNSFOLK, "", "",
                List.of(), 0, 0, "", "",
                List.of(), List.of(), false, List.of()
            )),
            this.override
        );
    }

    /**
     * Resolve the custom role from a script.
     * Returns this assignment with the full CustomRole data if found in the script.
     */
    public PendingRoleAssignment resolveCustomRole(Script script) {
        if (!isCustomRole() || script == null) {
            return this;
        }

        String id = customRole.get().id();
        Optional<CustomRole> resolved = script.getCustomRole(id);
        if (resolved.isPresent()) {
            return new PendingRoleAssignment(Role.NO_ROLE, resolved, this.override);
        }
        return this;
    }
}
