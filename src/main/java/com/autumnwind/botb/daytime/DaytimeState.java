package com.autumnwind.botb.daytime;

import java.util.*;

/**
 * Server-side state manager for the daytime nomination, voting, and execution system.
 *
 * <h2>Relationship to ElectionState</h2>
 * <p>Election state lives in two places, split by what reads it:</p>
 * <ul>
 *   <li><b>DaytimeState</b> backs the sync payloads sent to clients, and holds everything
 *       rule-specific: nomination eligibility, ghost votes, MFE tracking, Banshee / Legion /
 *       Organ Grinder handling, and exile state.</li>
 *   <li><b>{@link ElectionState}</b> holds the generic shape of an election, and is what
 *       {@link ElectionManager} drives the voting phase from.</li>
 * </ul>
 *
 * <p>The manager classes ({@link VotingManager}, {@link ExileSupportManager}) keep both in
 * step for the duration of an election. The overlapping fields are:</p>
 * <ul>
 *   <li>{@code currentNominator/currentNominee} ↔ {@code ElectionState.initiator/target}</li>
 *   <li>{@code lockedVotes} ↔ {@code ElectionState.lockedVotes}</li>
 *   <li>{@code currentLeverStates} ↔ {@code ElectionState.leverStates}</li>
 *   <li>{@code voteInProgress} ↔ {@code ElectionState.votingPhaseActive}</li>
 * </ul>
 */
public class DaytimeState {

    // ==================== Nomination Eligibility ====================
    // These track who can nominate/be nominated (reset daily)
    private static final Map<UUID, Boolean> canNominate = new HashMap<>();
    private static final Map<UUID, Boolean> canBeNominated = new HashMap<>();
    // Storyteller nomination tracking (for Atheist script - storyteller can only be nominated once per day)
    private static boolean storytellerCanBeNominated = true;

    // ==================== Ghost Vote Tracking ====================
    private static final Map<UUID, Boolean> hasUsedGhostVote = new HashMap<>();

    // ==================== Current Nomination/Vote State ====================
    // NOTE: These overlap with ElectionState.initiator/target - kept in sync by managers
    private static UUID currentNominator = null;
    private static UUID currentNominee = null;

    // ==================== Marked For Execution (MFE) ====================
    // What players see (may include Legion-protected votes)
    private static UUID markedForExecution = null;
    private static int votesForMarkedPlayer = 0;
    // Storyteller's view of MFE (ignores Legion evil-only votes)
    private static UUID storytellerMFE = null;
    private static int storytellerMFEVotes = 0;

    // ==================== Vote Phase State ====================
    // NOTE: voteInProgress overlaps with ElectionState.votingPhaseActive
    private static boolean nominationsOpen = false;
    private static boolean voteInProgress = false;
    private static boolean organGrinderMode = false;
    private static boolean organGrinderModeActiveToday = false; // Persistent for the day

    // ==================== Vote Tracking ====================
    // NOTE: These overlap with ElectionState.lockedVotes/leverStates - kept in sync
    private static final Map<UUID, Boolean> currentVotes = new HashMap<>();
    private static final Map<UUID, Boolean> lockedVotes = new HashMap<>();
    private static final Map<UUID, Boolean> currentLeverStates = new HashMap<>();

    // ==================== Organ Grinder Mode Tracking ====================
    // Ghost votes used during OG mode (visual update delayed until Dusk)
    private static final Set<UUID> pendingGhostVoteUpdates = new HashSet<>();
    // Ghost votes secretly consumed during OG mode - NOT synced to clients
    private static final Set<UUID> secretlyUsedGhostVotes = new HashSet<>();

    // ==================== May Not Nominate Tracking ====================
    // Players with "May Not Nominate" reminder (e.g., Golem) cannot nominate
    private static final Set<UUID> mayNotNominatePlayers = new HashSet<>();

    // ==================== Banshee Ability Tracking ====================
    // Players with "Has Ability" reminder for Banshee get double vote/nominations
    private static final Set<UUID> bansheeDoubleVotePlayers = new HashSet<>();
    private static final Map<UUID, Boolean> bansheeDoubleVoteActive = new HashMap<>(); // true = 2x, false = 1x
    private static final Set<UUID> bansheeHasVotedOnce = new HashSet<>();
    private static final Map<UUID, Integer> nominationsRemaining = new HashMap<>();
    // Underlying ghost vote state preserved when gaining Banshee ability
    private static final Map<UUID, Boolean> bansheeUnderlyingGhostVoteUsed = new HashMap<>();

