package com.autumnwind.botb.util;

/**
 * Represents the current phase of the game for UI purposes.
 * Used by AssignRolesScreen to determine which buttons to display.
 */
public enum GamePhase {
    /**
     * Setup phase (Night 0, Day 0).
     * Script import, role shuffling, seat assignment available.
     */
    SETUP,

    /**
     * Night phase (nightCount != dayCount).
     * Night order navigation, player visits happening.
     */
    NIGHT,

    /**
     * Day phase (nightCount == dayCount, not 0,0) with nominations closed.
     * Discussion time, waiting for nominations to open.
     */
    DAY,

    /**
     * Nominations open, no current nominee, no player marked for execution.
     * Storyteller can select nominator/nominee.
     */
    NOMINATIONS,

    /**
     * A player has been nominated but vote hasn't concluded.
     * Run Vote and Reset options available.
     */
    PLAYER_NOMINATED,

    /**
     * No current nominee but a player is marked for execution.
     * Execute or Execute Fail options available.
     */
    PLAYER_MARKED,

    /**
     * A traveler has been called for exile but support vote hasn't started.
     * Run Support and Reset options available.
     * Can occur during DAY or NOMINATIONS phases.
     */
    CALL_FOR_EXILE,

    /**
     * Exile support vote is in progress.
     * All players can support (vote), dead players included without consuming ghost votes.
     */
    EXILE_SUPPORT;

    /**
     * Determines the current game phase based on client state.
     *
     * @param currentNight the current night count
     * @param currentDay the current day count
     * @param nominationsOpen whether nominations are open
     * @param currentNominee the UUID of current nominee (null if none)
     * @param markedForExecution the UUID of marked player (null if none)
     * @return the current GamePhase
     */
    public static GamePhase determine(int currentNight, int currentDay,
            boolean nominationsOpen, Object currentNominee, Object markedForExecution) {
        return determine(currentNight, currentDay, nominationsOpen, currentNominee,
                markedForExecution, null, false);
    }

    /**
     * Determines the current game phase based on client state, including exile state.
     *
     * @param currentNight the current night count
     * @param currentDay the current day count
     * @param nominationsOpen whether nominations are open
     * @param currentNominee the UUID of current nominee (null if none)
     * @param markedForExecution the UUID of marked player (null if none)
     * @param currentExileTarget the UUID of traveler called for exile (null if none)
     * @param exileSupportInProgress whether exile support vote is in progress
     * @return the current GamePhase
     */
    public static GamePhase determine(int currentNight, int currentDay,
            boolean nominationsOpen, Object currentNominee, Object markedForExecution,
            Object currentExileTarget, boolean exileSupportInProgress) {

        // Setup phase: Night 0 and Day 0
        if (currentNight == 0 && currentDay == 0) {
            return SETUP;
        }

        // Night phase: night is ahead of day
        if (currentNight != currentDay) {
            return NIGHT;
        }

        // From here, we're in daytime (night == day, both > 0)

        // Check exile state first - exile can happen during DAY or NOMINATIONS
        if (exileSupportInProgress) {
            return EXILE_SUPPORT;
        }

        if (currentExileTarget != null) {
            return CALL_FOR_EXILE;
        }

        // If nominations are not open, it's regular day phase
        if (!nominationsOpen) {
            return DAY;
        }

        // Nominations are open - determine subphase

        // If someone is currently nominated
        if (currentNominee != null) {
            return PLAYER_NOMINATED;
        }

        // If no nominee but someone is marked for execution
        if (markedForExecution != null) {
            return PLAYER_MARKED;
        }

        // Nominations open but no nominee and no one marked
        return NOMINATIONS;
    }
}
