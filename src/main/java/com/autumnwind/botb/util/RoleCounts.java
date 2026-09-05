package com.autumnwind.botb.util;

public final class RoleCounts {

    // A simple record to hold the counts for a given player number.
    public record RoleCountInfo(int townsfolk, int outsiders, int minions, int demon) {}

    /**
     * Gets the default role counts for a given number of active players.
     * @param activePlayerCount The number of players with assigned roles.
     * @return A RoleCountInfo object, or null if the count is below the minimum of 5.
     */
    public static RoleCountInfo getCounts(int activePlayerCount) {
        if (activePlayerCount < 1) {
            return null; // Not a valid game size
        }
        if (activePlayerCount <= 5) return new RoleCountInfo(3, 0, 1, 1);
        if (activePlayerCount <= 6) return new RoleCountInfo(3, 1, 1, 1);
        if (activePlayerCount <= 7) return new RoleCountInfo(5, 0, 1, 1);
        if (activePlayerCount <= 8) return new RoleCountInfo(5, 1, 1, 1);
        if (activePlayerCount <= 9) return new RoleCountInfo(5, 2, 1, 1);
        if (activePlayerCount <= 10) return new RoleCountInfo(7, 0, 2, 1);
        if (activePlayerCount <= 11) return new RoleCountInfo(7, 1, 2, 1);
        if (activePlayerCount <= 12) return new RoleCountInfo(7, 2, 2, 1);
        if (activePlayerCount <= 13) return new RoleCountInfo(9, 0, 3, 1);
        if (activePlayerCount <= 14) return new RoleCountInfo(9, 1, 3, 1);
        // This covers 15 and above
        return new RoleCountInfo(9, 2, 3, 1);
    }
}