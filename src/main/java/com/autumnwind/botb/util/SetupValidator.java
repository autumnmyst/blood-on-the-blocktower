package com.autumnwind.botb.util;

import java.util.*;
import net.minecraft.network.chat.Component;

/**
 * Validates Blood on the Clocktower game setup for role assignments.
 * Checks that the assigned roles match valid setup configurations based on:
 * - Base role counts for the player count
 * - Role-specific modifiers (Baron +2 outsiders, etc.)
 * - Role requirements (Huntsman needs Damsel, Choirboy needs King, etc.)
 * - Alignment requirements (Bounty Hunter needs 1 evil townsfolk, Atheist has no evil, etc.)
 */
public class SetupValidator {

    /**
     * Result of validating a setup.
     */
    public record ValidationResult(boolean isValid, List<Component> errors, SetupCounts expectedCounts, SetupCounts actualCounts) {
        public static ValidationResult valid(SetupCounts expected, SetupCounts actual) {
            return new ValidationResult(true, Collections.emptyList(), expected, actual);
        }

        public static ValidationResult invalid(List<Component> errors, SetupCounts expected, SetupCounts actual) {
            return new ValidationResult(false, errors, expected, actual);
        }
    }

    /**
     * Represents the setup counts with possible ranges.
     */
    public record SetupCounts(
            int townsfolk, int outsiders, int minions, int demons,
            int minOutsiders, int maxOutsiders, // For flexible outsider counts
            int evilTownsfolk, // Number of townsfolk that must be evil
            boolean noEvil, // Atheist game - no evil players
            boolean isLegionGame, // Legion special setup
            boolean isSummonerGame, // Summoner - no demon at start, +1 minion
            boolean isLordOfTyphonGame, // Lord of Typhon - +1 minion (reduces total good by 1)
            boolean allowsLilMonstaSetup, // Script has Lil' Monsta, allowing demon -> minion swap
            boolean isKazaliGame // Kazali - 1 demon (Kazali), 0 minions at setup, minions chosen on night 1
    ) {
        public SetupCounts withOutsiderRange(int min, int max) {
            return new SetupCounts(townsfolk, outsiders, minions, demons, min, max, evilTownsfolk, noEvil, isLegionGame, isSummonerGame, isLordOfTyphonGame, allowsLilMonstaSetup, isKazaliGame);
        }

        public SetupCounts withEvilTownsfolk(int count) {
            return new SetupCounts(townsfolk, outsiders, minions, demons, minOutsiders, maxOutsiders, count, noEvil, isLegionGame, isSummonerGame, isLordOfTyphonGame, allowsLilMonstaSetup, isKazaliGame);
        }

        public SetupCounts withNoEvil(boolean noEvil) {
            return new SetupCounts(townsfolk, outsiders, minions, demons, minOutsiders, maxOutsiders, evilTownsfolk, noEvil, isLegionGame, isSummonerGame, isLordOfTyphonGame, allowsLilMonstaSetup, isKazaliGame);
        }

        public SetupCounts withLegion(boolean isLegion) {
            return new SetupCounts(townsfolk, outsiders, minions, demons, minOutsiders, maxOutsiders, evilTownsfolk, noEvil, isLegion, isSummonerGame, isLordOfTyphonGame, allowsLilMonstaSetup, isKazaliGame);
        }

        public SetupCounts withSummoner(boolean isSummoner) {
            return new SetupCounts(townsfolk, outsiders, minions, demons, minOutsiders, maxOutsiders, evilTownsfolk, noEvil, isLegionGame, isSummoner, isLordOfTyphonGame, allowsLilMonstaSetup, isKazaliGame);
        }

        public SetupCounts withLordOfTyphon(boolean isLordOfTyphon) {
            return new SetupCounts(townsfolk, outsiders, minions, demons, minOutsiders, maxOutsiders, evilTownsfolk, noEvil, isLegionGame, isSummonerGame, isLordOfTyphon, allowsLilMonstaSetup, isKazaliGame);
        }

        public SetupCounts withLilMonsta(boolean hasLilMonsta) {
            return new SetupCounts(townsfolk, outsiders, minions, demons, minOutsiders, maxOutsiders, evilTownsfolk, noEvil, isLegionGame, isSummonerGame, isLordOfTyphonGame, hasLilMonsta, isKazaliGame);
        }

        public SetupCounts withKazali(boolean isKazali) {
            return new SetupCounts(townsfolk, outsiders, minions, demons, minOutsiders, maxOutsiders, evilTownsfolk, noEvil, isLegionGame, isSummonerGame, isLordOfTyphonGame, allowsLilMonstaSetup, isKazali);
        }

        public static SetupCounts fromBase(RoleCounts.RoleCountInfo base) {
            return new SetupCounts(
                    base.townsfolk(), base.outsiders(), base.minions(), base.demon(),
                    base.outsiders(), base.outsiders(), // Default: exact outsider count
                    0, false, false, false, false, false, false
            );
        }
    }

