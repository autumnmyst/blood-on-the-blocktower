package com.autumnwind.botb.hud.nightorderhud;

import com.autumnwind.botb.states.ClientState;
import com.autumnwind.botb.states.StorytellerState;
import com.autumnwind.botb.util.*;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Helper methods for analyzing roles, associated roles, and reminders.
 */
public class RoleHelpers {

    /**
     * Checks if a player has any triggered role (either assigned or associated).
     * Used to determine if player should stay marked when killed.
     */
    public static boolean hasAnyTriggeredRole(UUID uuid) {
        // Check assigned role
        PendingRoleAssignment assignment = StorytellerState.PENDING_ROLES.get(uuid);
        if (assignment != null) {
            Role assignedRole = assignment.role();
            if (isTriggeredRole(assignedRole)) {
                return true;
            }
        }

        // Check associated roles
        List<Reminder> associatedReminders = getAssociatedRoleReminders(uuid);
        for (Reminder reminder : associatedReminders) {
            if (reminder.role().isPresent() && isTriggeredRole(reminder.role().get())) {
                return true;
            }
        }

        return false;
    }

    /**
     * Finds all "Special Reminders" on a player that grant associated roles/abilities.
     */
    public static List<Reminder> getAssociatedRoleReminders(UUID uuid) {
        List<Reminder> allReminders = StorytellerState.REMINDERS.getOrDefault(uuid, Collections.emptyList());
        List<Reminder> specialReminders = new ArrayList<>();

        // Get the player's assigned role to prevent self-association (e.g., Hermit receiving Hermit)
        PendingRoleAssignment assignment = StorytellerState.PENDING_ROLES.get(uuid);
        Role assignedRole = (assignment != null) ? assignment.role() : null;

        // Check if player has Pixie associated role and Has Ability reminder
        boolean hasPixieAssociated = allReminders.stream()
                .anyMatch(r -> r.text().equals("PIXIE") && r.role().isPresent() && r.role().get() == Role.PIXIE);
        boolean hasPixieAbility = allReminders.stream()
                .anyMatch(r -> r.text().equals("Has Ability") && r.role().isPresent() && r.role().get() == Role.PIXIE);

        for (Reminder r : allReminders) {
            String text = r.text();

            // Case 1: Standard associated role with role icon - text=[RoleName], role=[Role]
            if (r.role().isPresent()) {
                Role role = r.role().get();

                if (text.equals(role.name().replace('_', ' '))) {
                    RoleType type = role.getType();
                    if (type == RoleType.TOWNSFOLK || type == RoleType.OUTSIDER ||
                            type == RoleType.MINION || type == RoleType.DEMON) {

                        // Prevent self-association (e.g., Hermit receiving Hermit)
                        if (assignedRole != null && role == assignedRole) {
                            continue; // Skip self-association
                        }

                        // Block townsfolk associated roles if player has Pixie associated but no Has Ability
                        if (hasPixieAssociated && !hasPixieAbility && type == RoleType.TOWNSFOLK && role != Role.PIXIE) {
                            continue; // Skip this townsfolk associated role
                        }

                        specialReminders.add(r);
                    }
                }
            }
            // Case 2: All-caps custom reminder matching a role name
            else if (text.equals(text.toUpperCase(Locale.ROOT)) && text.length() > 0) {
                // Try to find a matching role (case-insensitive with trim)
                for (Role role : Role.values()) {
                    if (text.trim().equalsIgnoreCase(role.name().replace('_', ' '))) {
                        RoleType type = role.getType();
                        if (type == RoleType.TOWNSFOLK || type == RoleType.OUTSIDER ||
                                type == RoleType.MINION || type == RoleType.DEMON) {

                            // Prevent self-association (e.g., Hermit receiving Hermit)
                            if (assignedRole != null && role == assignedRole) {
                                break; // Skip self-association and stop checking
                            }

                            // Block townsfolk associated roles if player has Pixie associated but no Has Ability
                            if (hasPixieAssociated && !hasPixieAbility && type == RoleType.TOWNSFOLK && role != Role.PIXIE) {
                                break; // Skip this townsfolk associated role and stop checking
                            }

                            // Create a new reminder with the role icon
                            specialReminders.add(new Reminder(text, Optional.of(role)));
                        }
                        break; // Found a match, no need to check other roles
                    }
                }
            }
        }
        return specialReminders;
    }