    // Getters for nomination eligibility
    public static boolean canNominate(UUID player) {
        return canNominate.getOrDefault(player, false);
    }

    public static boolean canBeNominated(UUID player) {
        return canBeNominated.getOrDefault(player, false);
    }

    public static Map<UUID, Boolean> getCanNominateMap() {
        return new HashMap<>(canNominate);
    }

    public static Map<UUID, Boolean> getCanBeNominatedMap() {
        return new HashMap<>(canBeNominated);
    }

    // Setters for nomination eligibility
    public static void setCanNominate(UUID player, boolean value) {
        canNominate.put(player, value);
    }

    public static void setCanBeNominated(UUID player, boolean value) {
        canBeNominated.put(player, value);
    }

    // Storyteller nomination eligibility (for Atheist script)
    public static boolean canStorytellerBeNominated() {
        return storytellerCanBeNominated;
    }

    public static void setStorytellerCanBeNominated(boolean value) {
        storytellerCanBeNominated = value;
    }

    // Ghost vote methods
    public static boolean hasUsedGhostVote(UUID player) {
        return hasUsedGhostVote.getOrDefault(player, false);
    }

    public static void markGhostVoteUsed(UUID player) {
        hasUsedGhostVote.put(player, true);
    }

    public static void resetGhostVote(UUID player) {
        hasUsedGhostVote.put(player, false);
    }

    public static void clearAllGhostVotes() {
        hasUsedGhostVote.clear();
    }

    public static Map<UUID, Boolean> getGhostVoteMap() {
        return new HashMap<>(hasUsedGhostVote);
    }

    // Current nomination getters/setters
    public static UUID getCurrentNominator() {
        return currentNominator;
    }

    public static void setCurrentNominator(UUID nominator) {
        currentNominator = nominator;
    }

    public static UUID getCurrentNominee() {
        return currentNominee;
    }

    public static void setCurrentNominee(UUID nominee) {
        currentNominee = nominee;
    }

    public static boolean hasActiveNomination() {
        return currentNominator != null && currentNominee != null;
    }

    // MFE state getters/setters
    public static UUID getMarkedForExecution() {
        return markedForExecution;
    }

    public static void setMarkedForExecution(UUID player, int voteCount) {
        markedForExecution = player;
        votesForMarkedPlayer = voteCount;
    }

    public static void clearMarkedForExecution() {
        markedForExecution = null;
        votesForMarkedPlayer = 0;
        // Note: Do NOT clear storyteller MFE here - it's managed separately
        // Storyteller MFE is only cleared in hardReset() and resetDaily()
    }

    public static int getVotesForMarkedPlayer() {
        return votesForMarkedPlayer;
    }

    // Storyteller MFE getters/setters (for Legion games)
    public static UUID getStorytellerMFE() {
        return storytellerMFE;
    }

    public static int getStorytellerMFEVotes() {
        return storytellerMFEVotes;
    }

    public static void setStorytellerMFE(UUID player, int voteCount) {
        storytellerMFE = player;
        storytellerMFEVotes = voteCount;
    }

    public static void clearStorytellerMFE() {
        storytellerMFE = null;
        storytellerMFEVotes = 0;
    }

    // Nominations open state
    public static boolean areNominationsOpen() {
        return nominationsOpen;
    }

