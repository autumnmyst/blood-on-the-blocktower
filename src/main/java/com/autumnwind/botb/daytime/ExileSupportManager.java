package com.autumnwind.botb.daytime;

import com.autumnwind.botb.config.ServerConfig;
import com.autumnwind.botb.networking.*;
import com.autumnwind.botb.states.ServerState;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/**
 * Manages the exile support voting process.
 * This is separate from regular voting - no Organ Grinder, Banshee, or Legion rules apply.
 * All players (alive and dead) can participate without consuming ghost votes.
 * Timer logic is now unified through ElectionManager.runVotingPhase().
 */
public class ExileSupportManager {

    // Cached state for result calculation (set during exile support, used in callbacks)
    private static Set<UUID> cachedDeadPlayers = new HashSet<>();

    /**
     * Starts an exile support vote for the current exile target.
     * Uses ElectionManager.runVotingPhase() for unified timer logic.
     * @param server The server instance
     * @param deadPlayers Set of UUIDs of dead players
     */
    public static void startExileSupport(MinecraftServer server, Set<UUID> deadPlayers) {
        if (!DaytimeState.hasActiveExile()) {
            return; // No active exile call
        }

        UUID target = DaytimeState.getCurrentExileTarget();
        UUID caller = DaytimeState.getCurrentExileCaller();

        // Cache for callbacks
        cachedDeadPlayers = new HashSet<>(deadPlayers);

        // Get config - ElectionState was already initialized by ExileManager.executeExile()
        ElectionConfig config = ElectionConfig.forExileSupport();
        ElectionState.updateConfig(config);

        // Ghost used blocks were saved and removed in ExileManager.executeExile().

        // DaytimeState mirrors the same state, and plenty of callers read the exile flags from
        // there rather than from ElectionState, so both are kept in step.
        DaytimeState.startExileSupport();

        // ===== Exile-specific pre-work (before unified timer logic) =====

        // Set all exile support indicators to initial state AND initialize exile support votes
        List<UUID> supportOrder = ElectionState.getElectionOrder();
        initializeExileSupportVotes(server, supportOrder);
        setAllExileSupportIndicators(server, supportOrder, deadPlayers);

        // Keep clock hands in exile mode pointing at target
        ServerPlayer targetPlayer = server.getPlayerList().getPlayer(target);
        Vec3 targetPos = targetPlayer != null ? targetPlayer.position() : null;
        StateBroadcaster.broadcastClockHandsState(
                server,
                ClockHandsStateS2CPayload.MODE_EXILE,
                null,  // No hour hand for exile
                targetPos,
                false,  // No fade in (already visible)
                false   // No swivel
        );

        // Broadcast state update
        StateBroadcaster.broadcastDaytimeState(server);

        // ===== Run unified voting phase with exile-specific callbacks =====
        ElectionManager.runVotingPhase(
                server,
                config,
                deadPlayers,
                ExileSupportManager::onSupportLocked,      // Per-vote callback (clock hands)
                ExileSupportManager::broadcastExileSupportUpdate, // Broadcast callback (50ms)
                ExileSupportManager::onExileResult,        // Result callback
                ExileSupportManager::onExileCleanup        // Cleanup callback
        );
    }

    /**
     * Callback: Called when each individual support is locked.
     * Updates clock hands to point at the supporter.
     * Note: Indicator update is handled by ElectionManager.lockNextVote() already.
     */
    private static void onSupportLocked(MinecraftServer server, UUID supporter, int seat, boolean supportValue) {
        // Update clock hand to point at current supporter
        ServerPlayer supporterPlayer = server.getPlayerList().getPlayer(supporter);
        if (supporterPlayer != null) {
            StateBroadcaster.broadcastClockHandsState(
                    server,
                    ClockHandsStateS2CPayload.MODE_EXILE,
                    null,
                    supporterPlayer.position(),
                    false,
                    false
            );
        }
        // Note: Indicator block is set by ElectionManager.updateIndicator() - no need to call again here

        // Broadcast daytime state for client sync
        StateBroadcaster.broadcastDaytimeState(server);
    }

