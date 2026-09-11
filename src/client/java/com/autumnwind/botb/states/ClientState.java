package com.autumnwind.botb.states;

import com.autumnwind.botb.util.FloatingRoleIconMode;
import com.autumnwind.botb.util.PendingRoleAssignment;
import com.autumnwind.botb.util.Role;
import com.autumnwind.botb.util.Script;
import com.autumnwind.botb.config.PlayerConfig;
import com.autumnwind.botb.hud.RoleAssignmentAnimation;
import com.autumnwind.botb.sound.CustomSounds;
import com.autumnwind.botb.sound.ModSounds;
import com.autumnwind.botb.util.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.server.permissions.Permissions;

public class ClientState {
    public static Role myRole = null;  // For backwards compatibility - use myAssignment for full info
    public static PendingRoleAssignment myAssignment = null;  // Full assignment including custom roles
    public static Boolean myAlignment = null;
    public static boolean isRoleHudVisible = true;
    public static boolean isHudEnabled = true;

    // Base volumes for each sound (these are the "100%" values)
    public static final float BASE_VOLUME_DOORBELL = 0.5f;
    public static final float BASE_VOLUME_DAWN = 1.0f;
    public static final float BASE_VOLUME_DUSK = 1.0f;
    public static final float BASE_VOLUME_CALL_BACK = 1.0f;
    public static final float BASE_VOLUME_NOMINATION = 1.0f;
    public static final float BASE_VOLUME_VOTE_START = 1.0f;
    public static final float BASE_VOLUME_VOTE_MUSIC = 1.0f;
    public static final float BASE_VOLUME_CLOCK_TICKING = 1.0f;
    public static final float BASE_VOLUME_NOT_ENOUGH_VOTES = 1.0f;
    public static final float BASE_VOLUME_TIE = 1.0f;
    public static final float BASE_VOLUME_MARKED = 1.0f;
    public static final float BASE_VOLUME_EXECUTION = 1.0f;
    public static final float BASE_VOLUME_ROLE_RECEIVE = 0.6f;
    public static final float BASE_VOLUME_GAME_END = 1.0f;

    // Volume multipliers (0.0 to 2.0, where 1.0 = 100%)
    // These multiply against the base volumes above
    public static float volumeNominations = 1.0f;  // nomination, vote_start, vote_music, clock_ticking, not_enough_votes, tie, marked, execution, call_back
    public static float volumeDawnDusk = 1.0f;     // dawn, dusk
    public static float volumeDoorbell = 1.0f;     // doorbell/doorknock
    public static float volumeRoleReceive = 1.0f;  // role_receive
    public static float volumeFinalReveal = 1.0f;  // game_end

    // Grimoire settings
    public static boolean fadeOutOfGroupHeads = true;

    // Grimoire animation settings (only affects grimoire entry fade-in, not role receive animation)
    public static boolean grimoireAnimationsEnabled = true;

    // Hints settings
    public static boolean hintsEnabled = true;
    public static int activePlayerCount = 0;
    public static int travelerCount = 0;
    public static int lobbyPlayerCount = 0;      // Non-operators online, synced from server
    public static int lobbyStorytellerCount = 0; // Level 2+ operators online, synced from server
    public static Script currentScript = null;
    public static Map<UUID, Integer> playerSeatNumbers = new HashMap<>();
    public static Map<UUID, Boolean> playerDeathStatus = new HashMap<>();
    public static boolean isNightHudVisible = true; // Default to shown
    public static boolean isSidebarVisible = true; // Default to shown
    public static List<Madness> myMadnesses = new ArrayList<>(); // Active madnesses for the player

    // Day/Night tracking (synced from server)
    public static int currentNight = 0;
    public static int currentDay = 0;

    // Execution tracking for Undertaker (synced from server)
    public static boolean executionToday = false;

    // Game end animation state
    public static boolean gameEnding = false;

    // Latched true when the game-end animation fires. Stays true after the animation
    // completes so AFTER_END-mode floating role icons keep showing until game reset.
    public static boolean rolesRevealed = false;

