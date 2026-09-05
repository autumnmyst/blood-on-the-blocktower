package com.autumnwind.botb.world;

import com.autumnwind.botb.config.ServerConfig;
import com.autumnwind.botb.daytime.*;
import com.autumnwind.botb.networking.*;
import com.autumnwind.botb.states.ServerState;
import com.autumnwind.botb.world.VoteIndicators;
import java.util.*;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.PistonBlock;
import net.minecraft.block.PistonHeadBlock;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import com.autumnwind.botb.daytime.VotingManager;

/** Paints and reconciles the vote-indicator block stacks (piston, middle slot, indicator) for every seat. */
public final class VoteIndicators {

    private VoteIndicators() {}

    /**
     * Reconciles the vote indicator for a player at their new seat position.
     * Handles ghost votes, death status, and Organ Grinder secret ghost votes.
     * NEVER removes pistons - only places them when needed.
     */
    public static void reconcileVoteIndicator(MinecraftServer server, UUID playerUuid, int seat, boolean isDead) {
        // Check ghost vote state
        boolean ghostVoteUsed = DaytimeState.hasUsedGhostVote(playerUuid);
        boolean ghostVoteSecretlyUsed = DaytimeState.hasSecretlyUsedGhostVote(playerUuid);
        boolean isBansheeAbility = DaytimeState.hasBansheeDoubleVote(playerUuid);

        if (isDead && !isBansheeAbility && ghostVoteUsed && !ghostVoteSecretlyUsed) {
            // Dead player with used ghost vote (not secret) - place ghost used block + piston
            VotingManager.setUsedGhostVoteIndicator(server, seat);
        } else if (isDead && !isBansheeAbility && ghostVoteSecretlyUsed) {
            // Dead player with SECRETLY used ghost vote (Organ Grinder mode) - don't reveal!
            // Just show normal dead indicator without ghost used block
            VotingManager.updatePlayerDeathIndicator(server, playerUuid);
        } else {
            // Normal case: use standard indicator update
            // This handles: alive players, dead players without used ghost votes,
            // banshee players (treated as alive for voting)
            VotingManager.updatePlayerDeathIndicator(server, playerUuid);
        }
    }

    /**
     * Paints vote-indicator stacks: piston (bottom) -> middle slot -> indicator (top).
     *
     * Every seated seat gets its sticky piston written if missing, a leftover cage cleared
     * from the middle slot, and the indicator painted for the player's current state. The
     * unseated sync only rebuilds stacks on occupancy changes, so without this a seat that
     * was already taken at game start kept whatever the map had.
     *
     * With {@code includeEmptySeats}, every configured seat without a player is painted as a
     * fresh, votable seat too: piston if missing, cage or ghost-used block cleared from the
     * middle slot, and the OFF block as the indicator. Game start and reset pass false (the
     * unseated sync owns empty seats there, caging them at boot); map setup passes true.
     *
     * Run before the pistons are powered.
     */
    public static void paintVoteIndicatorStacks(MinecraftServer server, boolean includeEmptySeats) {
        ServerWorld world = server.getOverworld();
        Block unseatedBlock = VotingManager.getBlockFromString(ServerConfig.VOTE_INDICATOR_BLOCK_UNSEATED);
        BlockState pistonState = Blocks.STICKY_PISTON.getDefaultState()
                .with(PistonBlock.FACING, Direction.UP);

        for (Map.Entry<UUID, Integer> entry : ServerState.PLAYER_SEAT_NUMBERS.entrySet()) {
            BlockPos indicatorPos = ServerConfig.SEAT_VOTE_INDICATOR_POSITIONS.get(entry.getValue());
            if (indicatorPos == null) continue;
            BlockPos middle = indicatorPos.down();

            ensurePiston(world, middle.down(), pistonState);
            if (world.getBlockState(middle).getBlock().equals(unseatedBlock)) {
                world.setBlockState(middle, Blocks.AIR.getDefaultState());
            }
            VotingManager.updatePlayerDeathIndicator(server, entry.getKey());
        }

        if (!includeEmptySeats) return;

        Set<Integer> seatedSeats = new HashSet<>(ServerState.PLAYER_SEAT_NUMBERS.values());
        Block offBlock = VotingManager.getBlockFromString(ServerConfig.VOTE_INDICATOR_BLOCK_OFF);
        for (Map.Entry<Integer, BlockPos> entry : ServerConfig.SEAT_VOTE_INDICATOR_POSITIONS.entrySet()) {
            if (seatedSeats.contains(entry.getKey())) continue;
            BlockPos indicatorPos = entry.getValue();
            BlockPos middle = indicatorPos.down();

            ensurePiston(world, middle.down(), pistonState);
            // The middle slot has to be air for the piston to extend: on a freshly built map
            // it's usually still floor. Only an already-extended piston's head stays put.
            Block middleBlock = world.getBlockState(middle).getBlock();
            if (!(middleBlock instanceof PistonHeadBlock)
                    && !middleBlock.equals(Blocks.MOVING_PISTON)) {
                world.setBlockState(middle, Blocks.AIR.getDefaultState());
            }
            world.setBlockState(indicatorPos, offBlock.getDefaultState());
        }
    }

    /**
     * Writes the piston only when none is there; rewriting an extended one would orphan its
     * head in the middle slot.
     */
    public static void ensurePiston(ServerWorld world, BlockPos pistonPos, BlockState pistonState) {
        if (!(world.getBlockState(pistonPos).getBlock() instanceof PistonBlock)) {
            world.setBlockState(pistonPos, pistonState);
        }
    }