    /**
     * Callback: Called when exile support voting is complete to handle result.
     */
    private static void onExileResult(MinecraftServer server, int supportCount, int threshold) {
        // Determine result
        boolean exilePassed = supportCount >= threshold;

        // Get target name
        UUID target = DaytimeState.getCurrentExileTarget();
        ServerPlayer targetPlayer = server.getPlayerList().getPlayer(target);
        String targetName = targetPlayer != null ? targetPlayer.getName().getString() : Component.translatable("gui.blood-on-the-blocktower.common.unknown_player").getString();

        // Build result message
        Component resultText;
        if (exilePassed) {
            resultText = Component.translatable("message.blood-on-the-blocktower.daytime.exile_passed")
                    .withStyle(style -> style.withColor(0x9932CC)) // Purple
                    .append(Component.literal(" - ").withStyle(ChatFormatting.WHITE))
                    .append(Component.literal(targetName).withStyle(style -> style.withColor(0x9932CC)))
                    .append(Component.translatable("message.blood-on-the-blocktower.daytime.supports_count", supportCount).withStyle(ChatFormatting.GRAY));
        } else {
            resultText = Component.translatable("message.blood-on-the-blocktower.daytime.exile_failed")
                    .withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(" - ").withStyle(ChatFormatting.WHITE))
                    .append(Component.literal(targetName).withStyle(style -> style.withColor(0x9932CC)))
                    .append(Component.translatable("message.blood-on-the-blocktower.daytime.supports_count", supportCount).withStyle(ChatFormatting.GRAY));
        }

        // Build title/subtitle for display
        Component titleText;
        Component subtitleText;
        String soundType;

        if (exilePassed) {
            titleText = Component.literal(targetName).withStyle(style -> style.withColor(0x9932CC)); // Purple
            subtitleText = Component.translatable("message.blood-on-the-blocktower.daytime.exiled").withStyle(style -> style.withColor(0x9932CC));
            soundType = PlaySoundS2CPayload.MARKED;
        } else {
            titleText = Component.translatable("message.blood-on-the-blocktower.daytime.exile_failed").withStyle(ChatFormatting.GRAY);
            subtitleText = Component.translatable("message.blood-on-the-blocktower.daytime.not_enough_support", targetName).withStyle(ChatFormatting.GRAY);
            soundType = PlaySoundS2CPayload.NOT_ENOUGH_VOTES;
        }

        // Send result to all players
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            // Send chat message
            player.displayClientMessage(resultText, false);

            // Send title and subtitle
            player.connection.send(new ClientboundSetTitleTextPacket(titleText));
            player.connection.send(new ClientboundSetSubtitleTextPacket(subtitleText));