    /**
     * Expected counts with special setups (Summoner, Kazali, Lil' Monsta, Lord of Typhon)
     * resolved. Shared by validateCounts and the client's setup-counts display so the
     * numbers can't drift apart.
     */
    public record ExpectedCounts(int minions, int demons, int goodRoles) {}

    /**
     * Resolves the expected Minion/Demon counts and total good-role count for a setup.
     * actualDemons decides whether a Lil' Monsta 0-demon setup is in effect.
     */
    public static ExpectedCounts resolveExpectedCounts(SetupCounts expected, int actualDemons) {
        int minions;
        int demons;
        if (expected.isSummonerGame()) {
            minions = expected.minions() + 1;
            demons = 0;
        } else if (expected.isKazaliGame()) {
            // 1 Demon (the Kazali), 0 Minions until Kazali picks them on night 1
            minions = 0;
            demons = 1;
        } else if (expected.allowsLilMonstaSetup() && actualDemons == 0) {
            minions = expected.minions() + 1;
            demons = 0;
        } else {
            minions = expected.minions() + (expected.isLordOfTyphonGame() ? 1 : 0);
            demons = expected.demons();
        }
        // LoT's extra Minion removes one good slot; Kazali's empty Minion slots are
        // filled with good roles at setup
        int goodRoles = expected.townsfolk() + expected.outsiders()
                - (expected.isLordOfTyphonGame() ? 1 : 0)
                + (expected.isKazaliGame() ? expected.minions() : 0);
        return new ExpectedCounts(minions, demons, goodRoles);
    }

    /**
     * Validates a Blood on the Clocktower game setup, including checks that hidden roles
     * (Drunk, Marionette, Lunatic) have the required fake role reminder type placed on them.
     *
     * @param pendingRoles Map of player UUID to their pending role assignment
     * @param pendingSeatNumbers Map of player UUID to their seat number
     * @param scriptRoles Optional list of roles on the script (for checking Lil' Monsta)
     * @param script The current script (for Sentinel/Spirit of Ivory, and custom role types)
     * @param pendingReminders Map of player UUID to their pending reminder tokens
     * @param script The current script (needed to resolve custom role types for reminders)
     * @return ValidationResult with validity status and any errors
     */
    public static ValidationResult validate(
            Map<UUID, PendingRoleAssignment> pendingRoles,
            Map<UUID, Integer> pendingSeatNumbers,
            List<Role> scriptRoles,
            Map<UUID, List<Reminder>> pendingReminders,
            Script script
    ) {
        List<Component> errors = new ArrayList<>();

        // Count seated players with assigned roles (official or custom)
        // Travelers are excluded from the count that determines role type requirements
        int playerCount = 0;
        for (Map.Entry<UUID, PendingRoleAssignment> entry : pendingRoles.entrySet()) {
            if (pendingSeatNumbers.containsKey(entry.getKey()) &&
                    pendingSeatNumbers.get(entry.getKey()) > 0 &&
                    hasAssignedRole(entry.getValue())) {
                // Exclude travelers from the count
                RoleType roleType = getAssignmentRoleType(entry.getValue());
                if (roleType != RoleType.TRAVELER) {
                    playerCount++;
                }
            }
        }

        if (playerCount < 5) {
            errors.add(Component.translatable("gui.blood-on-the-blocktower.validation.min_players", playerCount));
            return ValidationResult.invalid(errors, null, null);
        }

        // Get base counts
        RoleCounts.RoleCountInfo baseCounts = RoleCounts.getCounts(playerCount);
        if (baseCounts == null) {
            errors.add(Component.translatable("gui.blood-on-the-blocktower.validation.invalid_player_count", playerCount));
            return ValidationResult.invalid(errors, null, null);
        }

        // Build expected counts with modifiers
        SetupCounts expected = calculateExpectedCounts(pendingRoles, pendingSeatNumbers, baseCounts, scriptRoles, script);

        // Count actual roles
        SetupCounts actual = countActualRoles(pendingRoles, pendingSeatNumbers);

        // Validate the setup
        validateCounts(expected, actual, pendingRoles, pendingSeatNumbers, errors);
        validateRoleRequirements(pendingRoles, pendingSeatNumbers, errors);
        validateAlignments(expected, pendingRoles, pendingSeatNumbers, script, errors);
        validateHiddenRoleReminders(pendingRoles, pendingSeatNumbers, pendingReminders, script, errors);
        validateLordOfTyphonSeating(pendingRoles, pendingSeatNumbers, errors);

        if (errors.isEmpty()) {
            return ValidationResult.valid(expected, actual);
        } else {
            return ValidationResult.invalid(errors, expected, actual);
        }
    }