    /**
     * Gets all storyteller-minion reminders (format: "ST: [MinionName]")
     * from all players in the game. Returns a map of minion role to list of player UUIDs who have that reminder.
     */
    public static Map<Role, List<UUID>> getStorytellerMinionReminders() {
        Map<Role, List<UUID>> storytellerMinions = new HashMap<>();

        for (Map.Entry<UUID, List<Reminder>> entry : StorytellerState.REMINDERS.entrySet()) {
            UUID playerUuid = entry.getKey();

            for (Reminder r : entry.getValue()) {
                if (r.isStorytellerMinionReminder() && r.role().isPresent()) {
                    Role minionRole = r.role().get();
                    storytellerMinions.computeIfAbsent(minionRole, k -> new ArrayList<>()).add(playerUuid);
                }
            }
        }

        return storytellerMinions;
    }

    /** Whether a player has an "ST: [Minion]" reminder for this minion. */
    public static boolean hasStorytellerMinionReminder(UUID uuid, Role minion) {
        return StorytellerState.REMINDERS.getOrDefault(uuid, Collections.emptyList()).stream()
                .anyMatch(r -> r.isStorytellerMinionReminder() && r.role().get() == minion);
    }

    /**
     * Gets standard "Drunk" and "Poisoned" reminders for a single player.
     * Excludes Minstrel's "Everyone Is Drunk" for the player who has it.
     */
    public static Set<Reminder> getStandardIconReminders(UUID uuid, UUID minstrelPlayer) {
        Set<Reminder> reminders = new HashSet<>();
        boolean isMinstrel = minstrelPlayer != null && minstrelPlayer.equals(uuid);

        for (Reminder r : StorytellerState.REMINDERS.getOrDefault(uuid, Collections.emptyList())) {
            if (isIdentityReminder(r)) continue;
            String lowerText = r.text().toLowerCase(Locale.ROOT);

            if (lowerText.contains("drunk")) {
                if (lowerText.equals("everyone is drunk") && isMinstrel) {
                    continue; // Minstrel doesn't get their own drunk token
                }
                reminders.add(new Reminder("Drunk", Optional.empty()));
            }
            if (lowerText.contains("poisoned")) {
                reminders.add(new Reminder("Poisoned", Optional.empty()));
            }
        }

        // Add Minstrel drunk if player is NOT the minstrel
        if (minstrelPlayer != null && !isMinstrel) {
            reminders.add(new Reminder("Drunk", Optional.empty()));
        }

        return reminders;
    }

    public static Reminder getVortoxReminder() {
        return new Reminder("Vortox Effect", Optional.of(Role.VORTOX));
    }

    public static boolean hasFirstNightsAbility(Role role) {
        return NightOrder.getFirstNightOrder().stream()
                .anyMatch(info -> info.isRole() && info.getRole() == role);
    }

    public static boolean hasOtherNightsAbility(Role role) {
        return NightOrder.getOtherNightOrder().stream()
                .anyMatch(info -> info.isRole() && info.getRole() == role);
    }

    public static boolean isTriggeredRole(Role role) {
        return NightOrder.getOtherNightOrder().stream()
                .anyMatch(info -> info.isRole() && info.getRole() == role && info.isTriggered());
    }

    /**
     * Checks if a reminder is a "Special Reminder" that grants an ability (associated role).
     */
    public static boolean isSpecialReminder(Reminder reminder) {
        if (reminder.role().isEmpty()) return false; // Must have a role

        Role role = reminder.role().get();
        String text = reminder.text();

        // Standard associated role: text=[RoleName], role=[Role]
        if (text.equals(role.name().replace('_', ' '))) {
            RoleType type = role.getType();
            // Check if it's an ability-granting type
            return type == RoleType.TOWNSFOLK || type == RoleType.OUTSIDER ||
                    type == RoleType.MINION || type == RoleType.DEMON;
        }

        return false;
    }

