package com.autumnwind.botb.util;

import com.autumnwind.botb.BloodOnTheBlocktower;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * Represents a single visit in the night order.
 * Can be an official Role, a CustomRole, or a StaticAction.
 */
public record RoleVisit(
        Role role,                              // Official role (null for custom/static)
        CustomRole customRole,                  // Custom role (null for official/static)
        NightOrder.StaticAction staticAction,   // Static action (null for roles)
        List<UUID> players,
        boolean seatTeleport,
        String instruction,
        List<Reminder> iconReminders,
        Optional<Role> associatedRole,          // Associated official role
        Optional<CustomRole> associatedCustomRole, // Associated custom role
        boolean triggered,
        double sourceNightOrderIndex,
        Optional<UUID> triggerSourcePlayer
) {

    /**
     * Check if this visit has any associated role (official or custom).
     */
    public boolean hasAssociatedRole() {
        return associatedRole.isPresent() || associatedCustomRole.isPresent();
    }

    /**
     * Get the display name of the associated role (official or custom).
     */
    public String getAssociatedRoleDisplayName() {
        if (associatedRole.isPresent()) {
            return associatedRole.get().getDisplayName();
        }
        if (associatedCustomRole.isPresent()) {
            return associatedCustomRole.get().getDisplayName();
        }
        return null;
    }

    // --- Factory methods for official roles ---

    public static RoleVisit forRole(Role role, List<UUID> players, boolean seatTeleport, String instruction, List<Reminder> iconReminders, Optional<Role> associatedRole, boolean triggered, double sourceNightOrderIndex, Optional<UUID> triggerSourcePlayer) {
        return new RoleVisit(role, null, null, players, seatTeleport, instruction, iconReminders, associatedRole, Optional.empty(), triggered, sourceNightOrderIndex, triggerSourcePlayer);
    }

    public static RoleVisit forRole(Role role, List<UUID> players, boolean seatTeleport, String instruction, List<Reminder> iconReminders, Optional<Role> associatedRole, boolean triggered, double sourceNightOrderIndex) {
        return new RoleVisit(role, null, null, players, seatTeleport, instruction, iconReminders, associatedRole, Optional.empty(), triggered, sourceNightOrderIndex, Optional.empty());
    }

    public static RoleVisit forRole(Role role, List<UUID> players, boolean seatTeleport, String instruction, List<Reminder> iconReminders, Optional<Role> associatedRole, boolean triggered) {
        return new RoleVisit(role, null, null, players, seatTeleport, instruction, iconReminders, associatedRole, Optional.empty(), triggered, -1, Optional.empty());
    }

    public static RoleVisit forRole(Role role, List<UUID> players, boolean seatTeleport, String instruction, List<Reminder> iconReminders, Optional<Role> associatedRole) {
        return new RoleVisit(role, null, null, players, seatTeleport, instruction, iconReminders, associatedRole, Optional.empty(), false, -1, Optional.empty());
    }

    public static RoleVisit forRole(Role role, List<UUID> players, boolean seatTeleport, String instruction, List<Reminder> iconReminders) {
        return new RoleVisit(role, null, null, players, seatTeleport, instruction, iconReminders, Optional.empty(), Optional.empty(), false, -1, Optional.empty());
    }

    public static RoleVisit forRole(Role role, List<UUID> players, boolean seatTeleport, String instruction) {
        return new RoleVisit(role, null, null, players, seatTeleport, instruction, Collections.emptyList(), Optional.empty(), Optional.empty(), false, -1, Optional.empty());
    }

    // --- Factory methods for custom roles ---

    public static RoleVisit forCustomRole(CustomRole customRole, List<UUID> players, boolean seatTeleport, String instruction, List<Reminder> iconReminders, boolean triggered, double sourceNightOrderIndex, Optional<UUID> triggerSourcePlayer) {
        return new RoleVisit(null, customRole, null, players, seatTeleport, instruction, iconReminders, Optional.empty(), Optional.empty(), triggered, sourceNightOrderIndex, triggerSourcePlayer);
    }

    public static RoleVisit forCustomRole(CustomRole customRole, List<UUID> players, boolean seatTeleport, String instruction, List<Reminder> iconReminders, boolean triggered, double sourceNightOrderIndex) {
        return new RoleVisit(null, customRole, null, players, seatTeleport, instruction, iconReminders, Optional.empty(), Optional.empty(), triggered, sourceNightOrderIndex, Optional.empty());
    }

    public static RoleVisit forCustomRole(CustomRole customRole, List<UUID> players, boolean seatTeleport, String instruction, List<Reminder> iconReminders) {
        return new RoleVisit(null, customRole, null, players, seatTeleport, instruction, iconReminders, Optional.empty(), Optional.empty(), false, -1, Optional.empty());
    }

    public static RoleVisit forCustomRole(CustomRole customRole, List<UUID> players, boolean seatTeleport, String instruction) {
        return new RoleVisit(null, customRole, null, players, seatTeleport, instruction, Collections.emptyList(), Optional.empty(), Optional.empty(), false, -1, Optional.empty());
    }

    // Factory for official role with associated custom role (e.g., Philosopher with custom role ability)
    public static RoleVisit forAssociatedCustomRole(Role assignedRole, CustomRole associatedCustomRole, List<UUID> players, boolean seatTeleport, String instruction, List<Reminder> iconReminders, double sourceNightOrderIndex) {
        return new RoleVisit(assignedRole, null, null, players, seatTeleport, instruction, iconReminders, Optional.empty(), Optional.of(associatedCustomRole), false, sourceNightOrderIndex, Optional.empty());
    }

    // Factory for custom role with associated custom role (e.g., custom Philosopher-like role with custom role ability)
    public static RoleVisit forCustomRoleWithAssociatedCustomRole(CustomRole assignedCustomRole, CustomRole associatedCustomRole, List<UUID> players, boolean seatTeleport, String instruction, List<Reminder> iconReminders, double sourceNightOrderIndex) {
        return new RoleVisit(null, assignedCustomRole, null, players, seatTeleport, instruction, iconReminders, Optional.empty(), Optional.of(associatedCustomRole), false, sourceNightOrderIndex, Optional.empty());
    }

    // Factory for custom role with associated official role (e.g., custom Philosopher-like role with official role ability)
    public static RoleVisit forCustomRoleWithAssociatedRole(CustomRole assignedCustomRole, Role associatedRole, List<UUID> players, boolean seatTeleport, String instruction, List<Reminder> iconReminders, double sourceNightOrderIndex) {
        return new RoleVisit(null, assignedCustomRole, null, players, seatTeleport, instruction, iconReminders, Optional.of(associatedRole), Optional.empty(), false, sourceNightOrderIndex, Optional.empty());
    }

    // --- Factory methods for static actions ---

    public static RoleVisit forStatic(NightOrder.StaticAction action, List<UUID> players, boolean seatTeleport, String instruction, List<Reminder> iconReminders, double sourceNightOrderIndex) {
        return new RoleVisit(null, null, action, players, seatTeleport, instruction, iconReminders, Optional.empty(), Optional.empty(), false, sourceNightOrderIndex, Optional.empty());
    }

    public static RoleVisit forStatic(NightOrder.StaticAction action, List<UUID> players, boolean seatTeleport, String instruction, List<Reminder> iconReminders) {
        return new RoleVisit(null, null, action, players, seatTeleport, instruction, iconReminders, Optional.empty(), Optional.empty(), false, -1, Optional.empty());
    }

    public static RoleVisit forStatic(NightOrder.StaticAction action, List<UUID> players, boolean seatTeleport, String instruction) {
        return new RoleVisit(null, null, action, players, seatTeleport, instruction, Collections.emptyList(), Optional.empty(), Optional.empty(), false, -1, Optional.empty());
    }

    // --- Type checking methods ---

    public boolean isRole() {
        return role != null;
    }

    public boolean isRole(Role r) {
        return role == r;
    }

    public boolean isCustomRole() {
        return customRole != null;
    }

    public boolean isCustomRole(String id) {
        return customRole != null && customRole.id().equals(id);
    }

    public boolean isAnyRole() {
        return role != null || customRole != null;
    }

    // --- Display methods ---

    public Component getName() {
        if (isRole()) {
            return Component.literal(role.getDisplayName());
        }
        if (isCustomRole()) {
            return Component.literal(customRole.getDisplayName());
        }
        if (staticAction != null) {
            return switch (staticAction) {
                case DAWN -> Component.translatable("nightorder.blood-on-the-blocktower.static.dawn");
                case NOMINATIONS -> Component.translatable("nightorder.blood-on-the-blocktower.static.nominations");
                case DUSK -> Component.translatable("nightorder.blood-on-the-blocktower.static.dusk");
                case MINION_INFO -> Component.translatable("nightorder.blood-on-the-blocktower.static.minion_info");
                case DEMON_INFO -> Component.translatable("nightorder.blood-on-the-blocktower.static.demon_info");
            };
        }
        return Component.translatable("nightorder.blood-on-the-blocktower.static.unknown");
    }

    public Identifier getIcon() {
        if (isRole()) {
            return role.getIcon();
        }

        if (isCustomRole()) {
            // Get custom role icon via UrlTextureLoader
            // This will return placeholder if not yet loaded
            return UrlTextureLoader.getTexture(customRole);
        }

        if (staticAction != null) {
            return switch (staticAction) {
                case DAWN -> Identifier.fromNamespaceAndPath(BloodOnTheBlocktower.MOD_ID, "textures/icons/dawn.png");
                case NOMINATIONS -> Identifier.fromNamespaceAndPath(BloodOnTheBlocktower.MOD_ID, "textures/icons/nominations.png");
                case DUSK -> Identifier.fromNamespaceAndPath(BloodOnTheBlocktower.MOD_ID, "textures/icons/dusk.png");
                case MINION_INFO -> Identifier.fromNamespaceAndPath(BloodOnTheBlocktower.MOD_ID, "textures/icons/minion_info.png");
                case DEMON_INFO -> Identifier.fromNamespaceAndPath(BloodOnTheBlocktower.MOD_ID, "textures/icons/demon_info.png");
            };
        }

        // Fallback: prevent crash
        return Reminder.CUSTOM_ICON;
    }

    /**
     * Get the role ID for this visit (works for both official and custom roles).
     */
    public String getRoleId() {
        if (isRole()) {
            return role.getId();
        }
        if (isCustomRole()) {
            return customRole.id();
        }
        return null;
    }

    /**
     * Get the team for this visit.
     */
    public RoleType getTeam() {
        if (isRole()) {
            return role.getType();
        }
        if (isCustomRole()) {
            return customRole.team();
        }
        return RoleType.NONE;
    }
}
