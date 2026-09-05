package com.autumnwind.botb.states;

import com.autumnwind.botb.util.PendingRoleAssignment;
import com.autumnwind.botb.util.Script;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Set;
import java.util.HashSet;

public class ServerState {
    /**
     * What each player was last told they are, the doctored map the storyteller sent, in which
     * a Drunk reads as their Townsfolk and an Ogre reads as good. The true grimoire never leaves
     * the storyteller's client, so nothing here may be used to decide game rules. It exists to
     * re-send a player their own role when they reconnect.
     */
    public static final Map<UUID, PendingRoleAssignment> PLAYER_ROLES = new HashMap<>();
    public static final Map<UUID, Integer> PLAYER_SEAT_NUMBERS = new HashMap<>();
    public static final Map<UUID, Boolean> PLAYER_DEATH_STATUS = new HashMap<>();

    // Day/Night tracking (starts at Night 0, Day 0)
    public static int currentNight = 0;
    public static int currentDay = 0;


    // Server-side script cache. Updated on every AssignRolesC2SPayload that includes a
    // script, so a player who joins (or rejoins) mid-game can request it via
    // RequestScriptC2SPayload and receive the latest broadcast state. In-memory only, and a
    // server restart clears it until the storyteller next sends roles.
    public static Script currentScript = null;

    // Execution tracking for Undertaker (reset at Dawn)
    public static boolean executionToday = false;

    // Set by End Game and cleared only by the resets. While set, dead players are no
    // longer made invisible (their death mark stays), so everyone can see each other again.
    public static boolean gameEnded = false;

    /** Every player currently marked dead. */
    public static Set<UUID> deadPlayers() {
        Set<UUID> dead = new HashSet<>();
        for (Map.Entry<UUID, Boolean> entry : PLAYER_DEATH_STATUS.entrySet()) {
            if (entry.getValue()) {
                dead.add(entry.getKey());
            }
        }
        return dead;
    }

    public static void updateRoles(Map<UUID, PendingRoleAssignment> newRoles) {
        PLAYER_ROLES.clear();
        PLAYER_ROLES.putAll(newRoles);
    }

    public static void updateSeats(Map<UUID, Integer> newSeats) {
        PLAYER_SEAT_NUMBERS.clear();
        PLAYER_SEAT_NUMBERS.putAll(newSeats);
    }

    public static void updateDeathStatus(Map<UUID, Boolean> newDeathStatus) {
        PLAYER_DEATH_STATUS.clear();
        PLAYER_DEATH_STATUS.putAll(newDeathStatus);
    }
}
