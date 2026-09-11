package com.autumnwind.botb.networking;

import com.autumnwind.botb.config.ServerConfig;
import com.autumnwind.botb.daytime.*;
import com.autumnwind.botb.states.ServerState;
import com.autumnwind.botb.world.VoteIndicators;
import java.util.*;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.Team;
import com.autumnwind.botb.world.TeamManager;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.scores.TeamColor;
import com.autumnwind.botb.world.WorldTime;

/** Server-bound packet handlers: The dusk and dawn transitions. */
final class DayNightHandlers {

    private DayNightHandlers() {}

    static void register() {
        ModPackets.registerGuarded(ExecuteDuskDawnC2SPayload.ID, (payload, context) -> {
            ServerPlayer player = context.player();
            if (player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
                String type = payload.transition();
                String command = null;
                String soundType = null;

                Component messageToAll = null;
                Component extraMessage = null; // Night/Day number message
                if (ExecuteDuskDawnC2SPayload.DUSK.equals(type)) {
                    // Check if there's an active nomination or storyteller's internal MFE - block dusk if so
                    // Use storyteller's internal MFE state (not public MFE) - allows proceeding when Legion fake-marks someone
                    UUID storytellerMFE = DaytimeState.getStorytellerMFE();
                    UUID currentNominee = DaytimeState.getCurrentNominee();

                    // Block if storyteller's internal state has an MFE (the "real" mark)
                    if (storytellerMFE != null) {
                        ServerPlayer mfePlayer = context.server().getPlayerList().getPlayer(storytellerMFE);
                        String mfeName = mfePlayer != null ? mfePlayer.getName().getString() : Component.translatable("gui.blood-on-the-blocktower.common.unknown_player").getString();
                        player.sendSystemMessage(Component.translatable("message.blood-on-the-blocktower.night.cannot_dusk_marked", mfeName).withStyle(ChatFormatting.RED), true);
                        return;
                    }

                    if (currentNominee != null) {
                        // Get nominee player name for the message
                        ServerPlayer nomineePlayer = context.server().getPlayerList().getPlayer(currentNominee);
                        String nomineeName = nomineePlayer != null ? nomineePlayer.getName().getString() : Component.translatable("gui.blood-on-the-blocktower.common.unknown_player").getString();
                        player.sendSystemMessage(Component.translatable("message.blood-on-the-blocktower.night.cannot_dusk_nominated", nomineeName).withStyle(ChatFormatting.RED), true);
                        return;
                    }

                    // Block dusk if a traveler is called for exile
                    UUID currentExileTarget = DaytimeState.getCurrentExileTarget();
                    if (currentExileTarget != null) {
                        ServerPlayer exilePlayer = context.server().getPlayerList().getPlayer(currentExileTarget);
                        String exileName = exilePlayer != null ? exilePlayer.getName().getString() : Component.translatable("gui.blood-on-the-blocktower.common.unknown_player").getString();
                        player.sendSystemMessage(Component.translatable("message.blood-on-the-blocktower.night.cannot_dusk_exile", exileName).withStyle(ChatFormatting.RED), true);
                        return;
                    }

                    command = ServerConfig.DUSK_COMMAND;
                    soundType = PlaySoundS2CPayload.DUSK;
                    messageToAll = Component.translatable("message.blood-on-the-blocktower.night.night_falls").withStyle(ChatFormatting.DARK_PURPLE);

                    // Calculate night number message (after incrementing)
                    int nightNumber = ServerState.currentNight <= ServerState.currentDay
                        ? ServerState.currentNight + 1  // Will be incremented below
                        : ServerState.currentNight;     // Already at max
                    extraMessage = Component.translatable("message.blood-on-the-blocktower.night.night_number", nightNumber).withStyle(ChatFormatting.LIGHT_PURPLE);

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
                    WorldTime.setOverworldTime(context.server(), ServerConfig.TIME_DUSK);

                    // Hide nametags for both player and traveler teams at dusk
                    Scoreboard scoreboard = context.server().getScoreboard();
                    PlayerTeam playerTeam = scoreboard.getPlayerTeam(TeamManager.PLAYER_TEAM);
                    if (playerTeam == null) {
                        playerTeam = scoreboard.addPlayerTeam(TeamManager.PLAYER_TEAM);
                        playerTeam.setColor(Optional.of(TeamColor.WHITE));
                    }
                    playerTeam.setNameTagVisibility(Team.Visibility.NEVER);

                    PlayerTeam travelerTeam = scoreboard.getPlayerTeam(TeamManager.TRAVELER_TEAM);
                    if (travelerTeam == null) {
                        travelerTeam = scoreboard.addPlayerTeam(TeamManager.TRAVELER_TEAM);
                        travelerTeam.setColor(Optional.of(TeamColor.LIGHT_PURPLE));
                    }
                    travelerTeam.setNameTagVisibility(Team.Visibility.NEVER);

                    // If we just transitioned from first night (<=1) to other nights (>1), trigger night order rebuild on clients
                    if (previousNight <= 1 && ServerState.currentNight > 1) {
                        // Send a special payload to trigger client-side night order rebuild
                        for (ServerPlayer onlinePlayer : context.server().getPlayerList().getPlayers()) {
                            if (onlinePlayer.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
                                // Trigger night order rebuild for storytellers
                                ServerPlayNetworking.send(onlinePlayer, new RebuildNightOrderS2CPayload());
                            }
                        }
                    }
                } else if (ExecuteDuskDawnC2SPayload.DAWN.equals(type)) {
                    command = ServerConfig.DAWN_COMMAND;
                    soundType = PlaySoundS2CPayload.DAWN;
                    messageToAll = Component.translatable("message.blood-on-the-blocktower.night.dawn_breaks").withStyle(ChatFormatting.GOLD);

                    // Calculate day number message (after incrementing)
                    int dayNumber = ServerState.currentDay < ServerState.currentNight
                        ? ServerState.currentDay + 1  // Will be incremented below
                        : ServerState.currentDay;     // Already at max
                    extraMessage = Component.translatable("message.blood-on-the-blocktower.night.day_number", dayNumber).withStyle(ChatFormatting.YELLOW);

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
                    for (ServerPlayer onlinePlayer : context.server().getPlayerList().getPlayers()) {
                        if (onlinePlayer.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
                            ServerPlayNetworking.send(onlinePlayer, new RebuildNightOrderS2CPayload());
                        }
                    }

                    // Set in-game time to dawn
                    WorldTime.setOverworldTime(context.server(), ServerConfig.TIME_DAWN);

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
                    PlayerTeam playerTeam = scoreboard.getPlayerTeam(TeamManager.PLAYER_TEAM);
                    if (playerTeam != null) {
                        playerTeam.setNameTagVisibility(Team.Visibility.ALWAYS);
                    }
                    PlayerTeam travelerTeam = scoreboard.getPlayerTeam(TeamManager.TRAVELER_TEAM);
                    if (travelerTeam != null) {
                        travelerTeam.setNameTagVisibility(Team.Visibility.ALWAYS);
                    }

                    // Teleport storyteller to town square for dawn
                    BlockPos townSquare = ServerConfig.TOWN_SQUARE;
                    if (townSquare != null) {
                        // Leave voice chat group before teleporting
                        ModPackets.leaveVoiceChatGroup(player);

                        ServerLevel world = player.level();
                        player.teleportTo(world, townSquare.getX() + 0.5, townSquare.getY(), townSquare.getZ() + 0.5, Set.of(), player.getYRot(), player.getXRot(), true);
                    }
                }

                // Execute command if set
                if (command != null && !command.isEmpty()) {
                    context.server().getCommands().performPrefixedCommand(
                            context.server().createCommandSourceStack().withSuppressedOutput(),
                            command
                    );
                }

                // Play sound and send message to all players
                if (soundType != null) {
                    for (ServerPlayer onlinePlayer : context.server().getPlayerList().getPlayers()) {
                        ServerPlayNetworking.send(onlinePlayer, new PlaySoundS2CPayload(soundType));
                        if (messageToAll != null) {
                            onlinePlayer.sendSystemMessage(messageToAll, false);
                        }
                        if (extraMessage != null) {
                            onlinePlayer.sendSystemMessage(extraMessage, false);
                        }
                    }
                }

                // Sync other storytellers' night visit index to Dawn/Dusk
                // This prevents storytellers from trapping each other behind the dawn/dusk barrier
                String syncType = ExecuteDuskDawnC2SPayload.DUSK.equals(type) ? SyncNightVisitS2CPayload.DUSK : SyncNightVisitS2CPayload.DAWN;
                for (ServerPlayer operator : context.server().getPlayerList().getPlayers()) {
                    if (operator.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER) && !operator.getUUID().equals(player.getUUID())) {
                        ServerPlayNetworking.send(operator, new SyncNightVisitS2CPayload(syncType));
                    }
                }
            }
        });
    }
}
