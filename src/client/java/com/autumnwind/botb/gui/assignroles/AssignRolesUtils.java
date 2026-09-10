package com.autumnwind.botb.gui.assignroles;

import com.autumnwind.botb.states.ClientState;
import com.autumnwind.botb.states.StorytellerState;
import com.autumnwind.botb.util.*;
import com.autumnwind.botb.util.Script;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Utility methods for the AssignRolesScreen.
 */
public class AssignRolesUtils {

    private AssignRolesUtils() {} // Prevent instantiation

    public static boolean hasFirstNightsAbility(Role role) {
        return NightOrder.getFirstNightOrder().stream()
                .anyMatch(info -> info.isRole() && info.getRole() == role);
    }

    public static boolean hasOtherNightsAbility(Role role) {
        return NightOrder.getOtherNightOrder().stream()
                .anyMatch(info -> info.isRole() && info.getRole() == role);
    }

    /**
     * Checks if a custom role has a first night ability (firstNight > 0).
     */
    public static boolean hasFirstNightsAbility(CustomRole customRole) {
        return customRole != null && customRole.wakesFirstNight();
    }

    /**
     * Checks if a custom role has an other nights ability (otherNight > 0).
     */
    public static boolean hasOtherNightsAbility(CustomRole customRole) {
        return customRole != null && customRole.wakesOtherNights();
    }

    /**
     * Checks if a role is deathBased (presence based on death status instead of marked status).
     */
    public static boolean isRoleDeathBased(Role role) {
        return NightOrder.getOtherNightOrder().stream()
                .filter(info -> info.isRole() && info.getRole() == role)
                .anyMatch(NightOrder.NightOrderInfo::isDeathBased);
    }

    /**
     * Custom roles count as mark-based: script JSON carries no death-based flag, so there's
     * nothing on {@link CustomRole} to read.
     */
    public static boolean isRoleDeathBased(CustomRole customRole) {
        return false;
    }

    /**
     * Checks if a player should be markable based on their assigned role
     * OR any special reminders they have.
     * Players with only deathBased roles are not markable.
     */
    public static boolean isPlayerMarkable(UUID playerUuid) {
        PendingRoleAssignment assignment = StorytellerState.PENDING_ROLES.get(playerUuid);
        if (assignment == null) return false;

        // Check if it's a custom role
        if (assignment.isCustomRole() && assignment.customRole().isPresent()) {
            CustomRole customRole = assignment.customRole().get();
            // Custom roles are markable if they have an other nights ability
            if (hasOtherNightsAbility(customRole) && !isRoleDeathBased(customRole)) {
                return true;
            }
        } else {
            // Check official assigned role
            Role assignedRole = assignment.role();
            if (hasOtherNightsAbility(assignedRole)) {
                // Only markable if not deathBased
                if (!isRoleDeathBased(assignedRole)) {
                    return true;
                }
            }
        }

        // Check special reminders (official and custom roles)
        List<Reminder> reminders = StorytellerState.REMINDERS.getOrDefault(playerUuid, Collections.emptyList());
        Script script = ClientState.currentScript;

        for (Reminder reminder : reminders) {
            if (!isSpecialReminder(reminder)) continue;

            // Check official role reminders
            if (reminder.role().isPresent()) {
                Role associatedRole = reminder.role().get();

                // Markable if the associated role has an ON ability AND is not deathBased
                if (hasOtherNightsAbility(associatedRole) && !isRoleDeathBased(associatedRole)) {
                    return true;
                }

                // Markable if Cannibal and associated role has FN ability AND is not deathBased
                Role assignedRole = assignment.role();
                if (assignedRole == Role.CANNIBAL) {
                    if (hasFirstNightsAbility(associatedRole) && !isRoleDeathBased(associatedRole)) {
                        return true;
                    }
                }
            }

            // Check custom role reminders (only those with customRoleId from role reminder screen)
            if (reminder.isCustomRoleReminder() && reminder.customRoleId().isPresent() && script != null) {
                String customRoleId = reminder.customRoleId().get();
                String reminderText = reminder.text();
                // Check both customRoles and travelers
                if (script.customRoles() != null) {
                    for (CustomRole cr : script.customRoles()) {
                        // Match by ID and verify text matches display name (associated role reminder)
                        if (cr.id().equals(customRoleId) && reminderText.equalsIgnoreCase(cr.getDisplayName())) {
                            // Found the associated custom role - check if it's markable
                            if (hasOtherNightsAbility(cr) && !isRoleDeathBased(cr)) {
                                return true;
                            }
                            break;
                        }
                    }
                }
                // Also check travelers
                if (script.travelers() != null) {
                    for (ScriptRole tr : script.travelers()) {
                        if (tr.getId().equals(customRoleId) && reminderText.equalsIgnoreCase(tr.getDisplayName())) {
                            // Only custom travelers have night abilities defined in the script
                            if (tr instanceof ScriptRole.Custom custom) {
                                if (hasOtherNightsAbility(custom.customRole()) && !isRoleDeathBased(custom.customRole())) {
                                    return true;
                                }
                            }
                            break;
                        }
                    }
                }
            }
        }

        return false; // Not markable otherwise
    }

    public static int getAlignedRoleColor(PendingRoleAssignment assignment) {
        if (assignment == null) {
            return Role.NO_ROLE.getType().getColor(); // White
        }

        boolean isDefaultGood = assignment.isRoleDefaultGood();
        boolean isFinalGood = assignment.isFinalGood();
        RoleType roleType = assignment.getRoleType();
        AlignmentOverride override = assignment.override();

        // Special handling for travelers: show alignment color when forced, purple when default
        if (roleType == RoleType.TRAVELER) {
            return switch (override) {
                case FORCE_GOOD -> RoleType.TOWNSFOLK.getColor(); // Blue
                case FORCE_BAD -> RoleType.MINION.getColor(); // Red
                default -> RoleType.TRAVELER.getColor(); // Purple
            };
        }

        if (isFinalGood && !isDefaultGood) return RoleType.TOWNSFOLK.getColor(); // Townsfolk blue
        if (!isFinalGood && isDefaultGood) return RoleType.MINION.getColor(); // Minion red

        return roleType.getColor();
    }

    public static boolean isSpecialReminder(Reminder reminder) {
        return reminder.isAssociatedRoleReminder(ClientState.currentScript);
    }
}
