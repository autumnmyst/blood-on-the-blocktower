package com.autumnwind.botb.daytime;

import com.autumnwind.botb.config.ServerConfig;
import com.autumnwind.botb.networking.ClockHandsStateS2CPayload;
import com.autumnwind.botb.networking.PlaySoundS2CPayload;
import com.autumnwind.botb.states.ServerState;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import java.util.*;
import com.autumnwind.botb.networking.StateBroadcaster;
import com.autumnwind.botb.world.TeamManager;
import net.minecraft.world.scores.TeamColor;

/**
 * Manages exile logic for travelers.
 * Exile is completely separate from nominations/voting.
 * - Any player (alive or dead) can call for exile of any traveler
 * - Travelers can only be called for exile once per day
 * - Dead players can participate in exile support without consuming ghost votes
 * - No Organ Grinder, Banshee, or Legion rules apply
 */
public class ExileManager {

    /**
     * Validates if an exile call is legal.
     * @param caller The player calling for exile
     * @param traveler The traveler being called for exile
     * @param override If true, skip eligibility checks
     * @return true if exile call is valid
     */
    public static boolean validateExile(UUID caller, UUID traveler, boolean override) {
        if (caller == null || traveler == null) {
            return false;
        }

        if (!override) {
            // Check if traveler can be exiled (hasn't been called for exile today)
            if (!DaytimeState.canBeExiled(traveler)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Executes an exile call: updates state, sends messages, shows clock hands.
     * @param server The server instance
     * @param caller The player calling for exile
     * @param traveler The traveler being called for exile
     * @param totalPlayerCount Total number of players (for vote threshold calculation - exile uses ALL players, not just alive)
     */
    public static void executeExile(MinecraftServer server, UUID caller, UUID traveler, int totalPlayerCount) {
        // Clear any existing exile first
        if (DaytimeState.hasActiveExile()) {
            resetExile(server);
        }

        // Update state
        DaytimeState.setCurrentExileCaller(caller);
        DaytimeState.setCurrentExileTarget(traveler);

        // Initialize ElectionState for this exile call (voting phase starts later)
        ElectionConfig config = ElectionConfig.forExileSupport();
        ElectionState.beginElection(
                ElectionType.EXILE_SUPPORT,
                config,
                traveler,
                caller,
                ServerState.PLAYER_SEAT_NUMBERS
        );

        // Mark this traveler as no longer eligible for exile today
        DaytimeState.setCanBeExiled(traveler, false);

        // Get player entities
        ServerPlayer callerPlayer = server.getPlayerList().getPlayer(caller);
        ServerPlayer travelerPlayer = server.getPlayerList().getPlayer(traveler);

        // Add traveler to botb_traveler team for purple glow color, then apply glowing effect
        if (travelerPlayer != null) {
            Scoreboard scoreboard = server.getScoreboard();
            String playerName = travelerPlayer.getGameProfile().name();

            // Remove from botb_player team if on it
            PlayerTeam playerTeam = scoreboard.getPlayerTeam(TeamManager.PLAYER_TEAM);
            if (playerTeam != null && scoreboard.getPlayersTeam(playerName) == playerTeam) {
                scoreboard.removePlayerFromTeam(playerName, playerTeam);
            }

            // Add to botb_traveler team for purple glow
            PlayerTeam travelerTeam = scoreboard.getPlayerTeam(TeamManager.TRAVELER_TEAM);
            if (travelerTeam == null) {
                travelerTeam = scoreboard.addPlayerTeam(TeamManager.TRAVELER_TEAM);
                travelerTeam.setColor(Optional.of(TeamColor.LIGHT_PURPLE));
            }
            scoreboard.addPlayerToTeam(playerName, travelerTeam);

            // Apply glowing effect
            travelerPlayer.addEffect(new MobEffectInstance(
                    MobEffects.GLOWING,
                    Integer.MAX_VALUE,
                    0,
                    false,
                    false,
                    false // Don't show icon in HUD
            ));
        }

        // Get player names
        String callerName = callerPlayer != null ? callerPlayer.getName().getString() : Component.translatable("gui.blood-on-the-blocktower.common.unknown_player").getString();
        String travelerName = travelerPlayer != null ? travelerPlayer.getName().getString() : Component.translatable("gui.blood-on-the-blocktower.common.unknown_player").getString();

        // Calculate support required - at least half of ALL players (not just alive)
        int supportRequired = (int) Math.ceil(totalPlayerCount / 2.0);

        // Build message - use purple for exile (traveler color)
        Component titleText = Component.translatable("message.blood-on-the-blocktower.daytime.calls_for_exile",
                Component.literal(callerName).withStyle(style -> style.withColor(0x9932CC)), // Purple (traveler/exile color)
                Component.literal(travelerName).withStyle(style -> style.withColor(0x9932CC))) // Purple
                .withStyle(ChatFormatting.WHITE);

        Component subtitleText = Component.translatable("message.blood-on-the-blocktower.daytime.support_required", supportRequired)
                .withStyle(ChatFormatting.GRAY);

        // Send title and chat message to all players
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.sendSystemMessage(titleText.copy().append(" ").append(subtitleText), false);

            // Exiles share the nomination sound.
            ServerPlayNetworking.send(player, new PlaySoundS2CPayload(PlaySoundS2CPayload.NOMINATION));
        }

        // Broadcast clock hands state - show exile mode (minute hand only, pointing at traveler)
        Vec3 travelerPos = travelerPlayer != null ? travelerPlayer.position() : null;
        StateBroadcaster.broadcastClockHandsState(
                server,
                ClockHandsStateS2CPayload.MODE_EXILE,
                null,  // No hour hand for exile
                travelerPos,
                true,  // fadeIn
                true   // swivel
        );

        // Save and remove ghost used blocks so all players can participate
        // (ghost votes don't apply to exile support)
        saveAndRemoveGhostUsedBlocks(server);

        // Set all exile indicators at the start of exile call
        setAllExileIndicators(server);

        // Broadcast lever states so sidebar can show them
        VotingManager.broadcastLeverStates(server);

        // Broadcast daytime state update
        StateBroadcaster.broadcastDaytimeState(server);
    }

    /**
     * Sets exile indicators for all seated players based on their current lever state.
     * Called at the start of an exile call.
     */
    public static void setAllExileIndicators(MinecraftServer server) {
        Level world = server.overworld();

        for (Map.Entry<UUID, Integer> entry : ServerState.PLAYER_SEAT_NUMBERS.entrySet()) {
            Integer seat = entry.getValue();
            if (seat == null) continue;

            BlockPos indicatorPos = ServerConfig.SEAT_VOTE_INDICATOR_POSITIONS.get(seat);
            if (indicatorPos == null) continue;

            BlockPos switchPos = ServerConfig.SEAT_SWITCH_POSITIONS.get(seat);
            if (switchPos == null) continue;

            BlockState leverState = world.getBlockState(switchPos);
            boolean isOn = leverState.getBlock() instanceof LeverBlock
                    && leverState.getValue(LeverBlock.POWERED);

            // Use exile indicator blocks
            String blockName = isOn ? ServerConfig.EXILE_SUPPORT_INDICATOR_BLOCK_ON
                                   : ServerConfig.EXILE_SUPPORT_INDICATOR_BLOCK_OFF;
            Block block = BuiltInRegistries.BLOCK.getValue(
                    Identifier.tryParse(blockName));
            if (block != null) {
                world.setBlockAndUpdate(indicatorPos, block.defaultBlockState());
            }
        }
    }

    /**
     * Updates a single exile indicator for a player.
     * Called when a lever is flipped during an active exile call.
     */
    public static void updateExileIndicator(MinecraftServer server, int seat, boolean isOn) {
        Level world = server.overworld();

        BlockPos indicatorPos = ServerConfig.SEAT_VOTE_INDICATOR_POSITIONS.get(seat);
        if (indicatorPos == null) return;

        String blockName = isOn ? ServerConfig.EXILE_SUPPORT_INDICATOR_BLOCK_ON
                               : ServerConfig.EXILE_SUPPORT_INDICATOR_BLOCK_OFF;
        Block block = BuiltInRegistries.BLOCK.getValue(
                Identifier.tryParse(blockName));
        if (block != null) {
            world.setBlockAndUpdate(indicatorPos, block.defaultBlockState());
        }
    }

    /**
     * Resets all exile indicators to the normal OFF state.
     * Called when an exile call is reset (before support runs).
     */
    public static void resetAllExileIndicators(MinecraftServer server) {
        Level world = server.overworld();
        Block offBlock = ElectionManager.getBlockFromString(ServerConfig.VOTE_INDICATOR_BLOCK_OFF);
        Block ghostOffBlock = ElectionManager.getBlockFromString(ServerConfig.VOTE_INDICATOR_BLOCK_GHOST_OFF);

        boolean voudonModeActive = DaytimeState.isVoudonModeActive();
        UUID voudonUuid = DaytimeState.getVoudonPlayerUuid();

        for (Map.Entry<UUID, Integer> entry : ServerState.PLAYER_SEAT_NUMBERS.entrySet()) {
            UUID player = entry.getKey();
            Integer seat = entry.getValue();
            if (seat == null) continue;

            BlockPos indicatorPos = ServerConfig.SEAT_VOTE_INDICATOR_POSITIONS.get(seat);
            if (indicatorPos == null) continue;

            boolean isDead = ServerState.PLAYER_DEATH_STATUS.getOrDefault(player, false);

            // Voudon-blocked alive players have no vote at all
            if (voudonModeActive && !isDead && !player.equals(voudonUuid)) {
                VotingManager.setUsedGhostVoteIndicator(server, seat, null);
                continue;
            }

            // Dead players with a used ghost vote were just restored to the frozen
            // ghost-used state by restoreGhostUsedBlocks; leave them be
            if (!voudonModeActive && isDead && DaytimeState.hasUsedGhostVote(player)) {
                continue;
            }

            // In Voudon mode dead players (and the Voudon) vote like alive ones
            Block block = (isDead && !voudonModeActive) ? ghostOffBlock : offBlock;
            if (block != null) {
                world.setBlockAndUpdate(indicatorPos, block.defaultBlockState());
            }
        }
    }

    /**
     * Clears the current exile call.
     * Restores exile eligibility for the traveler if the call was reset (not completed).
     * @param server The server instance
     */
    public static void resetExile(MinecraftServer server) {
        resetExile(server, true, true);
    }

    /**
     * Clears the current exile call.
     * @param server The server instance
     * @param restoreEligibility If true, restore exile eligibility for the traveler
     */
    public static void resetExile(MinecraftServer server, boolean restoreEligibility) {
        resetExile(server, restoreEligibility, true);
    }

    /**
     * Clears the current exile call.
     * @param server The server instance
     * @param restoreEligibility If true, restore exile eligibility for the traveler
     * @param resetIndicators If true, reset vote indicators to normal state
     */
    public static void resetExile(MinecraftServer server, boolean restoreEligibility, boolean resetIndicators) {
        // If exile was cancelled (not completed), restore ghost used blocks
        // Note: restoreEligibility=true means exile was cancelled, false means it was completed
        if (restoreEligibility) {
            restoreGhostUsedBlocks(server);
        }

        // Reset vote indicators to normal state (from exile blocks) - only if requested
        if (resetIndicators) {
            resetAllExileIndicators(server);
        }

        UUID traveler = DaytimeState.getCurrentExileTarget();

        // Remove glowing effect and move traveler back to botb_player team
        if (traveler != null) {
            ServerPlayer travelerPlayer = server.getPlayerList().getPlayer(traveler);
            if (travelerPlayer != null) {
                travelerPlayer.removeEffect(MobEffects.GLOWING);

                // Remove from botb_traveler team and add back to botb_player team
                Scoreboard scoreboard = server.getScoreboard();
                String playerName = travelerPlayer.getGameProfile().name();

                PlayerTeam travelerTeam = scoreboard.getPlayerTeam(TeamManager.TRAVELER_TEAM);
                if (travelerTeam != null && scoreboard.getPlayersTeam(playerName) == travelerTeam) {
                    scoreboard.removePlayerFromTeam(playerName, travelerTeam);
                }

                PlayerTeam playerTeam = scoreboard.getPlayerTeam(TeamManager.PLAYER_TEAM);
                if (playerTeam == null) {
                    playerTeam = scoreboard.addPlayerTeam(TeamManager.PLAYER_TEAM);
                    playerTeam.setColor(Optional.of(TeamColor.WHITE));
                }
                scoreboard.addPlayerToTeam(playerName, playerTeam);
            }
        }

        // Restore exile eligibility if this was a reset (not a completed exile)
        if (restoreEligibility && traveler != null) {
            DaytimeState.setCanBeExiled(traveler, true);
        }

        // Clear state
        DaytimeState.resetExile();

        // End election in ElectionState if active
        if (ElectionState.hasActiveElection() && ElectionState.isExileSupportType()) {
            ElectionState.endElection();
        }

        // Hide clock hands
        StateBroadcaster.broadcastClockHandsState(
                server,
                ClockHandsStateS2CPayload.MODE_HIDDEN,
                null,
                null,
                false,  // fadeIn (will fade out instead)
                false   // swivel
        );
    }

    // ========== Ghost Vote Block Helper Functions for Exile ==========

    /**
     * Saves which seats have ghost used blocks, then removes them temporarily.
     * Called when an exile is called so all players can participate.
     * This includes both dead players who used their ghost vote AND voudon-blocked alive players.
     * Also resets levers to off state.
     */
    private static void saveAndRemoveGhostUsedBlocks(MinecraftServer server) {
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

                // Also set the main indicator to the exile OFF state
                Block offBlock = ElectionManager.getBlockFromString(ServerConfig.EXILE_SUPPORT_INDICATOR_BLOCK_OFF);
                if (offBlock != null) {
                    world.setBlockAndUpdate(indicatorPos, offBlock.defaultBlockState());
                }

                // Reset the lever to off state
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
     * Restores ghost used blocks after exile is cancelled or completed.
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
}
