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

    /** Repaints a player's indicator for their seat. Never removes a piston. */
    public static void reconcileVoteIndicator(MinecraftServer server, UUID playerUuid, int seat, boolean isDead) {
        boolean ghostVoteUsed = DaytimeState.hasUsedGhostVote(playerUuid);
        boolean ghostVoteSecretlyUsed = DaytimeState.hasSecretlyUsedGhostVote(playerUuid);
        boolean isBansheeAbility = DaytimeState.hasBansheeDoubleVote(playerUuid);

        if (isDead && !isBansheeAbility && ghostVoteUsed && !ghostVoteSecretlyUsed) {
            VotingManager.setUsedGhostVoteIndicator(server, seat);
        } else {
            // A secretly used ghost vote (Organ Grinder) must not show the ghost-used block
            VotingManager.updatePlayerDeathIndicator(server, playerUuid);
        }
    }

    /**
     * Paints each seated seat's stack: piston if missing, cage cleared, indicator for the
     * player's state. With {@code includeEmptySeats} every empty seat is painted fresh too
     * (map setup and the game resets). Run before the pistons are powered.
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
            // Clear the middle so the piston can extend, but never knock out an extended head
            Block middleBlock = world.getBlockState(middle).getBlock();
            if (!(middleBlock instanceof PistonHeadBlock)
                    && !middleBlock.equals(Blocks.MOVING_PISTON)) {
                world.setBlockState(middle, Blocks.AIR.getDefaultState());
            }
            world.setBlockState(indicatorPos, offBlock.getDefaultState());
        }
    }

    /** Writes the piston only when missing; rewriting an extended one orphans its head. */
    public static void ensurePiston(ServerWorld world, BlockPos pistonPos, BlockState pistonState) {
        if (!(world.getBlockState(pistonPos).getBlock() instanceof PistonBlock)) {
            world.setBlockState(pistonPos, pistonState);
        }
    }

    /**
     * Updates seats whose occupancy changed: a newly taken seat loses its cage, a newly
     * vacated seat gets caged (from night 1). Pass every seat as {@code previouslySeatedSeats}
     * at the night-1 boot so all empty seats get caged.
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
                // Only clear an actual cage block; clearing an extended piston head breaks the piston
                if (world.getBlockState(belowIndicator).getBlock().equals(unseatedBlock)) {
                    world.setBlockState(belowIndicator, Blocks.AIR.getDefaultState());
                }
                UUID playerUuid = seatToPlayer.get(seatNumber);
                if (playerUuid != null) {
                    VotingManager.updatePlayerDeathIndicator(server, playerUuid);
                }
            } else if (!isNowSeated && wasSeated && cagePhase) {
                // Piston first so an extended head retracts rather than being overwritten,
                // cage before power so the piston jams instead of extending into it
                world.setBlockState(pistonPos, pistonState);
                world.setBlockState(belowIndicator, unseatedBlock.getDefaultState());
                world.setBlockState(pistonPos.down(), Blocks.REDSTONE_BLOCK.getDefaultState());
                world.setBlockState(indicatorPos, Blocks.AIR.getDefaultState());
            }
        }
    }

    /** Clears a leftover ghost-used block from every empty seat and paints it off. */
    public static void restoreUnseatedVoteIndicators(MinecraftServer server) {
        ServerWorld world = server.getOverworld();
        Set<Integer> seatedSeatNumbers = new HashSet<>(ServerState.PLAYER_SEAT_NUMBERS.values());
        Block ghostUsedBlock = VotingManager.getBlockFromString(ServerConfig.VOTE_INDICATOR_BLOCK_GHOST_USED);
        Block offBlock = VotingManager.getBlockFromString(ServerConfig.VOTE_INDICATOR_BLOCK_OFF);

        for (Map.Entry<Integer, BlockPos> entry : ServerConfig.SEAT_VOTE_INDICATOR_POSITIONS.entrySet()) {
            if (seatedSeatNumbers.contains(entry.getKey())) continue;
            BlockPos indicatorPos = entry.getValue();
            BlockPos belowIndicator = indicatorPos.down();
            if (world.getBlockState(belowIndicator).getBlock().equals(ghostUsedBlock)) {
                world.setBlockState(belowIndicator, Blocks.AIR.getDefaultState());
                world.setBlockState(indicatorPos, offBlock.getDefaultState());
            }
        }
    }

}