    /**
     * Reconciles each seat's vote-indicator stack with the new occupancy. The stack is
     * piston (bottom) -> middle slot -> indicator (top).
     *
     * - Seat transitioned <em>unassigned → assigned</em>: clear the cage's middle slot
     *   and refresh the indicator. The piston is left alone, since during normal play of a
     *   seated player nothing destroys it.
     * - Seat transitioned <em>assigned → unassigned</em> (only when {@code currentNight >= 1}):
     *   place the cage and rewrite the piston and its power block. An extended piston could
     *   have been destroyed by extending into the previously-occupied middle slot, so we
     *   always rewrite it on this transition.
     * - Seat unchanged: nothing.
     *
     * For the night-0 → night-1 boot, pass every configured seat number as
     * {@code previouslySeatedSeats}, and the diff then treats every currently-empty seat
     * as a fresh assigned→unassigned transition and the cage gets placed.
     */
    public static void syncUnseatedVoteIndicators(MinecraftServer server, Set<Integer> previouslySeatedSeats) {
        ServerWorld world = server.getOverworld();
        Set<Integer> nowSeatedSeats = new HashSet<>(ServerState.PLAYER_SEAT_NUMBERS.values());
        boolean cagePhase = ServerState.currentNight >= 1;

        Block unseatedBlock = VotingManager.getBlockFromString(ServerConfig.VOTE_INDICATOR_BLOCK_UNSEATED);
        BlockState pistonState = Blocks.STICKY_PISTON.getDefaultState()
                .with(PistonBlock.FACING, Direction.UP);

        Map<Integer, UUID> seatToPlayer = new HashMap<>();
        for (Map.Entry<UUID, Integer> entry : ServerState.PLAYER_SEAT_NUMBERS.entrySet()) {
            seatToPlayer.put(entry.getValue(), entry.getKey());
        }

        for (Map.Entry<Integer, BlockPos> entry : ServerConfig.SEAT_VOTE_INDICATOR_POSITIONS.entrySet()) {
            int seatNumber = entry.getKey();
            BlockPos indicatorPos = entry.getValue();
            BlockPos belowIndicator = indicatorPos.down();
            BlockPos pistonPos = belowIndicator.down();
            boolean wasSeated = previouslySeatedSeats.contains(seatNumber);
            boolean isNowSeated = nowSeatedSeats.contains(seatNumber);

            if (isNowSeated && !wasSeated) {
                // Player just took this seat, so clear the cage middle (only if the bedrock
                // cage block is actually there) and refresh indicator. Writing AIR
                // unconditionally would destroy an extended piston head occupying the
                // middle slot, which breaks the piston body below.
                if (world.getBlockState(belowIndicator).getBlock().equals(unseatedBlock)) {
                    world.setBlockState(belowIndicator, Blocks.AIR.getDefaultState());
                }
                UUID playerUuid = seatToPlayer.get(seatNumber);
                if (playerUuid != null) {
                    VotingManager.updatePlayerDeathIndicator(server, playerUuid);
                }
            } else if (!isNowSeated && wasSeated && cagePhase) {
                // Player just left this seat, so rebuild the cage. The piston is written first
                // and retracted: that lets an extended head in the middle slot vanish on its own,
                // whereas overwriting the head with the cage would break the base and drop it.
                // Cage before power block, so the piston jams instead of extending into it.
                world.setBlockState(pistonPos, pistonState);
                world.setBlockState(belowIndicator, unseatedBlock.getDefaultState());
                world.setBlockState(pistonPos.down(), Blocks.REDSTONE_BLOCK.getDefaultState());
                world.setBlockState(indicatorPos, Blocks.AIR.getDefaultState());
            }
        }
    }

    /**
     * Restores vote indicators for all unseated seats back to normal state.
     * Called during game reset.
     */
    public static void restoreUnseatedVoteIndicators(MinecraftServer server) {
        ServerWorld world = server.getOverworld();

        // Get all seated player seat numbers
        Set<Integer> seatedSeatNumbers = new HashSet<>(ServerState.PLAYER_SEAT_NUMBERS.values());

        // Loop through all configured vote indicator positions
        for (Map.Entry<Integer, BlockPos> entry : ServerConfig.SEAT_VOTE_INDICATOR_POSITIONS.entrySet()) {
            int seatNumber = entry.getKey();
            BlockPos indicatorPos = entry.getValue();

            // Skip if this seat has a player assigned
            if (seatedSeatNumbers.contains(seatNumber)) {
                continue;
            }

            // Check if ghost used block is present below indicator
            BlockPos belowIndicator = indicatorPos.down();
            BlockState blockState = world.getBlockState(belowIndicator);
            Block ghostUsedBlock = VotingManager.getBlockFromString(ServerConfig.VOTE_INDICATOR_BLOCK_GHOST_USED);

            if (blockState.getBlock().equals(ghostUsedBlock)) {
                // Remove ghost used block
                world.setBlockState(belowIndicator, Blocks.AIR.getDefaultState());

                // Restore vote indicator block to default off state
                Block offBlock = VotingManager.getBlockFromString(ServerConfig.VOTE_INDICATOR_BLOCK_OFF);
                world.setBlockState(indicatorPos, offBlock.getDefaultState());
            }
        }
    }

}
