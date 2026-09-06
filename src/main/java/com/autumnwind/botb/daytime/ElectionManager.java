package com.autumnwind.botb.daytime;

import com.autumnwind.botb.config.ServerConfig;
import com.autumnwind.botb.networking.*;
import com.autumnwind.botb.states.ServerState;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LeverBlock;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

/**
 * Manages the shared election logic for both voting and exile support.
 * This class provides utility methods that are parameterized by ElectionConfig
 * to handle the differences between vote and exile support flows.
 */
public class ElectionManager {

    // Current election state
    private static ElectionConfig currentConfig = null;
    private static List<UUID> electionOrder = new ArrayList<>();
    private static int currentPlayerIndex = 0;
    private static long electionStartTime = 0;
    private static Timer electionTimer = null;
    private static Timer updateTimer = null;
    private static Timer resultTimer = null;

    // True from the start of scheduleCleanup until the cleanup timer finishes.
    // Used by VotingManager.onBlockUse to FAIL lever toggles during the window
    // where blocks/levers are being reset, since flips here would cause stray
    // indicator blocks (placed at the extended piston slot while the piston is
    // retracted, then pushed up when it extends, leaving them stuck visible).
    private static boolean cleanupInProgress = false;

    public static boolean isCleanupInProgress() {
        return cleanupInProgress;
    }

    // Callback interface for result handling (different for vote vs exile)
    @FunctionalInterface
    public interface ResultCallback {
        void onResult(MinecraftServer server, int voteCount, int threshold);
    }

    // Callback interface for cleanup after result (different for vote vs exile)
    @FunctionalInterface
    public interface CleanupCallback {
        void onCleanup(MinecraftServer server);
    }

    // Callback fired as the clock hand starts toward a voter; their vote commits
    // PISTON_POWER_DELAY_TICKS later, so voteValue is always false here
    @FunctionalInterface
    public interface VoteLockCallback {
        void onVoteLocked(MinecraftServer server, UUID voter, int seat, boolean voteValue);
    }

    // Callback interface for broadcasting state updates
    @FunctionalInterface
    public interface BroadcastCallback {
        void broadcast(MinecraftServer server);
    }

    // Current callbacks (set by VotingManager or ExileSupportManager)
    private static ResultCallback resultCallback = null;
    private static CleanupCallback cleanupCallback = null;
    private static VoteLockCallback voteLockCallback = null;
    private static BroadcastCallback broadcastCallback = null;
    private static Set<UUID> currentDeadPlayers = new HashSet<>();

    /**
     * Gets a block from a string identifier.
     */
    public static Block getBlockFromString(String blockName) {
        if (blockName == null || blockName.isEmpty()) {
            return null;
        }
        Identifier id = Identifier.tryParse(blockName);
        if (id == null) {
            return null;
        }
        return Registries.BLOCK.get(id);
    }

    /**
     * Builds the election order starting one seat after the target.
     * This is identical for both voting and exile support.
     *
     * @param targetUuid The target player's UUID (nominee or exile target)
     * @param seatNumbers Map of player UUIDs to seat numbers
     * @return Ordered list of player UUIDs in voting order
     */
    public static List<UUID> buildElectionOrder(UUID targetUuid, Map<UUID, Integer> seatNumbers) {
        List<UUID> order = new ArrayList<>();
        Integer targetSeat = seatNumbers.get(targetUuid);

        // Sort players by seat number
        List<Map.Entry<UUID, Integer>> sortedSeats = new ArrayList<>(seatNumbers.entrySet());
        sortedSeats.sort(Map.Entry.comparingByValue());

        // Find the starting position (one seat higher than target)
        int startIndex = 0;
        if (targetSeat != null) {
            for (int i = 0; i < sortedSeats.size(); i++) {
                if (sortedSeats.get(i).getValue().equals(targetSeat)) {
                    startIndex = (i + 1) % sortedSeats.size();
                    break;
                }
            }
        }

        // Build order starting from startIndex
        for (int i = 0; i < sortedSeats.size(); i++) {
            int index = (startIndex + i) % sortedSeats.size();
            order.add(sortedSeats.get(index).getKey());
        }

        return order;
    }

