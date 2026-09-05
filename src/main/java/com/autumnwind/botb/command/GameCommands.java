package com.autumnwind.botb.command;

import com.autumnwind.botb.config.ServerConfig;
import com.autumnwind.botb.daytime.DaytimeState;
import com.autumnwind.botb.daytime.ElectionManager;
import com.autumnwind.botb.daytime.ExileManager;
import com.autumnwind.botb.daytime.ExileSupportManager;
import com.autumnwind.botb.daytime.VotingManager;
import com.autumnwind.botb.networking.ClearGrimoireS2CPayload;
import com.autumnwind.botb.networking.PlaySoundS2CPayload;
import com.autumnwind.botb.networking.RebuildNightOrderS2CPayload;
import com.autumnwind.botb.networking.RequestGameEndS2CPayload;
import com.autumnwind.botb.networking.SendDeathStatusS2CPayload;
import com.autumnwind.botb.networking.SendRoleS2CPayload;
import com.autumnwind.botb.networking.SendSeatsS2CPayload;
import com.autumnwind.botb.states.ServerState;
import com.autumnwind.botb.util.CustomNames;
import com.autumnwind.botb.util.Role;
import java.util.*;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.scoreboard.AbstractTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import com.autumnwind.botb.networking.StateBroadcaster;
import com.autumnwind.botb.world.VoteIndicators;
import com.autumnwind.botb.util.ServerCommands;
import com.autumnwind.botb.world.TeamManager;

/** Handlers for the /botb commands that act on a running game: resets, end game, names, teleports. */
final class GameCommands {

    private GameCommands() {}

    static int teleportToSeat(ServerCommandSource source, int seat) {
        try {
            ServerPlayerEntity player = source.getPlayerOrThrow();
            BlockPos pos = ServerConfig.SEAT_HOMES.get(seat);
            ServerWorld world = player.getServerWorld();

            if (pos != null) {
                player.teleport(world, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, player.getYaw(), player.getPitch());

                // Find the player assigned to this seat and play doorbell sound for them
                for (ServerPlayerEntity onlinePlayer : source.getServer().getPlayerManager().getPlayerList()) {
                    Integer playerSeat = ServerState.PLAYER_SEAT_NUMBERS.get(onlinePlayer.getUuid());
                    if (playerSeat != null && playerSeat == seat) {
                        ServerPlayNetworking.send(onlinePlayer, new PlaySoundS2CPayload(PlaySoundS2CPayload.DOORBELL));
                        break;
                    }
                }

                source.sendFeedback(() -> Text.literal("Teleported to seat " + seat).formatted(Formatting.GRAY), false);
                return 1;
            } else {
                source.sendError(Text.literal("Home for seat " + seat + " has not been set."));
                return 0;
            }
        } catch (Exception e) {
            source.sendError(Text.literal("Error teleporting: " + e.getMessage()));
            return 0;
        }
    }

    static int endGame(ServerCommandSource source, boolean goodWins) {
        try {
            ServerPlayerEntity player = source.getPlayerOrThrow();

            // Send request to the storyteller who ran the command to provide grimoire data
            ServerPlayNetworking.send(player,
                new RequestGameEndS2CPayload(goodWins));

            return 1;
        } catch (Exception e) {
            source.sendError(Text.literal("Error ending game: " + e.getMessage()));
            return 0;
        }
    }

    /** Make both game teams' name tags visible again, the same way dawn does. */
    static void showAllNameTags(MinecraftServer server) {
        Scoreboard scoreboard = server.getScoreboard();
        Team playerTeam = scoreboard.getTeam(TeamManager.PLAYER_TEAM);
        if (playerTeam != null) {
            playerTeam.setNameTagVisibilityRule(AbstractTeam.VisibilityRule.ALWAYS);
        }
        Team travelerTeam = scoreboard.getTeam(TeamManager.TRAVELER_TEAM);
        if (travelerTeam != null) {
            travelerTeam.setNameTagVisibilityRule(AbstractTeam.VisibilityRule.ALWAYS);
        }
    }

