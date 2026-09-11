package com.autumnwind.botb.networking;

import com.autumnwind.botb.config.ServerConfig;
import com.autumnwind.botb.daytime.*;
import com.autumnwind.botb.states.ServerState;
import com.autumnwind.botb.world.VoteIndicators;
import java.util.*;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.Block;
import net.minecraft.scoreboard.AbstractTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import com.autumnwind.botb.world.TeamManager;

/** Server-bound packet handlers: The dusk and dawn transitions. */
final class DayNightHandlers {

    private DayNightHandlers() {}

    static void register() {
        ModPackets.registerGuarded(ExecuteDuskDawnC2SPayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            if (player.hasPermissionLevel(2)) {
                String type = payload.type();
                String command = null;
                String soundType = null;

                Text messageToAll = null;
                Text extraMessage = null; // Night/Day number message
                if (ExecuteDuskDawnC2SPayload.DUSK.equals(type)) {
                    // Check if there's an active nomination or storyteller's internal MFE - block dusk if so
                    // Use storyteller's internal MFE state (not public MFE) - allows proceeding when Legion fake-marks someone
                    UUID storytellerMFE = DaytimeState.getStorytellerMFE();
                    UUID currentNominee = DaytimeState.getCurrentNominee();

                    // Block if storyteller's internal state has an MFE (the "real" mark)
                    if (storytellerMFE != null) {
                        ServerPlayerEntity mfePlayer = context.server().getPlayerManager().getPlayer(storytellerMFE);
                        String mfeName = mfePlayer != null ? mfePlayer.getName().getString() : "Unknown";
                        player.sendMessage(Text.translatable("message.blood-on-the-blocktower.night.cannot_dusk_marked", mfeName).formatted(Formatting.RED), true);
                        return;
                    }

                    if (currentNominee != null) {
                        // Get nominee player name for the message
                        ServerPlayerEntity nomineePlayer = context.server().getPlayerManager().getPlayer(currentNominee);
                        String nomineeName = nomineePlayer != null ? nomineePlayer.getName().getString() : "Unknown";
                        player.sendMessage(Text.translatable("message.blood-on-the-blocktower.night.cannot_dusk_nominated", nomineeName).formatted(Formatting.RED), true);
                        return;
                    }

                    // Block dusk if a traveler is called for exile
                    UUID currentExileTarget = DaytimeState.getCurrentExileTarget();
                    if (currentExileTarget != null) {
                        ServerPlayerEntity exilePlayer = context.server().getPlayerManager().getPlayer(currentExileTarget);
                        String exileName = exilePlayer != null ? exilePlayer.getName().getString() : "Unknown";
                        player.sendMessage(Text.translatable("message.blood-on-the-blocktower.night.cannot_dusk_exile", exileName).formatted(Formatting.RED), true);
                        return;
                    }

                    command = ServerConfig.DUSK_COMMAND;
                    soundType = PlaySoundS2CPayload.DUSK;
                    messageToAll = Text.translatable("message.blood-on-the-blocktower.night.night_falls").formatted(Formatting.DARK_PURPLE);

                    // Calculate night number message (after incrementing)
                    int nightNumber = ServerState.currentNight <= ServerState.currentDay
                        ? ServerState.currentNight + 1  // Will be incremented below
                        : ServerState.currentNight;     // Already at max
                    extraMessage = Text.translatable("message.blood-on-the-blocktower.night.night_number", nightNumber).formatted(Formatting.LIGHT_PURPLE);

                    // Process any pending ghost vote updates from Organ Grinder mode
                    // This places the ghost used blocks that were delayed during the vote
                    ModPackets.processPendingGhostVoteUpdates(context.server());

                    // Store previous night value to detect transition
                    int previousNight = ServerState.currentNight;

                    // Increment night count (with constraint: Night <= Day + 1)
                    if (ServerState.currentNight <= ServerState.currentDay) {
                        ServerState.currentNight++;
                    }
                    // If night is already > day, don't increment (prevents accidental reactivations)

                    // When transitioning from setup phase (night 0) to night 1, place the unseated cage on empty seats.
                    // Pretend every configured seat was previously seated so the diff treats every currently-empty
                    // seat as a fresh assigned→unassigned transition and gets its cage placed.
                    if (previousNight == 0 && ServerState.currentNight == 1) {
                        VoteIndicators.syncUnseatedVoteIndicators(context.server(), ServerConfig.SEAT_VOTE_INDICATOR_POSITIONS.keySet());
                        VoteIndicators.paintVoteIndicatorStacks(context.server(), false);
                        // Power every seat's vote piston (redstone block below the piston).
                        // After the cages, so pistons under caged seats jam instead of extending.
                        ElectionManager.powerAllSeatPistons(context.server());
                    }

                    // Reset daytime state (clear nominations, MFE, votes, etc.)
                    DaytimeState.resetDaily();
                    // Reset traveler exile eligibility for new day
                    DaytimeState.resetExileEligibilityDaily();
                    StateBroadcaster.broadcastDaytimeState(context.server());
                    StateBroadcaster.broadcastDayNightState(context.server());

                    // Set in-game time to dusk
                    context.server().getOverworld().setTimeOfDay(ServerConfig.TIME_DUSK);

                    // Hide nametags for both player and traveler teams at dusk
                    Scoreboard scoreboard = context.server().getScoreboard();
                    Team playerTeam = scoreboard.getTeam(TeamManager.PLAYER_TEAM);
                    if (playerTeam == null) {
                        playerTeam = scoreboard.addTeam(TeamManager.PLAYER_TEAM);
                        playerTeam.setColor(Formatting.WHITE);
                    }
                    playerTeam.setNameTagVisibilityRule(AbstractTeam.VisibilityRule.NEVER);

                    Team travelerTeam = scoreboard.getTeam(TeamManager.TRAVELER_TEAM);
                    if (travelerTeam == null) {
                        travelerTeam = scoreboard.addTeam(TeamManager.TRAVELER_TEAM);
                        travelerTeam.setColor(Formatting.LIGHT_PURPLE);
                    }
                    travelerTeam.setNameTagVisibilityRule(AbstractTeam.VisibilityRule.NEVER);

                    // If we just transitioned from first night (<=1) to other nights (>1), trigger night order rebuild on clients
                    if (previousNight <= 1 && ServerState.currentNight > 1) {
                        // Send a special payload to trigger client-side night order rebuild
                        for (ServerPlayerEntity onlinePlayer : context.server().getPlayerManager().getPlayerList()) {
                            if (onlinePlayer.hasPermissionLevel(2)) {
                                // Trigger night order rebuild for storytellers
                                ServerPlayNetworking.send(onlinePlayer, new RebuildNightOrderS2CPayload());
                            }
                        }
                    }
                } else if (ExecuteDuskDawnC2SPayload.DAWN.equals(type)) {
                    command = ServerConfig.DAWN_COMMAND;
                    soundType = PlaySoundS2CPayload.DAWN;
                    messageToAll = Text.translatable("message.blood-on-the-blocktower.night.dawn_breaks").formatted(Formatting.GOLD);

                    // Calculate day number message (after incrementing)
                    int dayNumber = ServerState.currentDay < ServerState.currentNight
                        ? ServerState.currentDay + 1  // Will be incremented below
                        : ServerState.currentDay;     // Already at max
                    extraMessage = Text.translatable("message.blood-on-the-blocktower.night.day_number", dayNumber).formatted(Formatting.YELLOW);

                    // Reset execution tracking for Undertaker (new day = new execution opportunity)
                    ServerState.executionToday = false;

                    // Increment day count (with constraint: Day <= Night)
                    if (ServerState.currentDay < ServerState.currentNight) {
                        ServerState.currentDay++;
                    }
                    // If day is already equal to night, don't increment (prevents accidental reactivations)

                    // Reset traveler exile eligibility for new day
                    DaytimeState.resetExileEligibilityDaily();
                    StateBroadcaster.broadcastDaytimeState(context.server());

                    StateBroadcaster.broadcastDayNightState(context.server());

                    // Rebuild night order for storyteller (executionToday changed, Undertaker may need to be removed)
                    for (ServerPlayerEntity onlinePlayer : context.server().getPlayerManager().getPlayerList()) {
                        if (onlinePlayer.hasPermissionLevel(2)) {
                            ServerPlayNetworking.send(onlinePlayer, new RebuildNightOrderS2CPayload());
                        }
                    }

                    // Set in-game time to dawn
                    context.server().getOverworld().setTimeOfDay(ServerConfig.TIME_DAWN);

                    // --- Banshee Vote Indicator Updates at Dawn ---
                    // Get the list of dead players
                    Set<UUID> deadPlayers = ServerState.deadPlayers();

                    // --- Voudon Mode Updates at Dawn ---
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

                    ModPackets.syncBansheeAbility(context.server(), payload.bansheeHasAbilityPlayers(), deadPlayers);

                    // Show nametags for both player and traveler teams at dawn
                    Scoreboard scoreboard = context.server().getScoreboard();
                    Team playerTeam = scoreboard.getTeam(TeamManager.PLAYER_TEAM);
                    if (playerTeam != null) {
                        playerTeam.setNameTagVisibilityRule(AbstractTeam.VisibilityRule.ALWAYS);
                    }
                    Team travelerTeam = scoreboard.getTeam(TeamManager.TRAVELER_TEAM);
                    if (travelerTeam != null) {
                        travelerTeam.setNameTagVisibilityRule(AbstractTeam.VisibilityRule.ALWAYS);
                    }

                    // Teleport storyteller to town square for dawn
                    BlockPos townSquare = ServerConfig.TOWN_SQUARE;
                    if (townSquare != null) {
                        // Leave voice chat group before teleporting
                        ModPackets.leaveVoiceChatGroup(player);

                        ServerWorld world = player.getServerWorld();
                        player.teleport(world, townSquare.getX() + 0.5, townSquare.getY(), townSquare.getZ() + 0.5, player.getYaw(), player.getPitch());
                    }
                }

                // Execute command if set
                if (command != null && !command.isEmpty()) {
                    context.server().getCommandManager().executeWithPrefix(
                            context.server().getCommandSource().withSilent(),
                            command
                    );
                }

                // Play sound and send message to all players
                if (soundType != null) {
                    for (ServerPlayerEntity onlinePlayer : context.server().getPlayerManager().getPlayerList()) {
                        ServerPlayNetworking.send(onlinePlayer, new PlaySoundS2CPayload(soundType));
                        if (messageToAll != null) {
                            onlinePlayer.sendMessage(messageToAll, false);
                        }
                        if (extraMessage != null) {
                            onlinePlayer.sendMessage(extraMessage, false);
                        }
                    }
                }

                // Sync other storytellers' night visit index to Dawn/Dusk
                // This prevents storytellers from trapping each other behind the dawn/dusk barrier
                String syncType = ExecuteDuskDawnC2SPayload.DUSK.equals(type) ? SyncNightVisitS2CPayload.DUSK : SyncNightVisitS2CPayload.DAWN;
                for (ServerPlayerEntity operator : context.server().getPlayerManager().getPlayerList()) {
                    if (operator.hasPermissionLevel(2) && !operator.getUuid().equals(player.getUuid())) {
                        ServerPlayNetworking.send(operator, new SyncNightVisitS2CPayload(syncType));
                    }
                }
            }
        });
    }
}
