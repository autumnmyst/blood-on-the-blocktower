package com.autumnwind.botb.daytime;

import java.util.*;

/**
 * Server-side state for an active election, either a vote or an exile support call. Both flow
 * through here, so {@link VotingManager} and {@link ExileSupportManager} share one shape.
 *
 * <ul>
 *   <li>Election state only. Eligibility lives in {@link DaytimeState}.</li>
 *   <li>{@link ElectionConfig} carries what differs between the two types.</li>
 *   <li>Rule-specific vote logic (Organ Grinder, Banshee, Legion, ghost votes) is reached
 *       through {@link DaytimeState}.</li>
 * </ul>
 */
public class ElectionState {

    // Election type and configuration
    private static ElectionType type = null;
    private static ElectionConfig config = null;

    // Target and initiator (unified naming)
    // For VOTE: target = nominee, initiator = nominator
    // For EXILE_SUPPORT: target = exile target, initiator = exile caller
    private static UUID target = null;
    private static UUID initiator = null;

    // Voting order (built once at election start)
    private static List<UUID> electionOrder = new ArrayList<>();

    // Current progress through the voting order
    private static int currentPlayerIndex = 0;

    // Whether the actual voting phase is in progress (as opposed to just being called/nominated)
    private static boolean votingPhaseActive = false;

    // Timing
    private static long startTime = 0;

    // Lever states during this election (mirrors what's in DaytimeState but unified here)
    private static final Map<UUID, Boolean> leverStates = new HashMap<>();

    // Locked votes during this election
    private static final Map<UUID, Boolean> lockedVotes = new HashMap<>();

    // ========== Lifecycle Methods ==========

    /**
     * Begins a new election (nomination or exile call).
     * This sets up the election but does NOT start the voting phase.
     *
     * @param electionType The type of election (VOTE or EXILE_SUPPORT)
     * @param electionConfig The configuration for this election
     * @param electionTarget The nominee (VOTE) or exile target (EXILE_SUPPORT)
     * @param electionInitiator The nominator (VOTE) or exile caller (EXILE_SUPPORT)
     * @param seatNumbers Map of player UUIDs to seat numbers for building election order
     */
    public static void beginElection(ElectionType electionType, ElectionConfig electionConfig,
                                      UUID electionTarget, UUID electionInitiator,
                                      Map<UUID, Integer> seatNumbers) {
        type = electionType;
        config = electionConfig;
        target = electionTarget;
        initiator = electionInitiator;
        currentPlayerIndex = 0;
        votingPhaseActive = false;
        startTime = 0;
        leverStates.clear();
        lockedVotes.clear();

        // Build election order starting one seat higher than target
        electionOrder = ElectionManager.buildElectionOrder(electionTarget, seatNumbers);
    }

    /**
     * Starts the voting phase of the election.
     * Called when the storyteller clicks "Run Vote" or "Run Exile Support".
     */
    public static void beginVotingPhase() {
        if (type == null) {
            throw new IllegalStateException("Cannot begin voting phase without an active election");
        }
        votingPhaseActive = true;
        startTime = System.currentTimeMillis();
        currentPlayerIndex = 0;
        lockedVotes.clear();
    }

    /**
     * Ends the current election and clears all state.
     */
    public static void endElection() {
        type = null;
        config = null;
        target = null;
        initiator = null;
        electionOrder.clear();
        currentPlayerIndex = 0;
        votingPhaseActive = false;
        startTime = 0;
        leverStates.clear();
        lockedVotes.clear();
    }

    /**
     * Resets just the voting phase (keeps the election/nomination active).
     * Used when resetting a vote without clearing the nomination.
     */
    public static void resetVotingPhase() {
        votingPhaseActive = false;
        startTime = 0;
        currentPlayerIndex = 0;
        lockedVotes.clear();
        // Keep leverStates - they track current lever positions
    }

    /**
     * Updates the election config without reinitializing the election.
     * Called when the voting phase starts (when OG mode flag is known).
     *
     * @param newConfig The updated configuration
     */
    public static void updateConfig(ElectionConfig newConfig) {
        if (newConfig != null) {
            config = newConfig;
        }
    }

    // ========== Query Methods ==========

    /**
     * @return true if there's an active election (nomination or exile call)
     */
    public static boolean hasActiveElection() {
        return type != null && target != null;
    }

    /**
     * @return true if the voting phase is currently in progress
     */
    public static boolean isVotingPhaseActive() {
        return votingPhaseActive;
    }

    /**
     * @return true if this is a regular vote (not exile support)
     */
    public static boolean isVoteType() {
        return type == ElectionType.VOTE;
    }

    /**
     * @return true if this is an exile support vote
     */
    public static boolean isExileSupportType() {
        return type == ElectionType.EXILE_SUPPORT;
    }

    /**
     * @return copy of the election order (voting order)
     */
    public static List<UUID> getElectionOrder() {
        return new ArrayList<>(electionOrder);
    }

    // ========== Lever State Methods ==========

    /**
     * Sets the lever state for a player.
     */
    public static void setLeverState(UUID player, boolean state) {
        leverStates.put(player, state);
    }

    // ========== Locked Vote Methods ==========

    /**
     * Locks a player's vote with the given value.
     */
    public static void lockVote(UUID player, boolean vote) {
        lockedVotes.put(player, vote);
    }

    /**
     * Gets the count of locked votes.
     * For VOTE type, this respects Banshee double vote multipliers.
     * For EXILE_SUPPORT type, each vote counts as 1.
     */
    public static int getLockedVoteCount() {
        int count = 0;
        for (Map.Entry<UUID, Boolean> entry : lockedVotes.entrySet()) {
            if (entry.getValue()) {
                if (config != null && config.applyBansheeMultiplier()) {
                    // Apply Banshee double vote multiplier (for regular votes)
                    count += DaytimeState.getVoteMultiplier(entry.getKey());
                } else {
                    // No multiplier (exile support)
                    count++;
                }
            }
        }
        return count;
    }

    // ========== Utility Methods ==========

    /**
     * Syncs lever states from world state.
     * Called at the start of voting phase to capture initial lever positions.
     *
     * @param worldLeverStates Map of player UUIDs to their lever states from the world
     */
    public static void syncLeverStatesFromWorld(Map<UUID, Boolean> worldLeverStates) {
        leverStates.clear();
        leverStates.putAll(worldLeverStates);
    }
}
