package com.autumnwind.botb.states;

import com.autumnwind.botb.hud.nightorderhud.RoleHelpers;
import com.autumnwind.botb.networking.SyncGrimoireC2SPayload;
import com.autumnwind.botb.util.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;

import java.util.*;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class StorytellerState {

    // ========== General Ability Checking ==========

    /**
     * Checks if a player has a specific role's ability.
     * A player has ability if:
     * - They are assigned the role OR have a role-associated reminder (text matches role name)
     * - They are NOT droisoned and the role is NOT a fake identity (see RoleHelpers)
     * - They do NOT have "No Ability" reminder (generic or role-specific)
     * - For minions: also checks Preacher "No Ability"
     * - They are ALIVE, OR they are DEAD with a "Has Ability" reminder (generic or Bone Collector)
     * - For minions: also checks Vigormortis "Has Ability"
     *
     * @param playerUuid The player to check
     * @param role The role to check for
     * @param deadPlayers Map of player UUIDs to their death status
     * @return true if the player has the role's ability
     */
    public static boolean hasRoleWithAbility(UUID playerUuid, Role role, Map<UUID, Boolean> deadPlayers) {
        PendingRoleAssignment assignment = PENDING_ROLES.get(playerUuid);
        if (assignment == null) return false;

        List<Reminder> playerReminders = REMINDERS.getOrDefault(playerUuid, Collections.emptyList());

        // Check if player has the role OR a role-associated reminder
        boolean hasRole = assignment.role() == role;
        String roleText = role.name().replace('_', ' ');
        boolean hasRoleReminder = playerReminders.stream()
                .anyMatch(r -> r.role().isPresent() && r.role().get() == role &&
                        r.text().equalsIgnoreCase(roleText));

        if (!hasRole && !hasRoleReminder) {
            return false;
        }

        if (!RoleHelpers.hasOutwardEffect(playerUuid, role)) {
            return false;
        }

        // Check for "No Ability" reminder (generic or role-specific)
        boolean hasNoAbility = playerReminders.stream()
                .anyMatch(r -> r.text().equals("No Ability") &&
                        (r.role().isEmpty() || r.role().get() == role));
        if (hasNoAbility) {
            return false;
        }

        // For minions: also check Preacher "No Ability"
        if (role.getType() == RoleType.MINION) {
            boolean hasPreacherNoAbility = playerReminders.stream()
                    .anyMatch(r -> r.text().equals("No Ability") &&
                            r.role().isPresent() && r.role().get() == Role.PREACHER);
            if (hasPreacherNoAbility) {
                return false;
            }
        }

        // Check death status and "Has Ability" reminder
        boolean isDead = deadPlayers.getOrDefault(playerUuid, false);
        if (isDead) {
            // Dead players need a "Has Ability" reminder to have their ability
            // Check generic "Has Ability" or Bone Collector "Has Ability"
            boolean hasAbilityReminder = playerReminders.stream()
                    .anyMatch(r -> r.text().equals("Has Ability") &&
                            (r.role().isEmpty() || r.role().get() == Role.BONE_COLLECTOR));

            // For minions: also check Vigormortis "Has Ability"
            if (!hasAbilityReminder && role.getType() == RoleType.MINION) {
                hasAbilityReminder = playerReminders.stream()
                        .anyMatch(r -> r.text().equals("Has Ability") &&
                                r.role().isPresent() && r.role().get() == Role.VIGORMORTIS);
            }

            if (!hasAbilityReminder) {
                return false;
            }
        }

        return true;
    }

    /**
     * Finds the first player who has a specific role's ability.
     *
     * @param role The role to check for
     * @param deadPlayers Map of player UUIDs to their death status
     * @return Optional containing the player's UUID if found, empty otherwise
     */
    public static Optional<UUID> getPlayerWithRoleAbility(Role role, Map<UUID, Boolean> deadPlayers) {
        for (UUID playerUuid : PENDING_ROLES.keySet()) {
            if (hasRoleWithAbility(playerUuid, role, deadPlayers)) {
                return Optional.of(playerUuid);
            }
        }
        return Optional.empty();
    }

    /**
     * Checks if any player has a specific role's ability.
     *
     * @param role The role to check for
     * @param deadPlayers Map of player UUIDs to their death status
     * @return true if any player has the role's ability
     */
    public static boolean isRoleAbilityActive(Role role, Map<UUID, Boolean> deadPlayers) {
        return getPlayerWithRoleAbility(role, deadPlayers).isPresent();
    }

    // ========== Specific Role Checks ==========

    /**
     * Checks if Organ Grinder mode should be active based on current roles and reminders.
     * Uses the general ability checking function, plus special handling for Plague Doctor's
     * "Storyteller Ability" case (format: "ST: Organ Grinder" reminder).
     *
     * @param deadPlayers map of player UUIDs to their death status
     * @return true if Organ Grinder mode should be active
     */
    public static boolean isOrganGrinderModeActive(Map<UUID, Boolean> deadPlayers) {
        // Standard check: player has Organ Grinder role or associated reminder with ability
        if (isRoleAbilityActive(Role.ORGAN_GRINDER, deadPlayers)) {
            return true;
        }

        // Special case: Plague Doctor's "Storyteller Ability" with Organ Grinder chosen
        // Storyteller-minion reminders use format "ST: [MinionName]" (e.g., "ST: Organ Grinder")
        for (Map.Entry<UUID, PendingRoleAssignment> entry : PENDING_ROLES.entrySet()) {
            UUID playerUuid = entry.getKey();
            List<Reminder> playerReminders = REMINDERS.getOrDefault(playerUuid, Collections.emptyList());

            boolean hasStorytellerAbility = playerReminders.stream()
                    .anyMatch(r -> r.text().equals("Storyteller Ability"));
            boolean hasStorytellerOrganGrinder = playerReminders.stream()
                    .anyMatch(r -> r.isStorytellerMinionReminder() &&
                            r.role().isPresent() && r.role().get() == Role.ORGAN_GRINDER);

            if (hasStorytellerAbility && hasStorytellerOrganGrinder) {
                // Found Storyteller Ability + ST: Organ Grinder - apply standard ability checks

                if (RoleHelpers.isDroisoned(playerUuid)) continue;

                // Check "No Ability" (generic or Organ Grinder-specific)
                boolean hasNoAbility = playerReminders.stream()
                        .anyMatch(r -> r.text().equals("No Ability") &&
                                (r.role().isEmpty() || r.role().get() == Role.ORGAN_GRINDER));
                if (hasNoAbility) continue;

                // Organ Grinder is a minion - check Preacher "No Ability"
                boolean hasPreacherNoAbility = playerReminders.stream()
                        .anyMatch(r -> r.text().equals("No Ability") &&
                                r.role().isPresent() && r.role().get() == Role.PREACHER);
                if (hasPreacherNoAbility) continue;

                // Check death status
                boolean isDead = deadPlayers.getOrDefault(playerUuid, false);
                if (isDead) {
                    // Check for "Has Ability" (generic, Bone Collector, or Vigormortis for minions)
                    boolean hasAbilityReminder = playerReminders.stream()
                            .anyMatch(r -> r.text().equals("Has Ability") &&
                                    (r.role().isEmpty() || r.role().get() == Role.BONE_COLLECTOR ||
                                            r.role().get() == Role.VIGORMORTIS));
                    if (!hasAbilityReminder) continue;
                }

                return true;
            }
        }

        return false;
    }
    public static final Map<UUID, PendingRoleAssignment> PENDING_ROLES = new HashMap<>();
    public static final Map<UUID, Integer> PENDING_SEAT_NUMBERS = new HashMap<>();

    /** Whether anyone currently holds a real seat. Seat zero means unseated. */
    public static boolean hasSeatedPlayers() {
        for (Integer seat : PENDING_SEAT_NUMBERS.values()) {
            if (seat != null && seat > 0) return true;
        }
        return false;
    }

    /**
     * Stores client-side only reminders for each player.
     * This is used by both Storytellers and Players for local bookkeeping.
     */
    public static final Map<UUID, List<Reminder>> REMINDERS = new HashMap<>();

    public static int nextSeatNumber = 1;
    public static boolean showUnseated = true;
    public static boolean showSelf = false;

    // Night HUD State
    public enum NightView { FIRST_NIGHT, OTHER_NIGHTS }
    public static NightView currentNightView = NightView.FIRST_NIGHT;

    public static boolean autoTeleportEnabled = true;
    public static boolean sendTeleportInfo = true; // Toggles visit/instruction messages
    public static boolean useDoorknock = false; // false = doorbell, true = doorknock

    // Current visit instructions HUD state (populated by showCurrentVisitInfo)
    public static ScriptRole currentVisitScriptRole = null; // Unified: Official, Custom, or null
    public static Identifier currentVisitIcon = null; // Icon for current visit (supports custom roles)
    public static String currentVisitInstructions = null;
    public static boolean currentVisitIsGood = true; // Alignment of the role for coloring
    public static Text currentVisitExtraInfo = null; // Extra info from NightOrderInfoGenerator
    public static Text currentVisitRoleText = null; // Role text for HUD with colors (e.g., "DRUNK / EMPATH (Drunk)")
    public static Text currentVisitPlayerNames = null; // Visited player name(s) for HUD in yellow

    // Night order info tracking (for Flowergirl, Town Crier, Undertaker, etc.)
    public static boolean demonVotedToday = false;      // For Flowergirl
    public static boolean minionNominatedToday = false; // For Town Crier
    public static Role lastExecutedRole = null;         // For Undertaker/Cannibal

    // Demon Bluff State - unified list using ScriptRole (null = empty slot)
    public static final List<ScriptRole> DEMON_BLUFFS = new ArrayList<>(Arrays.asList(null, null, null));
    public static boolean showBluffs = true;

    // Al-Hadikhia Homebrew: adds first visit asking all players if they want to live or die
    public static boolean alHadikhiaHomebrew = false;

    // Legion Vote Hiding: tracks players marked for execution by evil-only votes
    // These players appear marked to other players but not to the storyteller
    public static final Set<UUID> legionProtectedPlayers = new HashSet<>();

    // Storyteller's view of MFE (may differ from ClientState.markedForExecution if Legion)
    // This is the "real" MFE that ignores evil-only votes
    public static UUID storytellerMFE = null;
    public static int storytellerMFEVotes = 0;

    /**
     * Storyteller-local "is this player a traveler" check, sourced from PENDING_ROLES
     * rather than the server's canBeExiled mirror. Lets the storyteller's UI reflect a
     * just-changed role assignment immediately, before Send Roles propagates to the server.
     */
    public static boolean isTraveler(UUID uuid) {
        PendingRoleAssignment a = PENDING_ROLES.get(uuid);
        return a != null && a.getRoleType() == RoleType.TRAVELER;
    }

    /**
     * Storyteller-local "is there any traveler in this game" check. Same rationale as
     * {@link #isTraveler(UUID)}.
     */
    public static boolean hasAnyTravelers() {
        return PENDING_ROLES.values().stream()
                .anyMatch(a -> a.getRoleType() == RoleType.TRAVELER);
    }

    /**
     * "Is there any traveler who can currently be called for exile?" combines the
     * local-grimoire traveler check with the server's per-day exile-slot value. Used to
     * gate exile-related <em>UI hints</em> (purple highlights) so we don't dangle a
     * suggestion the storyteller can't actually act on. Defaults to <em>eligible</em>
     * when the server hasn't told us about a traveler yet (the storyteller just assigned
     * them locally and hasn't sent roles), matching the rest of the storyteller-grimoire-
     * is-truth policy. Only an explicit {@code false} from the server (slot used today)
     * marks them ineligible.
     */
    public static boolean hasExileEligibleTraveler() {
        for (UUID uuid : PENDING_ROLES.keySet()) {
            if (isTraveler(uuid) && ClientState.canBeExiled.getOrDefault(uuid, true)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets the UUID of the Butcher traveler if they have their ability.
     * Uses the general ability checking function.
     * @param deadPlayers map of player UUIDs to their death status
     * @return Optional containing Butcher's UUID if they have ability, empty otherwise
     */
    public static Optional<UUID> getButcherAliveWithAbility(Map<UUID, Boolean> deadPlayers) {
        return getPlayerWithRoleAbility(Role.BUTCHER, deadPlayers);
    }

    /**
     * Gets the UUID of the Voudon traveler if they have their ability.
     * Uses the general ability checking function.
     * @param deadPlayers map of player UUIDs to their death status
     * @return Optional containing Voudon's UUID if they have ability, empty otherwise
     */
    public static Optional<UUID> getVoudonAliveWithAbility(Map<UUID, Boolean> deadPlayers) {
        return getPlayerWithRoleAbility(Role.VOUDON, deadPlayers);
    }

    /**
     * Gets the UUID of the Bishop fabled if they have their ability.
     * Uses the general ability checking function.
     * @param deadPlayers map of player UUIDs to their death status
     * @return Optional containing Bishop's UUID if they have ability, empty otherwise
     */
    public static Optional<UUID> getBishopAliveWithAbility(Map<UUID, Boolean> deadPlayers) {
        return getPlayerWithRoleAbility(Role.BISHOP, deadPlayers);
    }

    public static final Set<UUID> markedPlayers = new HashSet<>();
    /** Wraith holders who could roam at the last night order rebuild; dropping off it fires their lost-ability visit. */
    public static final Set<UUID> wraithsWithAbility = new HashSet<>();

    // Storyteller toggle: when on, a mid-game role change for a player automatically
    // enqueues a "You are now the X" triggered visit so the night order surfaces the
    // switch. When off, role changes silently update the grimoire only.
    public static boolean createRoleSwitchTriggersOnRoleChange = true;

    // Triggered visits: source night order index -> visits that appear after that index.
    // Cleared at DUSK.
    // Keys are night order slots: list positions for official roles, the script's decimal for
    // custom roles. Always parsed or cast, never computed, so double equality is exact.
    public static final Map<Double, List<RoleVisit>> triggeredVisits = new HashMap<>();

    // Where the current visit sits, tracked semantically so it survives a night order rebuild.
    public static Double currentVisitSourceIndex = null; // source night order index
    public static boolean currentVisitIsTriggered = false; // whether it's a triggered visit
    public static int currentTriggerChainIndex = 0; // position within the trigger chain (0-based)
    // The cursor's visit vanished on rebuild and the cursor was parked on an earlier visit. The
    // semantic position above still names the vanished slot, so triggers anchor there and the
    // cursor returns to it if the visit comes back. Cleared once the storyteller navigates.
    public static boolean cursorDisplaced = false;
    public static int cursorDisplacedLanding = -1;

    public static final List<RoleVisit> activeNightOrder = new ArrayList<>();
    public static int currentNightVisitIndex = 0;

    public static void clear() {
        PENDING_ROLES.clear();
        PENDING_SEAT_NUMBERS.clear();
        REMINDERS.clear();
        nextSeatNumber = 1;

        legionProtectedPlayers.clear();
        storytellerMFE = null;
        storytellerMFEVotes = 0;
        markedPlayers.clear();
        triggeredVisits.clear();
        wraithsWithAbility.clear();
        currentVisitSourceIndex = null;
        currentVisitIsTriggered = false;
        currentTriggerChainIndex = 0;
        cursorDisplaced = false;
        cursorDisplacedLanding = -1;
        activeNightOrder.clear();
        currentNightVisitIndex = 0;
        currentNightView = NightView.FIRST_NIGHT;
        currentVisitExtraInfo = null;

        // Clear night order info tracking
        clearDailyTracking();

        DEMON_BLUFFS.set(0, null);
        DEMON_BLUFFS.set(1, null);
        DEMON_BLUFFS.set(2, null);
        showBluffs = true;
    }

    /**
     * Clears daily tracking fields at the start of a new day (Dawn).
     * Called when advancing to Dawn in the night order.
     */
    public static void clearDailyTracking() {
        demonVotedToday = false;
        minionNominatedToday = false;
        lastExecutedRole = null;
    }

    /**
     * Sends grimoire state to the server for syncing with other storytellers.
     * The server will only broadcast if there are multiple operators online.
     * Call this when meaningful grimoire changes occur (role assignments, reminders, etc.)
     */
    public static void syncGrimoire() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || !client.player.hasPermissionLevel(2)) {
            return; // Only operators can sync
        }

        // Convert ScriptRole bluffs to string format for network
        List<String> demonBluffsForNetwork = bluffsToStrings(DEMON_BLUFFS);

        // Create the sync payload with current state
        SyncGrimoireC2SPayload payload = new SyncGrimoireC2SPayload(
                new HashMap<>(PENDING_ROLES),
                new HashMap<>(PENDING_SEAT_NUMBERS),
                copyReminders(),
                Optional.ofNullable(ClientState.currentScript),
                new HashSet<>(markedPlayers),
                demonBluffsForNetwork
        );

        // Send to server - server will decide whether to broadcast to other operators
        ClientPlayNetworking.send(payload);
    }

    /**
     * Creates a deep copy of the reminders map for syncing.
     */
    private static Map<UUID, List<Reminder>> copyReminders() {
        Map<UUID, List<Reminder>> copy = new HashMap<>();
        for (Map.Entry<UUID, List<Reminder>> entry : REMINDERS.entrySet()) {
            copy.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }
        return copy;
    }

    /**
     * Gets all players who have the Banshee "Has Ability" reminder.
     * These players can double vote during voting.
     */
    public static List<UUID> getBansheeHasAbilityPlayers() {
        List<UUID> bansheeAbilityPlayers = new ArrayList<>();
        for (Map.Entry<UUID, List<Reminder>> entry : REMINDERS.entrySet()) {
            UUID playerUuid = entry.getKey();
            List<Reminder> reminders = entry.getValue();

            boolean hasBansheeAbility = reminders.stream()
                    .anyMatch(r -> r.text().equals("Has Ability") &&
                            r.role().isPresent() && r.role().get() == Role.BANSHEE);

            if (hasBansheeAbility) {
                bansheeAbilityPlayers.add(playerUuid);
            }
        }
        return bansheeAbilityPlayers;
    }

    /**
     * Every player the grimoire says is evil, but only when a Legion is assigned, and otherwise
     * empty.
     *
     * <p>Legion's "executions fail if only evil voted" rule needs real alignments, and the
     * grimoire is the only place they exist: the role map the server holds is the doctored one
     * players were sent, in which a Lunatic reads as a genuine Legion and an Ogre reads as good.
     * Sent per-vote on {@code RunVoteC2SPayload}, whose presence tells the server the rule applies.
     */
    public static Optional<List<UUID>> getEvilsForLegion() {
        if (!RoleHelpers.isRoleAbilityInPlay(Role.LEGION)) return Optional.empty();

        List<UUID> evils = new ArrayList<>();
        for (Map.Entry<UUID, PendingRoleAssignment> entry : PENDING_ROLES.entrySet()) {
            if (!entry.getValue().isFinalGood()) evils.add(entry.getKey());
        }
        return Optional.of(evils);
    }

    /**
     * Gets all players whose vote counts as 2 because of the God of Ug "Ug hat" reminder.
     */
    public static List<UUID> getUgHatPlayers() {
        List<UUID> result = new ArrayList<>();
        for (Map.Entry<UUID, List<Reminder>> entry : REMINDERS.entrySet()) {
            boolean has = entry.getValue().stream()
                    .anyMatch(r -> r.text().equals("Ug hat") &&
                            r.role().isPresent() && r.role().get() == Role.GOD_OF_UG);
            if (has) result.add(entry.getKey());
        }
        return result;
    }

    /**
     * Gets all players whose vote counts as 3 because of the Bureaucrat "3 Votes" reminder.
     */
    public static List<UUID> getBureaucrat3VotePlayers() {
        List<UUID> result = new ArrayList<>();
        for (Map.Entry<UUID, List<Reminder>> entry : REMINDERS.entrySet()) {
            boolean has = entry.getValue().stream()
                    .anyMatch(r -> r.text().equals("3 Votes") &&
                            r.role().isPresent() && r.role().get() == Role.BUREAUCRAT);
            if (has) result.add(entry.getKey());
        }
        return result;
    }

    /**
     * Gets all players whose vote counts as -1 because of the Thief "Negative Vote" reminder.
     */
    public static List<UUID> getThiefNegativeVotePlayers() {
        List<UUID> result = new ArrayList<>();
        for (Map.Entry<UUID, List<Reminder>> entry : REMINDERS.entrySet()) {
            boolean has = entry.getValue().stream()
                    .anyMatch(r -> r.text().equals("Negative Vote") &&
                            r.role().isPresent() && r.role().get() == Role.THIEF);
            if (has) result.add(entry.getKey());
        }
        return result;
    }

    /**
     * Gets all players who have any "May Not Nominate" reminder (e.g., Golem).
     * These players cannot nominate during nominations.
     */
    public static List<UUID> getMayNotNominatePlayers() {
        List<UUID> mayNotNominatePlayers = new ArrayList<>();
        for (Map.Entry<UUID, List<Reminder>> entry : REMINDERS.entrySet()) {
            UUID playerUuid = entry.getKey();
            List<Reminder> reminders = entry.getValue();

            boolean hasMayNotNominate = reminders.stream()
                    .anyMatch(r -> r.text().equals("May Not Nominate"));

            if (hasMayNotNominate) {
                mayNotNominatePlayers.add(playerUuid);
            }
        }
        return mayNotNominatePlayers;
    }

    /**
     * Converts a list of ScriptRole bluffs to string format for network transmission.
     * Format: "" = empty, "ROLE_NAME" = official, "custom:id" = custom
     */
    public static List<String> bluffsToStrings(List<ScriptRole> bluffs) {
        List<String> result = new ArrayList<>();
        for (ScriptRole sr : bluffs) {
            if (sr == null) {
                result.add("");
            } else if (sr.isCustom()) {
                result.add("custom:" + sr.getId());
            } else {
                result.add(sr.asRole().name());
            }
        }
        return result;
    }

    /**
     * Converts a list of string-format bluffs to ScriptRole list.
     * Resolves custom role IDs from the current script.
     * Format: "" = empty (null), "ROLE_NAME" = official, "custom:id" = custom
     */
    public static List<ScriptRole> stringsToBluffs(List<String> bluffStrings) {
        List<ScriptRole> result = new ArrayList<>();
        for (String str : bluffStrings) {
            if (str == null || str.isEmpty()) {
                result.add(null);
            } else if (str.startsWith("custom:")) {
                String customId = str.substring(7);
                // Try to resolve from current script
                if (ClientState.currentScript != null) {
                    Optional<CustomRole> customRole = ClientState.currentScript.getCustomRole(customId);
                    if (customRole.isPresent()) {
                        result.add(new ScriptRole.Custom(customRole.get()));
                    } else {
                        result.add(null); // Custom role not found in script
                    }
                } else {
                    result.add(null); // No script to resolve from
                }
            } else {
                // Official role
                try {
                    Role role = Role.valueOf(str);
                    result.add(new ScriptRole.Official(role));
                } catch (IllegalArgumentException e) {
                    result.add(null); // Invalid role name
                }
            }
        }
        return result;
    }
}