    /**
     * Calculates expected counts based on base counts and role modifiers.
     */
    private static SetupCounts calculateExpectedCounts(
            Map<UUID, PendingRoleAssignment> pendingRoles,
            Map<UUID, Integer> pendingSeatNumbers,
            RoleCounts.RoleCountInfo baseCounts,
            List<Role> scriptRoles,
            Script script
    ) {
        SetupCounts counts = SetupCounts.fromBase(baseCounts);

        int outsiderModifier = 0;
        int minOutsiderMod = 0;
        int maxOutsiderMod = 0;
        boolean hasFlexibleOutsiders = false; // For Xaan, Kazali, Lord of Typhon
        boolean hasAtheist = false;
        boolean hasLegion = false;
        boolean hasSummoner = false;
        boolean hasLordOfTyphon = false;
        boolean hasKazali = false;
        int bountyHunterCount = 0;
        boolean scriptHasLilMonsta = scriptRoles != null && scriptRoles.contains(Role.LIL_MONSTA);

        // Check assigned roles for modifiers (only official roles have modifiers)
        for (Map.Entry<UUID, PendingRoleAssignment> entry : pendingRoles.entrySet()) {
            if (!pendingSeatNumbers.containsKey(entry.getKey()) || pendingSeatNumbers.get(entry.getKey()) <= 0) {
                continue; // Skip unseated players
            }

            PendingRoleAssignment assignment = entry.getValue();
            if (!hasAssignedRole(assignment)) continue;

            // Custom roles don't have setup modifiers, skip them
            if (assignment.isCustomRole()) continue;

            Role role = assignment.role();

            switch (role) {
                case BARON -> outsiderModifier += 2;
                case GODFATHER -> {
                    // +1 or -1 outsider (each instance contributes independently)
                    minOutsiderMod -= 1;
                    maxOutsiderMod += 1;
                }
                case FANG_GU -> outsiderModifier += 1;
                case VIGORMORTIS -> outsiderModifier -= 1;
                case BALLOONIST -> maxOutsiderMod += 1; // +0 or +1 outsider
                case HUNTSMAN -> maxOutsiderMod += 1;   // Damsel can add +1 outsider
                case HERMIT -> minOutsiderMod -= 1;     // -0 or -1 outsider
                case MARIONETTE -> minOutsiderMod -= 1; // Can take an outsider slot
                case BOUNTY_HUNTER -> bountyHunterCount++;
                case ATHEIST -> hasAtheist = true;
                case XAAN -> hasFlexibleOutsiders = true;
                case KAZALI -> {
                    hasFlexibleOutsiders = true;
                    hasKazali = true;
                }
                case LORD_OF_TYPHON -> {
                    hasFlexibleOutsiders = true;
                    hasLordOfTyphon = true;
                }
                case LEGION -> hasLegion = true;
                case SUMMONER -> hasSummoner = true;
                default -> {}
            }
        }

        // Apply outsider modifier
        int baseOutsiders = baseCounts.outsiders() + outsiderModifier;
        int minOutsiders = baseOutsiders + minOutsiderMod;
        int maxOutsiders = baseOutsiders + maxOutsiderMod;

        // Clamp to valid range
        minOutsiders = Math.max(0, minOutsiders);
        maxOutsiders = Math.max(minOutsiders, maxOutsiders);

        // For flexible outsider roles, allow any number of outsiders replacing townsfolk
        if (hasFlexibleOutsiders) {
            minOutsiders = 0;
            // All good roles could be outsiders. LoT shrinks good total by 1 due to +1 Minion, and
            // Kazali grows good total by baseMinions (Minion slots filled with good roles at
            // setup until Kazali picks Minions on night 1).
            maxOutsiders = baseCounts.townsfolk() + baseCounts.outsiders()
                    - (hasLordOfTyphon ? 1 : 0)
                    + (hasKazali ? baseCounts.minions() : 0);
        }

        // Sentinel fabled: +1/-1 outsider flexibility
        boolean hasSentinel = script != null && script.hasFabledOrLoric("sentinel");
        if (hasSentinel) {
            minOutsiders = Math.max(0, minOutsiders - 1);
            maxOutsiders = maxOutsiders + 1;
        }

        counts = counts.withOutsiderRange(minOutsiders, maxOutsiders);

        // Bounty Hunter requires 1 evil townsfolk per Bounty Hunter
        if (bountyHunterCount > 0) {
            counts = counts.withEvilTownsfolk(bountyHunterCount);
        }

        // Atheist - no evil players
        if (hasAtheist) {
            counts = counts.withNoEvil(true);
        }

        // Legion - special setup
        if (hasLegion) {
            counts = counts.withLegion(true);
        }

        // Summoner - no demon, +1 minion
        if (hasSummoner) {
            counts = counts.withSummoner(true);
        }

        // Lord of Typhon - +1 minion (reduces total good by 1)
        if (hasLordOfTyphon) {
            counts = counts.withLordOfTyphon(true);
        }

        // Kazali - 1 demon (the Kazali), 0 minions at setup, and Kazali picks Minions on night 1
        if (hasKazali) {
            counts = counts.withKazali(true);
        }

        // Lil' Monsta on script allows demon->minion swap
        if (scriptHasLilMonsta) {
            counts = counts.withLilMonsta(true);
        }

        return counts;
    }

