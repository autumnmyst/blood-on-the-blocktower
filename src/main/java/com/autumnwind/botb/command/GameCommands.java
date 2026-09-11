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
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.Team;
import com.autumnwind.botb.networking.StateBroadcaster;
import com.autumnwind.botb.world.VoteIndicators;
import com.autumnwind.botb.util.ServerCommands;
import com.autumnwind.botb.world.TeamManager;

/** Handlers for the /botb commands that act on a running game: resets, end game, names, teleports. */
final class GameCommands {

    private GameCommands() {}

    static int teleportToSeat(CommandSourceStack source, int seat) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            BlockPos pos = ServerConfig.SEAT_HOMES.get(seat);
            ServerLevel world = player.serverLevel();

            if (pos != null) {
                player.teleportTo(world, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, player.getYRot(), player.getXRot());

                // Find the player assigned to this seat and play doorbell sound for them
                for (ServerPlayer onlinePlayer : source.getServer().getPlayerList().getPlayers()) {
                    Integer playerSeat = ServerState.PLAYER_SEAT_NUMBERS.get(onlinePlayer.getUUID());
                    if (playerSeat != null && playerSeat == seat) {
                        ServerPlayNetworking.send(onlinePlayer, new PlaySoundS2CPayload(PlaySoundS2CPayload.DOORBELL));
                        break;
                    }
                }

                source.sendSuccess(() -> Component.translatable("message.blood-on-the-blocktower.command.teleported_to_seat", seat).withStyle(ChatFormatting.GRAY), false);
                return 1;
            } else {
                source.sendFailure(Component.translatable("message.blood-on-the-blocktower.teleport.seat_home_not_set", seat));
                return 0;
            }
        } catch (Exception e) {
            source.sendFailure(Component.translatable("message.blood-on-the-blocktower.command.error.teleport", e.getMessage()));
            return 0;
        }
    }

    static int endGame(CommandSourceStack source, boolean goodWins) {
        try {
            ServerPlayer player = source.getPlayerOrException();

            // Send request to the storyteller who ran the command to provide grimoire data
            ServerPlayNetworking.send(player,
                new RequestGameEndS2CPayload(goodWins));

            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.translatable("message.blood-on-the-blocktower.command.error.end_game", e.getMessage()));
            return 0;
        }
    }

    /** Make both game teams' name tags visible again, the same way dawn does. */
    static void showAllNameTags(MinecraftServer server) {
        Scoreboard scoreboard = server.getScoreboard();
        PlayerTeam playerTeam = scoreboard.getPlayerTeam(TeamManager.PLAYER_TEAM);
        if (playerTeam != null) {
            playerTeam.setNameTagVisibility(Team.Visibility.ALWAYS);
        }
        PlayerTeam travelerTeam = scoreboard.getPlayerTeam(TeamManager.TRAVELER_TEAM);
        if (travelerTeam != null) {
            travelerTeam.setNameTagVisibility(Team.Visibility.ALWAYS);
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

    static int resetGameHard(CommandSourceStack source) {
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
                    ServerPlayer targetPlayer = server.getPlayerList().getPlayer(uuid);

                    // Execute revive command
                    if (seat != null) {
                        String reviveCommand = ServerConfig.REVIVE_COMMANDS.get(seat);
                        if (reviveCommand != null && !reviveCommand.isEmpty() && targetPlayer != null) {
                            ServerCommands.runAs(server, targetPlayer.getStringUUID(), reviveCommand);
                        }
                    }

                    // Remove invisibility effect
                    if (targetPlayer != null) {
                        String clearInvisCommand = "effect clear @s invisibility";
                        ServerCommands.runAs(server, targetPlayer.getStringUUID(), clearInvisCommand);
                    }

                    // Remove ghost used block below their indicator (only if it's actually the ghost used block)
                    if (seat != null) {
                        BlockPos indicatorPos = ServerConfig.SEAT_VOTE_INDICATOR_POSITIONS.get(seat);
                        if (indicatorPos != null) {
                            BlockPos belowIndicator = indicatorPos.below();
                            // Check if the block is actually the ghost used block before removing it
                            BlockState blockState = server.overworld().getBlockState(belowIndicator);
                            String ghostUsedBlockName = ServerConfig.VOTE_INDICATOR_BLOCK_GHOST_USED;
                            if (blockState.getBlock().equals(VotingManager.getBlockFromString(ghostUsedBlockName))) {
                                server.overworld().setBlockAndUpdate(belowIndicator, Blocks.AIR.defaultBlockState());
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
                ServerPlayer targetPlayer = server.getPlayerList().getPlayer(uuid);
                if (targetPlayer != null) {
                    String clearEffectsCommand = "effect clear @s glowing";
                    ServerCommands.runAs(server, targetPlayer.getStringUUID(), clearEffectsCommand);
                    String clearInvisCommand = "effect clear @s invisibility";
                    ServerCommands.runAs(server, targetPlayer.getStringUUID(), clearInvisCommand);
                }
            }

            // Clear glowing and invisibility from storyteller
            ServerPlayer storyteller = source.getPlayer();
            if (storyteller != null) {
                String clearGlowing = "effect clear @s glowing";
                ServerCommands.runAs(server, storyteller.getStringUUID(), clearGlowing);
                String clearInvis = "effect clear @s invisibility";
                ServerCommands.runAs(server, storyteller.getStringUUID(), clearInvis);
            }

            // Remove ghost vote blocks and update vote indicators for ALL players
            for (UUID uuid : seatedPlayers) {
                Integer seat = ServerState.PLAYER_SEAT_NUMBERS.get(uuid);
                if (seat != null) {
                    BlockPos indicatorPos = ServerConfig.SEAT_VOTE_INDICATOR_POSITIONS.get(seat);
                    if (indicatorPos != null) {
                        // Remove ghost used block if present
                        BlockPos belowIndicator = indicatorPos.below();
                        BlockState blockState = server.overworld().getBlockState(belowIndicator);
                        String ghostUsedBlockName = ServerConfig.VOTE_INDICATOR_BLOCK_GHOST_USED;
                        if (blockState.getBlock().equals(VotingManager.getBlockFromString(ghostUsedBlockName))) {
                            server.overworld().setBlockAndUpdate(belowIndicator, Blocks.AIR.defaultBlockState());
                        }
                    }
                }
                // Update vote indicator block
                VotingManager.updatePlayerDeathIndicator(server, uuid);
            }

            // Restore vote indicators for unseated seats to normal state (before seats are cleared)
            VoteIndicators.restoreUnseatedVoteIndicators(server);

            // Rebuild every seat's indicator stack, empty seats included, then re-power every
            // seat's vote piston (redstone block below the piston)
            VoteIndicators.paintVoteIndicatorStacks(server, true);
            ElectionManager.powerAllSeatPistons(server);

            // Reset lever states (physically and in state) and broadcast to clients
            VotingManager.resetLeverStates(server);

            // Set in-game time to dawn
            server.overworld().setDayTime(ServerConfig.TIME_DAWN);

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
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                ServerPlayNetworking.send(player,
                    SendRoleS2CPayload.ofRole(Role.NO_ROLE, true, 0, 0, false));
                ServerPlayNetworking.send(player,
                    new SendSeatsS2CPayload(new HashMap<>()));
                ServerPlayNetworking.send(player,
                    new SendDeathStatusS2CPayload(new HashMap<>()));
                ServerPlayNetworking.send(player,
                    new ClearGrimoireS2CPayload());
            }

            source.sendSuccess(() -> Component.translatable("message.blood-on-the-blocktower.command.full_reset")
                    .withStyle(ChatFormatting.GREEN), true);
            return 1;

        } catch (Exception e) {
            source.sendFailure(Component.translatable("message.blood-on-the-blocktower.command.error.reset_game", e.getMessage()));
            return 0;
        }
    }

    static int resetGame(CommandSourceStack source) {
        try {
            MinecraftServer server = source.getServer();
            ServerPlayer storyteller = source.getPlayerOrException();

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
                    ServerPlayer targetPlayer = server.getPlayerList().getPlayer(uuid);

                    // Execute revive command
                    if (seat != null) {
                        String reviveCommand = ServerConfig.REVIVE_COMMANDS.get(seat);
                        if (reviveCommand != null && !reviveCommand.isEmpty() && targetPlayer != null) {
                            ServerCommands.runAs(server, targetPlayer.getStringUUID(), reviveCommand);
                        }
                    }

                    // Remove invisibility effect
                    if (targetPlayer != null) {
                        String clearInvisCommand = "effect clear @s invisibility";
                        ServerCommands.runAs(server, targetPlayer.getStringUUID(), clearInvisCommand);
                    }

                    // Remove ghost used block below their indicator (only if it's actually the ghost used block)
                    if (seat != null) {
                        BlockPos indicatorPos = ServerConfig.SEAT_VOTE_INDICATOR_POSITIONS.get(seat);
                        if (indicatorPos != null) {
                            BlockPos belowIndicator = indicatorPos.below();
                            // Check if the block is actually the ghost used block before removing it
                            BlockState blockState = server.overworld().getBlockState(belowIndicator);
                            String ghostUsedBlockName = ServerConfig.VOTE_INDICATOR_BLOCK_GHOST_USED;
                            if (blockState.getBlock().equals(VotingManager.getBlockFromString(ghostUsedBlockName))) {
                                server.overworld().setBlockAndUpdate(belowIndicator, Blocks.AIR.defaultBlockState());
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
                ServerPlayer targetPlayer = server.getPlayerList().getPlayer(uuid);
                if (targetPlayer != null) {
                    String clearGlowing = "effect clear @s glowing";
                    ServerCommands.runAs(server, targetPlayer.getStringUUID(), clearGlowing);
                    String clearInvis = "effect clear @s invisibility";
                    ServerCommands.runAs(server, targetPlayer.getStringUUID(), clearInvis);
                }
            }

            // Clear glowing and invisibility from the storyteller as well
            String clearStorytellerGlowing = "effect clear @s glowing";
            ServerCommands.runAs(server, storyteller.getStringUUID(), clearStorytellerGlowing);
            String clearStorytellerInvis = "effect clear @s invisibility";
            ServerCommands.runAs(server, storyteller.getStringUUID(), clearStorytellerInvis);

            // Remove ghost vote blocks and update vote indicators for ALL players
            for (UUID uuid : seatedPlayers) {
                Integer seat = ServerState.PLAYER_SEAT_NUMBERS.get(uuid);
                if (seat != null) {
                    BlockPos indicatorPos = ServerConfig.SEAT_VOTE_INDICATOR_POSITIONS.get(seat);
                    if (indicatorPos != null) {
                        // Remove ghost used block if present
                        BlockPos belowIndicator = indicatorPos.below();
                        BlockState blockState = server.overworld().getBlockState(belowIndicator);
                        String ghostUsedBlockName = ServerConfig.VOTE_INDICATOR_BLOCK_GHOST_USED;
                        if (blockState.getBlock().equals(VotingManager.getBlockFromString(ghostUsedBlockName))) {
                            server.overworld().setBlockAndUpdate(belowIndicator, Blocks.AIR.defaultBlockState());
                        }
                    }
                }
                // Update vote indicator block
                VotingManager.updatePlayerDeathIndicator(server, uuid);
            }

            // Restore vote indicators for unseated seats to normal state
            VoteIndicators.restoreUnseatedVoteIndicators(server);

            // Rebuild every seat's indicator stack, empty seats included, then re-power every
            // seat's vote piston (redstone block below the piston)
            VoteIndicators.paintVoteIndicatorStacks(server, true);
            ElectionManager.powerAllSeatPistons(server);

            // Reset lever states (physically and in state) and broadcast to clients
            VotingManager.resetLeverStates(server);

            // Set in-game time to dawn
            server.overworld().setDayTime(ServerConfig.TIME_DAWN);

            // A reset during the night would otherwise leave everyone nameless until dawn
            showAllNameTags(server);

            // 2. Broadcast death status to ALL clients (so everyone sees players as alive)
            SendDeathStatusS2CPayload deathStatusPayload =
                    new SendDeathStatusS2CPayload(newDeathStatus);
            for (ServerPlayer onlinePlayer : server.getPlayerList().getPlayers()) {
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
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                UUID playerUuid = player.getUUID();
                // Only send NO_ROLE to players who were seated (not storyteller)
                if (seatedPlayers.contains(playerUuid)) {
                    ServerPlayNetworking.send(player,
                        SendRoleS2CPayload.ofRole(Role.NO_ROLE, true, 0, 0, true));
                }
            }

            // 5. Rebuild night order HUD for operators (after day/night reset to 0)
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (player.hasPermissions(2)) {
                    ServerPlayNetworking.send(player,
                        new RebuildNightOrderS2CPayload());
                }
            }

            source.sendSuccess(() -> Component.translatable("message.blood-on-the-blocktower.command.game_reset")
                    .withStyle(ChatFormatting.GREEN), true);
            return 1;

        } catch (Exception e) {
            source.sendFailure(Component.translatable("message.blood-on-the-blocktower.command.error.new_game", e.getMessage()));
            return 0;
        }
    }

    static int setPlayerName(CommandSourceStack source, String name) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            MinecraftServer server = source.getServer();

            // An empty name means "reset", so drop the saved entry instead of storing a blank.
            boolean reset = name == null || name.isBlank();
            if (!reset && name.trim().length() > ServerConfig.MAX_NAME_LENGTH) {
                source.sendFailure(Component.translatable("message.blood-on-the-blocktower.command.name_too_long", ServerConfig.MAX_NAME_LENGTH));
                return 0;
            }
            if (reset) {
                ServerConfig.CUSTOM_PLAYER_NAMES.remove(player.getUUID().toString());
            } else {
                ServerConfig.CUSTOM_PLAYER_NAMES.put(player.getUUID().toString(), name.trim());
            }
            ServerConfig.save();
            CustomNames.set(player.getUUID(), reset ? null : name.trim());
            StateBroadcaster.syncCustomNames(server, player);

            source.sendSuccess(() -> (reset
                    ? Component.translatable("message.blood-on-the-blocktower.command.name_reset", player.getGameProfile().getName())
                    : Component.translatable("message.blood-on-the-blocktower.command.name_set", name.trim()))
                    .withStyle(ChatFormatting.GREEN), false);
            return 1;

        } catch (Exception e) {
            source.sendFailure(Component.translatable("message.blood-on-the-blocktower.command.error.set_name", e.getMessage()));
            return 0;
        }
    }
}