    // How floating role icons above player heads should render
    public static FloatingRoleIconMode floatingRoleIconMode =
            FloatingRoleIconMode.AFTER_END;

    // Crossed out roles for player tracking - uses role IDs
    public static final Set<String> crossedOutRoles = new HashSet<>();

    // Daytime system state
    public static Map<UUID, Boolean> canNominate = new HashMap<>();
    public static Map<UUID, Boolean> canBeNominated = new HashMap<>();
    public static Map<UUID, Boolean> hasUsedGhostVote = new HashMap<>();
    public static Map<UUID, Integer> nominationsRemaining = new HashMap<>(); // For Banshee double nominations
    public static UUID currentNominator = null;
    public static UUID currentNominee = null;
    public static UUID markedForExecution = null;
    public static int votesForMarkedPlayer = 0;
    public static boolean nominationsOpen = false;
    public static boolean storytellerCanBeNominated = true; // For Atheist script - storyteller nomination tracking
    public static boolean voteInProgress = false;
    public static boolean organGrinderMode = false; // If true, hide vote info for non-operators during vote
    public static boolean organGrinderModeActiveToday = false; // Persistent for the day - hides MFE from non-operators
    public static boolean hasSecretlyUsedGhostVote = false; // If true, player secretly used ghost vote in OG mode (can't vote)
    public static boolean isVoudonBlocked = false; // If true, player is alive non-Voudon in Voudon mode (can't vote)
    public static boolean voudonModeActive = false; // Reversed voting eligibility (dead vote, alive don't), synced to all clients
    public static UUID voudonPlayerUuid = null; // The Voudon, who votes normally while alive in Voudon mode
    public static Set<UUID> bansheePlayers = new HashSet<>(); // Banshees with their ability (vote like living players)
    public static Set<UUID> bansheeDoubleActivePlayers = new HashSet<>(); // Banshees whose double vote is currently active
    public static int myVoteLockInTime = 0; // Countdown in seconds
    public static int myVotePosition = 0; // Position in voting order (1st, 2nd, 3rd, etc.)
    public static Map<UUID, Boolean> lockedVotes = new HashMap<>();
    public static Map<UUID, Boolean> leverStates = new HashMap<>();
    public static int currentVoteCount = 0;

    // Exile system state
    public static Map<UUID, Boolean> canBeExiled = new HashMap<>();
    public static UUID currentExileCaller = null;
    public static UUID currentExileTarget = null;
    public static boolean exileSupportInProgress = false;
    public static int exileSupportCount = 0;

    // Exile support countdown state (mirrors vote state for exile support)
    public static int exileSupportLockInTime = 0; // Countdown in seconds until player's support locks
    public static int exileSupportPosition = 0; // Position in support order (1st, 2nd, etc.)
    public static Map<UUID, Boolean> lockedExileSupports = new HashMap<>();
    public static Map<UUID, Boolean> exileLeverStates = new HashMap<>();

    // ========== Unified Election Helpers ==========
    // These provide a unified view of either vote or exile support state

    /**
     * Returns true if there's an active election (either nomination/vote or exile call/support).
     */
    public static boolean hasActiveElection() {
        return currentNominee != null || currentExileTarget != null;
    }

    /**
     * Returns true if this is an exile election (vs regular vote).
     */
    public static boolean isExileElection() {
        return currentExileTarget != null;
    }

    /**
     * Returns true if the voting/support phase is in progress.
     */
    public static boolean isVotingPhaseActive() {
        return voteInProgress || exileSupportInProgress;
    }

