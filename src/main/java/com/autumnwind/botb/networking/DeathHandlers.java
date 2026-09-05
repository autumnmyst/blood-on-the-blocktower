package com.autumnwind.botb.networking;

import com.autumnwind.botb.config.ServerConfig;
import com.autumnwind.botb.daytime.*;
import com.autumnwind.botb.states.ServerState;
import java.util.*;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import com.autumnwind.botb.util.ServerCommands;

/** Server-bound packet handlers: Death status updates and ghost vote toggles. */
final class DeathHandlers {

    private DeathHandlers() {}

    static void register() {
        ModPackets.registerGuarded(UpdateDeadPlayersC2SPayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            if (player.hasPermissionLevel(2)) {
                Map<UUID, Boolean> newDeathStatus = payload.deadPlayers();
                Map<UUID, Integer> seatNumbers = payload.seatNumbers();
                Map<UUID, Boolean> previousDeathStatus = new HashMap<>(ServerState.PLAYER_DEATH_STATUS);

                // Execute death/revive commands based on status changes
                for (Map.Entry<UUID, Boolean> entry : newDeathStatus.entrySet()) {
                    UUID playerUuid = entry.getKey();
                    boolean isDead = entry.getValue();
                    boolean wasDeadBefore = previousDeathStatus.getOrDefault(playerUuid, false);

                    // Get seat number for this player
                    Integer seatNumber = seatNumbers.get(playerUuid);
                    if (seatNumber != null) {
                        ServerPlayerEntity targetPlayer = context.server().getPlayerManager().getPlayer(playerUuid);
                        // Player went from alive -> dead
                        if (isDead && !wasDeadBefore) {
                            String deathCommand = ServerConfig.DEATH_COMMANDS.get(seatNumber);
                            if (deathCommand != null && !deathCommand.isEmpty() && targetPlayer != null) {
                                // Execute command with server permissions targeting the player
                                ServerCommands.runAs(context.server(), targetPlayer.getUuidAsString(), deathCommand);
                            }

                            // Give invisibility effect to dead player
                            if (targetPlayer != null) {
                                String invisibilityCommand = "effect give @s invisibility infinite 0 true";
                                ServerCommands.runAs(context.server(), targetPlayer.getUuidAsString(), invisibilityCommand);

                                // Daytime mark-dead gets a cosmetic lightning strike at the
                                // player's position. Cosmetic lightning plays the visual +
                                // thunder sound but doesn't damage entities or set fires.
                                // Night deaths stay silent, both because the time-of-day
                                // check filters night-marked deaths, and because the dawn
                                // batch sends silent=true to suppress the visual for kills
                                // that happened at night but reveal at dawn.
                                boolean isDaytime = ServerState.currentNight == ServerState.currentDay
                                        && ServerState.currentDay > 0;
                                if (isDaytime && !payload.silent()) {
                                    ServerWorld targetWorld = targetPlayer.getServerWorld();
                                    LightningEntity lightning = EntityType.LIGHTNING_BOLT.create(targetWorld);
                                    if (lightning != null) {
                                        lightning.refreshPositionAfterTeleport(targetPlayer.getX(), targetPlayer.getY(), targetPlayer.getZ());
                                        lightning.setCosmetic(true);
                                        targetWorld.spawnEntity(lightning);
                                    }
                                }
                            }
                        }
                        // Player went from dead -> alive
                        else if (!isDead && wasDeadBefore) {
                            String reviveCommand = ServerConfig.REVIVE_COMMANDS.get(seatNumber);
                            if (reviveCommand != null && !reviveCommand.isEmpty() && targetPlayer != null) {
                                // Execute command with server permissions targeting the player
                                ServerCommands.runAs(context.server(), targetPlayer.getUuidAsString(), reviveCommand);
                            }

                            // Remove invisibility effect when resurrected
                            if (targetPlayer != null) {
                                String clearInvisCommand = "effect clear @s invisibility";
                                ServerCommands.runAs(context.server(), targetPlayer.getUuidAsString(), clearInvisCommand);
                            }
                        }
                    }
                }

                // Store and broadcast death status
                ServerState.updateDeathStatus(newDeathStatus);
                SendDeathStatusS2CPayload deathStatusPayload = new SendDeathStatusS2CPayload(newDeathStatus);
                for (ServerPlayerEntity onlinePlayer : context.server().getPlayerManager().getPlayerList()) {
                    ServerPlayNetworking.send(onlinePlayer, deathStatusPayload);
                }

                // Update daytime state when death status changes
                boolean daytimeStateChanged = false;

                // --- Voudon mode updates ---
                // The storyteller evaluates Voudon-alive-with-ability against the new death
                // status, so killing or reviving the Voudon flips the mode right here rather
                // than waiting for the next nomination or dawn.
                boolean wasVoudonActive = DaytimeState.isVoudonModeActive();
                Set<UUID> voudonDeadPlayers = new HashSet<>();
                for (Map.Entry<UUID, Boolean> deathEntry : newDeathStatus.entrySet()) {
                    if (deathEntry.getValue()) {
                        voudonDeadPlayers.add(deathEntry.getKey());
                    }
                }
                if (payload.voudonModeActive()) {
                    UUID voudonUuid = payload.voudonPlayerUuid().orElse(null);
                    DaytimeState.activateVoudonMode(voudonUuid);
                    VotingManager.applyVoudonModeIndicators(context.server(), voudonDeadPlayers, voudonUuid);
                } else if (wasVoudonActive) {
                    VotingManager.restoreVoudonModeIndicators(context.server(), voudonDeadPlayers);
                    DaytimeState.deactivateVoudonMode();
                }
                if (wasVoudonActive != payload.voudonModeActive()) {
                    daytimeStateChanged = true;
                }
                for (Map.Entry<UUID, Boolean> entry : newDeathStatus.entrySet()) {
                    UUID playerUuid = entry.getKey();
                    boolean isDead = entry.getValue();
                    boolean wasDeadBefore = previousDeathStatus.getOrDefault(playerUuid, false);

                    // If player died, remove them from canNominate immediately
                    // Also remove travelers from exile eligibility
                    if (isDead && !wasDeadBefore) {
                        DaytimeState.setCanNominate(playerUuid, false);
                        // Dead travelers cannot be called for exile
                        if (DaytimeState.isTraveler(playerUuid)) {
                            DaytimeState.setCanBeExiled(playerUuid, false);
                        }
                        daytimeStateChanged = true;
                    }

                    // If player was revived, reset their ghost vote, restore canNominate, and remove ghost used block
                    // Also restore travelers' exile eligibility
                    if (!isDead && wasDeadBefore) {
                        DaytimeState.resetGhostVote(playerUuid);
                        daytimeStateChanged = true;

                        // Restore canNominate if nominations are open
                        if (DaytimeState.areNominationsOpen()) {
                            DaytimeState.setCanNominate(playerUuid, true);
                        }

                        // Revived travelers can be called for exile again
                        if (DaytimeState.isTraveler(playerUuid)) {
                            DaytimeState.setCanBeExiled(playerUuid, true);
                        }

                        // Remove ghost used block below their indicator (only if it's actually the ghost used block)
                        Integer seatNumber = seatNumbers.get(playerUuid);
                        if (seatNumber != null) {
                            BlockPos indicatorPos = ServerConfig.SEAT_VOTE_INDICATOR_POSITIONS.get(seatNumber);
                            if (indicatorPos != null) {
                                BlockPos belowIndicator = indicatorPos.down();
                                // Check if the block is actually the ghost used block before removing it
                                BlockState blockState = context.server().getOverworld().getBlockState(belowIndicator);
                                String ghostUsedBlockName = ServerConfig.VOTE_INDICATOR_BLOCK_GHOST_USED;
                                if (blockState.getBlock().equals(VotingManager.getBlockFromString(ghostUsedBlockName))) {
                                    context.server().getOverworld().setBlockState(belowIndicator, Blocks.AIR.getDefaultState());
                                }
                            }
                        }
                    }

                    // Update vote indicator block to reflect new death status
                    VotingManager.updatePlayerDeathIndicator(context.server(), playerUuid);
                }

                // Broadcast daytime state if any changes were made
                if (daytimeStateChanged) {
                    StateBroadcaster.broadcastDaytimeState(context.server());
                }
            }
        });