    public static void openNominations(Set<UUID> alivePlayers, Set<UUID> seatedPlayers, List<UUID> bansheeHasAbilityPlayers, List<UUID> mayNotNominatePlayers) {
        nominationsOpen = true;

        // Reset eligibility for new day
        canNominate.clear();
        canBeNominated.clear();
        nominationsRemaining.clear();
        storytellerCanBeNominated = true; // Storyteller can be nominated once per day (Atheist)

        // All alive players can nominate (except those with "May Not Nominate" reminder)
        Set<UUID> blocked = new HashSet<>(mayNotNominatePlayers);
        for (UUID player : alivePlayers) {
            if (!blocked.contains(player)) {
                canNominate.put(player, true);
            }
        }

        // Banshee players with "Has Ability" can nominate even if dead, and get 2 nominations
        // (unless they have "May Not Nominate" reminder)
        for (UUID bansheePlayer : bansheeHasAbilityPlayers) {
            if (!blocked.contains(bansheePlayer)) {
                canNominate.put(bansheePlayer, true); // Can nominate even if dead
            }
            nominationsRemaining.put(bansheePlayer, 2); // Gets 2 nominations (even if blocked)
        }

        // All seated players can be nominated, EXCEPT travelers (they can only be exiled).
        // canBeExiled.keySet() is the server's traveler set: it's seeded by the storyteller's
        // most recent role assignment and refreshed at dusk/dawn. No separate cache needed.
        for (UUID player : seatedPlayers) {
            if (!isTraveler(player)) {
                canBeNominated.put(player, true);
            }
        }
    }

    public static void closeNominations() {
        nominationsOpen = false;
    }

    // Vote state
    public static boolean isVoteInProgress() {
        return voteInProgress;
    }

    public static boolean isOrganGrinderMode() {
        return organGrinderMode;
    }

    public static boolean isOrganGrinderModeActiveToday() {
        return organGrinderModeActiveToday;
    }

    public static void startVote(boolean isOrganGrinderMode) {
        voteInProgress = true;
        organGrinderMode = isOrganGrinderMode;
        // Once OG mode is used in a vote, it stays active for the day (for hiding MFE)
        if (isOrganGrinderMode) {
            organGrinderModeActiveToday = true;
        }
        currentVotes.clear();
        lockedVotes.clear();
        currentLeverStates.clear();
    }

    public static void endVote() {
        voteInProgress = false;
        organGrinderMode = false;
        currentVotes.clear();
        lockedVotes.clear();
        currentLeverStates.clear();
    }

    // Pending ghost vote updates (for Organ Grinder mode)
    public static void addPendingGhostVoteUpdate(UUID player) {
        pendingGhostVoteUpdates.add(player);
    }

    public static Set<UUID> getPendingGhostVoteUpdates() {
        return new HashSet<>(pendingGhostVoteUpdates);
    }

    public static void clearPendingGhostVoteUpdates() {
        pendingGhostVoteUpdates.clear();
    }

    // Secretly used ghost votes (OG mode - NOT synced to clients)
    public static void markGhostVoteSecretlyUsed(UUID player) {
        secretlyUsedGhostVotes.add(player);
    }

    public static boolean hasSecretlyUsedGhostVote(UUID player) {
        return secretlyUsedGhostVotes.contains(player);
    }

    // Convert secretly used ghost votes to actually used (called at Dusk)
    public static void finalizeSecretGhostVotes() {
        for (UUID player : secretlyUsedGhostVotes) {
            hasUsedGhostVote.put(player, true);
        }
        secretlyUsedGhostVotes.clear();
    }

    public static void setCurrentVote(UUID player, boolean vote) {
        currentVotes.put(player, vote);
    }

    public static void lockVote(UUID player, boolean vote) {
        lockedVotes.put(player, vote);
    }

    public static Map<UUID, Boolean> getLockedVotes() {
        return new HashMap<>(lockedVotes);
    }

    public static void setLeverState(UUID player, boolean state) {
        currentLeverStates.put(player, state);
    }

    public static Map<UUID, Boolean> getLeverStates() {
        return new HashMap<>(currentLeverStates);
    }

    public static void clearLeverStates() {
        currentLeverStates.clear();
    }

    // Reset methods
    public static void resetNomination() {
        currentNominator = null;
        currentNominee = null;
    }

    public static void resetDaily() {
        // Reset nomination eligibility
        canNominate.clear();
        canBeNominated.clear();
        storytellerCanBeNominated = true; // Reset storyteller nomination (Atheist)

        // Clear current nomination
        resetNomination();

        // Clear MFE state (player view)
        clearMarkedForExecution();

        // Clear storyteller MFE state (Legion tracking)
        clearStorytellerMFE();

        // Close nominations
        closeNominations();

        // Reset Organ Grinder mode for the day
        organGrinderModeActiveToday = false;

        // Clear reminder-based vote multipliers (Ug hat, Bureaucrat, Thief)
        clearUgHatPlayers();
        clearTravelerMultipliers();

        // End any active vote
        if (voteInProgress) {
            endVote();
        }
    }