    /**
     * Counts actual role assignments (including custom roles).
     */
    private static SetupCounts countActualRoles(
            Map<UUID, PendingRoleAssignment> pendingRoles,
            Map<UUID, Integer> pendingSeatNumbers
    ) {
        int townsfolk = 0, outsiders = 0, minions = 0, demons = 0;
        int evilTownsfolk = 0;

        for (Map.Entry<UUID, PendingRoleAssignment> entry : pendingRoles.entrySet()) {
            if (!pendingSeatNumbers.containsKey(entry.getKey()) || pendingSeatNumbers.get(entry.getKey()) <= 0) {
                continue;
            }

            PendingRoleAssignment assignment = entry.getValue();
            if (!hasAssignedRole(assignment)) continue;

            RoleType roleType = getAssignmentRoleType(assignment);

            switch (roleType) {
                case TOWNSFOLK -> {
                    townsfolk++;
                    if (!assignment.isFinalGood()) {
                        evilTownsfolk++;
                    }
                }
                case OUTSIDER -> outsiders++;
                case MINION -> minions++;
                case DEMON -> demons++;
                case TRAVELER -> {} // Travelers don't affect role type counts
                default -> {}
            }
        }

        return new SetupCounts(townsfolk, outsiders, minions, demons, outsiders, outsiders, evilTownsfolk, false, false, false, false, false, false);
    }

    /**
     * Validates role counts against expected values.
     */
    private static void validateCounts(
            SetupCounts expected,
            SetupCounts actual,
            Map<UUID, PendingRoleAssignment> pendingRoles,
            Map<UUID, Integer> pendingSeatNumbers,
            List<Component> errors
    ) {
        // Special case: Legion game
        if (expected.isLegionGame()) {
            validateLegionSetup(pendingRoles, pendingSeatNumbers, errors);
            return;
        }

        // Special case: Atheist game (all players are good, no demons/minions)
        if (expected.noEvil()) {
            if (actual.demons() > 0) {
                errors.add(Component.translatable("gui.blood-on-the-blocktower.validation.atheist_no_demons", actual.demons()));
            }
            if (actual.minions() > 0) {
                errors.add(Component.translatable("gui.blood-on-the-blocktower.validation.atheist_no_minions", actual.minions()));
            }
            // All roles should be Townsfolk/Outsiders - no count check needed
            return;
        }

        ExpectedCounts resolved = resolveExpectedCounts(expected, actual.demons());

        // Demon/Minion checks; branches only pick the error wording
        if (expected.isSummonerGame()) {
            if (actual.demons() != resolved.demons()) {
                errors.add(Component.translatable("gui.blood-on-the-blocktower.validation.summoner_no_demons", actual.demons()));
            }
            if (actual.minions() != resolved.minions()) {
                errors.add(Component.translatable("gui.blood-on-the-blocktower.validation.summoner_minions", resolved.minions(), actual.minions()));
            }
        } else if (expected.isKazaliGame()) {
            if (actual.demons() != resolved.demons()) {
                errors.add(Component.translatable("gui.blood-on-the-blocktower.validation.kazali_one_demon", actual.demons()));
            }
            if (actual.minions() != resolved.minions()) {
                errors.add(Component.translatable("gui.blood-on-the-blocktower.validation.kazali_no_minions", actual.minions()));
            }
        } else if (expected.allowsLilMonstaSetup() && actual.demons() == 0) {
            if (actual.minions() != resolved.minions()) {
                errors.add(Component.translatable("gui.blood-on-the-blocktower.validation.lilmonsta_minions", resolved.minions(), actual.minions()));
            }
        } else {
            if (actual.demons() != resolved.demons()) {
                errors.add(Component.translatable("gui.blood-on-the-blocktower.validation.demons", resolved.demons(), actual.demons()));
            }
            if (actual.minions() != resolved.minions()) {
                errors.add(Component.translatable("gui.blood-on-the-blocktower.validation.minions", resolved.minions(), actual.minions()));
            }
        }

        // Outsider count validation (with range)
        if (actual.outsiders() < expected.minOutsiders() || actual.outsiders() > expected.maxOutsiders()) {
            if (expected.minOutsiders() == expected.maxOutsiders()) {
                errors.add(Component.translatable("gui.blood-on-the-blocktower.validation.outsiders", expected.minOutsiders(), actual.outsiders()));
            } else {
                errors.add(Component.translatable("gui.blood-on-the-blocktower.validation.outsiders_range", expected.minOutsiders(), expected.maxOutsiders(), actual.outsiders()));
            }
        }

        int actualGoodRoles = actual.townsfolk() + actual.outsiders();

        // For flexible outsiders (Xaan, Kazali, Lord of Typhon), just check total good roles
        if (expected.minOutsiders() == 0 && expected.maxOutsiders() > expected.outsiders()) {
            // Flexible - check total
            if (actualGoodRoles != resolved.goodRoles()) {
                errors.add(Component.translatable("gui.blood-on-the-blocktower.validation.good_roles", resolved.goodRoles(), actualGoodRoles));
            }
        } else {
            // Non-flexible - townsfolk count matters
            int expectedTownsfolk = resolved.goodRoles() - actual.outsiders(); // Remaining must be townsfolk
            if (expectedTownsfolk < 0) expectedTownsfolk = 0;

            // Only check if outsiders are in valid range
            if (actual.outsiders() >= expected.minOutsiders() && actual.outsiders() <= expected.maxOutsiders()) {
                if (actual.townsfolk() != expectedTownsfolk) {
                    errors.add(Component.translatable("gui.blood-on-the-blocktower.validation.townsfolk", expectedTownsfolk, actual.townsfolk()));
                }
            }
        }
    }