        ModPackets.registerGuarded(ToggleGhostVoteC2SPayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            if (!player.hasPermissionLevel(2)) {
                return; // Only operators can toggle ghost votes
            }

            UUID targetUuid = payload.playerUuid();
            boolean setUsed = payload.setUsed();

            // Verify target is dead
            boolean isDead = ServerState.PLAYER_DEATH_STATUS.getOrDefault(targetUuid, false);
            if (!isDead) {
                return; // Can only toggle ghost vote for dead players
            }

            // Get seat number
            Integer seat = ServerState.PLAYER_SEAT_NUMBERS.get(targetUuid);
            if (seat == null) {
                return;
            }

            // Update the ghost vote state
            if (setUsed) {
                DaytimeState.markGhostVoteUsed(targetUuid);
                // Set the physical ghost vote indicator
                VotingManager.setUsedGhostVoteIndicator(context.server(), seat);
            } else {
                DaytimeState.resetGhostVote(targetUuid);
                // Remove the physical ghost vote indicator - show dead OFF
                Block ghostOffBlock = VotingManager.getBlockFromString(ServerConfig.VOTE_INDICATOR_BLOCK_GHOST_OFF);
                VotingManager.removeUsedGhostVoteIndicator(context.server(), seat, ghostOffBlock);
            }

            // Broadcast state update
            StateBroadcaster.broadcastDaytimeState(context.server());
        });
    }
}