    public static void resetVote() {
        // Clear current nomination only
        resetNomination();

        // End vote if in progress
        if (voteInProgress) {
            endVote();
        }

        // Do NOT clear MFE state - that persists until dawn
    }

    public static void hardReset(Set<UUID> alivePlayers, Set<UUID> seatedPlayers) {
        // Clear current nomination
        resetNomination();

        // Clear MFE state (player view)
        clearMarkedForExecution();

        // Clear storyteller MFE state (Legion tracking)
        clearStorytellerMFE();

        // End any active vote
        if (voteInProgress) {
            endVote();
        }

        // Reset eligibility like openNominations does (but keep nominations open)
        canNominate.clear();
        canBeNominated.clear();
        nominationsRemaining.clear(); // Reset nomination counts
        storytellerCanBeNominated = true; // Reset storyteller nomination (Atheist)

        // All alive players can nominate (except those with "May Not Nominate" reminder)
        for (UUID player : alivePlayers) {
            if (!mayNotNominatePlayers.contains(player)) {
                canNominate.put(player, true);
            }
        }

        // Banshee players with ability can nominate even if dead, and get 2 nominations
        // (unless they have "May Not Nominate" reminder)
        for (UUID bansheePlayer : bansheeDoubleVotePlayers) {
            if (!mayNotNominatePlayers.contains(bansheePlayer)) {
                canNominate.put(bansheePlayer, true); // Can nominate even if dead
            }
            nominationsRemaining.put(bansheePlayer, 2); // Gets 2 nominations (even if blocked)
        }

        // All seated players can be nominated, EXCEPT travelers (they can only be exiled).
        // canBeExiled.keySet() is the server's traveler set.
        for (UUID player : seatedPlayers) {
            if (!isTraveler(player)) {
                canBeNominated.put(player, true);
            }
        }

        // Keep nominations open - do NOT change nominationsOpen flag
    }

    // ========== Banshee Double Voting ==========

    /**
     * Marks a player as having the Banshee "Has Ability" reminder, allowing double voting.
     * Saves their current ghost vote state - voting via banshee system does NOT consume ghost vote.
     * When they lose the ability, their original ghost vote state will be restored.
     */
    public static void enableBansheeDoubleVote(UUID player) {
        bansheeDoubleVotePlayers.add(player);
        bansheeDoubleVoteActive.put(player, true); // Default to double vote active
        // Save current ghost vote state - this is preserved while they have the ability
        bansheeUnderlyingGhostVoteUsed.put(player, hasUsedGhostVote(player));
    }

    /**
     * Removes a player's Banshee double vote ability.
     * Restores their original ghost vote state from before they gained the ability.
     */
    public static void disableBansheeDoubleVote(UUID player) {
        // Restore original ghost vote state
        boolean underlyingUsed = bansheeUnderlyingGhostVoteUsed.getOrDefault(player, false);
        hasUsedGhostVote.put(player, underlyingUsed);

        // Clean up banshee state
        bansheeDoubleVotePlayers.remove(player);
        bansheeDoubleVoteActive.remove(player);
        bansheeHasVotedOnce.remove(player);
        bansheeUnderlyingGhostVoteUsed.remove(player);
    }

    /**
     * Checks if a player has Banshee double vote ability.
     */
    public static boolean hasBansheeDoubleVote(UUID player) {
        return bansheeDoubleVotePlayers.contains(player);
    }

    /**
     * Toggles a player's Banshee double vote state (1 vote <-> 2 votes).
     * Only toggles if they've already voted ON at least once this session.
     * Returns true if toggle happened, false if this was their first ON.
     */
    public static boolean toggleBansheeDoubleVoteActive(UUID player) {
        if (bansheeDoubleVotePlayers.contains(player)) {
            if (bansheeHasVotedOnce.contains(player)) {
                // They've voted before, so toggle
                boolean current = bansheeDoubleVoteActive.getOrDefault(player, true);
                bansheeDoubleVoteActive.put(player, !current);
                return true;
            } else {
                // First time voting ON - mark as voted, don't toggle (stay at 2)
                bansheeHasVotedOnce.add(player);
                return false;
            }
        }
        return false;
    }