    /**
     * Gets the first night instructions for a given role.
     * Returns empty string if the role has no first night ability.
     */
    public static String getFirstNightInstructions(Role role) {
        return NightOrder.getFirstNightOrder().stream()
                .filter(info -> info.isRole() && info.getRole() == role)
                .findFirst()
                .map(NightOrder.NightOrderInfo::roleInstructions)
                .orElse("");
    }

    /**
     * Appends icon reminder instructions to the base instruction string.
     * Princess "Doesn't Kill" reminder gets special instruction text.
     */
    public static String appendIconReminderInstructions(String baseInstructions, List<Reminder> iconReminders) {
        if (iconReminders == null || iconReminders.isEmpty()) {
            return baseInstructions;
        }

        StringBuilder result = new StringBuilder(baseInstructions);
        for (Reminder reminder : iconReminders) {
            // Skip if not a role-based reminder
            if (reminder.role().isEmpty()) continue;

            Role role = reminder.role().get();

            // Special case: Princess "Doesn't Kill" reminder. Two newlines = a blank
            // line before each modifier so the storyteller can scan them at a glance.
            if (role == Role.PRINCESS && reminder.text().equals("Doesn't Kill")) {
                result.append("\n\n").append("PRINCESS: The demon doesn't kill tonight");
                continue;
            }

            // Special case: Xaan "X" icon on townsfolk visits
            if (role == Role.XAAN && reminder.text().equals("X")) {
                result.append("\n\n").append("XAAN: All Townsfolk are poisoned until dusk");
                continue;
            }

            // Special case: Riot day modifier on Nominations
            if (role == Role.RIOT && reminder.text().equals("Riot")) {
                result.append("\n\n").append("RIOT: Each nominee dies immediately, then must nominate an alive player right away (3... 2... 1...). If nobody nominates, the Storyteller nominates. Good wins when all Riot players are dead.");
                continue;
            }

            // Special case: Organ Grinder mode modifier on Nominations
            if (role == Role.ORGAN_GRINDER && reminder.text().equals("Organ Grinder")) {
                result.append("\n\n").append("ORGAN GRINDER: Votes are secret. Players are blinded while voting and see only \"???\" afterwards; you see the real count and marked player. Do not reveal results until nominations are over.");
                continue;
            }

            // Special case: Bishop mode modifier on Nominations
            if (role == Role.BISHOP && reminder.text().equals("Bishop")) {
                result.append("\n\n").append("BISHOP: Only you can nominate. Nominate at least one player of the opposite alignment to the Bishop each day.");
                continue;
            }

            // Special case: Toymaker "Final Night: No Attack" on the demon's visit
            if (role == Role.TOYMAKER && reminder.text().equals("Final Night: No Attack")) {
                result.append("\n\n").append("TOYMAKER: The Demon does not attack tonight.");
                continue;
            }

            // Special case: Buddhist modifier on Dawn
            if (role == Role.BUDDHIST && reminder.text().equals("Buddhist")) {
                result.append("\n\n").append("BUDDHIST: For the first 2 minutes of the day, veteran players may not talk.");
                continue;
            }

            // Special case: Legion modifier on Nominations
            if (role == Role.LEGION && reminder.text().equals("Legion")) {
                result.append("\n\n").append("LEGION: A vote where only evil players voted counts as zero. Players don't see this; only you are told.");
                continue;
            }

            // Get first night instructions for this role
            String roleInstructions = getFirstNightInstructions(role);
            if (!roleInstructions.isBlank()) {
                result.append("\n\n").append(role.getDisplayName().toUpperCase(Locale.ROOT)).append(": ").append(roleInstructions);
            }
        }

        return result.toString();
    }

    /**
     * Finds the minstrel player (player with "Everyone Is Drunk" reminder).
     */
    public static UUID findMinstrelPlayer() {
        return StorytellerState.REMINDERS.entrySet().stream()
                .filter(e -> e.getValue().stream().anyMatch(r -> r.text().equals("Everyone Is Drunk")))
                .map(Map.Entry::getKey)
                .findFirst().orElse(null);
    }

    /**
     * Checks if the Vortox ability is in play (any holder with outward effect).
     */
    public static boolean isVortoxInPlay() {
        return isRoleAbilityInPlay(Role.VORTOX);
    }

