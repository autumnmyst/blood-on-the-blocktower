package com.autumnwind.botb.daytime;

import com.autumnwind.botb.config.ServerConfig;
import com.autumnwind.botb.networking.PlaySoundS2CPayload;
import com.autumnwind.botb.states.ServerState;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import com.autumnwind.botb.networking.SendDeathStatusS2CPayload;
import com.autumnwind.botb.networking.StateBroadcaster;
import com.autumnwind.botb.util.ServerCommands;

/**
 * Manages player execution logic.
 */
public class ExecutionManager {

    // Timer for keeping player locked in execution position
    private static Timer lockTimer = null;

    /**
     * Executes a player - runs their execution command, displays title, plays sound.
     * Works for both seated players (normal execution) and unseated players (e.g., storyteller in Atheist script).
     * @param server The server instance
     * @param player The player to execute
     * @param butcherAliveWithAbility If true, Butcher mode activates (nominations continue after execution)
     * @param butcherUuid The UUID of the Butcher player (only they can nominate after execution), null if not applicable
     */
    public static void executePlayer(MinecraftServer server, UUID player, boolean butcherAliveWithAbility, UUID butcherUuid) {
        if (player == null) return;

        ServerPlayer serverPlayer = server.getPlayerList().getPlayer(player);
        if (serverPlayer == null) return;

        String playerName = serverPlayer.getName().getString();
        Integer seat = ServerState.PLAYER_SEAT_NUMBERS.get(player);

        // Check if this is an unseated player (e.g., storyteller in Atheist script)
        boolean isUnseatedPlayer = seat == null;

        // IMMEDIATELY: Teleport player to execution position if set
        BlockPos executionPos = ServerConfig.EXECUTION_POSITION;
        if (executionPos != null) {
            // For unseated players, set gamemode to adventure before teleporting
            if (isUnseatedPlayer) {
                serverPlayer.setGameMode(GameType.ADVENTURE);
            }

            serverPlayer.teleportTo(
                    server.overworld(),
                    executionPos.getX() + 0.5,
                    executionPos.getY(),
                    executionPos.getZ() + 0.5,
                    serverPlayer.getYRot(),
                    serverPlayer.getXRot()
            );
        }

        // IMMEDIATELY: Spawn anvil at anvil height if configured (> 0)
        if (ServerConfig.ANVIL_HEIGHT > 0 && executionPos != null) {
            Level world = server.overworld();
            BlockPos anvilPos = executionPos.above(ServerConfig.ANVIL_HEIGHT);
            world.setBlockAndUpdate(anvilPos, Blocks.ANVIL.defaultBlockState());
        }

        // IMMEDIATELY: Start locking player in position if enabled
        if (ServerConfig.LOCK_IN_EXECUTION_POSITION && executionPos != null) {
            // Cancel any existing lock timer
            if (lockTimer != null) {
                lockTimer.cancel();
            }
            lockTimer = new Timer();

            // Store final reference for lambda
            final BlockPos lockPos = executionPos;

            // Keep player frozen until 1 second after death title is displayed
            long lockDuration = ServerConfig.EXECUTION_DEATH_TITLE_DELAY + 1000;
            final long lockEndTime = System.currentTimeMillis() + lockDuration;

            lockTimer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    if (System.currentTimeMillis() >= lockEndTime) {
                        // Time's up, cancel the lock
                        cancel();
                        if (lockTimer != null) {
                            lockTimer.cancel();
                            lockTimer = null;
                        }
                        return;
                    }

                    server.execute(() -> {
                        ServerPlayer p = server.getPlayerList().getPlayer(player);
                        if (p == null) return;

                        // Check if player moved too far from execution position (more than 0.5 blocks)
                        double dx = p.getX() - (lockPos.getX() + 0.5);
                        double dy = p.getY() - lockPos.getY();
                        double dz = p.getZ() - (lockPos.getZ() + 0.5);
                        double distanceSquared = dx * dx + dy * dy + dz * dz;

                        if (distanceSquared > 0.25) { // 0.5 blocks squared
                            // Teleport player back to execution position
                            p.teleportTo(
                                    server.overworld(),
                                    lockPos.getX() + 0.5,
                                    lockPos.getY(),
                                    lockPos.getZ() + 0.5,
                                    p.getYRot(),
                                    p.getXRot()
                            );
                        }
                    });
                }
            }, 50, 50); // Check every 50ms
        }

        // IMMEDIATELY: Execute the player's seat execution command (seated players only)
        if (!isUnseatedPlayer) {
            String executionCommand = ServerConfig.EXECUTION_COMMANDS.get(seat);
            if (executionCommand != null && !executionCommand.isEmpty()) {
                // Execute command with server permissions targeting the player
                ServerCommands.runAs(server, serverPlayer.getStringUUID(), executionCommand);
            }
        }

        // Track execution for Undertaker/Cannibal: only a living player dies by execution
        if (!ServerState.PLAYER_DEATH_STATUS.getOrDefault(player, false)) {
            ServerState.executionToday = true;
        }

        // Broadcast day/night state to sync executionToday to clients (for Undertaker role visits)
        StateBroadcaster.broadcastDayNightState(server);

        // Reset daytime state (close nominations, clear eligibility, MFE, etc.) like Dusk does
        // First remove MFE glow before resetting
        UUID currentMFE = DaytimeState.getMarkedForExecution();
        if (currentMFE != null) {
            NominationManager.updateMarkedGlow(server, currentMFE, false);
        }

        // Reset vote (removes blocks, cancels timers, removes nominee glow)
        VotingManager.resetVote(server);

        // Handle Butcher mode or reset all daytime state
        if (butcherAliveWithAbility && butcherUuid != null) {
            // Butcher mode: nominations continue, only Butcher can nominate

            // Reset nomination eligibility - only Butcher can nominate
            for (UUID uuid : ServerState.PLAYER_SEAT_NUMBERS.keySet()) {
                DaytimeState.setCanNominate(uuid, uuid.equals(butcherUuid));
                // All non-travelers can be nominated again
                if (!DaytimeState.isTraveler(uuid)) {
                    DaytimeState.setCanBeNominated(uuid, true);
                }
            }

            // Clear MFE state (but keep nominations open)
            DaytimeState.clearMarkedForExecution();
            DaytimeState.clearStorytellerMFE();

            // Broadcast updated state
            StateBroadcaster.broadcastDaytimeState(server);
        } else {
            // Normal case: reset all daytime state (like Dusk)
            DaytimeState.resetDaily();
        }

        // DELAYED: Play execution sound after configured delay (using Timer for non-blocking delay)
        if (ServerConfig.EXECUTION_SOUND_DELAY > 0) {
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    server.execute(() -> {
                        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                            ServerPlayNetworking.send(p, new PlaySoundS2CPayload(PlaySoundS2CPayload.EXECUTION));
                        }
                    });
                }
            }, ServerConfig.EXECUTION_SOUND_DELAY);
        } else {
            // No delay - play immediately
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                ServerPlayNetworking.send(p, new PlaySoundS2CPayload(PlaySoundS2CPayload.EXECUTION));
            }
        }

        // DELAYED: Display execution title and mark as dead after configured delay (using Timer for non-blocking delay)
        Component titleText = Component.literal(playerName).withStyle(ChatFormatting.DARK_RED);
        Component subtitleText = Component.translatable("message.blood-on-the-blocktower.daytime.has_been_executed").withStyle(ChatFormatting.DARK_RED);

        if (ServerConfig.EXECUTION_DEATH_TITLE_DELAY > 0) {
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    server.execute(() -> {
                        // Mark player as dead at the moment of title display
                        ServerState.PLAYER_DEATH_STATUS.put(player, true);

                        // Cancel lock timer - player is now dead and should not be locked anymore
                        cancelLockTimer();

                        // Apply invisibility effect to dead player
                        String invisibilityCommand = "effect give @s invisibility infinite 0 true";
                        ServerCommands.runAs(server, player.toString(), invisibilityCommand);

                        // Broadcast death status to all players (including storyteller)
                        SendDeathStatusS2CPayload deathStatusPayload =
                                new SendDeathStatusS2CPayload(ServerState.PLAYER_DEATH_STATUS);
                        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                            ServerPlayNetworking.send(p, deathStatusPayload);
                        }

                        // Update daytime state (remove from canNominate)
                        DaytimeState.setCanNominate(player, false);

                        // Update vote indicator block to reflect death status
                        VotingManager.updatePlayerDeathIndicator(server, player);

                        // Display title and message
                        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                            p.connection.send(new ClientboundSetTitleTextPacket(titleText));
                            p.connection.send(new ClientboundSetSubtitleTextPacket(subtitleText));
                            p.displayClientMessage(titleText.copy().append(" ").append(subtitleText), false);
                        }

                        // Schedule anvil cleanup 1 second after title display
                        scheduleAnvilCleanup(server);
                    });
                }
            }, ServerConfig.EXECUTION_DEATH_TITLE_DELAY);
        } else {
            // No delay - mark as dead and display immediately
            ServerState.PLAYER_DEATH_STATUS.put(player, true);

            // Cancel lock timer - player is now dead and should not be locked anymore
            cancelLockTimer();

            // Apply invisibility effect to dead player
            String invisibilityCommand = "effect give @s invisibility infinite 0 true";
            ServerCommands.runAs(server, player.toString(), invisibilityCommand);

            // Broadcast death status to all players (including storyteller)
            SendDeathStatusS2CPayload deathStatusPayload =
                    new SendDeathStatusS2CPayload(ServerState.PLAYER_DEATH_STATUS);
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                ServerPlayNetworking.send(p, deathStatusPayload);
            }

            // Update daytime state (remove from canNominate)
            DaytimeState.setCanNominate(player, false);

            // Update vote indicator block to reflect death status
            VotingManager.updatePlayerDeathIndicator(server, player);

            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                p.connection.send(new ClientboundSetTitleTextPacket(titleText));
                p.connection.send(new ClientboundSetSubtitleTextPacket(subtitleText));
                p.displayClientMessage(titleText.copy().append(" ").append(subtitleText), false);
            }

            // Schedule anvil cleanup 1 second after title display
            scheduleAnvilCleanup(server);
        }
    }

    /**
     * Cancels the execution lock timer if running.
     */
    private static void cancelLockTimer() {
        if (lockTimer != null) {
            lockTimer.cancel();
            lockTimer = null;
        }
    }

    /**
     * Schedules anvil cleanup 1 second after execution.
     * Replaces all anvil variants within 2 blocks vertically of the execution position with air.
     */
    private static void scheduleAnvilCleanup(MinecraftServer server) {
        BlockPos executionPos = ServerConfig.EXECUTION_POSITION;
        if (executionPos == null) return;

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                server.execute(() -> {
                    Level world = server.overworld();
                    // Check execution position and up to 2 blocks above
                    for (int yOffset = 0; yOffset <= 2; yOffset++) {
                        BlockPos checkPos = executionPos.above(yOffset);
                        Block block = world.getBlockState(checkPos).getBlock();
                        // Check for all anvil variants (anvil, chipped_anvil, damaged_anvil)
                        if (block == Blocks.ANVIL || block == Blocks.CHIPPED_ANVIL || block == Blocks.DAMAGED_ANVIL) {
                            world.setBlockAndUpdate(checkPos, Blocks.AIR.defaultBlockState());
                        }
                    }
                });
            }
        }, 1000); // 1 second delay
    }

    /**
     * Executes a player (failed execution - no death marking).
     * Same as executePlayer but does not mark as dead and gives resistance.
     * @param server The server instance
     * @param player The player to execute
     * @param butcherAliveWithAbility If true, Butcher mode activates (nominations continue after execution)
     * @param butcherUuid The UUID of the Butcher player (only they can nominate after execution), null if not applicable
     */
    public static void executePlayerFail(MinecraftServer server, UUID player, boolean butcherAliveWithAbility, UUID butcherUuid) {
        if (player == null) return;

        ServerPlayer serverPlayer = server.getPlayerList().getPlayer(player);
        if (serverPlayer == null) return;

        String playerName = serverPlayer.getName().getString();
        Integer seat = ServerState.PLAYER_SEAT_NUMBERS.get(player);

        // IMMEDIATELY: Teleport player to execution position if set
        BlockPos executionPos = ServerConfig.EXECUTION_POSITION;
        if (executionPos != null) {
            serverPlayer.teleportTo(
                    server.overworld(),
                    executionPos.getX() + 0.5,
                    executionPos.getY(),
                    executionPos.getZ() + 0.5,
                    serverPlayer.getYRot(),
                    serverPlayer.getXRot()
            );
        }

        // IMMEDIATELY: Spawn anvil at anvil height if configured (> 0)
        if (ServerConfig.ANVIL_HEIGHT > 0 && executionPos != null) {
            Level world = server.overworld();
            BlockPos anvilPos = executionPos.above(ServerConfig.ANVIL_HEIGHT);
            world.setBlockAndUpdate(anvilPos, Blocks.ANVIL.defaultBlockState());
        }

        // IMMEDIATELY: Start locking player in position if enabled
        if (ServerConfig.LOCK_IN_EXECUTION_POSITION && executionPos != null) {
            // Cancel any existing lock timer
            if (lockTimer != null) {
                lockTimer.cancel();
            }
            lockTimer = new Timer();

            // Store final reference for lambda
            final BlockPos lockPos = executionPos;

            // Keep player frozen until 1 second after death title is displayed
            long lockDuration = ServerConfig.EXECUTION_DEATH_TITLE_DELAY + 1000;
            final long lockEndTime = System.currentTimeMillis() + lockDuration;

            lockTimer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    if (System.currentTimeMillis() >= lockEndTime) {
                        // Time's up, cancel the lock
                        cancel();
                        if (lockTimer != null) {
                            lockTimer.cancel();
                            lockTimer = null;
                        }
                        return;
                    }

                    server.execute(() -> {
                        ServerPlayer p = server.getPlayerList().getPlayer(player);
                        if (p == null) return;

                        // Check if player moved too far from execution position (more than 0.5 blocks)
                        double dx = p.getX() - (lockPos.getX() + 0.5);
                        double dy = p.getY() - lockPos.getY();
                        double dz = p.getZ() - (lockPos.getZ() + 0.5);
                        double distanceSquared = dx * dx + dy * dy + dz * dz;

                        if (distanceSquared > 0.25) { // 0.5 blocks squared
                            // Teleport player back to execution position
                            p.teleportTo(
                                    server.overworld(),
                                    lockPos.getX() + 0.5,
                                    lockPos.getY(),
                                    lockPos.getZ() + 0.5,
                                    p.getYRot(),
                                    p.getXRot()
                            );
                        }
                    });
                }
            }, 50, 50); // Check every 50ms
        }

        // IMMEDIATELY: Execute the player's seat execution command
        if (seat != null) {
            String executionCommand = ServerConfig.EXECUTION_COMMANDS.get(seat);
            if (executionCommand != null && !executionCommand.isEmpty()) {
                // Execute command with server permissions targeting the player
                ServerCommands.runAs(server, serverPlayer.getStringUUID(), executionCommand);
            }
        }

        // Calculate resistance timing (6 seconds total: 1 second before title + 5 seconds after)
        // Title is displayed at EXECUTION_DEATH_TITLE_DELAY
        long resistanceStartDelay = Math.max(0, ServerConfig.EXECUTION_DEATH_TITLE_DELAY - 1000); // 1 second before title
        int resistanceDurationSeconds = 6; // 6 seconds

        // Apply resistance effect with delay
        if (resistanceStartDelay > 0) {
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    server.execute(() -> {
                        String resistanceCommand = "effect give @s resistance " + resistanceDurationSeconds + " 4 true";
                        ServerCommands.runAs(server, player.toString(), resistanceCommand);
                    });
                }
            }, resistanceStartDelay);
        } else {
            // Apply immediately if title delay is very short
            String resistanceCommand = "effect give @s resistance " + resistanceDurationSeconds + " 4 true";
            ServerCommands.runAs(server, player.toString(), resistanceCommand);
        }

        // Reset daytime state (close nominations, clear eligibility, MFE, etc.) like Dusk does
        // First remove MFE glow before resetting
        UUID currentMFE = DaytimeState.getMarkedForExecution();
        if (currentMFE != null) {
            NominationManager.updateMarkedGlow(server, currentMFE, false);
        }

        // Reset vote (removes blocks, cancels timers, removes nominee glow)
        VotingManager.resetVote(server);

        // Handle Butcher mode or reset all daytime state
        if (butcherAliveWithAbility && butcherUuid != null) {
            // Butcher mode: nominations continue, only Butcher can nominate

            // Reset nomination eligibility - only Butcher can nominate
            for (UUID uuid : ServerState.PLAYER_SEAT_NUMBERS.keySet()) {
                DaytimeState.setCanNominate(uuid, uuid.equals(butcherUuid));
                // All non-travelers can be nominated again
                if (!DaytimeState.isTraveler(uuid)) {
                    DaytimeState.setCanBeNominated(uuid, true);
                }
            }

            // Clear MFE state (but keep nominations open)
            DaytimeState.clearMarkedForExecution();
            DaytimeState.clearStorytellerMFE();

            // Broadcast updated state
            StateBroadcaster.broadcastDaytimeState(server);
        } else {
            // Normal case: reset all daytime state (like Dusk)
            DaytimeState.resetDaily();
        }

        // DELAYED: Play execution survived sound after configured delay (using Timer for non-blocking delay)
        if (ServerConfig.EXECUTION_SURVIVED_SOUND_DELAY > 0) {
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    server.execute(() -> {
                        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                            ServerPlayNetworking.send(p, new PlaySoundS2CPayload(PlaySoundS2CPayload.EXECUTION_SURVIVED));
                        }
                    });
                }
            }, ServerConfig.EXECUTION_SURVIVED_SOUND_DELAY);
        } else {
            // No delay - play immediately
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                ServerPlayNetworking.send(p, new PlaySoundS2CPayload(PlaySoundS2CPayload.EXECUTION_SURVIVED));
            }
        }

        // DELAYED: Display execution title and chat message after configured delay (using Timer for non-blocking delay)
        Component titleText = Component.literal(playerName).withStyle(ChatFormatting.DARK_RED);
        Component subtitleText = Component.translatable("message.blood-on-the-blocktower.daytime.survives_execution").withStyle(ChatFormatting.GOLD);

        if (ServerConfig.EXECUTION_DEATH_TITLE_DELAY > 0) {
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    server.execute(() -> {
                        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                            p.connection.send(new ClientboundSetTitleTextPacket(titleText));
                            p.connection.send(new ClientboundSetSubtitleTextPacket(subtitleText));
                            p.displayClientMessage(titleText.copy().append(" ").append(subtitleText), false);
                        }

                        // Schedule anvil cleanup 1 second after title display
                        scheduleAnvilCleanup(server);
                    });
                }
            }, ServerConfig.EXECUTION_DEATH_TITLE_DELAY);
        } else {
            // No delay - display immediately
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                p.connection.send(new ClientboundSetTitleTextPacket(titleText));
                p.connection.send(new ClientboundSetSubtitleTextPacket(subtitleText));
                p.displayClientMessage(titleText.copy().append(" ").append(subtitleText), false);
            }

            // Schedule anvil cleanup 1 second after title display
            scheduleAnvilCleanup(server);
        }
    }

}