    /**
     * Cancels any in-progress nomination, vote, or exile: stops the election timers,
     * restores piston power, and hides the clock hands on every client.
     */
    static void cancelActiveElections(MinecraftServer server) {
        // Also clears a bare nomination (no vote yet) and always hides the clock hands
        VotingManager.resetVote(server);

        if (DaytimeState.isExileSupportInProgress()) {
            Set<UUID> deadPlayers = ServerState.deadPlayers();
            ExileSupportManager.resetExileSupport(server, deadPlayers);
        } else if (DaytimeState.hasActiveExile()) {
            ExileManager.resetExile(server);
        }
    }

    static int resetGameHard(ServerCommandSource source) {
        try {
            MinecraftServer server = source.getServer();

            // Stop any nomination, vote, or exile still running before state is wiped
            cancelActiveElections(server);

            // Reset day and night to 0
            ServerState.currentNight = 0;
            ServerState.currentDay = 0;

            // Broadcast day/night state to all clients
            StateBroadcaster.broadcastDayNightState(server);

            // Get all seated players (not just assigned - includes setup/unassigned players)
            Set<UUID> seatedPlayers = new HashSet<>(ServerState.PLAYER_SEAT_NUMBERS.keySet());

            // Revive all dead players with full revival logic
            Map<UUID, Boolean> newDeathStatus = new HashMap<>();
            for (UUID uuid : seatedPlayers) {
                boolean wasDead = ServerState.PLAYER_DEATH_STATUS.getOrDefault(uuid, false);
                newDeathStatus.put(uuid, false); // Set all to alive

                // If player was dead, run revive command, remove invisibility, and update blocks
                if (wasDead) {
                    Integer seat = ServerState.PLAYER_SEAT_NUMBERS.get(uuid);
                    ServerPlayerEntity targetPlayer = server.getPlayerManager().getPlayer(uuid);

                    // Execute revive command
                    if (seat != null) {
                        String reviveCommand = ServerConfig.REVIVE_COMMANDS.get(seat);
                        if (reviveCommand != null && !reviveCommand.isEmpty() && targetPlayer != null) {
                            ServerCommands.runAs(server, targetPlayer.getUuidAsString(), reviveCommand);
                        }
                    }

                    // Remove invisibility effect
                    if (targetPlayer != null) {
                        String clearInvisCommand = "effect clear @s invisibility";
                        ServerCommands.runAs(server, targetPlayer.getUuidAsString(), clearInvisCommand);
                    }

                    // Remove ghost used block below their indicator (only if it's actually the ghost used block)
                    if (seat != null) {
                        BlockPos indicatorPos = ServerConfig.SEAT_VOTE_INDICATOR_POSITIONS.get(seat);
                        if (indicatorPos != null) {
                            BlockPos belowIndicator = indicatorPos.down();
                            // Check if the block is actually the ghost used block before removing it
                            BlockState blockState = server.getOverworld().getBlockState(belowIndicator);
                            String ghostUsedBlockName = ServerConfig.VOTE_INDICATOR_BLOCK_GHOST_USED;
                            if (blockState.getBlock().equals(VotingManager.getBlockFromString(ghostUsedBlockName))) {
                                server.getOverworld().setBlockState(belowIndicator, Blocks.AIR.getDefaultState());
                            }
                        }
                    }

                    // Update vote indicator block to reflect alive status
                    VotingManager.updatePlayerDeathIndicator(server, uuid);
                }
            }

            // Update server-side death status
            ServerState.updateDeathStatus(newDeathStatus);

            // Clear glowing and invisibility from ALL seated players (not just dead ones)
            for (UUID uuid : seatedPlayers) {
                ServerPlayerEntity targetPlayer = server.getPlayerManager().getPlayer(uuid);
                if (targetPlayer != null) {
                    String clearEffectsCommand = "effect clear @s glowing";
                    ServerCommands.runAs(server, targetPlayer.getUuidAsString(), clearEffectsCommand);
                    String clearInvisCommand = "effect clear @s invisibility";
                    ServerCommands.runAs(server, targetPlayer.getUuidAsString(), clearInvisCommand);
                }
            }

            // Clear glowing and invisibility from storyteller
            ServerPlayerEntity storyteller = source.getPlayer();
            if (storyteller != null) {
                String clearGlowing = "effect clear @s glowing";
                ServerCommands.runAs(server, storyteller.getUuidAsString(), clearGlowing);
                String clearInvis = "effect clear @s invisibility";
                ServerCommands.runAs(server, storyteller.getUuidAsString(), clearInvis);
            }

            // Remove ghost vote blocks and update vote indicators for ALL players
            for (UUID uuid : seatedPlayers) {
                Integer seat = ServerState.PLAYER_SEAT_NUMBERS.get(uuid);
                if (seat != null) {
                    BlockPos indicatorPos = ServerConfig.SEAT_VOTE_INDICATOR_POSITIONS.get(seat);
                    if (indicatorPos != null) {
                        // Remove ghost used block if present
                        BlockPos belowIndicator = indicatorPos.down();
                        BlockState blockState = server.getOverworld().getBlockState(belowIndicator);
                        String ghostUsedBlockName = ServerConfig.VOTE_INDICATOR_BLOCK_GHOST_USED;
                        if (blockState.getBlock().equals(VotingManager.getBlockFromString(ghostUsedBlockName))) {
                            server.getOverworld().setBlockState(belowIndicator, Blocks.AIR.getDefaultState());
                        }
                    }
                }
                // Update vote indicator block
                VotingManager.updatePlayerDeathIndicator(server, uuid);
            }

            // Restore vote indicators for unseated seats to normal state (before seats are cleared)
            VoteIndicators.restoreUnseatedVoteIndicators(server);

            // Rebuild seated seats' indicator stacks, then re-power every seat's vote piston
            // (redstone block below the piston)
            VoteIndicators.paintVoteIndicatorStacks(server, false);
            ElectionManager.powerAllSeatPistons(server);

            // Reset lever states (physically and in state) and broadcast to clients
            VotingManager.resetLeverStates(server);

            // Set in-game time to dawn
            server.getOverworld().setTimeOfDay(ServerConfig.TIME_DAWN);

            // A reset during the night would otherwise leave everyone nameless until dawn
            showAllNameTags(server);

            // Clear all ghost votes and reset daytime state
            DaytimeState.clearAllGhostVotes();
            DaytimeState.clearBansheeDoubleVotes();
            ServerState.gameEnded = false;
            DaytimeState.resetDaily();
            // Wipe the traveler set (canBeExiled keys) so a stale entry from the previous
            // game cannot leak into the next one before the storyteller hits Send Roles.
            DaytimeState.initializeExileEligibility(Collections.emptySet());

            // Broadcast daytime state to sync ghost votes to clients
            StateBroadcaster.broadcastDaytimeState(server);

            // Unassign ALL players (including offline ones)
            ServerState.PLAYER_ROLES.clear();
            ServerState.PLAYER_SEAT_NUMBERS.clear();
            ServerState.PLAYER_DEATH_STATUS.clear();

            // Send NO_ROLE payload to all online players and broadcast empty maps
            // Also send clear grimoire to reset client-side storyteller state
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                ServerPlayNetworking.send(player,
                    SendRoleS2CPayload.ofRole(Role.NO_ROLE, true, 0, 0, false));
                ServerPlayNetworking.send(player,
                    new SendSeatsS2CPayload(new HashMap<>()));
                ServerPlayNetworking.send(player,
                    new SendDeathStatusS2CPayload(new HashMap<>()));
                ServerPlayNetworking.send(player,
                    new ClearGrimoireS2CPayload());
            }

            source.sendFeedback(() -> Text.literal("Full reset: everything Reset Game does, plus all players unseated and their grimoires wiped.")
                    .formatted(Formatting.GREEN), true);
            return 1;

        } catch (Exception e) {
            source.sendError(Text.literal("Error resetting game: " + e.getMessage()));
            return 0;
        }
    }

    static int resetGame(ServerCommandSource source) {
        try {
            MinecraftServer server = source.getServer();
            ServerPlayerEntity storyteller = source.getPlayerOrThrow();

            // Stop any nomination, vote, or exile still running before state is wiped
            cancelActiveElections(server);

            // Get all seated players (not just assigned - includes setup/unassigned players)
            Set<UUID> seatedPlayers = new HashSet<>(ServerState.PLAYER_SEAT_NUMBERS.keySet());

            // Reset day/night to 0 (SETUP phase)
            ServerState.currentNight = 0;
            ServerState.currentDay = 0;

            // Broadcast day/night state to all clients (puts everyone in SETUP phase)
            StateBroadcaster.broadcastDayNightState(server);

            // 1. Revive all dead players - update server state and remove invisibility
            Map<UUID, Boolean> newDeathStatus = new HashMap<>();
            for (UUID uuid : seatedPlayers) {
                boolean wasDead = ServerState.PLAYER_DEATH_STATUS.getOrDefault(uuid, false);
                newDeathStatus.put(uuid, false); // Set all to alive

                // If player was dead, run revive command and remove invisibility
                if (wasDead) {
                    Integer seat = ServerState.PLAYER_SEAT_NUMBERS.get(uuid);
                    ServerPlayerEntity targetPlayer = server.getPlayerManager().getPlayer(uuid);

                    // Execute revive command
                    if (seat != null) {
                        String reviveCommand = ServerConfig.REVIVE_COMMANDS.get(seat);
                        if (reviveCommand != null && !reviveCommand.isEmpty() && targetPlayer != null) {
                            ServerCommands.runAs(server, targetPlayer.getUuidAsString(), reviveCommand);
                        }
                    }

                    // Remove invisibility effect
                    if (targetPlayer != null) {
                        String clearInvisCommand = "effect clear @s invisibility";
                        ServerCommands.runAs(server, targetPlayer.getUuidAsString(), clearInvisCommand);
                    }

                    // Remove ghost used block below their indicator (only if it's actually the ghost used block)
                    if (seat != null) {
                        BlockPos indicatorPos = ServerConfig.SEAT_VOTE_INDICATOR_POSITIONS.get(seat);
                        if (indicatorPos != null) {
                            BlockPos belowIndicator = indicatorPos.down();
                            // Check if the block is actually the ghost used block before removing it
                            BlockState blockState = server.getOverworld().getBlockState(belowIndicator);
                            String ghostUsedBlockName = ServerConfig.VOTE_INDICATOR_BLOCK_GHOST_USED;
                            if (blockState.getBlock().equals(VotingManager.getBlockFromString(ghostUsedBlockName))) {
                                server.getOverworld().setBlockState(belowIndicator, Blocks.AIR.getDefaultState());
                            }
                        }
                    }

                    // Update vote indicator block to reflect alive status
                    VotingManager.updatePlayerDeathIndicator(server, uuid);
                }
            }

            // Update server-side death status
            ServerState.updateDeathStatus(newDeathStatus);

            // Clear glowing and invisibility from ALL seated players (not just dead ones)
            for (UUID uuid : seatedPlayers) {
                ServerPlayerEntity targetPlayer = server.getPlayerManager().getPlayer(uuid);
                if (targetPlayer != null) {
                    String clearGlowing = "effect clear @s glowing";
                    ServerCommands.runAs(server, targetPlayer.getUuidAsString(), clearGlowing);
                    String clearInvis = "effect clear @s invisibility";
                    ServerCommands.runAs(server, targetPlayer.getUuidAsString(), clearInvis);
                }
            }

            // Clear glowing and invisibility from the storyteller as well
            String clearStorytellerGlowing = "effect clear @s glowing";
            ServerCommands.runAs(server, storyteller.getUuidAsString(), clearStorytellerGlowing);
            String clearStorytellerInvis = "effect clear @s invisibility";
            ServerCommands.runAs(server, storyteller.getUuidAsString(), clearStorytellerInvis);

            // Remove ghost vote blocks and update vote indicators for ALL players
            for (UUID uuid : seatedPlayers) {
                Integer seat = ServerState.PLAYER_SEAT_NUMBERS.get(uuid);
                if (seat != null) {
                    BlockPos indicatorPos = ServerConfig.SEAT_VOTE_INDICATOR_POSITIONS.get(seat);
                    if (indicatorPos != null) {
                        // Remove ghost used block if present
                        BlockPos belowIndicator = indicatorPos.down();
                        BlockState blockState = server.getOverworld().getBlockState(belowIndicator);
                        String ghostUsedBlockName = ServerConfig.VOTE_INDICATOR_BLOCK_GHOST_USED;
                        if (blockState.getBlock().equals(VotingManager.getBlockFromString(ghostUsedBlockName))) {
                            server.getOverworld().setBlockState(belowIndicator, Blocks.AIR.getDefaultState());
                        }
                    }
                }
                // Update vote indicator block
                VotingManager.updatePlayerDeathIndicator(server, uuid);
            }

            // Restore vote indicators for unseated seats to normal state
            VoteIndicators.restoreUnseatedVoteIndicators(server);

            // Rebuild seated seats' indicator stacks, then re-power every seat's vote piston
            // (redstone block below the piston)
            VoteIndicators.paintVoteIndicatorStacks(server, false);
            ElectionManager.powerAllSeatPistons(server);

            // Reset lever states (physically and in state) and broadcast to clients
            VotingManager.resetLeverStates(server);

            // Set in-game time to dawn
            server.getOverworld().setTimeOfDay(ServerConfig.TIME_DAWN);

            // A reset during the night would otherwise leave everyone nameless until dawn
            showAllNameTags(server);

            // 2. Broadcast death status to ALL clients (so everyone sees players as alive)
            SendDeathStatusS2CPayload deathStatusPayload =
                    new SendDeathStatusS2CPayload(newDeathStatus);
            for (ServerPlayerEntity onlinePlayer : server.getPlayerManager().getPlayerList()) {
                ServerPlayNetworking.send(onlinePlayer, deathStatusPayload);
            }

            // 3. Clear server-side role state for players (but keep seat numbers for storyteller reference)
            ServerState.PLAYER_ROLES.clear();
            ServerState.PLAYER_DEATH_STATUS.clear();

            // Clear ghost votes and daytime state
            DaytimeState.clearAllGhostVotes();
            DaytimeState.clearBansheeDoubleVotes();
            ServerState.gameEnded = false;
            DaytimeState.resetDaily();
            // Wipe the traveler set (canBeExiled keys) so a stale entry from the previous
            // game cannot leak into the next one before the storyteller hits Send Roles.
            DaytimeState.initializeExileEligibility(Collections.emptySet());

            // Broadcast updated daytime state
            StateBroadcaster.broadcastDaytimeState(server);

            // 4. Send NO_ROLE payload to all seated players to unassign them
            // Do NOT send ClearGrimoireS2CPayload - players keep their grimoire to review the previous game
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                UUID playerUuid = player.getUuid();
                // Only send NO_ROLE to players who were seated (not storyteller)
                if (seatedPlayers.contains(playerUuid)) {
                    ServerPlayNetworking.send(player,
                        SendRoleS2CPayload.ofRole(Role.NO_ROLE, true, 0, 0, true));
                }
            }

            // 5. Rebuild night order HUD for operators (after day/night reset to 0)
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                if (player.hasPermissionLevel(2)) {
                    ServerPlayNetworking.send(player,
                        new RebuildNightOrderS2CPayload());
                }
            }

            source.sendFeedback(() -> Text.literal("Game reset: everyone revived, roles cleared, day/night back to 0. Seats and grimoires kept.")
                    .formatted(Formatting.GREEN), true);
            return 1;

        } catch (Exception e) {
            source.sendError(Text.literal("Error starting new game: " + e.getMessage()));
            return 0;
        }
    }

    static int setPlayerName(ServerCommandSource source, String name) {
        try {
            ServerPlayerEntity player = source.getPlayerOrThrow();
            MinecraftServer server = source.getServer();

            // An empty name means "reset", so drop the saved entry instead of storing a blank.
            boolean reset = name == null || name.isBlank();
            if (!reset && name.trim().length() > ServerConfig.MAX_NAME_LENGTH) {
                source.sendError(Text.literal("Names can be at most " + ServerConfig.MAX_NAME_LENGTH + " characters."));
                return 0;
            }
            if (reset) {
                ServerConfig.CUSTOM_PLAYER_NAMES.remove(player.getUuid().toString());
            } else {
                ServerConfig.CUSTOM_PLAYER_NAMES.put(player.getUuid().toString(), name.trim());
            }
            ServerConfig.save();
            CustomNames.set(player.getUuid(), reset ? null : name.trim());
            StateBroadcaster.syncCustomNames(server, player);

            source.sendFeedback(() -> (reset
                    ? Text.literal("Name reset to " + player.getGameProfile().getName())
                    : Text.literal("Set your name to: " + name.trim()))
                    .formatted(Formatting.GREEN), false);
            return 1;

        } catch (Exception e) {
            source.sendError(Text.literal("Error setting name: " + e.getMessage()));
            return 0;
        }
    }
}
