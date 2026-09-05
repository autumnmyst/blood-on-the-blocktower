package com.autumnwind.botb.networking;

import com.autumnwind.botb.config.ServerConfig;
import com.autumnwind.botb.daytime.*;
import com.autumnwind.botb.states.ServerState;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;

import java.util.*;
import com.autumnwind.botb.BloodOnTheBlocktower;
import com.autumnwind.botb.voicechat.VoiceChatServerCompat;
import net.minecraft.block.BlockState;
import net.minecraft.item.Item;
import net.minecraft.network.packet.CustomPayload;
public class ModPackets {

    /**
     * Registers a server-bound receiver so that an exception inside the handler is logged and
     * dropped instead of escaping into the server's task queue, where it would take the whole
     * server down. Handlers run on the server thread, so the catch is the only guard needed.
     */
    static <T extends CustomPayload> void registerGuarded(
            CustomPayload.Id<T> id,
            ServerPlayNetworking.PlayPayloadHandler<T> handler) {
        ServerPlayNetworking.registerGlobalReceiver(id, (payload, context) -> {
            try {
                handler.receive(payload, context);
            } catch (Exception e) {
                BloodOnTheBlocktower.LOGGER.error("Error handling packet {} from {}",
                        id.id(), context.player().getGameProfile().getName(), e);
            }
        });
    }

    /** Registers every server-bound handler, grouped by domain. */
    public static void registerC2SReceivers() {
        RoleHandlers.register();
        TeleportHandlers.register();
        DayNightHandlers.register();
        DeathHandlers.register();
        ScriptHandlers.register();
        GrimoireHandlers.register();
        WhisperHandlers.register();
        DaytimeHandlers.register();
        TimerHandlers.register();
        GameEndHandlers.register();
    }

    /**
     * Removes a player from their current voice chat group.
     * @param player The player to remove from voice chat group
     */
    static void leaveVoiceChatGroup(ServerPlayerEntity player) {
        VoiceChatServerCompat.leaveGroup(player.getUuid());
    }

    /**
     * Processes pending ghost vote updates that were delayed during Organ Grinder mode.
     * Places the ghost used indicator blocks below the vote indicators for players who used their ghost vote.
     * @param server The server instance
     */
    static void processPendingGhostVoteUpdates(MinecraftServer server) {
        Set<UUID> pendingUpdates = DaytimeState.getPendingGhostVoteUpdates();

        // Finalize secretly used ghost votes (mark them as actually used now that Dusk is happening)
        DaytimeState.finalizeSecretGhostVotes();

        if (pendingUpdates.isEmpty()) {
            // Still need to broadcast daytime state to sync the finalized ghost votes
            StateBroadcaster.broadcastDaytimeState(server);
            return;
        }

        for (UUID playerUuid : pendingUpdates) {
            Integer seat = ServerState.PLAYER_SEAT_NUMBERS.get(playerUuid);
            if (seat == null) continue;

            VotingManager.setUsedGhostVoteIndicator(server, seat);
        }

        // Clear the pending updates now that they've been processed
        DaytimeState.clearPendingGhostVoteUpdates();
    }

    /**
     * Brings the server's Banshee double-vote set in line with the grimoire's "Has Ability"
     * reminders. Players who just gained the ability join the Banshee system (a dead Banshee's
     * spent ghost vote is revived); players who lost it fall back to their original ghost-vote
     * state. Run at dawn and again when nominations open, so a Banshee whose status changed
     * during the day (died, lost the reminder) votes correctly that day.
     */
    public static void syncBansheeAbility(MinecraftServer server, Collection<UUID> currentBansheeAbility, Set<UUID> deadPlayers) {
        // Determine who lost the ability by comparing current list with server's internal set
                Set<UUID> previousBansheeAbility = DaytimeState.getBansheeDoubleVotePlayers();
        Set<UUID> lostAbility = new HashSet<>(previousBansheeAbility);
        lostAbility.removeAll(currentBansheeAbility);

        // Handle players who CURRENTLY have the Banshee "Has Ability" reminder
        // They should use the banshee system (normal indicators, not ghost indicators)
        for (UUID bansheePlayer : currentBansheeAbility) {
            // Only enable if they're not already in the banshee system (first time gaining ability)
            // This preserves the underlying ghost vote state from before they gained the ability
            boolean isNewlyGainingAbility = !DaytimeState.hasBansheeDoubleVote(bansheePlayer);

            if (isNewlyGainingAbility) {
                // Enable banshee double vote for this player (saves underlying ghost vote state)
                DaytimeState.enableBansheeDoubleVote(bansheePlayer);

                // If they're dead and had previously used their ghost vote, revive their vote token
                boolean isDead = deadPlayers.contains(bansheePlayer);
                if (isDead && DaytimeState.hasUsedGhostVote(bansheePlayer)) {
                    // Reset their ghost vote used status (revive vote token)
                    DaytimeState.resetGhostVote(bansheePlayer);

                    // Remove ghost used block below their indicator
                    Integer seat = ServerState.PLAYER_SEAT_NUMBERS.get(bansheePlayer);
                    if (seat != null) {
                        BlockPos indicatorPos = ServerConfig.SEAT_VOTE_INDICATOR_POSITIONS.get(seat);
                        if (indicatorPos != null) {
                            BlockPos belowIndicator = indicatorPos.down();
                            BlockState blockState = server.getOverworld().getBlockState(belowIndicator);
                            String ghostUsedBlockName = ServerConfig.VOTE_INDICATOR_BLOCK_GHOST_USED;
                            if (blockState.getBlock().equals(VotingManager.getBlockFromString(ghostUsedBlockName))) {
                                server.getOverworld().setBlockState(belowIndicator, Blocks.AIR.getDefaultState());
                            }
                        }
                    }
                }
            }

            // Update their indicator to normal OFF (not ghost OFF)
            VotingManager.updatePlayerDeathIndicator(server, bansheePlayer);
        }

        // Handle players who LOST the Banshee "Has Ability" reminder
        // They should revert to normal ghost vote system with their ORIGINAL state
        for (UUID lostAbilityPlayer : lostAbility) {
            // Check their ORIGINAL ghost vote state (from before they gained the ability)
            // Voting via banshee system does NOT consume ghost vote
            boolean hadUsedGhostVote = DaytimeState.isBansheeUnderlyingGhostVoteUsed(lostAbilityPlayer);
            boolean isDead = deadPlayers.contains(lostAbilityPlayer);

            // Disable banshee double vote - this restores their original ghost vote state
            DaytimeState.disableBansheeDoubleVote(lostAbilityPlayer);

            // If they're dead and had ALREADY used their ghost vote before gaining the ability,
            // set them to used ghost vote state:
            // - indicator = AIR, ghost used block below, upward-facing sticky piston below that
            if (isDead && hadUsedGhostVote) {
                Integer seat = ServerState.PLAYER_SEAT_NUMBERS.get(lostAbilityPlayer);
                if (seat != null) {
                    VotingManager.setUsedGhostVoteIndicator(server, seat);
                }
                // Do NOT call updatePlayerDeathIndicator - indicator is now frozen
            } else {
                // Normal case: update their indicator via the standard method
                VotingManager.updatePlayerDeathIndicator(server, lostAbilityPlayer);
            }
        }
    }

    /**
     * Helper method to check if a player has a specific item in their inventory.
     */
    static boolean playerHasItem(ServerPlayerEntity player, Item item) {
        // Check main inventory
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.isOf(item)) {
                return true;
            }
        }
        return false;
    }
}
