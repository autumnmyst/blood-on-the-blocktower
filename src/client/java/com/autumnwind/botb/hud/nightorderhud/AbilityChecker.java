package com.autumnwind.botb.hud.nightorderhud;

import com.autumnwind.botb.states.ClientState;
import com.autumnwind.botb.states.StorytellerState;
import com.autumnwind.botb.util.*;

import java.util.*;

/**
 * Handles checking player abilities, active status, and ability blocking.
 */
public class AbilityChecker {

    /**
     * Checks if a player should be active for a visit based on whether the role is deathBased and triggered.
     * If deathBased AND triggered: checks death status (dead = active) - triggers on death
     * If deathBased AND NOT triggered: checks death status (alive = active) - loses ability when dead
     * If not deathBased: checks marked status (marked = active)
     * Exception: If player has Vigormortis "Has Ability" reminder, deathBased roles are always active
     */
    public static boolean isPlayerActiveForVisit(UUID uuid, boolean deathBased, boolean isTriggered) {
        // Preacher "No Ability" overrides everything - player never active (disables all abilities)
        if (hasPreacherNoAbility(uuid)) {
            return false;
        }

        // Check if their role ability is blocked by "No Ability" reminder
        // This check applies to both death-based and mark-based roles
        PendingRoleAssignment assignment = StorytellerState.PENDING_ROLES.get(uuid);
        if (assignment != null && isRoleAbilityBlocked(uuid, assignment.role())) {
            return false;
        }

        // Check for Vigormortis "Has Ability" reminder
        boolean hasVigormortisAbility = StorytellerState.REMINDERS.getOrDefault(uuid, Collections.emptyList()).stream()
                .anyMatch(r -> r.text().equals(Reminders.HAS_ABILITY) && r.role().isPresent() && r.role().get() == Role.VIGORMORTIS);

        // Check for Bone Collector "Has Ability" reminder (traveler that gives dead players their ability back once)
        boolean hasBoneCollectorAbility = StorytellerState.REMINDERS.getOrDefault(uuid, Collections.emptyList()).stream()
                .anyMatch(r -> r.text().equals(Reminders.HAS_ABILITY) && r.role().isPresent() && r.role().get() == Role.BONE_COLLECTOR);

        if (deathBased) {
            boolean isDead = ClientState.playerDeathStatus.getOrDefault(uuid, false);

            // If has Vigormortis or Bone Collector ability, always active (alive or dead)
            if (hasVigormortisAbility || hasBoneCollectorAbility) {
                return true;
            }

            // If triggered: active when dead (triggers on death)
            // If not triggered: active when alive (loses ability on death)
            return isTriggered ? isDead : !isDead;
        } else {
            // For non-death-based other nights roles, check if marked
            if (!StorytellerState.markedPlayers.contains(uuid)) {
                return false;
            }

            return true;
        }
    }

    /**
     * Checks if a player has Preacher "No Ability" reminder (disables minion abilities).
     */
    public static boolean hasPreacherNoAbility(UUID uuid) {
        return StorytellerState.REMINDERS.getOrDefault(uuid, Collections.emptyList()).stream()
                .anyMatch(r -> r.text().equals(Reminders.NO_ABILITY) && r.role().isPresent() && r.role().get() == Role.PREACHER);
    }

    /**
     * Checks if a player has their specific role's "No Ability" reminder.
     * This is used for once-per-game abilities - when they have this reminder, they've used their ability.
     * @param uuid The player to check
     * @param role The role to check for (must match the reminder's associated role)
     * @return true if the player has this role's "No Ability" reminder
     */
    public static boolean hasRoleNoAbility(UUID uuid, Role role) {
        return StorytellerState.REMINDERS.getOrDefault(uuid, Collections.emptyList()).stream()
                .anyMatch(r -> r.text().equals(Reminders.NO_ABILITY) && r.role().isPresent() && r.role().get() == role);
    }

    /**
     * Checks if a player has a generic "No Ability" reminder (no associated role).
     * This blocks ALL role visits for the player's assigned role and associated roles.
     * @param uuid The player to check
     * @return true if the player has a generic "No Ability" reminder
     */
    public static boolean hasGenericNoAbility(UUID uuid) {
        return StorytellerState.REMINDERS.getOrDefault(uuid, Collections.emptyList()).stream()
                .anyMatch(r -> r.text().equals(Reminders.NO_ABILITY) && r.role().isEmpty());
    }

    /**
     * Checks if a player should be blocked from having their role ability due to "No Ability" reminders.
     * @param uuid The player to check
     * @param role The role ability being checked
     * @return true if the player's ability for this role should be blocked
     */
    public static boolean isRoleAbilityBlocked(UUID uuid, Role role) {
        // Preacher blocks minion abilities
        if (role.getType() == RoleType.MINION && hasPreacherNoAbility(uuid)) {
            return true;
        }
        // Specific role's "No Ability" blocks that role
        if (hasRoleNoAbility(uuid, role)) {
            return true;
        }
        // Generic "No Ability" blocks all roles
        if (hasGenericNoAbility(uuid)) {
            return true;
        }
        return false;
    }

    /**
     * Checks if a player's death qualifies for a specific death trigger type.
     * Only called when deathBased=true AND triggered=true.
     * @param uuid The player who died
     * @param deathTriggerType The type of death trigger to check (ANY, NIGHT, or DEMON)
     * @return true if the death qualifies for the given trigger type
     */
    public static boolean doesDeathQualifyForTrigger(UUID uuid, NightOrder.DeathTriggerType deathTriggerType) {
        if (deathTriggerType == NightOrder.DeathTriggerType.ANY) {
            return true; // Always qualifies
        }

        // Check if it's currently night (day != night means nighttime)
        boolean isNighttime = ClientState.currentDay != ClientState.currentNight;

        if (!isNighttime) {
            return false; // NIGHT and DEMON types only trigger at night
        }

        if (deathTriggerType == NightOrder.DeathTriggerType.NIGHT) {
            return true; // Qualifies for night-only trigger
        }

        if (deathTriggerType == NightOrder.DeathTriggerType.DEMON) {
            // Check for demon "Dead" reminder OR al_hadikhia "1"/"2"/"3" reminder
            List<Reminder> reminders = StorytellerState.REMINDERS.getOrDefault(uuid, Collections.emptyList());

            // Check for demon "Dead" reminder
            boolean hasDemonDeadReminder = reminders.stream()
                    .anyMatch(r -> r.text().equals(Reminders.DEAD) &&
                              r.role().isPresent() &&
                              r.role().get().getType() == RoleType.DEMON);

            if (hasDemonDeadReminder) {
                return true;
            }

            // Check for al_hadikhia "1", "2", or "3" reminder
            boolean hasAlHadikhiaReminder = reminders.stream()
                    .anyMatch(r -> (r.text().equals(Reminders.ONE) || r.text().equals(Reminders.TWO) || r.text().equals(Reminders.THREE)) &&
                              r.role().isPresent() &&
                              r.role().get() == Role.AL_HADIKHIA);

            return hasAlHadikhiaReminder;
        }

        return false;
    }
}