    /**
     * Checks if a player is drunk or poisoned: any reminder containing "drunk" or "poisoned",
     * or Minstrel drunkenness (which covers everyone except the Minstrel). Associated-role
     * reminders such as a Hermit's DRUNK are identities, not impairment, and the Minstrel's own
     * "Everyone Is Drunk" reminder doesn't count against them.
     */
    public static boolean isDroisoned(UUID uuid) {
        UUID minstrelPlayer = findMinstrelPlayer();
        if (minstrelPlayer != null && !minstrelPlayer.equals(uuid)) {
            return true;
        }
        if (isAssignedTownsfolk(uuid) && isXaanPoisonActive()) {
            return true;
        }
        for (Reminder r : StorytellerState.REMINDERS.getOrDefault(uuid, Collections.emptyList())) {
            if (isIdentityReminder(r)) continue;
            String lowerText = r.text().toLowerCase(Locale.ROOT);
            if (lowerText.equals("everyone is drunk")) continue;
            if (lowerText.contains("drunk") || lowerText.contains("poisoned")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if a player's associated role is part of a false identity rather than a real
     * ability. Fake: townsfolk and minions on a Drunk; townsfolk, minions, and demons on a
     * Hermit; townsfolk, minions, and outsiders on a Marionette; demons on a Lunatic. Always
     * false for assigned roles.
     */
    public static boolean isFakeRoleHolder(UUID uuid, Role role) {
        PendingRoleAssignment assignment = StorytellerState.PENDING_ROLES.get(uuid);
        if (assignment == null || assignment.role() == role) {
            return false;
        }
        RoleType type = role.getType();
        return switch (assignment.role()) {
            case DRUNK -> type == RoleType.TOWNSFOLK || type == RoleType.MINION;
            case HERMIT -> type == RoleType.TOWNSFOLK || type == RoleType.MINION || type == RoleType.DEMON;
            case MARIONETTE -> type == RoleType.TOWNSFOLK || type == RoleType.MINION || type == RoleType.OUTSIDER;
            case LUNATIC -> type == RoleType.DEMON;
            default -> false;
        };
    }

    /**
     * Night visits that only remind the storyteller to act; no player is woken, so a droisoned
     * or fake holder gets no visit at all.
     */
    public static boolean isStorytellerActionVisit(Role role, boolean isFirstNight) {
        if (isFirstNight) {
            return role == Role.VIZIER || role == Role.LEVIATHAN || role == Role.CULT_LEADER;
        }
        return switch (role) {
            case GOSSIP, TINKER, MOONCHILD, MEZEPHELES, CULT_LEADER, PRINCESS, LEGION, RIOT -> true;
            default -> false;
        };
    }

    /**
     * Checks if a player's copy of a role should affect gameplay beyond their own visits.
     * Droisoned or fake holders keep their own visits but leave no outward footprint on
     * other visits or game systems.
     */
    public static boolean hasOutwardEffect(UUID uuid, Role role) {
        return !isDroisoned(uuid) && !isFakeRoleHolder(uuid, role);
    }

    /** Whether a player holds a working Wraith ability, dead-with-"Has Ability" included, so they may roam. */
    public static boolean wraithHasAbility(UUID uuid) {
        return StorytellerState.hasRoleWithAbility(uuid, Role.WRAITH, ClientState.playerDeathStatus);
    }

    /** Associated-role reminders (chooser-placed or hand-typed all-caps DRUNK) name an identity, not impairment. */
    private static boolean isIdentityReminder(Reminder r) {
        return isSpecialReminder(r) || r.text().equals("DRUNK");
    }

    private static boolean isAssignedTownsfolk(UUID uuid) {
        PendingRoleAssignment assignment = StorytellerState.PENDING_ROLES.get(uuid);
        return assignment != null && assignment.getRoleType() == RoleType.TOWNSFOLK;
    }

    /** Checks if a player holds a role, either assigned or as an associated-role reminder. */
    public static boolean holdsRole(UUID uuid, Role role) {
        PendingRoleAssignment assignment = StorytellerState.PENDING_ROLES.get(uuid);
        if (assignment == null) return false;
        if (assignment.role() == role) return true;
        return getAssociatedRoleReminders(uuid).stream()
                .anyMatch(r -> r.role().isPresent() && r.role().get() == role);
    }

    /** Players holding a role (assigned or associated) whose copy of it has outward effect. */
    public static List<UUID> getRoleAbilityHolders(Role role) {
        List<UUID> holders = new ArrayList<>();
        for (UUID uuid : StorytellerState.PENDING_ROLES.keySet()) {
            if (holdsRole(uuid, role) && hasOutwardEffect(uuid, role)) {
                holders.add(uuid);
            }
        }
        return holders;
    }

    /**
     * Checks if a role's ability is live in the game: some holder, assigned or associated, has
     * outward effect. This is about the ability, not the player's team: an associated demon
     * ability doesn't make its holder a demon or evil.
     */
    public static boolean isRoleAbilityInPlay(Role role) {
        return !getRoleAbilityHolders(role).isEmpty();
    }

    /**
     * Icon reminders that modify a demon's visit because of a reminder sitting on the demon:
     * Princess "Doesn't Kill" and Toymaker "Final Night: No Attack". Each gets its own
     * instruction line in {@link #appendIconReminderInstructions}.
     */
    public static List<Reminder> getDemonVisitModifiers(UUID demon) {
        List<Reminder> result = new ArrayList<>();
        for (Reminder r : StorytellerState.REMINDERS.getOrDefault(demon, Collections.emptyList())) {
            if (r.role().isEmpty()) continue;
            if (r.role().get() == Role.PRINCESS && r.text().equals("Doesn't Kill")) {
                result.add(new Reminder("Doesn't Kill", Optional.of(Role.PRINCESS)));
            } else if (r.role().get() == Role.TOYMAKER && r.text().equals("Final Night: No Attack")) {
                result.add(new Reminder("Final Night: No Attack", Optional.of(Role.TOYMAKER)));
            }
        }
        return result;
    }

    /** Whether an official fabled or loric character is on the current script. */
    public static boolean isFabledOrLoricOnScript(Role role) {
        return ClientState.currentScript != null
                && ClientState.currentScript.hasFabledOrLoric(role.getId());
    }

    // ========== Xaan ==========

    private static final Pattern XAAN_NIGHT = Pattern.compile("Night (\\d+)");
    private static boolean evaluatingXaan = false;

    /** Number of assigned outsiders (custom roles count by team). */
    public static int countAssignedOutsiders() {
        int count = 0;
        for (PendingRoleAssignment a : StorytellerState.PENDING_ROLES.values()) {
            if (a.getRoleType() == RoleType.OUTSIDER) count++;
        }
        return count;
    }

    /** A real Xaan: assigned, or an associated copy that isn't a fake identity. */
    public static boolean isRealXaanHolder(UUID uuid) {
        return holdsRole(uuid, Role.XAAN) && !isFakeRoleHolder(uuid, Role.XAAN);
    }

    /** A "Night N" reminder on a real Xaan holder, catalog or hand-typed. */
    public static boolean isXaanNightReminder(UUID uuid, Reminder r) {
        if (r.role().isPresent() && r.role().get() != Role.XAAN) return false;
        return XAAN_NIGHT.matcher(r.text()).matches() && isRealXaanHolder(uuid);
    }

    /**
     * The night the Xaan poisons: the "Night N" reminder on the Xaan (recorded at game start or
     * placed by the storyteller), or the live outsider count before the game starts (or if the
     * reminder was removed). 0 = never.
     */
    public static int getXaanNight() {
        for (Map.Entry<UUID, List<Reminder>> entry : StorytellerState.REMINDERS.entrySet()) {
            for (Reminder r : entry.getValue()) {
                if (isXaanNightReminder(entry.getKey(), r)) {
                    Matcher m = XAAN_NIGHT.matcher(r.text());
                    m.matches();
                    return Integer.parseInt(m.group(1));
                }
            }
        }
        return countAssignedOutsiders();
    }

    /**
     * Records the Xaan's night at game start as a "Night N" reminder on the Xaan player, from the
     * outsider count at that moment. Reminders persist and sync, so the value survives a relog.
     * A "Night N" reminder already in place is kept as a storyteller override.
     */
    public static void recordXaanNight() {
        UUID xaan = null;
        for (UUID uuid : StorytellerState.PENDING_ROLES.keySet()) {
            if (holdsRole(uuid, Role.XAAN)) {
                xaan = uuid;
                if (StorytellerState.PENDING_ROLES.get(uuid).role() == Role.XAAN) break;
            }
        }
        if (xaan == null) return;
        boolean alreadyRecorded = StorytellerState.REMINDERS.entrySet().stream()
                .anyMatch(e -> e.getValue().stream().anyMatch(r -> isXaanNightReminder(e.getKey(), r)));
        if (alreadyRecorded) return;
        int count = countAssignedOutsiders();
        if (count > 0) {
            StorytellerState.REMINDERS.computeIfAbsent(xaan, k -> new ArrayList<>())
                    .add(new Reminder("Night " + count, Optional.of(Role.XAAN)));
            StorytellerState.syncGrimoire();
        }
    }

    /**
     * Checks if the Xaan is poisoning townsfolk right now: it is night X or the day after it
     * (until dusk) and a living Xaan with outward effect exists, or the storyteller has placed
     * the Xaan's "X" reminder on someone as a manual override.
     */
    public static boolean isXaanPoisonActive() {
        if (evaluatingXaan) return false;
        if (findXaanXPlayer() != null) return true;
        evaluatingXaan = true;
        try {
            int night = getXaanNight();
            if (night == 0 || Math.max(ClientState.currentNight, 1) != night) return false;
            return getRoleAbilityHolders(Role.XAAN).stream()
                    .anyMatch(uuid -> !ClientState.playerDeathStatus.getOrDefault(uuid, false));
        } finally {
            evaluatingXaan = false;
        }
    }

    /**
     * Finds the Xaan X player (player with "X" reminder from Xaan).
     */
    public static UUID findXaanXPlayer() {
        return StorytellerState.REMINDERS.entrySet().stream()
                .filter(e -> e.getValue().stream().anyMatch(r -> r.text().equals("X") && r.role().isPresent() && r.role().get() == Role.XAAN))
                .map(Map.Entry::getKey)
                .findFirst().orElse(null);
    }

    /**
     * Gets the player reminder suffix (Drunk/Poisoned).
     */
    public static String getPlayerReminderSuffix(UUID uuid, UUID minstrelPlayer) {
        List<String> reminderTexts = new ArrayList<>();
        boolean isMinstrel = minstrelPlayer != null && minstrelPlayer.equals(uuid);

        for (Reminder r : StorytellerState.REMINDERS.getOrDefault(uuid, Collections.emptyList())) {
            if (isIdentityReminder(r)) continue;
            String lowerText = r.text().toLowerCase(Locale.ROOT);
            if (lowerText.contains("drunk")) {
                if (lowerText.equals("everyone is drunk") && isMinstrel) continue;
                if (!reminderTexts.contains("Drunk")) reminderTexts.add("Drunk");
            }
            if (lowerText.contains("poisoned")) {
                if (!reminderTexts.contains("Poisoned")) reminderTexts.add("Poisoned");
            }
        }

        if (minstrelPlayer != null && !isMinstrel) {
            if (!reminderTexts.contains("Drunk")) reminderTexts.add("Drunk");
        }
        if (isAssignedTownsfolk(uuid) && isXaanPoisonActive()) {
            if (!reminderTexts.contains("Poisoned")) reminderTexts.add("Poisoned");
        }

        if (reminderTexts.isEmpty()) return "";
        return " (" + String.join(", ", reminderTexts) + ")";
    }

    /**
     * Gets aligned role color based on final alignment.
     * Works for both official and custom roles.
     */
    public static int getAlignedRoleColor(PendingRoleAssignment assignment) {
        if (assignment == null) {
            return 0xFFAAAAAA; // Gray
        }

        boolean isDefaultGood = assignment.isRoleDefaultGood();
        boolean isFinalGood = assignment.isFinalGood();

        // Alignment override colors
        if (isFinalGood && !isDefaultGood) return 0xFF00AAFF; // Townsfolk blue (forced good)
        if (!isFinalGood && isDefaultGood) return 0xFFFF5555; // Minion red (forced evil)

        // Use role type color (works for both official and custom roles)
        return assignment.getRoleType().getColor();
    }
}
