package com.autumnwind.botb.networking;

import com.autumnwind.botb.config.ServerConfig;
import com.autumnwind.botb.daytime.*;
import com.autumnwind.botb.states.ServerState;
import java.util.*;
import java.util.stream.Collectors;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import com.autumnwind.botb.world.WorldTime;

/** Server-bound packet handlers: Nominations, votes, executions, and exiles. */
final class DaytimeHandlers {

    private DaytimeHandlers() {}

    static void register() {
        // Open Nominations
        ModPackets.registerGuarded(OpenNominationsC2SPayload.ID, (payload, context) -> {
            if (context.player().permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
                // Get all alive players
                Map<UUID, Boolean> deathStatus = ServerState.PLAYER_DEATH_STATUS;
                Set<UUID> alivePlayers = ServerState.PLAYER_SEAT_NUMBERS.keySet().stream()
                        .filter(uuid -> !deathStatus.getOrDefault(uuid, false))
                        .collect(Collectors.toSet());

                // Get all seated players
                Set<UUID> seatedPlayers = ServerState.PLAYER_SEAT_NUMBERS.keySet();

                // Banshee players get their 2 nominations for the day (their voting state is
                // reconciled below, after the Voudon block)
                for (UUID bansheePlayer : payload.bansheeHasAbilityPlayers()) {
                    DaytimeState.setNominationsRemaining(bansheePlayer, 2);
                }

                // Store "May Not Nominate" players for hard reset support
                DaytimeState.setMayNotNominatePlayers(payload.mayNotNominatePlayers());

                // Open nominations (Banshee players can nominate even if dead, travelers excluded from nomination,
                // "May Not Nominate" players cannot nominate)
                DaytimeState.openNominations(alivePlayers, seatedPlayers, payload.bansheeHasAbilityPlayers(), payload.mayNotNominatePlayers());

                // Get dead players for Voudon mode
                Set<UUID> deadPlayers = ServerState.deadPlayers();

                // Handle Voudon mode visual indicators
                boolean wasVoudonActive = DaytimeState.isVoudonModeActive();
                if (payload.voudonModeActive()) {
                    // Voudon is alive with ability, so activate (or update, if the Voudon
                    // role was reassigned to a different player) and apply indicators.
                    UUID voudonUuid = payload.voudonPlayerUuid().orElse(null);
                    DaytimeState.activateVoudonMode(voudonUuid);
                    VotingManager.applyVoudonModeIndicators(
                            context.server(),
                            deadPlayers,
                            voudonUuid
                    );
                } else if (wasVoudonActive) {
                    // Voudon mode was active but no longer (Voudon died or lost ability)
                    // Restore normal indicators
                    VotingManager.restoreVoudonModeIndicators(context.server(), deadPlayers);
                    DaytimeState.deactivateVoudonMode();
                }

                // A Banshee's status can change between dawn and nominations (death, reminder
                // removed), so reconcile the double-vote set and indicators again here
                ModPackets.syncBansheeAbility(context.server(), payload.bansheeHasAbilityPlayers(), deadPlayers);

                // Set in-game time to evening when nominations open
                WorldTime.setOverworldTime(context.server(), ServerConfig.TIME_EVENING);

                // Broadcast state
                StateBroadcaster.broadcastDaytimeState(context.server());

                // Send chat message to all players
                Component message = Component.translatable("message.blood-on-the-blocktower.daytime.nominations_open").withStyle(ChatFormatting.YELLOW);
                for (ServerPlayer player : context.server().getPlayerList().getPlayers()) {
                    player.sendSystemMessage(message, false);

                    // Play call back sound
                    ServerPlayNetworking.send(player, new PlaySoundS2CPayload(PlaySoundS2CPayload.CALL_BACK));
                }
            }
        });

        // Nominate Player
        ModPackets.registerGuarded(NominatePlayerC2SPayload.ID, (payload, context) -> {
            if (context.player().permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
                UUID nominator = payload.nominator();
                UUID nominee = payload.nominee();
                boolean override = payload.override();

                // Validate nomination
                if (NominationManager.validateNomination(nominator, nominee, override)) {
                    // Get alive player count and dead players
                    Map<UUID, Boolean> deathStatus = ServerState.PLAYER_DEATH_STATUS;
                    int aliveCount = (int) ServerState.PLAYER_SEAT_NUMBERS.keySet().stream()
                            .filter(uuid -> !deathStatus.getOrDefault(uuid, false))
                            .count();

                    Set<UUID> deadPlayers = ServerState.deadPlayers();

                    // Check Voudon mode changes (Voudon may have died since nominations opened)
                    boolean wasVoudonActive = DaytimeState.isVoudonModeActive();
                    if (payload.voudonModeActive()) {
                        // Voudon is alive with ability, so activate (or update, if the Voudon
                        // role was reassigned to a different player) and apply indicators.
                        UUID voudonUuid = payload.voudonPlayerUuid().orElse(null);
                        DaytimeState.activateVoudonMode(voudonUuid);
                        VotingManager.applyVoudonModeIndicators(
                                context.server(),
                                deadPlayers,
                                voudonUuid
                        );
                    } else if (wasVoudonActive) {
                        // Voudon mode was active but no longer (Voudon died or lost ability)
                        VotingManager.restoreVoudonModeIndicators(context.server(), deadPlayers);
                        DaytimeState.deactivateVoudonMode();
                    }

                    // Execute nomination
                    NominationManager.executeNomination(context.server(), nominator, nominee, aliveCount);

                    // Broadcast state
                    StateBroadcaster.broadcastDaytimeState(context.server());

                    // Broadcast current lever states so sidebar shows correct initial state
                    VotingManager.broadcastLeverStates(context.server());
                }
            }
        });

        // Run Vote
        ModPackets.registerGuarded(RunVoteC2SPayload.ID, (payload, context) -> {
            if (context.player().permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
                // Get dead players
                Map<UUID, Boolean> deathStatus = ServerState.PLAYER_DEATH_STATUS;
                Set<UUID> deadPlayers = ServerState.deadPlayers();

                // Update Banshee double vote players (may have changed since nominations opened)
                // Don't clear - just ensure all current Banshee players are enabled
                for (UUID bansheePlayer : payload.bansheeHasAbilityPlayers()) {
                    if (!DaytimeState.hasBansheeDoubleVote(bansheePlayer)) {
                        DaytimeState.enableBansheeDoubleVote(bansheePlayer);
                    }
                }

                // Sync reminder-based vote multipliers (Ug hat × 2, Bureaucrat × 3, Thief × -1)
                VoteMultiplierLists vm = payload.voteMultipliers();
                DaytimeState.setUgHatPlayers(vm.ugHatPlayers());
                DaytimeState.clearTravelerMultipliers();
                for (UUID p : vm.bureaucrat3VotePlayers()) {
                    DaytimeState.setBureaucratMultiplier(p, true);
                }
                for (UUID p : vm.thiefNegativeVotePlayers()) {
                    DaytimeState.setThiefMultiplier(p, true);
                }

                // Start vote with Organ Grinder mode and Voudon mode if applicable
                VotingManager.startVote(
                        context.server(),
                        deadPlayers,
                        payload.organGrinderMode(),
                        payload.voudonModeActive(),
                        payload.voudonPlayerUuid().orElse(null),
                        payload.evilsForLegion().map(HashSet::new).orElse(null)
                );

                // Broadcast state
                StateBroadcaster.broadcastDaytimeState(context.server());
            }
        });

        // Reset Vote
        ModPackets.registerGuarded(ResetVoteC2SPayload.ID, (payload, context) -> {
            if (context.player().permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
                // Reset vote
                VotingManager.resetVote(context.server());

                // Broadcast state
                StateBroadcaster.broadcastDaytimeState(context.server());
            }
        });

        // Hard Reset Vote
        ModPackets.registerGuarded(HardResetVoteC2SPayload.ID, (payload, context) -> {
            if (context.player().permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
                // Remove glowing from old MFE if any
                UUID oldMFE = DaytimeState.getMarkedForExecution();
                if (oldMFE != null) {
                    NominationManager.updateMarkedGlow(context.server(), oldMFE, false);
                }

                // Remove glowing from nominated player if any
                UUID oldNominee = DaytimeState.getCurrentNominee();
                if (oldNominee != null) {
                    NominationManager.resetNomination(context.server());
                }

                // Reset vote and clear all state
                VotingManager.resetVote(context.server());

                // Get alive and seated players for resetting eligibility
                Map<UUID, Boolean> deathStatus = ServerState.PLAYER_DEATH_STATUS;
                Set<UUID> alivePlayers = ServerState.PLAYER_SEAT_NUMBERS.keySet().stream()
                        .filter(uuid -> !deathStatus.getOrDefault(uuid, false))
                        .collect(Collectors.toSet());
                Set<UUID> seatedPlayers = ServerState.PLAYER_SEAT_NUMBERS.keySet();

                DaytimeState.hardReset(alivePlayers, seatedPlayers);

                // Broadcast state
                StateBroadcaster.broadcastDaytimeState(context.server());
            }
        });

        // Execute Player
        ModPackets.registerGuarded(ExecutePlayerC2SPayload.ID, (payload, context) -> {
            if (context.player().permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
                UUID player = payload.player();
                boolean forced = payload.forced();
                boolean butcherAliveWithAbility = payload.butcherAliveWithAbility();
                UUID butcherUuid = payload.butcherUuid().orElse(null);

                // Execute the player (ExecutionManager handles all cleanup and state reset)
                if (forced) {
                    ExecutionManager.executePlayer(context.server(), player, butcherAliveWithAbility, butcherUuid);
                } else {
                    // Execute marked player (verify it's the same)
                    if (player.equals(DaytimeState.getMarkedForExecution())) {
                        ExecutionManager.executePlayer(context.server(), player, butcherAliveWithAbility, butcherUuid);
                    }
                }

                // Broadcast state (including executionToday for Undertaker)
                StateBroadcaster.broadcastDaytimeState(context.server());
                StateBroadcaster.broadcastDayNightState(context.server());
            }
        });

        // Execute Player (Failed - no death)
        ModPackets.registerGuarded(ExecutePlayerFailC2SPayload.ID, (payload, context) -> {
            if (context.player().permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
                UUID player = payload.player();
                boolean forced = payload.forced();
                boolean butcherAliveWithAbility = payload.butcherAliveWithAbility();
                UUID butcherUuid = payload.butcherUuid().orElse(null);

                // Execute the player without marking as dead
                if (forced) {
                    ExecutionManager.executePlayerFail(context.server(), player, butcherAliveWithAbility, butcherUuid);
                } else {
                    // Execute marked player (verify it's the same)
                    if (player.equals(DaytimeState.getMarkedForExecution())) {
                        UUID markedPlayer = DaytimeState.getMarkedForExecution();
                        if (markedPlayer != null) {
                            ExecutionManager.executePlayerFail(context.server(), markedPlayer, butcherAliveWithAbility, butcherUuid);
                        }
                    }
                }

                // Broadcast state
                StateBroadcaster.broadcastDaytimeState(context.server());
            }
        });

        // Call for Exile (traveler exile system)
        ModPackets.registerGuarded(CallForExileC2SPayload.ID, (payload, context) -> {
            if (context.player().permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
                UUID caller = payload.caller();
                UUID traveler = payload.traveler();
                boolean override = payload.override();

                // Validate exile
                if (ExileManager.validateExile(caller, traveler, override)) {
                    // Get total player count (exile uses ALL players, not just alive)
                    int totalPlayerCount = ServerState.PLAYER_SEAT_NUMBERS.size();

                    // Execute exile call
                    ExileManager.executeExile(context.server(), caller, traveler, totalPlayerCount);

                    // Broadcast state
                    StateBroadcaster.broadcastDaytimeState(context.server());

                    // Broadcast lever states for sidebar
                    VotingManager.broadcastLeverStates(context.server());
                }
            }
        });

        // Run Exile Support
        ModPackets.registerGuarded(RunExileSupportC2SPayload.ID, (payload, context) -> {
            if (context.player().permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
                // Get dead players
                Map<UUID, Boolean> deathStatus = ServerState.PLAYER_DEATH_STATUS;
                Set<UUID> deadPlayers = ServerState.deadPlayers();

                // Start exile support vote
                ExileSupportManager.startExileSupport(context.server(), deadPlayers);

                // Broadcast state
                StateBroadcaster.broadcastDaytimeState(context.server());
            }
        });

        // Reset Exile
        ModPackets.registerGuarded(ResetExileC2SPayload.ID, (payload, context) -> {
            if (context.player().permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
                // Check if exile support is in progress
                if (DaytimeState.isExileSupportInProgress()) {
                    // Get dead players for restoring indicators
                    Map<UUID, Boolean> deathStatus = ServerState.PLAYER_DEATH_STATUS;
                    Set<UUID> deadPlayers = ServerState.deadPlayers();

                    // Reset exile support (restores ghost used blocks)
                    ExileSupportManager.resetExileSupport(context.server(), deadPlayers);
                } else {
                    // Just reset the exile call
                    ExileManager.resetExile(context.server());
                }

                // Broadcast state
                StateBroadcaster.broadcastDaytimeState(context.server());
            }
        });
    }
}