    /**
     * Validates Legion special setup.
     * Legion: majority are Legion (the only demon type allowed), no minions,
     * and the remaining players can be any mix of townsfolk and outsiders.
     */
    private static void validateLegionSetup(
            Map<UUID, PendingRoleAssignment> pendingRoles,
            Map<UUID, Integer> pendingSeatNumbers,
            List<Component> errors
    ) {
        int totalSeated = 0;
        int legionCount = 0;
        int minionCount = 0;
        int otherDemonCount = 0;

        for (Map.Entry<UUID, PendingRoleAssignment> entry : pendingRoles.entrySet()) {
            if (!pendingSeatNumbers.containsKey(entry.getKey()) || pendingSeatNumbers.get(entry.getKey()) <= 0) {
                continue;
            }

            PendingRoleAssignment assignment = entry.getValue();
            if (!hasAssignedRole(assignment)) continue;

            totalSeated++;

            // Check for Legion (official role only)
            if (!assignment.isCustomRole() && assignment.role() == Role.LEGION) {
                legionCount++;
            } else {
                RoleType roleType = getAssignmentRoleType(assignment);
                if (roleType == RoleType.MINION) {
                    minionCount++;
                } else if (roleType == RoleType.DEMON) {
                    otherDemonCount++;
                }
            }
            // Townsfolk and Outsiders are allowed in any combination for non-Legion players
        }

        // Majority must be Legion
        if (legionCount <= totalSeated / 2) {
            errors.add(Component.translatable("gui.blood-on-the-blocktower.validation.legion_majority", legionCount, totalSeated));
        }

        // No minions allowed
        if (minionCount > 0) {
            errors.add(Component.translatable("gui.blood-on-the-blocktower.validation.legion_no_minions", minionCount));
        }

        // No other demons allowed (only Legion)
        if (otherDemonCount > 0) {
            errors.add(Component.translatable("gui.blood-on-the-blocktower.validation.legion_only_legion", otherDemonCount));
        }
    }