            // Send sound
            ServerPlayNetworking.send(player, new PlaySoundS2CPayload(soundType));
        }

        // Reset exile state (don't restore eligibility - the exile was completed)
        // Don't reset indicators here - onExileCleanup will handle it after the piston delay
        ExileManager.resetExile(server, false, false);

        // End exile support
        DaytimeState.endExileSupport();

        // Broadcast state update
        StateBroadcaster.broadcastDaytimeState(server);
    }

    /**
     * Callback: Called after result display for cleanup.
     * Restores ghost used blocks and resets indicators.
     */
    private static void onExileCleanup(MinecraftServer server) {
        // Restore ghost used blocks
        restoreGhostUsedBlocks(server);

        // Reset vote indicators to normal state
        List<UUID> supportOrder = ElectionManager.getElectionOrder();
        resetAllExileSupportIndicators(server, supportOrder);

        // Clear cached state
        cachedDeadPlayers.clear();
    }

    /**
     * Saves which seats have ghost used blocks, then removes them temporarily.
     * This includes both dead players who used their ghost vote AND voudon-blocked alive players.
     * Also resets levers to off state, like after a vote is counted.
     */
    private static void saveAndRemoveGhostUsedBlocks(MinecraftServer server, Set<UUID> deadPlayers) {
        Level world = server.overworld();
        Set<Integer> seatsWithGhostBlocks = new HashSet<>();
        Block ghostUsedBlock = ElectionManager.getBlockFromString(ServerConfig.VOTE_INDICATOR_BLOCK_GHOST_USED);

        // Check ALL seated players for ghost used blocks (not just dead ones)
        for (Map.Entry<UUID, Integer> entry : ServerState.PLAYER_SEAT_NUMBERS.entrySet()) {
            UUID player = entry.getKey();
            Integer seat = entry.getValue();
            if (seat == null || seat <= 0) continue;

            BlockPos indicatorPos = ServerConfig.SEAT_VOTE_INDICATOR_POSITIONS.get(seat);
            if (indicatorPos == null) continue;

            BlockPos belowIndicator = indicatorPos.below();
            BlockState blockState = world.getBlockState(belowIndicator);

            if (blockState.getBlock().equals(ghostUsedBlock)) {
                seatsWithGhostBlocks.add(seat);
                // Remove the ghost used block (set to AIR)
                world.setBlockAndUpdate(belowIndicator, Blocks.AIR.defaultBlockState());

                // Also set the main indicator to the normal OFF state
                // (for Voudon-blocked players, their main indicator may be AIR)
                Block offBlock = ElectionManager.getBlockFromString(ServerConfig.VOTE_INDICATOR_BLOCK_OFF);
                if (offBlock != null) {
                    world.setBlockAndUpdate(indicatorPos, offBlock.defaultBlockState());
                }

                // Reset the lever to off state (like after a vote is counted)
                BlockPos switchPos = ServerConfig.SEAT_SWITCH_POSITIONS.get(seat);
                if (switchPos != null) {
                    BlockState leverState = world.getBlockState(switchPos);
                    if (leverState.getBlock() instanceof LeverBlock) {
                        if (leverState.getValue(LeverBlock.POWERED)) {
                            world.setBlockAndUpdate(switchPos, leverState.setValue(LeverBlock.POWERED, false));
                        }
                    }
                }
            }
        }

        // Save the seats for later restoration
        DaytimeState.saveGhostUsedBlockSeats(seatsWithGhostBlocks);
    }

    /**
     * Restores ghost used blocks after exile support ends.
     * Logic:
     * - Voudon-blocked alive player → back to no indicator at all over the ghost used block
     * - Dead player who used their ghost vote → ghost used block, except in Voudon mode
     *   (dead vote freely there, so they show a normal OFF and the seat stays saved for
     *   when Voudon mode ends) or while Organ Grinder delays the reveal
     * - Otherwise → ghost OFF for dead, normal OFF for alive (Voudon dead use normal OFF)
     */
    private static void restoreGhostUsedBlocks(MinecraftServer server) {
        Set<Integer> seatsToRestore = DaytimeState.getAndClearGhostUsedBlockSeats();
        Block offBlock = ElectionManager.getBlockFromString(ServerConfig.VOTE_INDICATOR_BLOCK_OFF);
        Block ghostOffBlock = ElectionManager.getBlockFromString(ServerConfig.VOTE_INDICATOR_BLOCK_GHOST_OFF);

        // Build reverse map: seat -> player UUID
        Map<Integer, UUID> seatToPlayer = new HashMap<>();
        for (Map.Entry<UUID, Integer> entry : ServerState.PLAYER_SEAT_NUMBERS.entrySet()) {
            seatToPlayer.put(entry.getValue(), entry.getKey());
        }

        boolean voudonModeActive = DaytimeState.isVoudonModeActive();
        Set<UUID> voudonBlockedPlayers = DaytimeState.getVoudonBlockedPlayers();
        boolean organGrinderDelaying = DaytimeState.isOrganGrinderMode();
        Set<Integer> seatsToKeepSaved = new HashSet<>();

        for (Integer seat : seatsToRestore) {
            UUID playerUuid = seatToPlayer.get(seat);
            if (playerUuid == null) continue;

            boolean isDead = ServerState.PLAYER_DEATH_STATUS.getOrDefault(playerUuid, false);
            boolean hasUsedGhostVote = DaytimeState.hasUsedGhostVote(playerUuid);
            boolean isVoudonBlocked = voudonModeActive && voudonBlockedPlayers.contains(playerUuid);

            if (isVoudonBlocked) {
                // Voudon-blocked alive player: no vote at all, not even an "off" indicator
                VotingManager.setUsedGhostVoteIndicator(server, seat, null);
            } else if (isDead && hasUsedGhostVote && voudonModeActive) {
                // Dead players vote freely in Voudon mode: normal indicator now, and the
                // seat stays saved so the ghost used block returns when Voudon mode ends
                VotingManager.removeUsedGhostVoteIndicator(server, seat, offBlock);
                seatsToKeepSaved.add(seat);
            } else if (isDead && hasUsedGhostVote && !organGrinderDelaying) {
                // Dead player who used their ghost vote: ghost used block, indicator = AIR
                VotingManager.setUsedGhostVoteIndicator(server, seat);
            } else if (isDead) {
                VotingManager.removeUsedGhostVoteIndicator(server, seat,
                        voudonModeActive ? offBlock : ghostOffBlock);
            } else {
                // Alive player - use normal OFF indicator
                VotingManager.removeUsedGhostVoteIndicator(server, seat, offBlock);
            }
        }

        if (!seatsToKeepSaved.isEmpty()) {
            DaytimeState.saveGhostUsedBlockSeats(seatsToKeepSaved);
        }
    }

    /**
     * Resets all exile support indicators to the appropriate OFF state.
     * This restores the normal vote indicator blocks after exile support ends.
     * - Skips players who have used ghost vote (their indicators are set by restoreGhostUsedBlocks)
     * - Skips voudon-blocked players (their indicators are set by restoreGhostUsedBlocks)
     * - Uses ghost OFF for dead players
     * - Uses normal OFF for alive players
     * @param server The server instance
     * @param supportOrder The support order list
     */
    private static void resetAllExileSupportIndicators(MinecraftServer server, List<UUID> supportOrder) {
        Level world = server.overworld();
        Block offBlock = ElectionManager.getBlockFromString(ServerConfig.VOTE_INDICATOR_BLOCK_OFF);
        Block ghostOffBlock = ElectionManager.getBlockFromString(ServerConfig.VOTE_INDICATOR_BLOCK_GHOST_OFF);

        boolean voudonModeActive = DaytimeState.isVoudonModeActive();
        Set<UUID> voudonBlockedPlayers = DaytimeState.getVoudonBlockedPlayers();

        for (UUID player : supportOrder) {
            Integer seat = ServerState.PLAYER_SEAT_NUMBERS.get(player);
            if (seat == null) continue;

            BlockPos indicatorPos = ServerConfig.SEAT_VOTE_INDICATOR_POSITIONS.get(seat);
            if (indicatorPos == null) continue;

            boolean isDead = ServerState.PLAYER_DEATH_STATUS.getOrDefault(player, false);
            boolean hasUsedGhostVote = DaytimeState.hasUsedGhostVote(player);
            boolean isVoudonBlocked = voudonModeActive && voudonBlockedPlayers.contains(player);

            // Voudon-blocked alive players have no vote at all; re-asserted here in case
            // restoreGhostUsedBlocks had no saved seat for them
            if (isVoudonBlocked) {
                VotingManager.setUsedGhostVoteIndicator(server, seat, null);
                continue;
            }

            // Skip players whose ghost used indicators were just restored
            // They were handled by restoreGhostUsedBlocks already
            if (isDead && hasUsedGhostVote) {
                continue;
            }

            // Set appropriate OFF indicator. In Voudon mode dead players vote like
            // alive ones, so they get the normal OFF rather than the ghost variant.
            Block indicatorBlock;
            if (isDead && !voudonModeActive) {
                // Dead player (without used ghost vote) - use ghost OFF
                indicatorBlock = ghostOffBlock;
            } else {
                // Alive player - use normal OFF
                indicatorBlock = offBlock;
            }

            if (indicatorBlock != null) {
                world.setBlockAndUpdate(indicatorPos, indicatorBlock.defaultBlockState());
            }
        }
    }

    /**
     * Initializes exile support votes from current lever states.
     * This populates DaytimeState.exileSupportVotes so the HUD shows correct initial states.
     * @param server The server instance
     * @param supportOrder The support order list
     */
    private static void initializeExileSupportVotes(MinecraftServer server, List<UUID> supportOrder) {
        Level world = server.overworld();

        for (UUID player : supportOrder) {
            Integer seat = ServerState.PLAYER_SEAT_NUMBERS.get(player);
            if (seat == null) continue;

            BlockPos switchPos = ServerConfig.SEAT_SWITCH_POSITIONS.get(seat);
            if (switchPos == null) continue;

            BlockState leverState = world.getBlockState(switchPos);
            boolean isOn = leverState.getBlock() instanceof LeverBlock && leverState.getValue(LeverBlock.POWERED);

            DaytimeState.setExileSupportVote(player, isOn);
        }
    }

    /**
     * Sets exile support indicators for all players.
     * Uses exile-specific indicator blocks (pearlescent froglight for on, amethyst for off).
     * @param server The server instance
     * @param supportOrder The support order list
     * @param deadPlayers Set of dead player UUIDs
     */
    private static void setAllExileSupportIndicators(MinecraftServer server, List<UUID> supportOrder, Set<UUID> deadPlayers) {
        Level world = server.overworld();

        for (UUID player : supportOrder) {
            Integer seat = ServerState.PLAYER_SEAT_NUMBERS.get(player);
            if (seat == null) continue;

            BlockPos indicatorPos = ServerConfig.SEAT_VOTE_INDICATOR_POSITIONS.get(seat);
            if (indicatorPos == null) continue;

            BlockPos switchPos = ServerConfig.SEAT_SWITCH_POSITIONS.get(seat);
            if (switchPos == null) continue;

            BlockState leverState = world.getBlockState(switchPos);
            boolean isOn = leverState.getBlock() instanceof LeverBlock && leverState.getValue(LeverBlock.POWERED);

            // Use exile support indicator blocks
            String blockName = isOn ? ServerConfig.EXILE_SUPPORT_INDICATOR_BLOCK_ON
                                   : ServerConfig.EXILE_SUPPORT_INDICATOR_BLOCK_OFF;
            Block block = ElectionManager.getBlockFromString(blockName);
            if (block != null) {
                world.setBlockAndUpdate(indicatorPos, block.defaultBlockState());
            }
        }
    }

    /**
     * Broadcasts exile support state update to all players.
     * Similar to VotingManager.broadcastVoteUpdate() but for exile support.
     * Uses ElectionManager getters for support order and timing.
     */
    public static void broadcastExileSupportUpdate(MinecraftServer server) {
        if (!DaytimeState.isExileSupportInProgress()) {
            return;
        }

        List<UUID> supportOrder = ElectionManager.getElectionOrder();
        long electionStartTime = ElectionManager.getElectionStartTime();

        Map<UUID, Boolean> lockedSupports = DaytimeState.getLockedExileSupportVotes();
        // Count only YES votes (true), not all locked votes
        int lockedCount = (int) lockedSupports.values().stream().filter(Boolean.TRUE::equals).count();

        // Calculate time until each player's vote locks
        long elapsedMs = System.currentTimeMillis() - electionStartTime;
        long firstLockTime = 3000; // Initial 3-second countdown
        long perPlayerTime = ServerConfig.VOTE_TIME_PER_PLAYER;

        // Get current lever states for exile support
        Map<UUID, Boolean> leverStates = DaytimeState.getExileSupportVotes();

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            UUID playerUuid = player.getUUID();

            // Calculate when this player's vote locks
            int playerPosition = supportOrder.indexOf(playerUuid) + 1;

            // Calculate seconds until this player's vote locks (announce + commit delay)
            long playerLockTime = firstLockTime + (playerPosition - 1) * perPlayerTime
                    + ElectionManager.PISTON_POWER_DELAY_TICKS * 50L;
            long remainingTime = playerLockTime - elapsedMs;
            int secondsUntilLock = (int) Math.max(0, Math.ceil(remainingTime / 1000.0));

            // If player's vote is already locked, show 0
            if (lockedSupports.containsKey(playerUuid)) {
                secondsUntilLock = 0;
            }

            VoteStateUpdateS2CPayload updatePayload = new VoteStateUpdateS2CPayload(
                lockedCount,
                secondsUntilLock,
                playerPosition,
                lockedSupports,
                leverStates,
                false, // No Organ Grinder mode for exile
                false, // No secretly used ghost vote for exile
                true,  // isExileSupport = true
                false, // No Voudon blocking for exile support
                DaytimeState.getBansheeDoubleVotePlayers(),
                DaytimeState.getBansheeDoubleVoteActivePlayers()
            );

            ServerPlayNetworking.send(player, updatePayload);
        }
    }

    /**
     * Resets the exile support vote in progress.
     * Uses ElectionManager.cancelVotingPhase() for unified timer handling.
     */
    public static void resetExileSupport(MinecraftServer server, Set<UUID> deadPlayers) {
        // Get support order before cancelling (needed for cleanup)
        List<UUID> supportOrder = ElectionManager.getElectionOrder();

        // Cancel voting phase in ElectionManager (handles timers and piston power)
        ElectionManager.cancelVotingPhase(server);

        // End election in ElectionState
        ElectionState.endElection();

        // Stop sounds
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            ServerPlayNetworking.send(player, new PlaySoundS2CPayload(PlaySoundS2CPayload.VOTE_MUSIC_STOP));
            ServerPlayNetworking.send(player, new PlaySoundS2CPayload(PlaySoundS2CPayload.CLOCK_TICKING_STOP));
        }

        // Restore ghost used blocks
        restoreGhostUsedBlocks(server);

        // Reset vote indicators
        resetAllExileSupportIndicators(server, supportOrder);

        // Reset exile state (restore eligibility since this was a reset, not completion)
        ExileManager.resetExile(server, true);

        // End exile support
        DaytimeState.endExileSupport();

        // Broadcast state
        StateBroadcaster.broadcastDaytimeState(server);

        // Clear cached state
        cachedDeadPlayers.clear();
    }

}