    /**
     * Teleports all players in the election order to their town square seats.
     *
     * @param server The server instance
     * @param order The election order
     */
    public static void freezePlayersInSeats(MinecraftServer server, List<UUID> order) {
        for (UUID playerUuid : order) {
            Integer seat = ServerState.PLAYER_SEAT_NUMBERS.get(playerUuid);
            if (seat == null) continue;

            BlockPos seatPos = ServerConfig.TOWN_SQUARE_SEATS.get(seat);
            if (seatPos == null) continue;

            ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerUuid);
            if (player != null) {
                player.teleport(
                        server.getOverworld(),
                        seatPos.getX() + 0.5,
                        seatPos.getY(),
                        seatPos.getZ() + 0.5,
                        player.getYaw(),
                        player.getPitch()
                );
            }
        }
    }

    /**
     * Keeps players frozen in their town square seats.
     * Called every 50ms during the election.
     *
     * @param server The server instance
     * @param order The election order
     */
    public static void keepPlayersFrozen(MinecraftServer server, List<UUID> order) {
        for (UUID playerUuid : order) {
            Integer seat = ServerState.PLAYER_SEAT_NUMBERS.get(playerUuid);
            if (seat == null) continue;

            BlockPos seatPos = ServerConfig.TOWN_SQUARE_SEATS.get(seat);
            if (seatPos == null) continue;

            ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerUuid);
            if (player != null) {
                double targetX = seatPos.getX() + 0.5;
                double targetZ = seatPos.getZ() + 0.5;
                double currentX = player.getX();
                double currentZ = player.getZ();

                // If player has moved too far, teleport them back
                if (Math.abs(currentX - targetX) > 0.5 || Math.abs(currentZ - targetZ) > 0.5) {
                    player.teleport(
                            server.getOverworld(),
                            targetX,
                            seatPos.getY(),
                            targetZ,
                            player.getYaw(),
                            player.getPitch()
                    );
                }
            }
        }
    }

    /**
     * Reads the lever state for a player's seat.
     *
     * @param server The server instance
     * @param playerUuid The player's UUID
     * @return The lever state (true = on, false = off), or null if not found
     */
    public static Boolean readLeverState(MinecraftServer server, UUID playerUuid) {
        Integer seat = ServerState.PLAYER_SEAT_NUMBERS.get(playerUuid);
        if (seat == null) return null;

        BlockPos switchPos = ServerConfig.SEAT_SWITCH_POSITIONS.get(seat);
        if (switchPos == null) return null;

        World world = server.getOverworld();
        BlockState state = world.getBlockState(switchPos);
        return state.getBlock() instanceof LeverBlock && state.get(LeverBlock.POWERED);
    }

    /**
     * Reads all lever states for players in the election order.
     *
     * @param server The server instance
     * @param order The election order
     * @return Map of player UUID to lever state
     */
    public static Map<UUID, Boolean> readAllLeverStates(MinecraftServer server, List<UUID> order) {
        Map<UUID, Boolean> leverStates = new HashMap<>();
        World world = server.getOverworld();

        for (UUID player : order) {
            Integer seat = ServerState.PLAYER_SEAT_NUMBERS.get(player);
            if (seat == null) continue;

            BlockPos switchPos = ServerConfig.SEAT_SWITCH_POSITIONS.get(seat);
            if (switchPos == null) continue;

            BlockState state = world.getBlockState(switchPos);
            boolean isOn = state.getBlock() instanceof LeverBlock && state.get(LeverBlock.POWERED);
            leverStates.put(player, isOn);
        }

        return leverStates;
    }

    /**
     * Updates an indicator block for a player's seat.
     *
     * @param server The server instance
     * @param seat The seat number
     * @param voteOn Whether the vote is on or off
     * @param config The election config (determines which blocks to use)
     * @param isDead Whether the player is dead (for ghost vote indicators)
     * @param isDoubleVote Whether this is a Banshee double vote
     */
    public static void updateIndicator(MinecraftServer server, int seat, boolean voteOn,
                                        ElectionConfig config, boolean isDead, boolean isDoubleVote) {
        BlockPos indicatorPos = ServerConfig.SEAT_VOTE_INDICATOR_POSITIONS.get(seat);
        if (indicatorPos == null) return;

        World world = server.getOverworld();
        Block block;

        if (isDead && config.getIndicatorBlockGhostOn() != null) {
            // Dead player with ghost vote
            block = voteOn
                    ? getBlockFromString(config.getIndicatorBlockGhostOn())
                    : getBlockFromString(config.getIndicatorBlockGhostOff());
        } else if (voteOn && isDoubleVote && config.getIndicatorBlockDouble() != null) {
            // Banshee double vote
            block = getBlockFromString(config.getIndicatorBlockDouble());
        } else {
            // Normal vote
            block = voteOn
                    ? getBlockFromString(config.getIndicatorBlockOn())
                    : getBlockFromString(config.getIndicatorBlockOff());
        }

        if (block != null) {
            world.setBlockState(indicatorPos, block.getDefaultState());

            // Clear a stray block from the slot below: under heavy lag the repaint can
            // land before the piston has pushed the sunken indicator back up, and the
            // piston would then shove both blocks up, exposing the stale vote state.
            BlockPos below = indicatorPos.down();
            BlockState belowState = world.getBlockState(below);
            if (!belowState.isAir()
                    && !belowState.isOf(Blocks.PISTON_HEAD)
                    && !belowState.isOf(Blocks.MOVING_PISTON)) {
                Block ghostUsedBlock = getBlockFromString(ServerConfig.VOTE_INDICATOR_BLOCK_GHOST_USED);
                Block unseatedBlock = getBlockFromString(ServerConfig.VOTE_INDICATOR_BLOCK_UNSEATED);
                if ((ghostUsedBlock == null || !belowState.isOf(ghostUsedBlock))
                        && (unseatedBlock == null || !belowState.isOf(unseatedBlock))) {
                    world.setBlockState(below, Blocks.AIR.getDefaultState());
                }
            }
        }
    }

    // 1 redstone tick (2 game ticks): the gap between a voter's clock-hand announce and
    // their vote committing (lever read + piston sink), and the re-extension delay after a vote
    public static final int PISTON_POWER_DELAY_TICKS = 2;

    // Tick-delayed actions, decremented and run on START_SERVER_TICK (server thread only)
    private static final List<TickDelayedAction> pendingTickActions = new ArrayList<>();

    private static final class TickDelayedAction {
        int ticksLeft;
        final Runnable action;

        TickDelayedAction(int ticksLeft, Runnable action) {
            this.ticksLeft = ticksLeft;
            this.action = action;
        }
    }

    /**
     * Registers the server-tick scheduler backing tick-delayed piston power changes.
     * Called once from mod init.
     */
    public static void registerTickScheduler() {
        ServerTickEvents.START_SERVER_TICK.register(server -> {
            if (pendingTickActions.isEmpty()) return;
            List<Runnable> due = new ArrayList<>();
            Iterator<TickDelayedAction> it = pendingTickActions.iterator();
            while (it.hasNext()) {
                TickDelayedAction pending = it.next();
                if (--pending.ticksLeft <= 0) {
                    it.remove();
                    due.add(pending.action);
                }
            }
            due.forEach(Runnable::run);
        });
        // A pending action must not leak into the next loaded world (singleplayer world switch)
        ServerLifecycleEvents.SERVER_STOPPING.register(
                server -> pendingTickActions.clear());
    }

    private static void runAfterTicks(int ticks, Runnable action) {
        pendingTickActions.add(new TickDelayedAction(ticks, action));
    }

    /**
     * Position of the mod-managed redstone block that holds a seat's vote piston
     * extended: directly below the piston, 3 below the vote indicator.
     */
    public static BlockPos getPistonPowerPos(int seat) {
        BlockPos indicatorPos = ServerConfig.SEAT_VOTE_INDICATOR_POSITIONS.get(seat);
        return indicatorPos == null ? null : indicatorPos.down(3);
    }

    /**
     * Places or removes the redstone block powering a seat's vote piston.
     * Powered = piston extended (indicator up); unpowered = retracted (indicator sunk).
     */
    public static void setPistonPowerBlock(MinecraftServer server, int seat, boolean powered) {
        BlockPos powerPos = getPistonPowerPos(seat);
        if (powerPos == null) return;
        server.getOverworld().setBlockState(powerPos,
                (powered ? Blocks.REDSTONE_BLOCK : Blocks.AIR).getDefaultState());
    }

    /**
     * Powers every configured seat's vote piston. Run at game start and reset so the
     * pistons are held extended.
     */
    public static void powerAllSeatPistons(MinecraftServer server) {
        for (Integer seat : ServerConfig.SEAT_VOTE_INDICATOR_POSITIONS.keySet()) {
            setPistonPowerBlock(server, seat, true);
        }
    }

    /**
     * Restores piston power for every seat in the order, PISTON_POWER_DELAY_TICKS from
     * now. Ghost-used blocks must already be in place when this is called: a jammed
     * re-extension is what freezes a spent ghost vote's indicator in the sunken position.
     */
    public static void restoreAllPistonPower(MinecraftServer server, List<UUID> order) {
        List<Integer> seats = new ArrayList<>();
        for (UUID playerUuid : order) {
            Integer seat = ServerState.PLAYER_SEAT_NUMBERS.get(playerUuid);
            if (seat != null) seats.add(seat);
        }
        runAfterTicks(PISTON_POWER_DELAY_TICKS, () -> {
            for (int seat : seats) {
                setPistonPowerBlock(server, seat, true);
            }
        });
    }

    /**
     * Resets all levers to the off position.
     *
     * @param server The server instance
     * @param order The election order
     */
    public static void resetAllLevers(MinecraftServer server, List<UUID> order) {
        World world = server.getOverworld();
        for (UUID playerUuid : order) {
            Integer seat = ServerState.PLAYER_SEAT_NUMBERS.get(playerUuid);
            if (seat == null) continue;

            BlockPos switchPos = ServerConfig.SEAT_SWITCH_POSITIONS.get(seat);
            if (switchPos == null) continue;

            BlockState currentState = world.getBlockState(switchPos);
            if (currentState.getBlock() instanceof LeverBlock) {
                if (currentState.get(LeverBlock.POWERED)) {
                    world.setBlockState(switchPos, currentState.with(LeverBlock.POWERED, false));
                }
            }
        }
    }

    /**
     * Calculates the vote threshold based on the election config.
     *
     * @param aliveCount Number of alive players
     * @param totalCount Total number of seated players
     * @param config The election config
     * @return The required vote count
     */
    public static int calculateThreshold(int aliveCount, int totalCount, ElectionConfig config) {
        int divisor = config.useAliveForThreshold() ? aliveCount : totalCount;
        return (int) Math.ceil(divisor / 2.0);
    }

    /**
     * Sends election start sounds to all players.
     *
     * @param server The server instance
     */
    public static void sendStartSounds(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            ServerPlayNetworking.send(player, new PlaySoundS2CPayload(PlaySoundS2CPayload.VOTE_START));
            ServerPlayNetworking.send(player, new PlaySoundS2CPayload(PlaySoundS2CPayload.VOTE_MUSIC));
        }
    }

    /**
     * Sends clock ticking sound to all players.
     *
     * @param server The server instance
     */
    public static void sendClockTickingSound(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            ServerPlayNetworking.send(player, new PlaySoundS2CPayload(PlaySoundS2CPayload.CLOCK_TICKING));
        }
    }

    /**
     * Sends stop sounds to all players.
     *
     * @param server The server instance
     */
    public static void sendStopSounds(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            ServerPlayNetworking.send(player, new PlaySoundS2CPayload(PlaySoundS2CPayload.VOTE_MUSIC_STOP));
            ServerPlayNetworking.send(player, new PlaySoundS2CPayload(PlaySoundS2CPayload.CLOCK_TICKING_STOP));
        }
    }

    /**
     * Cancels all election timers.
     */
    public static void cancelAllTimers() {
        if (electionTimer != null) {
            electionTimer.cancel();
            electionTimer = null;
        }
        if (updateTimer != null) {
            updateTimer.cancel();
            updateTimer = null;
        }
        if (resultTimer != null) {
            resultTimer.cancel();
            resultTimer = null;
        }
    }

    /**
     * Gets the current election order.
     */
    public static List<UUID> getElectionOrder() {
        return electionOrder;
    }

    /**
     * Gets the election start time.
     */
    public static long getElectionStartTime() {
        return electionStartTime;
    }

    // ========== Unified Voting Phase Methods ==========

    /**
     * Runs the voting phase of an election.
     * This is the unified entry point for both regular votes and exile support.
     *
     * <p>The voting phase consists of:</p>
     * <ol>
     *   <li>Set up ElectionState for voting phase</li>
     *   <li>Read initial lever states</li>
     *   <li>Freeze players in seats</li>
     *   <li>Send start sounds</li>
     *   <li>Start update timer (50ms) for freezing and broadcast</li>
     *   <li>Start lock timer (3s delay, then per-player delays)</li>
     *   <li>Lock votes one by one</li>
     *   <li>Calculate and display result</li>
     *   <li>Cleanup</li>
     * </ol>
     *
     * @param server The server instance
     * @param config The election configuration
     * @param deadPlayers Set of dead player UUIDs
     * @param onVoteLock Callback for each vote lock (for clock hands, indicator updates)
     * @param onBroadcast Callback for broadcasting state updates (50ms intervals)
     * @param onResult Callback for result handling (type-specific MFE/exile logic)
     * @param onCleanup Callback for cleanup (type-specific cleanup logic)
     */
    public static void runVotingPhase(MinecraftServer server, ElectionConfig config,
                                       Set<UUID> deadPlayers,
                                       VoteLockCallback onVoteLock,
                                       BroadcastCallback onBroadcast,
                                       ResultCallback onResult,
                                       CleanupCallback onCleanup) {
        // Store config and callbacks
        currentConfig = config;
        currentDeadPlayers = new HashSet<>(deadPlayers);
        voteLockCallback = onVoteLock;
        broadcastCallback = onBroadcast;
        resultCallback = onResult;
        cleanupCallback = onCleanup;

        // Get election order from ElectionState
        electionOrder = ElectionState.getElectionOrder();
        currentPlayerIndex = 0;
        electionStartTime = System.currentTimeMillis();

        // Begin voting phase in ElectionState
        ElectionState.beginVotingPhase();

        // Read initial lever states
        Map<UUID, Boolean> initialStates = readAllLeverStates(server, electionOrder);
        ElectionState.syncLeverStatesFromWorld(initialStates);

        // DaytimeState holds the same lever states for the callers that read them from there.
        for (Map.Entry<UUID, Boolean> entry : initialStates.entrySet()) {
            DaytimeState.setLeverState(entry.getKey(), entry.getValue());
            if (config.getType() == ElectionType.VOTE) {
                DaytimeState.setCurrentVote(entry.getKey(), entry.getValue());
            } else {
                DaytimeState.setExileSupportVote(entry.getKey(), entry.getValue());
            }
        }

        // Freeze players in seats
        freezePlayersInSeats(server, electionOrder);

        // Send start sounds
        sendStartSounds(server);

        // Initial broadcast
        if (broadcastCallback != null) {
            broadcastCallback.broadcast(server);
        }

        // Start periodic update timer (50ms)
        updateTimer = new Timer();
        updateTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                server.execute(() -> {
                    // Keep players frozen
                    keepPlayersFrozen(server, electionOrder);
                    // Broadcast state updates
                    if (broadcastCallback != null) {
                        broadcastCallback.broadcast(server);
                    }
                });
            }
        }, 50, 50);

        // Start lock timer (3 second initial delay)
        electionTimer = new Timer();
        electionTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                server.execute(() -> {
                    // Start clock ticking sound
                    sendClockTickingSound(server);
                    // Start locking votes
                    lockNextVote(server);
                });
            }
        }, 3000);
    }

    /**
     * Announces the next voter (clock hand starts toward them), then commits their
     * vote lock PISTON_POWER_DELAY_TICKS later, so the lever read, state lock, and
     * piston sink all happen together while the hand is mid-swing - the same
     * choreography the old torch wiring produced.
     */
    private static void lockNextVote(MinecraftServer server) {
        if (currentPlayerIndex >= electionOrder.size()) {
            // All votes locked, calculate result
            calculateResult(server);
            return;
        }

        UUID voter = electionOrder.get(currentPlayerIndex);
        Integer seat = ServerState.PLAYER_SEAT_NUMBERS.get(voter);

        // Check if this voter should be skipped (vote-specific logic)
        boolean shouldSkip = shouldSkipVoter(voter);
        boolean isLastVoter = currentPlayerIndex == electionOrder.size() - 1;

        // Announce: swing the clock hand toward this voter (callbacks ignore the
        // vote value, which doesn't exist until the commit below)
        if (seat != null && voteLockCallback != null) {
            voteLockCallback.onVoteLocked(server, voter, seat, false);
        }

        // Commit the lock 1 redstone tick into the hand's swing
        runAfterTicks(PISTON_POWER_DELAY_TICKS, () -> {
            if (!ElectionState.isVotingPhaseActive()) return; // Vote was reset
            commitVoteLock(server, voter, seat, shouldSkip);
            if (isLastVoter) {
                calculateResult(server);
            }
        });

        currentPlayerIndex++;

        // Schedule next vote lock
        if (currentPlayerIndex < electionOrder.size()) {
            int delay = ServerConfig.VOTE_TIME_PER_PLAYER;
            electionTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    server.execute(() -> lockNextVote(server));
                }
            }, delay);
        }
    }

    /**
     * Locks a voter's vote: reads their lever, stores the lock, de-powers their
     * piston, and paints their indicator.
     */
    private static void commitVoteLock(MinecraftServer server, UUID voter, Integer seat, boolean shouldSkip) {
        if (seat != null && !shouldSkip) {
            // Read final lever state
            Boolean leverState = readLeverState(server, voter);
            boolean isOn = leverState != null && leverState;

            // Lock the vote in ElectionState
            ElectionState.lockVote(voter, isOn);
            ElectionState.setLeverState(voter, isOn);

            // Also update DaytimeState (for backward compatibility)
            if (currentConfig.getType() == ElectionType.VOTE) {
                DaytimeState.lockVote(voter, isOn);
                DaytimeState.setLeverState(voter, isOn);
            } else {
                DaytimeState.lockExileSupportVote(voter, isOn);
            }

            // De-power the piston so the indicator sinks to lock in
            setPistonPowerBlock(server, seat, false);

            // Update indicator
            boolean isDead = currentDeadPlayers.contains(voter);
            boolean isBanshee = currentConfig.applyBansheeMultiplier() && DaytimeState.hasBansheeDoubleVote(voter);
            // For indicator: only treat as dead if NOT banshee.
            // In Voudon mode dead players vote like alive ones, so they get the
            // normal on/off blocks rather than the ghost variants.
            boolean treatAsDead = isDead && !isBanshee && !DaytimeState.isVoudonModeActive();
            boolean isDoubleVote = currentConfig.applyBansheeMultiplier() && DaytimeState.isBansheeDoubleVoteActive(voter);
            updateIndicator(server, seat, isOn, currentConfig, treatAsDead, isDoubleVote);

            // Broadcast update
            if (broadcastCallback != null) {
                broadcastCallback.broadcast(server);
            }
        } else if (shouldSkip && seat != null) {
            // Dead player with used ghost vote - clock hand still moved to them, but vote not counted
            // Mark as locked with false vote (for tracking purposes)
            if (currentConfig.getType() == ElectionType.VOTE) {
                DaytimeState.lockVote(voter, false);
            }
            ElectionState.lockVote(voter, false);

            if (broadcastCallback != null) {
                broadcastCallback.broadcast(server);
            }
        }
    }

    /**
     * Determines if a voter should be skipped.
     * For votes: skip dead players who have used their ghost vote (except Banshee).
     * For exile support: never skip (all can vote freely).
     */
    private static boolean shouldSkipVoter(UUID voter) {
        if (currentConfig == null || !currentConfig.skipUsedGhostVotes()) {
            return false; // Exile support - don't skip anyone
        }

        // Vote mode - check ghost vote status
        boolean isDead = currentDeadPlayers.contains(voter);
        boolean isBanshee = DaytimeState.hasBansheeDoubleVote(voter);
        boolean hasUsedGhost = DaytimeState.hasUsedGhostVote(voter);

        // Voudon mode reverses eligibility: alive non-Voudon players have no vote at
        // all, while dead players vote freely (used ghost votes don't matter)
        if (DaytimeState.isVoudonModeActive()) {
            boolean isVoudon = voter.equals(DaytimeState.getVoudonPlayerUuid());
            return !isDead && !isVoudon;
        }

        // Skip if dead (not banshee) and has used ghost vote
        return isDead && !isBanshee && hasUsedGhost;
    }

    /**
     * Calculates the result of the election.
     * Calls the result callback for type-specific handling.
     */
    private static void calculateResult(MinecraftServer server) {
        // Cancel election timer (keep update timer for OG mode)
        if (electionTimer != null) {
            electionTimer.cancel();
            electionTimer = null;
        }

        // For non-OG mode, cancel update timer now
        boolean isOGMode = currentConfig != null && currentConfig.applyOrganGrinderMode()
                && DaytimeState.isOrganGrinderMode();
        if (!isOGMode && updateTimer != null) {
            updateTimer.cancel();
            updateTimer = null;
        }

        // Count votes
        int voteCount = ElectionState.getLockedVoteCount();

        // Calculate threshold
        int aliveCount = (int) electionOrder.stream()
                .filter(p -> !currentDeadPlayers.contains(p))
                .count();
        int totalCount = electionOrder.size();
        int threshold = calculateThreshold(aliveCount, totalCount, currentConfig);

        // Call result callback after 1 second delay
        final int finalVoteCount = voteCount;
        final int finalThreshold = threshold;

        resultTimer = new Timer();
        resultTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                server.execute(() -> {
                    // Stop sounds
                    sendStopSounds(server);

                    // Call result callback (type-specific handling)
                    if (resultCallback != null) {
                        resultCallback.onResult(server, finalVoteCount, finalThreshold);
                    }

                    // Schedule cleanup
                    scheduleCleanup(server);
                });
            }
        }, 1000);
    }

    /**
     * Schedules cleanup after result display.
     */
    private static void scheduleCleanup(MinecraftServer server) {
        cleanupInProgress = true;

        // Place ghost used blocks for voting elections, BEFORE piston power returns so the
        // obsidian is already jamming the piston when it tries to re-extend.
        // Only for vote elections, not exile support. Only if not OG mode.
        // Skipped in Voudon mode, where dead players' votes don't consume ghost votes.
        if (currentConfig != null && currentConfig.getType() == ElectionType.VOTE &&
                !(currentConfig.applyOrganGrinderMode() && DaytimeState.isOrganGrinderMode()) &&
                !DaytimeState.isVoudonModeActive()) {
            for (UUID voter : electionOrder) {
                boolean isBanshee = currentConfig.applyBansheeMultiplier() && DaytimeState.hasBansheeDoubleVote(voter);
                if (currentDeadPlayers.contains(voter) && !isBanshee && DaytimeState.hasUsedGhostVote(voter)) {
                    Integer seat = ServerState.PLAYER_SEAT_NUMBERS.get(voter);
                    if (seat != null) {
                        VotingManager.setUsedGhostVoteIndicator(server, seat);
                    }
                }
            }
        }

        // Re-power the pistons (delayed one redstone tick) so unlocked indicators rise back up
        restoreAllPistonPower(server, electionOrder);

        // Reset levers
        resetAllLevers(server, electionOrder);

        // Clear lever states in ElectionState
        for (UUID player : electionOrder) {
            ElectionState.setLeverState(player, false);
            DaytimeState.setLeverState(player, false);
        }

        // Wait 1 second for pistons to re-extend, then update indicators
        Timer cleanupTimer = new Timer();
        cleanupTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                server.execute(() -> {
                    // Re-read lever states and update indicators
                    boolean voudonMode = DaytimeState.isVoudonModeActive();
                    for (UUID player : electionOrder) {
                        Integer seat = ServerState.PLAYER_SEAT_NUMBERS.get(player);
                        if (seat == null) continue;

                        Boolean leverState = readLeverState(server, player);
                        boolean isOn = leverState != null && leverState;

                        ElectionState.setLeverState(player, isOn);
                        DaytimeState.setLeverState(player, isOn);

                        boolean isDead = currentDeadPlayers.contains(player);

                        // Voudon-blocked alive players have no vote at all: re-assert the
                        // empty indicator over the GHOST_USED block instead of a lever state
                        if (voudonMode && !isDead && !player.equals(DaytimeState.getVoudonPlayerUuid())) {
                            VotingManager.setUsedGhostVoteIndicator(server, seat, null);
                            continue;
                        }

                        // Update indicator (skip if ghost vote used in non-OG mode; in Voudon
                        // mode dead players vote freely, so their indicator is never frozen)
                        boolean isBanshee = currentConfig != null && currentConfig.applyBansheeMultiplier()
                                && DaytimeState.hasBansheeDoubleVote(player);
                        boolean hasUsedGhost = DaytimeState.hasUsedGhostVote(player);
                        boolean isOG = currentConfig != null && currentConfig.applyOrganGrinderMode()
                                && DaytimeState.isOrganGrinderMode();

                        if (isDead && !isBanshee && hasUsedGhost && !isOG && !voudonMode) {
                            continue; // Skip - indicator frozen
                        }

                        boolean isDoubleVote = currentConfig != null && currentConfig.applyBansheeMultiplier()
                                && DaytimeState.isBansheeDoubleVoteActive(player);
                        boolean treatAsDead = isDead && !isBanshee && !voudonMode;

                        updateIndicator(server, seat, isOn, currentConfig, treatAsDead, isDoubleVote);
                    }

                    // Cancel update timer if still running (OG mode)
                    if (updateTimer != null) {
                        updateTimer.cancel();
                        updateTimer = null;
                    }

                    // Call cleanup callback
                    if (cleanupCallback != null) {
                        cleanupCallback.onCleanup(server);
                    }

                    // End election in ElectionState
                    ElectionState.endElection();

                    cleanupInProgress = false;
                });
            }
        }, 1000);
    }

    /**
     * Cancels the current voting phase without processing results.
     * Called when resetting a vote/exile.
     */
    public static void cancelVotingPhase(MinecraftServer server) {
        cancelAllTimers();

        // Re-power the pistons so any sunken indicators rise back up
        if (!electionOrder.isEmpty()) {
            restoreAllPistonPower(server, electionOrder);
        }

        // Reset ElectionState
        ElectionState.resetVotingPhase();

        // Clear local state
        currentPlayerIndex = 0;
        electionStartTime = 0;
    }
}