    /**
     * Checks if a player's double vote is currently active (counts as 2 votes).
     */
    public static boolean isBansheeDoubleVoteActive(UUID player) {
        return bansheeDoubleVotePlayers.contains(player) &&
               bansheeDoubleVoteActive.getOrDefault(player, true);
    }

    /**
     * Gets the vote multiplier for a player.
     * Base is 1 (or 2 for Banshee with active double vote).
     * Multiplied by 2 if player has God of Ug "Ug hat" reminder.
     * Multiplied by 3 if player has Bureaucrat "3 Votes" reminder.
     * Multiplied by -1 if player has Thief "Negative Vote" reminder.
     */
    public static int getVoteMultiplier(UUID player) {
        int multiplier = isBansheeDoubleVoteActive(player) ? 2 : 1;

        // Apply God of Ug 2x multiplier
        if (ugHatPlayers.contains(player)) {
            multiplier *= 2;
        }

        // Apply Bureaucrat 3x multiplier
        if (bureaucratMultipliers.containsKey(player)) {
            multiplier *= bureaucratMultipliers.get(player);
        }

        // Apply Thief -1x multiplier
        if (thiefMultipliers.containsKey(player)) {
            multiplier *= thiefMultipliers.get(player);
        }

        return multiplier;
    }

    /**
     * Clears all Banshee double vote tracking.
     */
    public static void clearBansheeDoubleVotes() {
        bansheeDoubleVotePlayers.clear();
        bansheeDoubleVoteActive.clear();
        bansheeHasVotedOnce.clear();
        bansheeUnderlyingGhostVoteUsed.clear();
    }

    // ========== May Not Nominate (Golem etc.) ==========

    /**
     * Updates the set of players who cannot nominate (e.g., Golem with "May Not Nominate" reminder).
     */
    public static void setMayNotNominatePlayers(List<UUID> players) {
        mayNotNominatePlayers.clear();
        mayNotNominatePlayers.addAll(players);

        // If nominations are open, update canNominate for these players
        for (UUID player : players) {
            canNominate.put(player, false);
        }
    }

    /**
     * Checks if a player has the "May Not Nominate" restriction.
     */
    public static boolean isMayNotNominate(UUID player) {
        return mayNotNominatePlayers.contains(player);
    }

    /**
     * Checks if a banshee player's underlying (original) ghost vote was used.
     */
    public static boolean isBansheeUnderlyingGhostVoteUsed(UUID player) {
        return bansheeUnderlyingGhostVoteUsed.getOrDefault(player, false);
    }

    /**
     * Gets a copy of the set of players with Banshee double vote ability.
     * Used to track who had the ability when comparing before/after dawn.
     */
    public static Set<UUID> getBansheeDoubleVotePlayers() {
        return new HashSet<>(bansheeDoubleVotePlayers);
    }

    public static Set<UUID> getBansheeDoubleVoteActivePlayers() {
        Set<UUID> active = new HashSet<>();
        for (UUID player : bansheeDoubleVotePlayers) {
            if (isBansheeDoubleVoteActive(player)) {
                active.add(player);
            }
        }
        return active;
    }

    // ========== Banshee Double Nominations ==========

    /**
     * Sets the remaining nominations for a player.
     */
    public static void setNominationsRemaining(UUID player, int count) {
        nominationsRemaining.put(player, count);
    }

    /**
     * Gets the remaining nominations for a player.
     */
    public static int getNominationsRemaining(UUID player) {
        return nominationsRemaining.getOrDefault(player, 1);
    }

    /**
     * Uses one nomination for a player.
     */
    public static void useNomination(UUID player) {
        int remaining = getNominationsRemaining(player);
        if (remaining > 0) {
            nominationsRemaining.put(player, remaining - 1);
        }
    }

    /**
     * Checks if a player has any nominations remaining.
     */
    public static boolean hasNominationsRemaining(UUID player) {
        return getNominationsRemaining(player) > 0;
    }

    /**
     * Gets the nominations remaining map for syncing.
     */
    public static Map<UUID, Integer> getNominationsRemainingMap() {
        return new HashMap<>(nominationsRemaining);
    }