    /**
     * Updates player state with a full PendingRoleAssignment (supports both official and custom roles).
     */
    public static void updatePlayerState(PendingRoleAssignment assignment, int newPlayerCount, int newTravelerCount, boolean silent) {
        // If the server sends NO_ROLE assignment, clear our state but preserve grimoire entry.
        if (assignment == null || (!assignment.isCustomRole() && assignment.role() == Role.NO_ROLE)) {
            myRole = null;
            myAssignment = null;
            myAlignment = null;
            activePlayerCount = newPlayerCount;
            travelerCount = newTravelerCount;
            return;
        }

        // Check if player is being assigned for the first time during SETUP phase
        boolean wasUnassigned = myAssignment == null && myRole == null;
        boolean isSetupPhase = currentNight == 0;
        Minecraft mc = Minecraft.getInstance();
        boolean isOperator = mc.player != null && mc.player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);

        if (wasUnassigned && isSetupPhase && !isOperator) {
            StorytellerState.PENDING_ROLES.clear();
            StorytellerState.PENDING_SEAT_NUMBERS.clear();
            StorytellerState.REMINDERS.clear();
            StorytellerState.DEMON_BLUFFS.set(0, null);
            StorytellerState.DEMON_BLUFFS.set(1, null);
            StorytellerState.DEMON_BLUFFS.set(2, null);
        }

        // Resolve custom role from current script if needed
        PendingRoleAssignment resolvedAssignment = assignment;
        if (assignment.isCustomRole() && currentScript != null) {
            resolvedAssignment = assignment.resolveCustomRole(currentScript);
        }

        boolean roleChanged = myAssignment == null || !assignmentsMatch(myAssignment, resolvedAssignment);
        boolean newIsGood = resolvedAssignment.isFinalGood();
        boolean alignmentChanged = myAlignment == null || myAlignment != newIsGood;

        myAssignment = resolvedAssignment;
        myRole = resolvedAssignment.role();  // For backwards compat - NO_ROLE for custom roles
        myAlignment = newIsGood;
        activePlayerCount = newPlayerCount;
        travelerCount = newTravelerCount;

