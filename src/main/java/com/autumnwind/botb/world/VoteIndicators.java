package com.autumnwind.botb.world;

import com.autumnwind.botb.config.ServerConfig;
import com.autumnwind.botb.daytime.*;
import com.autumnwind.botb.networking.*;
import com.autumnwind.botb.states.ServerState;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.piston.PistonHeadBlock;
import net.minecraft.world.level.block.state.BlockState;

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
        ServerLevel world = server.overworld();
        Block unseatedBlock = VotingManager.getBlockFromString(ServerConfig.VOTE_INDICATOR_BLOCK_UNSEATED);
        BlockState pistonState = Blocks.STICKY_PISTON.defaultBlockState()
                .setValue(PistonBaseBlock.FACING, Direction.UP);

        for (Map.Entry<UUID, Integer> entry : ServerState.PLAYER_SEAT_NUMBERS.entrySet()) {
            BlockPos indicatorPos = ServerConfig.SEAT_VOTE_INDICATOR_POSITIONS.get(entry.getValue());
            if (indicatorPos == null) continue;
            BlockPos middle = indicatorPos.below();

            ensurePiston(world, middle.below(), pistonState);
            if (world.getBlockState(middle).getBlock().equals(unseatedBlock)) {
                world.setBlockAndUpdate(middle, Blocks.AIR.defaultBlockState());
            }
            VotingManager.updatePlayerDeathIndicator(server, entry.getKey());
        }

        if (!includeEmptySeats) return;

        Set<Integer> seatedSeats = new HashSet<>(ServerState.PLAYER_SEAT_NUMBERS.values());
        Block offBlock = VotingManager.getBlockFromString(ServerConfig.VOTE_INDICATOR_BLOCK_OFF);
        for (Map.Entry<Integer, BlockPos> entry : ServerConfig.SEAT_VOTE_INDICATOR_POSITIONS.entrySet()) {
            if (seatedSeats.contains(entry.getKey())) continue;
            BlockPos indicatorPos = entry.getValue();
            BlockPos middle = indicatorPos.below();

            ensurePiston(world, middle.below(), pistonState);
            // Clear the middle so the piston can extend, but never knock out an extended head
            Block middleBlock = world.getBlockState(middle).getBlock();
            if (!(middleBlock instanceof PistonHeadBlock)
                    && !middleBlock.equals(Blocks.MOVING_PISTON)) {
                world.setBlockAndUpdate(middle, Blocks.AIR.defaultBlockState());
            }
            world.setBlockAndUpdate(indicatorPos, offBlock.defaultBlockState());
        }
    }

    /** Writes the piston only when missing; rewriting an extended one orphans its head. */
    public static void ensurePiston(ServerLevel world, BlockPos pistonPos, BlockState pistonState) {
        if (!(world.getBlockState(pistonPos).getBlock() instanceof PistonBaseBlock)) {
            world.setBlockAndUpdate(pistonPos, pistonState);
        }
    }

    /**
     * Updates seats whose occupancy changed: a newly taken seat loses its cage, a newly
     * vacated seat gets caged (from night 1). Pass every seat as {@code previouslySeatedSeats}
     * at the night-1 boot so all empty seats get caged.
     */
    public static void syncUnseatedVoteIndicators(MinecraftServer server, Set<Integer> previouslySeatedSeats) {
        ServerLevel world = server.overworld();
        Set<Integer> nowSeatedSeats = new HashSet<>(ServerState.PLAYER_SEAT_NUMBERS.values());
        boolean cagePhase = ServerState.currentNight >= 1;

        Block unseatedBlock = VotingManager.getBlockFromString(ServerConfig.VOTE_INDICATOR_BLOCK_UNSEATED);
        BlockState pistonState = Blocks.STICKY_PISTON.defaultBlockState()
                .setValue(PistonBaseBlock.FACING, Direction.UP);

        Map<Integer, UUID> seatToPlayer = new HashMap<>();
        for (Map.Entry<UUID, Integer> entry : ServerState.PLAYER_SEAT_NUMBERS.entrySet()) {
            seatToPlayer.put(entry.getValue(), entry.getKey());
        }

        for (Map.Entry<Integer, BlockPos> entry : ServerConfig.SEAT_VOTE_INDICATOR_POSITIONS.entrySet()) {
            int seatNumber = entry.getKey();
            BlockPos indicatorPos = entry.getValue();
            BlockPos belowIndicator = indicatorPos.below();
            BlockPos pistonPos = belowIndicator.below();
            boolean wasSeated = previouslySeatedSeats.contains(seatNumber);
            boolean isNowSeated = nowSeatedSeats.contains(seatNumber);

            if (isNowSeated && !wasSeated) {
                // Only clear an actual cage block; clearing an extended piston head breaks the piston
                if (world.getBlockState(belowIndicator).getBlock().equals(unseatedBlock)) {
                    world.setBlockAndUpdate(belowIndicator, Blocks.AIR.defaultBlockState());
                }
                UUID playerUuid = seatToPlayer.get(seatNumber);
                if (playerUuid != null) {
                    VotingManager.updatePlayerDeathIndicator(server, playerUuid);
                }
            } else if (!isNowSeated && wasSeated && cagePhase) {
                // Piston first so an extended head retracts rather than being overwritten,
                // cage before power so the piston jams instead of extending into it
                world.setBlockAndUpdate(pistonPos, pistonState);
                world.setBlockAndUpdate(belowIndicator, unseatedBlock.defaultBlockState());
                world.setBlockAndUpdate(pistonPos.below(), Blocks.REDSTONE_BLOCK.defaultBlockState());
                world.setBlockAndUpdate(indicatorPos, Blocks.AIR.defaultBlockState());
            }
        }
    }

    /** Clears a leftover ghost-used block from every empty seat and paints it off. */
    public static void restoreUnseatedVoteIndicators(MinecraftServer server) {
        ServerLevel world = server.overworld();
        Set<Integer> seatedSeatNumbers = new HashSet<>(ServerState.PLAYER_SEAT_NUMBERS.values());
        Block ghostUsedBlock = VotingManager.getBlockFromString(ServerConfig.VOTE_INDICATOR_BLOCK_GHOST_USED);
        Block offBlock = VotingManager.getBlockFromString(ServerConfig.VOTE_INDICATOR_BLOCK_OFF);

        for (Map.Entry<Integer, BlockPos> entry : ServerConfig.SEAT_VOTE_INDICATOR_POSITIONS.entrySet()) {
            if (seatedSeatNumbers.contains(entry.getKey())) continue;
            BlockPos indicatorPos = entry.getValue();
            BlockPos belowIndicator = indicatorPos.below();
            if (world.getBlockState(belowIndicator).getBlock().equals(ghostUsedBlock)) {
                world.setBlockAndUpdate(belowIndicator, Blocks.AIR.defaultBlockState());
                world.setBlockAndUpdate(indicatorPos, offBlock.defaultBlockState());
            }
        }
    }

}