    /**
     * Validates role-specific requirements.
     */
    private static void validateRoleRequirements(
            Map<UUID, PendingRoleAssignment> pendingRoles,
            Map<UUID, Integer> pendingSeatNumbers,
            List<Component> errors
    ) {
        boolean hasHuntsman = false;
        boolean hasDamsel = false;
        boolean hasChoirboy = false;
        boolean hasKing = false;
        Set<Integer> demonSeats = new HashSet<>();
        Set<Integer> marionetteSeats = new HashSet<>();

        for (Map.Entry<UUID, PendingRoleAssignment> entry : pendingRoles.entrySet()) {
            Integer seat = pendingSeatNumbers.get(entry.getKey());
            if (seat == null || seat <= 0) continue;

            PendingRoleAssignment assignment = entry.getValue();
            if (!hasAssignedRole(assignment)) continue;

            // Role requirements only apply to official roles
            if (!assignment.isCustomRole()) {
                Role role = assignment.role();
                switch (role) {
                    case HUNTSMAN -> hasHuntsman = true;
                    case DAMSEL -> hasDamsel = true;
                    case CHOIRBOY -> hasChoirboy = true;
                    case KING -> hasKing = true;
                    case MARIONETTE -> marionetteSeats.add(seat);
                    default -> {}
                }
            }

            // Track demon seats for Marionette check (both official and custom demons)
            RoleType roleType = getAssignmentRoleType(assignment);
            if (roleType == RoleType.DEMON) {
                demonSeats.add(seat);
            }
        }

        // Huntsman requires Damsel
        if (hasHuntsman && !hasDamsel) {
            errors.add(Component.translatable("gui.blood-on-the-blocktower.validation.huntsman_damsel"));
        }

        // Choirboy requires King
        if (hasChoirboy && !hasKing) {
            errors.add(Component.translatable("gui.blood-on-the-blocktower.validation.choirboy_king"));
        }

        // Each Marionette must be adjacent to a Demon (+1 or -1 seat, with wrap-around)
        if (!marionetteSeats.isEmpty() && !demonSeats.isEmpty()) {
            int maxSeat = pendingSeatNumbers.values().stream().filter(s -> s > 0).mapToInt(Integer::intValue).max().orElse(0);

            for (int marionetteSeat : marionetteSeats) {
                boolean isAdjacent = false;
                for (int demonSeat : demonSeats) {
                    int diff = Math.abs(marionetteSeat - demonSeat);
                    if (diff == 1 || diff == maxSeat - 1) {
                        isAdjacent = true;
                        break;
                    }
                }
                if (!isAdjacent) {
                    errors.add(Component.translatable("gui.blood-on-the-blocktower.validation.marionette_adjacent", marionetteSeat));
                }
            }
        }
    }

    /**
     * Validates alignment assignments.
     */
    private static void validateAlignments(
            SetupCounts expected,
            Map<UUID, PendingRoleAssignment> pendingRoles,
            Map<UUID, Integer> pendingSeatNumbers,
            Script script,
            List<Component> errors
    ) {
        int actualEvilTownsfolk = 0;
        int totalEvilPlayers = 0; // For Spirit of Ivory check
        List<Component> wrongAlignments = new ArrayList<>();

        for (Map.Entry<UUID, PendingRoleAssignment> entry : pendingRoles.entrySet()) {
            if (!pendingSeatNumbers.containsKey(entry.getKey()) || pendingSeatNumbers.get(entry.getKey()) <= 0) {
                continue;
            }

            PendingRoleAssignment assignment = entry.getValue();
            if (!hasAssignedRole(assignment)) continue;

            RoleType roleType = getAssignmentRoleType(assignment);
            boolean isDefaultGood = assignment.isRoleDefaultGood();
            boolean isFinalGood = assignment.isFinalGood();
            String displayName = assignment.getDisplayName();

            // Count evil townsfolk
            if (roleType == RoleType.TOWNSFOLK && !isFinalGood) {
                actualEvilTownsfolk++;
            }

            // Count total evil players (for Spirit of Ivory check)
            // Evil = demons, minions, or anyone with final alignment evil
            if (!isFinalGood) {
                totalEvilPlayers++;
            }

            // In Atheist game, all players must be good
            if (expected.noEvil() && !isFinalGood) {
                wrongAlignments.add(Component.translatable("gui.blood-on-the-blocktower.validation.atheist_evil_player", displayName));
            }

            // Check for unnecessary alignment overrides (not required by any role)
            // Skip if Bounty Hunter is in play (allows 1 evil townsfolk)
            // Skip if Atheist is in play (handled above)
            // Skip for custom roles (they may have different alignment rules)
            if (!expected.noEvil() && expected.evilTownsfolk() == 0 && !assignment.isCustomRole()) {
                // No role requires changed alignments
                if (isDefaultGood != isFinalGood && roleType == RoleType.TOWNSFOLK) {
                    wrongAlignments.add(Component.translatable("gui.blood-on-the-blocktower.validation.alignment_without_role", displayName));
                }
            }
        }

        // Check Bounty Hunter requirement
        if (expected.evilTownsfolk() > 0 && actualEvilTownsfolk != expected.evilTownsfolk()) {
            errors.add(Component.translatable("gui.blood-on-the-blocktower.validation.bounty_hunter_evil_townsfolk", expected.evilTownsfolk(), actualEvilTownsfolk));
        }

        // Spirit of Ivory: "There can't be more than 1 extra evil player"
        // Max allowed evil = base minions + demons + 1 (the +1 is Spirit of Ivory's allowance)
        boolean hasSpiritOfIvory = script != null && script.hasFabledOrLoric("spirit_of_ivory");
        if (hasSpiritOfIvory && !expected.noEvil() && !expected.isLegionGame()) {
            int baseEvil = expected.minions() + expected.demons();
            int maxAllowedEvil = baseEvil + 1; // Spirit of Ivory allows 1 extra evil
            if (totalEvilPlayers > maxAllowedEvil) {
                errors.add(Component.translatable("gui.blood-on-the-blocktower.validation.spirit_of_ivory", maxAllowedEvil, totalEvilPlayers));
            }
        }

        // Add wrong alignment errors
        errors.addAll(wrongAlignments);
    }