        if (roleChanged || alignmentChanged) {
            if (gameEnding) {
                isRoleHudVisible = false;
                isHudEnabled = false;
            } else {
                isRoleHudVisible = true;
            }
            PlayerConfig.save();

            float roleReceiveVolume = BASE_VOLUME_ROLE_RECEIVE * volumeRoleReceive;
            if (!gameEnding && !silent && roleReceiveVolume > 0) {
                Minecraft client = Minecraft.getInstance();
                if (client.player != null && client.level != null) {
                    CustomSounds.playOneShot(client, CustomSounds.roleReceiveCandidates(resolvedAssignment),
                            ModSounds.ROLE_RECEIVE, roleReceiveVolume);
                }
            }

            // Start role assignment animation
            if (!gameEnding && !silent) {
                RoleAssignmentAnimation.startAnimation(resolvedAssignment);
            }

            UUID selfUUID = Minecraft.getInstance().player.getUUID();
            if (selfUUID != null) {
                StorytellerState.PENDING_ROLES.put(selfUUID, resolvedAssignment);
            }
        }
    }

    /**
     * Helper to check if two assignments represent the same role.
     */
    private static boolean assignmentsMatch(PendingRoleAssignment a, PendingRoleAssignment b) {
        if (a == null || b == null) return false;
        if (a.isCustomRole() != b.isCustomRole()) return false;
        if (a.isCustomRole()) {
            return a.customRole().isPresent() && b.customRole().isPresent() &&
                   a.customRole().get().id().equals(b.customRole().get().id());
        }
        return a.role() == b.role();
    }

    /**
     * Overload for an official {@link Role}, wrapping it in a {@link PendingRoleAssignment}.
     */
    public static void updatePlayerState(Role newRole, boolean newIsGood, int newPlayerCount, int newTravelerCount, boolean silent) {
        if (newRole == Role.NO_ROLE) {
            updatePlayerState((PendingRoleAssignment) null, newPlayerCount, newTravelerCount, silent);
            return;
        }
        AlignmentOverride override = AlignmentOverride.DEFAULT;
        if (newRole.isDefaultGood() != newIsGood) {
            override = newIsGood ? AlignmentOverride.FORCE_GOOD : AlignmentOverride.FORCE_BAD;
        }
        updatePlayerState(new PendingRoleAssignment(newRole, override), newPlayerCount, newTravelerCount, silent);
    }

    /**
     * Updates the daytime state from the server sync payload.
     */
    public static void updateDaytimeState(
            Map<UUID, Boolean> newCanNominate,
            Map<UUID, Boolean> newCanBeNominated,
            Map<UUID, Boolean> newHasUsedGhostVote,
            Map<UUID, Integer> newNominationsRemaining,
            UUID newCurrentNominator,
            UUID newCurrentNominee,
            UUID newMarkedForExecution,
            int newVotesForMarkedPlayer,
            boolean newNominationsOpen,
            boolean newOrganGrinderModeActiveToday,
            boolean newStorytellerCanBeNominated,
            Map<UUID, Boolean> newCanBeExiled,
            UUID newCurrentExileCaller,
            UUID newCurrentExileTarget,
            boolean newExileSupportInProgress,
            int newExileSupportCount,
            boolean newVoudonModeActive,
            UUID newVoudonPlayerUuid
    ) {
        canNominate = new HashMap<>(newCanNominate);
        canBeNominated = new HashMap<>(newCanBeNominated);
        hasUsedGhostVote = new HashMap<>(newHasUsedGhostVote);
        nominationsRemaining = new HashMap<>(newNominationsRemaining);
        currentNominator = newCurrentNominator;
        currentNominee = newCurrentNominee;
        markedForExecution = newMarkedForExecution;
        votesForMarkedPlayer = newVotesForMarkedPlayer;
        nominationsOpen = newNominationsOpen;
        organGrinderModeActiveToday = newOrganGrinderModeActiveToday;
        storytellerCanBeNominated = newStorytellerCanBeNominated;
        canBeExiled = new HashMap<>(newCanBeExiled);
        currentExileCaller = newCurrentExileCaller;
        currentExileTarget = newCurrentExileTarget;
        exileSupportInProgress = newExileSupportInProgress;
        exileSupportCount = newExileSupportCount;
        voudonModeActive = newVoudonModeActive;
        voudonPlayerUuid = newVoudonPlayerUuid;
    }

    /**
     * Updates vote state during an active vote.
     */
    public static void updateVoteState(int newLockedCount, int secondsUntilLock, int votePosition, Map<UUID, Boolean> newLockedVotes, Map<UUID, Boolean> newLeverStates, boolean newOrganGrinderMode, boolean newHasSecretlyUsedGhostVote, boolean newIsVoudonBlocked) {
        currentVoteCount = newLockedCount;
        myVoteLockInTime = secondsUntilLock;
        myVotePosition = votePosition;
        lockedVotes = new HashMap<>(newLockedVotes);
        leverStates = new HashMap<>(newLeverStates);
        organGrinderMode = newOrganGrinderMode;
        hasSecretlyUsedGhostVote = newHasSecretlyUsedGhostVote;
        isVoudonBlocked = newIsVoudonBlocked;
        voteInProgress = true;
    }

    /**
     * Clears vote state after a vote ends.
     */
    public static void clearVoteState() {
        voteInProgress = false;
        organGrinderMode = false;
        hasSecretlyUsedGhostVote = false;
        isVoudonBlocked = false;
        myVoteLockInTime = 0;
        myVotePosition = 0;
        lockedVotes.clear();
        leverStates.clear();
        currentVoteCount = 0;
    }

    /**
     * Updates exile support state during an active exile support vote.
     * Similar to updateVoteState() but for exile support.
     */
    public static void updateExileSupportState(int newLockedCount, int secondsUntilLock, int supportPosition, Map<UUID, Boolean> newLockedSupports, Map<UUID, Boolean> newLeverStates) {
        exileSupportCount = newLockedCount;
        exileSupportLockInTime = secondsUntilLock;
        exileSupportPosition = supportPosition;
        lockedExileSupports = new HashMap<>(newLockedSupports);
        exileLeverStates = new HashMap<>(newLeverStates);
    }
}