    // ========== Voudon Voting System ==========
    // Voudon reverses who can vote - alive players can't vote, dead players can
    private static boolean voudonModeActive = false;
    // Track which players have been "Voudon-blocked" (visual only, ghost vote not actually consumed)
    private static final Set<UUID> voudonBlockedPlayers = new HashSet<>();
    // Track the Voudon player's UUID (needed for death/revival indicator updates)
    private static UUID voudonPlayerUuid = null;

    /**
     * Checks if Voudon mode is currently active.
     */
    public static boolean isVoudonModeActive() {
        return voudonModeActive;
    }

    /**
     * Gets the Voudon player UUID (if Voudon mode is active).
     */
    public static UUID getVoudonPlayerUuid() {
        return voudonPlayerUuid;
    }

    /**
     * Activates Voudon mode with the Voudon player's UUID.
     */
    public static void activateVoudonMode(UUID voudonUuid) {
        voudonModeActive = true;
        voudonPlayerUuid = voudonUuid;
    }

    /**
     * Deactivates Voudon mode. Called when Voudon dies or loses ability.
     */
    public static void deactivateVoudonMode() {
        voudonModeActive = false;
        voudonPlayerUuid = null;
        voudonBlockedPlayers.clear();
    }

    /**
     * Adds a player to the Voudon-blocked set (visual only).
     */
    public static void addVoudonBlockedPlayer(UUID player) {
        voudonBlockedPlayers.add(player);
    }

    /**
     * Removes a player from the Voudon-blocked set.
     */
    public static void removeVoudonBlockedPlayer(UUID player) {
        voudonBlockedPlayers.remove(player);
    }

    /**
     * Gets all Voudon-blocked players.
     */
    public static Set<UUID> getVoudonBlockedPlayers() {
        return new HashSet<>(voudonBlockedPlayers);
    }

    /**
     * Clears Voudon-blocked players.
     */
    public static void clearVoudonBlockedPlayers() {
        voudonBlockedPlayers.clear();
    }

    // ========== God of Ug ==========
    // Players wearing the Ug hat have their vote count as 2
    private static final Set<UUID> ugHatPlayers = new HashSet<>();

    /**
     * Replaces the set of players wearing the God of Ug "Ug hat" reminder.
     */
    public static void setUgHatPlayers(Collection<UUID> players) {
        ugHatPlayers.clear();
        ugHatPlayers.addAll(players);
    }

    public static void clearUgHatPlayers() {
        ugHatPlayers.clear();
    }

    // ========== Traveler Vote Multipliers ==========
    // Bureaucrat gives 3x votes, Thief gives -1x votes
    // These are tracked per-player based on reminders
    private static final Map<UUID, Integer> bureaucratMultipliers = new HashMap<>();
    private static final Map<UUID, Integer> thiefMultipliers = new HashMap<>();

    /**
     * Sets the Bureaucrat multiplier for a player (3 if has "3 Votes" reminder).
     */
    public static void setBureaucratMultiplier(UUID player, boolean hasReminder) {
        if (hasReminder) {
            bureaucratMultipliers.put(player, 3);
        } else {
            bureaucratMultipliers.remove(player);
        }
    }

    /**
     * Sets the Thief multiplier for a player (-1 if has "Negative Vote" reminder).
     */
    public static void setThiefMultiplier(UUID player, boolean hasReminder) {
        if (hasReminder) {
            thiefMultipliers.put(player, -1);
        } else {
            thiefMultipliers.remove(player);
        }
    }

    /**
     * Clears all traveler multipliers.
     */
    public static void clearTravelerMultipliers() {
        bureaucratMultipliers.clear();
        thiefMultipliers.clear();
    }

    // ========== Traveler Exile System ==========
    // Exile is completely separate from nominations/voting.
    // Dead players can call for and support exiles without consuming ghost votes.
    // No Organ Grinder, Banshee double vote, or Legion rules apply.

    // Exile eligibility (resets daily, tracks which travelers can be called for exile)
    private static final Map<UUID, Boolean> canBeExiled = new HashMap<>();

    // Current exile state
    private static UUID currentExileCaller = null;
    private static UUID currentExileTarget = null;
    private static boolean exileSupportInProgress = false;

    // Track seats with ghost used blocks (for restore after exile support)
    private static final Set<Integer> seatsWithGhostUsedBlocks = new HashSet<>();