    /**
     * Validates that hidden roles have a reminder token of the required fake role type:
     *   Drunk      -> a Townsfolk reminder (their "you think you are X" townsfolk)
     *   Marionette -> a Townsfolk or Outsider reminder
     *   Lunatic    -> a Demon reminder
     *
     * Type-based so this works for custom roles too: any reminder (official or custom role)
     * whose role-type matches counts, as long as it does not point back to the hidden role itself.
     */
    private static void validateHiddenRoleReminders(
            Map<UUID, PendingRoleAssignment> pendingRoles,
            Map<UUID, Integer> pendingSeatNumbers,
            Map<UUID, List<Reminder>> pendingReminders,
            Script script,
            List<Component> errors
    ) {
        if (pendingReminders == null) return;

        // The official rules, due to the physical nature, prevent the drunk and marionette from being told an in play role
        Set<String> inPlayRoleIds = new HashSet<>();
        for (PendingRoleAssignment assignment : pendingRoles.values()) {
            if (hasAssignedRole(assignment)) inPlayRoleIds.add(assignment.getRoleId());
        }

        for (Map.Entry<UUID, PendingRoleAssignment> entry : pendingRoles.entrySet()) {
            UUID uuid = entry.getKey();
            Integer seat = pendingSeatNumbers.get(uuid);
            if (seat == null || seat <= 0) continue;

            PendingRoleAssignment assignment = entry.getValue();
            if (!hasAssignedRole(assignment)) continue;
            if (assignment.isCustomRole()) continue; // Only official hidden roles trigger this check

            Role role = assignment.role();
            if (role == Role.HERMIT) {
                validateHermitReminders(uuid, seat, pendingReminders, script, inPlayRoleIds, errors);
                continue;
            }
            if (role != Role.DRUNK && role != Role.MARIONETTE && role != Role.LUNATIC) continue;

            Set<RoleType> requiredTypes = switch (role) {
                case DRUNK -> Set.of(RoleType.TOWNSFOLK);
                case MARIONETTE -> Set.of(RoleType.TOWNSFOLK, RoleType.OUTSIDER);
                case LUNATIC -> Set.of(RoleType.DEMON);
                default -> Set.of();
            };

            List<Reminder> reminders = pendingReminders.getOrDefault(uuid, Collections.emptyList());
            boolean hasFakeRoleReminder = false;
            for (Reminder reminder : reminders) {
                // Must be a real AssociatedRoleReminder (text = role's ALL CAPS name), not
                // just a reminder that happens to carry a role's icon.
                if (!reminder.isAssociatedRoleReminder(script)) continue;
                // Skip reminders that point back to the hidden role itself
                if (reminder.role().isPresent() && reminder.role().get() == role) continue;
                if (requiredTypes.contains(reminder.getRoleType(script))) {
                    hasFakeRoleReminder = true;
                    // The Lunatic may match the real demon
                    if (role != Role.LUNATIC && inPlayRoleIds.contains(reminderRoleId(reminder))) {
                        errors.add(Component.translatable("gui.blood-on-the-blocktower.validation.fake_role_in_play",
                                role.getDisplayName(), seat, reminderDisplayName(reminder, script)));
                    }
                    break;
                }
            }

            if (!hasFakeRoleReminder) {
                errors.add(switch (role) {
                    case DRUNK -> Component.translatable("gui.blood-on-the-blocktower.validation.drunk_reminder", seat);
                    case MARIONETTE -> Component.translatable("gui.blood-on-the-blocktower.validation.marionette_reminder", seat);
                    case LUNATIC -> Component.translatable("gui.blood-on-the-blocktower.validation.lunatic_reminder", seat);
                    default -> Component.empty();
                });
            }
        }
    }

