package com.autumnwind.botb.daytime;

/**
 * Represents the type of election being conducted.
 * Used to determine which rules and behaviors apply during the voting process.
 */
public enum ElectionType {
    /**
     * Regular nomination -> vote flow.
     * - Ghost votes are consumed
     * - Banshee double vote applies
     * - Organ Grinder mode can hide results
     * - Legion tracking applies
     * - Threshold uses alive player count
     * - Triggers role abilities (Flower Girl, Town Crier)
     */
    VOTE,

    /**
     * Exile call -> exile support flow.
     * - Ghost votes are NOT consumed (all can vote freely)
     * - No Banshee double vote
     * - No Organ Grinder hiding
     * - No Legion tracking
     * - Threshold uses total player count
     * - Does NOT trigger role abilities
     */
    EXILE_SUPPORT
}