    // Exile support vote tracking (simpler than regular voting - no special rules)
    private static final Map<UUID, Boolean> exileSupportVotes = new HashMap<>();
    private static final Map<UUID, Boolean> lockedExileSupportVotes = new HashMap<>();

    // Getters for exile eligibility
    public static boolean canBeExiled(UUID player) {
        return canBeExiled.getOrDefault(player, false);
    }

    public static Map<UUID, Boolean> getCanBeExiledMap() {
        return new HashMap<>(canBeExiled);
    }

    /**
     * Server-side traveler check. The keyset of {@code canBeExiled} is the authoritative
     * traveler set: travelers stay as keys even when their value flips to false (already
     * exiled today / dead).
     * No code path removes keys without re-adding them in the same write.
     */
    public static boolean isTraveler(UUID player) {
        return canBeExiled.containsKey(player);
    }

    // Setters for exile eligibility
    public static void setCanBeExiled(UUID player, boolean value) {
        canBeExiled.put(player, value);
    }

    /**
     * Initialize exile eligibility for travelers.
     * Called when the storyteller sends roles, replacing the traveler set wholesale.
     */
    public static void initializeExileEligibility(Set<UUID> travelers) {
        canBeExiled.clear();
        for (UUID traveler : travelers) {
            canBeExiled.put(traveler, true);
        }
    }

    /**
     * Resets exile eligibility for all current travelers at dawn/dusk. Preserves the
     * keyset (= travelers) and just flips every value back to true.
     */
    public static void resetExileEligibilityDaily() {
        for (UUID traveler : canBeExiled.keySet()) {
            canBeExiled.put(traveler, true);
        }
    }

    // Current exile state getters/setters
    public static UUID getCurrentExileCaller() {
        return currentExileCaller;
    }

    public static void setCurrentExileCaller(UUID caller) {
        currentExileCaller = caller;
    }

    public static UUID getCurrentExileTarget() {
        return currentExileTarget;
    }

    public static void setCurrentExileTarget(UUID target) {
        currentExileTarget = target;
    }

    public static boolean hasActiveExile() {
        return currentExileCaller != null && currentExileTarget != null;
    }

    // Exile support state
    public static boolean isExileSupportInProgress() {
        return exileSupportInProgress;
    }

    public static void startExileSupport() {
        exileSupportInProgress = true;
        exileSupportVotes.clear();
        lockedExileSupportVotes.clear();
    }

    public static void endExileSupport() {
        exileSupportInProgress = false;
        exileSupportVotes.clear();
        lockedExileSupportVotes.clear();
    }

    // Exile support vote tracking
    public static void setExileSupportVote(UUID player, boolean vote) {
        exileSupportVotes.put(player, vote);
    }

    public static Map<UUID, Boolean> getExileSupportVotes() {
        return new HashMap<>(exileSupportVotes);
    }

    public static void lockExileSupportVote(UUID player, boolean vote) {
        lockedExileSupportVotes.put(player, vote);
    }

    public static Map<UUID, Boolean> getLockedExileSupportVotes() {
        return new HashMap<>(lockedExileSupportVotes);
    }

    public static int getLockedExileSupportCount() {
        int count = 0;
        for (Boolean vote : lockedExileSupportVotes.values()) {
            if (vote) {
                count++;
            }
        }
        return count;
    }

    // Ghost used block tracking, shared by Voudon mode and exile support. Merges rather
    // than replaces, so an exile called during Voudon mode can't drop the seats Voudon
    // already saved; getAndClearGhostUsedBlockSeats is what empties the set.
    public static void saveGhostUsedBlockSeats(Set<Integer> seats) {
        seatsWithGhostUsedBlocks.addAll(seats);
    }

    public static Set<Integer> getAndClearGhostUsedBlockSeats() {
        Set<Integer> seats = new HashSet<>(seatsWithGhostUsedBlocks);
        seatsWithGhostUsedBlocks.clear();
        return seats;
    }

    /**
     * Resets the current exile call (but not eligibility).
     */
    public static void resetExile() {
        currentExileCaller = null;
        currentExileTarget = null;
        if (exileSupportInProgress) {
            endExileSupport();
        }
    }

}