    /**
     * A Hermit with the Drunk ability needs a Townsfolk reminder, and one with the
     * Lunatic ability needs a Demon reminder - same as the real Drunk/Lunatic.
     */
    private static void validateHermitReminders(
            UUID uuid,
            int seat,
            Map<UUID, List<Reminder>> pendingReminders,
            Script script,
            Set<String> inPlayRoleIds,
            List<Component> errors
    ) {
        boolean hasDrunkAbility = false;
        boolean hasLunaticAbility = false;
        boolean hasTownsfolkReminder = false;
        boolean hasDemonReminder = false;

        for (Reminder reminder : pendingReminders.getOrDefault(uuid, Collections.emptyList())) {
            if (!reminder.isAssociatedRoleReminder(script)) continue;
            if (reminder.role().isPresent()) {
                if (reminder.role().get() == Role.DRUNK) hasDrunkAbility = true;
                if (reminder.role().get() == Role.LUNATIC) hasLunaticAbility = true;
            }
            RoleType type = reminder.getRoleType(script);
            if (type == RoleType.TOWNSFOLK) hasTownsfolkReminder = true;
            if (type == RoleType.DEMON) hasDemonReminder = true;
            // Only the Hermit's Drunk townsfolk is shown as a token; minion and demon abilities are told, so they may match
            if (type == RoleType.TOWNSFOLK && inPlayRoleIds.contains(reminderRoleId(reminder))) {
                errors.add(Component.translatable("gui.blood-on-the-blocktower.validation.hermit_fake_in_play", seat, reminderDisplayName(reminder, script)));
            }
        }

        if (hasDrunkAbility && !hasTownsfolkReminder) {
            errors.add(Component.translatable("gui.blood-on-the-blocktower.validation.hermit_drunk_reminder", seat));
        }
        if (hasLunaticAbility && !hasDemonReminder) {
            errors.add(Component.translatable("gui.blood-on-the-blocktower.validation.hermit_lunatic_reminder", seat));
        }
    }

    /**
     * Validates Lord of Typhon seating: "Evil characters are in a line. You are in the middle."
     * All evil players must be seated in a single contiguous run (with table wrap-around),
     * and the Lord of Typhon cannot be at either end of that run (both neighbors must be evil).
     * Travelers are ignored for this check.
     */
    private static void validateLordOfTyphonSeating(
            Map<UUID, PendingRoleAssignment> pendingRoles,
            Map<UUID, Integer> pendingSeatNumbers,
            List<Component> errors
    ) {
        Set<Integer> evilSeats = new HashSet<>();
        Set<Integer> lotSeats = new HashSet<>();
        int maxSeat = 0;

        for (Map.Entry<UUID, PendingRoleAssignment> entry : pendingRoles.entrySet()) {
            Integer seat = pendingSeatNumbers.get(entry.getKey());
            if (seat == null || seat <= 0) continue;

            PendingRoleAssignment assignment = entry.getValue();
            if (!hasAssignedRole(assignment)) continue;
            if (getAssignmentRoleType(assignment) == RoleType.TRAVELER) continue;

            if (seat > maxSeat) maxSeat = seat;
            if (!assignment.isFinalGood()) evilSeats.add(seat);
            if (!assignment.isCustomRole() && assignment.role() == Role.LORD_OF_TYPHON) {
                lotSeats.add(seat);
            }
        }

        if (lotSeats.isEmpty()) return;

        // Count evil -> non-evil transitions around the table.
        // 0 transitions = everyone evil, 1 = single contiguous line, >1 = broken into groups.
        int gaps = 0;
        for (int s = 1; s <= maxSeat; s++) {
            int next = s == maxSeat ? 1 : s + 1;
            if (evilSeats.contains(s) && !evilSeats.contains(next)) gaps++;
        }
        if (gaps > 1) {
            errors.add(Component.translatable("gui.blood-on-the-blocktower.validation.lord_of_typhon_line"));
        }

        // LoT cannot sit at either end of the line - both neighbors must be evil
        for (int lotSeat : lotSeats) {
            int left = lotSeat == 1 ? maxSeat : lotSeat - 1;
            int right = lotSeat == maxSeat ? 1 : lotSeat + 1;
            if (!evilSeats.contains(left) || !evilSeats.contains(right)) {
                errors.add(Component.translatable("gui.blood-on-the-blocktower.validation.lord_of_typhon_sides", lotSeat));
            }
        }
    }

    /**
     * Checks if an assignment has an actual role (official or custom).
     */
    private static boolean hasAssignedRole(PendingRoleAssignment assignment) {
        if (assignment == null) return false;
        return assignment.role() != Role.NO_ROLE || assignment.isCustomRole();
    }

    /** Script id of the role a reminder points at, official or custom. */
    private static String reminderRoleId(Reminder reminder) {
        return reminder.role().map(Role::getId).orElseGet(() -> reminder.customRoleId().orElse(""));
    }

    private static String reminderDisplayName(Reminder reminder, Script script) {
        if (reminder.role().isPresent()) return reminder.role().get().getDisplayName();
        return reminder.customRoleId()
                .flatMap(id -> script == null ? Optional.<CustomRole>empty() : script.getCustomRole(id))
                .map(CustomRole::getDisplayName)
                .orElse(reminder.displayText().getString());
    }

    /**
     * Gets the RoleType for an assignment, handling both official and custom roles.
     */
    private static RoleType getAssignmentRoleType(PendingRoleAssignment assignment) {
        if (assignment == null) return RoleType.NONE;
        if (assignment.isCustomRole() && assignment.customRole().isPresent()) {
            return assignment.customRole().get().team();
        }
        return assignment.role().getType();
    }
}
