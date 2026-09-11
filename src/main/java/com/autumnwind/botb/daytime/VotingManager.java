package com.autumnwind.botb.daytime;

import com.autumnwind.botb.config.ServerConfig;
import com.autumnwind.botb.networking.*;
import com.autumnwind.botb.states.ServerState;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import java.util.*;
import com.autumnwind.botb.world.TeamManager;

/**
 * Manages the voting process including timers, lever reading, and result calculation.
 * Timer logic is now unified through ElectionManager.runVotingPhase().
 */
public class VotingManager {

    // Cached state for result calculation (set during vote, used in callbacks)
    private static Set<UUID> cachedDeadPlayers = new HashSet<>();
    private static boolean cachedOrganGrinderMode = false;
    private static boolean cachedVoudonMode = false;
    private static UUID cachedVoudonPlayer = null;

    /**
     * Set only for a Legion game, holding every player the storyteller considers evil. Supplied
     * per-vote by the storyteller, who is the source of truth for alignments.
     */
    private static Set<UUID> cachedEvilsForLegion = null;

    /**
     * Starts a vote, additionally told who is evil for Legion's execution rule.
     * @param evilsForLegion every player the storyteller considers evil, or null when no Legion
     *                       is in play
     */
    public static void startVote(MinecraftServer server, Set<UUID> deadPlayers, boolean organGrinderMode,
                                  boolean voudonMode, UUID voudonPlayer, Set<UUID> evilsForLegion) {
        if (!DaytimeState.hasActiveNomination()) {
            return; // No active nomination
        }

        UUID nominee = DaytimeState.getCurrentNominee();
        UUID nominator = DaytimeState.getCurrentNominator();

        // Cache for callbacks
        cachedDeadPlayers = new HashSet<>(deadPlayers);
        cachedOrganGrinderMode = organGrinderMode;
        cachedVoudonMode = voudonMode;
        cachedVoudonPlayer = voudonPlayer;
        cachedEvilsForLegion = evilsForLegion == null ? null : new HashSet<>(evilsForLegion);

        // Get config - ElectionState was already initialized by NominationManager.executeNomination()
        // Just update the OG mode flag
        ElectionConfig config = ElectionConfig.forVote(organGrinderMode);
        ElectionState.updateConfig(config);

        // DaytimeState mirrors the same state, and plenty of callers read vote flags from there
        // rather than from ElectionState, so both are kept in step.
        DaytimeState.startVote(organGrinderMode);

        // ===== Vote-specific pre-work (before unified timer logic) =====

        // Apply blindness to non-operator players in Organ Grinder mode
        if (organGrinderMode) {
            // Remove glowing from nominee immediately so players can't see their hand moving
            if (nominee != null) {
                ServerPlayer nomineePlayer = server.getPlayerList().getPlayer(nominee);
                if (nomineePlayer != null) {
                    nomineePlayer.removeEffect(MobEffects.GLOWING);
                }
            }

            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (!player.hasPermissions(2)) {
                    player.addEffect(new MobEffectInstance(
                            MobEffects.BLINDNESS,
                            Integer.MAX_VALUE, 0, false, false, false
                    ));
                    player.addEffect(new MobEffectInstance(
                            MobEffects.NIGHT_VISION,
                            Integer.MAX_VALUE, 0, false, false, false
                    ));
                }
            }
        }

        // Switch clock hands to VOTING mode
        ServerPlayer nomineePlayer = server.getPlayerList().getPlayer(nominee);
        Vec3 nomineePos = nomineePlayer != null ? nomineePlayer.position() : null;
        StateBroadcaster.broadcastClockHandsState(
                server,
                ClockHandsStateS2CPayload.MODE_VOTING,
                null,  // hour hand hidden
                nomineePos,
                false,  // no fade in (already visible from nomination)
                false   // no swivel
        );

        // ===== Run unified voting phase with vote-specific callbacks =====
        ElectionManager.runVotingPhase(
                server,
                config,
                deadPlayers,
                VotingManager::onVoteLocked,      // Per-vote callback (clock hands)
                VotingManager::broadcastVoteUpdate, // Broadcast callback (50ms)
                VotingManager::onVoteResult,      // Result callback (MFE, Legion, titles)
                VotingManager::onVoteCleanup      // Cleanup callback (OG effects, ghost blocks)
        );
    }

    /**
     * Callback: Called when each individual vote is locked.
     * Updates clock hands to point at the voter.
     * Note: Indicator update is handled by ElectionManager.lockNextVote() already.
     */
    private static void onVoteLocked(MinecraftServer server, UUID voter, int seat, boolean voteValue) {
        // Update clock hand to point at current voter
        ServerPlayer voterPlayer = server.getPlayerList().getPlayer(voter);
        if (voterPlayer != null) {
            StateBroadcaster.broadcastClockHandsState(
                    server,
                    ClockHandsStateS2CPayload.MODE_VOTING,
                    null,  // hour hand hidden
                    voterPlayer.position(),
                    false,  // no fade in
                    false   // no swivel
            );
        }
        // Note: Indicator block is set by ElectionManager.updateIndicator() - no need to call again here
    }

    /**
     * Callback: Called when voting is complete to handle result.
     * Handles MFE calculation, Legion tracking, and result display.
     */
    private static void onVoteResult(MinecraftServer server, int voteCount, int threshold) {
        List<UUID> votingOrder = ElectionManager.getElectionOrder();
        Map<UUID, Boolean> lockedVotes = DaytimeState.getLockedVotes();

        // Recalculate vote count with Banshee multipliers and ghost vote handling
        int adjustedVoteCount = 0;
        List<UUID> voters = new ArrayList<>();

        for (UUID voter : votingOrder) {
            Boolean voted = lockedVotes.get(voter);
            if (voted == null || !voted) continue;

            boolean isDead = cachedDeadPlayers.contains(voter);
            boolean isBansheeAbility = DaytimeState.hasBansheeDoubleVote(voter);
            boolean treatAsDead = isDead && !isBansheeAbility;

            // Voudon mode: reversed voting eligibility
            // - Alive players can't vote (except Voudon traveler)
            // - Dead players vote but don't consume ghost votes
            if (cachedVoudonMode) {
                boolean isVoudon = voter.equals(cachedVoudonPlayer);
                if (!isDead && !isVoudon) {
                    continue; // Alive non-Voudon players can't vote in Voudon mode
                }
            }

            if (!cachedVoudonMode && treatAsDead && (DaytimeState.hasUsedGhostVote(voter) || DaytimeState.hasSecretlyUsedGhostVote(voter))) {
                continue; // Ghost vote already used (skip in normal mode only - Voudon mode doesn't consume)
            }

            int voteMultiplier = DaytimeState.getVoteMultiplier(voter);
            adjustedVoteCount += voteMultiplier;
            voters.add(voter);
            if (voteMultiplier == 2) {
                voters.add(voter); // Add twice for display
            }

            // Mark ghost vote as used (for dead players or Beggar players)
            // In Voudon mode: dead players don't consume ghost votes (reversed eligibility)
            if (treatAsDead && !cachedVoudonMode) {
                if (cachedOrganGrinderMode) {
                    DaytimeState.markGhostVoteSecretlyUsed(voter);
                    DaytimeState.addPendingGhostVoteUpdate(voter);
                } else {
                    DaytimeState.markGhostVoteUsed(voter);
                }
            }
        }

        // Determine result
        UUID nominee = DaytimeState.getCurrentNominee();
        VoteResult result;
        UUID currentMFE = DaytimeState.getMarkedForExecution();
        int currentMFEVotes = DaytimeState.getVotesForMarkedPlayer();

        // In Voudon mode: threshold is 1 vote (any votes = marked), but still need most votes
        int effectiveThreshold = cachedVoudonMode ? 1 : threshold;

        if (currentMFEVotes > 0) {
            if (adjustedVoteCount > currentMFEVotes) {
                result = VoteResult.MARKED;
            } else if (adjustedVoteCount == currentMFEVotes && currentMFE != null) {
                result = VoteResult.TIE;
            } else {
                result = VoteResult.NOT_ENOUGH;
            }
        } else {
            if (adjustedVoteCount >= effectiveThreshold) {
                result = VoteResult.MARKED;
            } else {
                result = VoteResult.NOT_ENOUGH;
            }
        }

        // Apply MFE state changes
        if (result == VoteResult.MARKED) {
            if (currentMFE != null) {
                NominationManager.updateMarkedGlow(server, currentMFE, false);
            }
            DaytimeState.setMarkedForExecution(nominee, adjustedVoteCount);
            if (!cachedOrganGrinderMode) {
                NominationManager.updateMarkedGlow(server, nominee, true);
            } else {
                ServerPlayer nomineePlayer = server.getPlayerList().getPlayer(nominee);
                if (nomineePlayer != null) {
                    nomineePlayer.removeEffect(MobEffects.GLOWING);
                }
            }
        } else if (result == VoteResult.TIE) {
            if (currentMFE != null) {
                NominationManager.updateMarkedGlow(server, currentMFE, false);
            }
            DaytimeState.setMarkedForExecution(null, adjustedVoteCount);
        }

        // Clear nomination and end vote
        NominationManager.resetNomination(server);
        DaytimeState.endVote();
        StateBroadcaster.broadcastDaytimeState(server);

        // Legion vote handling. The storyteller supplies the evil list per-vote, and its presence is
        // what means "a Legion is in play".
        boolean isLegionGame = cachedEvilsForLegion != null;
        boolean isEvilOnlyVote = isLegionGame
                && !voters.isEmpty()
                && cachedEvilsForLegion.containsAll(voters);
        int storytellerVoteCount = isEvilOnlyVote ? 0 : adjustedVoteCount;

        UUID prevStorytellerMFE = DaytimeState.getStorytellerMFE();
        int prevStorytellerMFEVotes = DaytimeState.getStorytellerMFEVotes();

        VoteResult storytellerResult;
        UUID storytellerMFE;
        int storytellerMFEVotes;

        if (prevStorytellerMFEVotes > 0) {
            if (storytellerVoteCount > prevStorytellerMFEVotes) {
                storytellerResult = VoteResult.MARKED;
                storytellerMFE = nominee;
                storytellerMFEVotes = storytellerVoteCount;
            } else if (storytellerVoteCount == prevStorytellerMFEVotes && prevStorytellerMFE != null) {
                storytellerResult = VoteResult.TIE;
                storytellerMFE = null;
                storytellerMFEVotes = storytellerVoteCount;
            } else {
                storytellerResult = VoteResult.NOT_ENOUGH;
                storytellerMFE = prevStorytellerMFE;
                storytellerMFEVotes = prevStorytellerMFEVotes;
            }
        } else {
            if (storytellerVoteCount >= effectiveThreshold) {
                storytellerResult = VoteResult.MARKED;
                storytellerMFE = nominee;
                storytellerMFEVotes = storytellerVoteCount;
            } else {
                storytellerResult = VoteResult.NOT_ENOUGH;
                storytellerMFE = null;
                storytellerMFEVotes = 0;
            }
        }

        DaytimeState.setStorytellerMFE(storytellerMFE, storytellerMFEVotes);

        boolean legionProtectedVote = (result == VoteResult.MARKED || result == VoteResult.TIE) &&
                !Objects.equals(nominee, storytellerMFE) && isLegionGame;

        // Get nominee name
        ServerPlayer nomineePlayer = server.getPlayerList().getPlayer(nominee);
        String nomineeName = nomineePlayer != null ? nomineePlayer.getName().getString() : Component.translatable("gui.blood-on-the-blocktower.common.unknown_player").getString();

        // Send result payload and display titles
        VoteResultS2CPayload resultPayload = new VoteResultS2CPayload(
                result, nomineeName, adjustedVoteCount, voters, cachedOrganGrinderMode,
                legionProtectedVote, storytellerResult, storytellerMFE, storytellerMFEVotes);

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            ServerPlayNetworking.send(player, resultPayload);
        }

        displayResultTitle(server, result, nomineeName, cachedOrganGrinderMode);
        // Ghost block placement is handled in ElectionManager.scheduleCleanup before piston power returns
    }

    /**
     * Callback: Called after result display for cleanup.
     * Removes OG mode effects.
     * Note: Indicator updates are handled by ElectionManager.scheduleCleanup() - don't duplicate here.
     */
    private static void onVoteCleanup(MinecraftServer server) {
        // Remove OG mode effects after a short delay
        if (cachedOrganGrinderMode) {
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    server.execute(() -> {
                        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                            if (!player.hasPermissions(2)) {
                                player.removeEffect(MobEffects.BLINDNESS);
                                player.removeEffect(MobEffects.NIGHT_VISION);
                            }
                        }
                    });
                }
            }, 500);
        }

        // Clear cached state
        cachedDeadPlayers.clear();
        cachedOrganGrinderMode = false;
        cachedVoudonMode = false;
        cachedVoudonPlayer = null;
    }

    /**
     * Updates the vote indicator block for a player.
     */
    private static void updateVoteIndicator(MinecraftServer server, int seat, UUID player, boolean voteOn, Set<UUID> deadPlayers) {
        BlockPos indicatorPos = ServerConfig.SEAT_VOTE_INDICATOR_POSITIONS.get(seat);
        if (indicatorPos == null) {
            return;
        }

        Level world = server.overworld();
        boolean isDead = deadPlayers.contains(player);
        // Banshee players with "Has Ability" are NOT treated as dead for voting purposes
        // In Voudon mode, dead players vote like alive players (use normal indicators)
        boolean isBansheeAbility = DaytimeState.hasBansheeDoubleVote(player);
        boolean isVoudonMode = DaytimeState.isVoudonModeActive();
        boolean treatAsDead = isDead && !isBansheeAbility && !isVoudonMode;

        Block block;
        if (treatAsDead) {
            // During voting, show normal ghost on/off blocks
            // Ghost used indicator is only placed below when results are displayed
            block = voteOn
                ? getBlockFromString(ServerConfig.VOTE_INDICATOR_BLOCK_GHOST_ON)
                : getBlockFromString(ServerConfig.VOTE_INDICATOR_BLOCK_GHOST_OFF);
        } else {
            // Check for Banshee double vote - use the double-vote indicator block
            boolean isDoubleVote = DaytimeState.isBansheeDoubleVoteActive(player);
            if (voteOn && isDoubleVote) {
                block = getBlockFromString(ServerConfig.VOTE_INDICATOR_BLOCK_DOUBLE);
            } else {
                block = voteOn
                    ? getBlockFromString(ServerConfig.VOTE_INDICATOR_BLOCK_ON)
                    : getBlockFromString(ServerConfig.VOTE_INDICATOR_BLOCK_OFF);
            }
        }

        world.setBlockAndUpdate(indicatorPos, block.defaultBlockState());
    }

    /**
     * Displays the result title to all players.
     * In Organ Grinder mode, non-operators see "???" instead of actual results.
     */
    private static void displayResultTitle(MinecraftServer server, VoteResult result, String nomineeName, boolean organGrinderMode) {
        // Prepare titles for operators (real results)
        Component operatorTitleText;
        Component operatorSubtitleText;
        String soundType;

        switch (result) {
            case MARKED:
                operatorTitleText = Component.literal(nomineeName).withStyle(ChatFormatting.RED);
                operatorSubtitleText = Component.translatable("message.blood-on-the-blocktower.daytime.marked_for_execution").withStyle(ChatFormatting.RED);
                soundType = PlaySoundS2CPayload.MARKED;
                break;
            case TIE:
                operatorTitleText = Component.translatable("message.blood-on-the-blocktower.daytime.tie").withStyle(ChatFormatting.YELLOW);
                operatorSubtitleText = Component.translatable("message.blood-on-the-blocktower.daytime.all_players_pardoned").withStyle(ChatFormatting.YELLOW);
                soundType = PlaySoundS2CPayload.TIE;
                break;
            case NOT_ENOUGH:
            default:
                operatorTitleText = Component.translatable("message.blood-on-the-blocktower.daytime.not_enough_votes").withStyle(ChatFormatting.WHITE);
                operatorSubtitleText = Component.translatable("message.blood-on-the-blocktower.daytime.to_execute", nomineeName).withStyle(ChatFormatting.WHITE);
                soundType = PlaySoundS2CPayload.NOT_ENOUGH_VOTES;
                break;
        }

        // Prepare titles for non-operators in Organ Grinder mode
        Component hiddenTitleText = Component.literal("???").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD);
        Component hiddenSubtitleText = Component.translatable("message.blood-on-the-blocktower.daytime.vote_has_been_counted").withStyle(ChatFormatting.LIGHT_PURPLE);

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            boolean hideFromPlayer = organGrinderMode && !player.hasPermissions(2);

            // Send result sound - non-operators in OG mode always hear "Not Enough Votes" sound
            String playerSoundType = hideFromPlayer ? PlaySoundS2CPayload.NOT_ENOUGH_VOTES : soundType;
            ServerPlayNetworking.send(player, new PlaySoundS2CPayload(playerSoundType));

            // Send title and subtitle - hidden for non-operators in Organ Grinder mode
            Component titleText = hideFromPlayer ? hiddenTitleText : operatorTitleText;
            Component subtitleText = hideFromPlayer ? hiddenSubtitleText : operatorSubtitleText;

            player.connection.send(new ClientboundSetTitleTextPacket(titleText));
            player.connection.send(new ClientboundSetSubtitleTextPacket(subtitleText));
        }
    }

    /**
     * Broadcasts only lever states to all players during nomination phase (before vote starts).
     * This allows the sidebar to show lever positions without triggering vote-in-progress state.
     */
    public static void broadcastLeverStates(MinecraftServer server) {
        LeverStateUpdateS2CPayload payload = new LeverStateUpdateS2CPayload(DaytimeState.getLeverStates(),
                DaytimeState.getBansheeDoubleVotePlayers(), DaytimeState.getBansheeDoubleVoteActivePlayers());

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            ServerPlayNetworking.send(player, payload);
        }
    }

    /**
     * Resets all lever states, vote indicators, and player teams.
     * Called during game reset/newGame.
     */
    public static void resetLeverStates(MinecraftServer server) {
        Level world = server.overworld();

        // Physically reset all levers to "off" state
        for (Integer seat : ServerConfig.SEAT_SWITCH_POSITIONS.keySet()) {
            BlockPos switchPos = ServerConfig.SEAT_SWITCH_POSITIONS.get(seat);
            if (switchPos != null) {
                BlockState currentState = world.getBlockState(switchPos);
                if (currentState.getBlock() instanceof LeverBlock) {
                    // Set lever to off (unpowered) while preserving facing and attachment
                    if (currentState.getValue(LeverBlock.POWERED)) {
                        world.setBlockAndUpdate(switchPos, currentState.setValue(LeverBlock.POWERED, false));
                    }
                }
            }
        }

        // Reset all vote indicator blocks to "off" state
        // Also clear any ghost used / unseated blocks below the indicator
        Block ghostUsedBlock = getBlockFromString(ServerConfig.VOTE_INDICATOR_BLOCK_GHOST_USED);
        Block unseatedBlock = getBlockFromString(ServerConfig.VOTE_INDICATOR_BLOCK_UNSEATED);
        for (Integer seat : ServerConfig.SEAT_VOTE_INDICATOR_POSITIONS.keySet()) {
            BlockPos indicatorPos = ServerConfig.SEAT_VOTE_INDICATOR_POSITIONS.get(seat);
            if (indicatorPos != null) {
                Block offBlock = getBlockFromString(ServerConfig.VOTE_INDICATOR_BLOCK_OFF);
                world.setBlockAndUpdate(indicatorPos, offBlock.defaultBlockState());

                // Clear blocks below indicator (ghost used / unseated blocks)
                BlockPos belowIndicator = indicatorPos.below();
                BlockState belowState = world.getBlockState(belowIndicator);
                if (belowState.getBlock().equals(ghostUsedBlock) || belowState.getBlock().equals(unseatedBlock)) {
                    world.setBlockAndUpdate(belowIndicator, Blocks.AIR.defaultBlockState());
                }
            }
        }

        // Reset all players (seated + operators) to botb_player team
        // Travelers only go to botb_traveler team when called for exile (for purple glow color)
        // Reveal teams (rev_*) from the previous game-end are also cleared here.
        Scoreboard scoreboard = server.getScoreboard();
        PlayerTeam playerTeam = scoreboard.getPlayerTeam(TeamManager.PLAYER_TEAM);
        if (playerTeam == null) {
            playerTeam = scoreboard.addPlayerTeam(TeamManager.PLAYER_TEAM);
            playerTeam.setColor(ChatFormatting.WHITE);
        }
        PlayerTeam mfeTeam = scoreboard.getPlayerTeam(TeamManager.MFE_TEAM);
        PlayerTeam travelerTeam = scoreboard.getPlayerTeam(TeamManager.TRAVELER_TEAM);
        PlayerTeam[] revealTeams = new PlayerTeam[
                TeamManager.REVEAL_TEAM_NAMES.length];
        for (int i = 0; i < revealTeams.length; i++) {
            revealTeams[i] = scoreboard.getPlayerTeam(TeamManager.REVEAL_TEAM_NAMES[i]);
        }

        // Add all seated players to botb_player team
        for (UUID playerUuid : ServerState.PLAYER_SEAT_NUMBERS.keySet()) {
            ServerPlayer player = server.getPlayerList().getPlayer(playerUuid);
            if (player != null) {
                String playerName = player.getGameProfile().getName();

                // Remove from MFE team if present
                if (mfeTeam != null && scoreboard.getPlayersTeam(playerName) == mfeTeam) {
                    scoreboard.removePlayerFromTeam(playerName, mfeTeam);
                }
                // Remove from traveler team if present
                if (travelerTeam != null && scoreboard.getPlayersTeam(playerName) == travelerTeam) {
                    scoreboard.removePlayerFromTeam(playerName, travelerTeam);
                }
                // Remove from any reveal team if present
                for (PlayerTeam revealTeam : revealTeams) {
                    if (revealTeam != null && scoreboard.getPlayersTeam(playerName) == revealTeam) {
                        scoreboard.removePlayerFromTeam(playerName, revealTeam);
                    }
                }
                // Add all players to botb_player team
                scoreboard.addPlayerToTeam(playerName, playerTeam);
            }
        }

        // Also add all operators (storytellers) to the player team
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.hasPermissions(2)) {
                String playerName = player.getGameProfile().getName();
                // Remove from MFE team if present
                if (mfeTeam != null && scoreboard.getPlayersTeam(playerName) == mfeTeam) {
                    scoreboard.removePlayerFromTeam(playerName, mfeTeam);
                }
                // Remove from any reveal team if present
                for (PlayerTeam revealTeam : revealTeams) {
                    if (revealTeam != null && scoreboard.getPlayersTeam(playerName) == revealTeam) {
                        scoreboard.removePlayerFromTeam(playerName, revealTeam);
                    }
                }
                scoreboard.addPlayerToTeam(playerName, playerTeam);
            }
        }

        // Notify clients to clear the AFTER_END floating-role-icon latch so it disappears
        // together with the scoreboard reveal-team wipe above.
        for (ServerPlayer broadcastTarget : server.getPlayerList().getPlayers()) {
            ServerPlayNetworking.send(broadcastTarget,
                    new ResetRevealActiveS2CPayload());
        }

        // Clear lever states in DaytimeState
        DaytimeState.clearLeverStates();

        // Broadcast empty lever states to all clients
        LeverStateUpdateS2CPayload payload = new LeverStateUpdateS2CPayload(new HashMap<>(),
                new HashSet<>(), new HashSet<>());
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            ServerPlayNetworking.send(player, payload);
        }
    }

    /**
     * Applies Voudon mode visual indicators.
     * Alive non-Voudon players: no indicator at all, GHOST_USED block + sticky piston below (can't vote)
     * Dead players: show normal OFF indicator (can vote without consuming ghost votes)
     *   - Also temporarily removes ghost used blocks for dead players who had used their ghost vote
     * Alive Voudon: leaves the normal indicator in place, but cleans up any stale
     *   Voudon-blocked state from a prior assignment (e.g. role reassigned mid-day).
     */
    public static void applyVoudonModeIndicators(MinecraftServer server, Set<UUID> deadPlayers, UUID voudonPlayer) {
        Level world = server.overworld();
        Block normalOffBlock = getBlockFromString(ServerConfig.VOTE_INDICATOR_BLOCK_OFF);
        Block ghostUsedBlock = getBlockFromString(ServerConfig.VOTE_INDICATOR_BLOCK_GHOST_USED);
        Set<Integer> seatsWithGhostBlocks = new HashSet<>();

        for (Map.Entry<UUID, Integer> entry : ServerState.PLAYER_SEAT_NUMBERS.entrySet()) {
            UUID playerUuid = entry.getKey();
            Integer seat = entry.getValue();

            boolean isDead = deadPlayers.contains(playerUuid);
            boolean isVoudon = playerUuid.equals(voudonPlayer);

            if (!isDead && !isVoudon) {
                // Alive non-Voudon player: GHOST_USED block with no indicator above,
                // since they have no vote at all - not even an "off" one
                setUsedGhostVoteIndicator(server, seat, null);

                // Track this player as Voudon-blocked for visual purposes
                DaytimeState.addVoudonBlockedPlayer(playerUuid);
            } else if (isDead) {
                // Dead player: show normal OFF indicator (can vote like alive players)
                // First, check if they had a ghost used block (used ghost vote before Voudon mode)
                BlockPos indicatorPos = ServerConfig.SEAT_VOTE_INDICATOR_POSITIONS.get(seat);
                if (indicatorPos != null) {
                    BlockPos belowIndicator = indicatorPos.below();
                    BlockState blockState = world.getBlockState(belowIndicator);

                    if (blockState.getBlock().equals(ghostUsedBlock)) {
                        // Save this seat for later restoration
                        seatsWithGhostBlocks.add(seat);
                        // Remove the ghost used block temporarily
                        world.setBlockAndUpdate(belowIndicator, Blocks.AIR.defaultBlockState());
                    }

                    // Set normal OFF indicator (they vote like alive players in Voudon mode)
                    world.setBlockAndUpdate(indicatorPos, normalOffBlock.defaultBlockState());
                }
            } else {
                // Alive Voudon: clean up any stale Voudon-blocked state from a prior
                // assignment (storyteller reassigned the role to this player mid-day, or
                // state was lost across a server restart). Gated on the actual ghost-used
                // block being present so the common first-activation path is a no-op and
                // the Voudon's normal indicator/lever state isn't overwritten.
                DaytimeState.removeVoudonBlockedPlayer(playerUuid);
                BlockPos indicatorPos = ServerConfig.SEAT_VOTE_INDICATOR_POSITIONS.get(seat);
                if (indicatorPos != null) {
                    BlockPos belowIndicator = indicatorPos.below();
                    if (world.getBlockState(belowIndicator).getBlock().equals(ghostUsedBlock)) {
                        world.setBlockAndUpdate(belowIndicator, Blocks.AIR.defaultBlockState());
                        world.setBlockAndUpdate(indicatorPos, normalOffBlock.defaultBlockState());
                    }
                }
            }
        }

        // Save ghost used block seats for restoration when Voudon mode ends
        // Merge with any existing saved seats (in case of nested modes)
        if (!seatsWithGhostBlocks.isEmpty()) {
            Set<Integer> existingSeats = DaytimeState.getAndClearGhostUsedBlockSeats();
            seatsWithGhostBlocks.addAll(existingSeats);
            DaytimeState.saveGhostUsedBlockSeats(seatsWithGhostBlocks);
        }
    }

    /**
     * Restores indicators after Voudon mode deactivates.
     * - Removes GHOST_USED blocks from alive players who were Voudon-blocked
     * - Restores ghost used blocks for dead players who had them before Voudon mode
     * Pistons are left in place (never removed).
     */
    public static void restoreVoudonModeIndicators(MinecraftServer server, Set<UUID> deadPlayers) {
        Block normalOffBlock = getBlockFromString(ServerConfig.VOTE_INDICATOR_BLOCK_OFF);
        Block ghostOffBlock = getBlockFromString(ServerConfig.VOTE_INDICATOR_BLOCK_GHOST_OFF);

        // First, restore ghost used blocks for dead players who had them before Voudon mode
        Set<Integer> seatsToRestore = DaytimeState.getAndClearGhostUsedBlockSeats();
        Map<Integer, UUID> seatToPlayer = new HashMap<>();
        for (Map.Entry<UUID, Integer> entry : ServerState.PLAYER_SEAT_NUMBERS.entrySet()) {
            seatToPlayer.put(entry.getValue(), entry.getKey());
        }

        for (Integer seat : seatsToRestore) {
            UUID playerUuid = seatToPlayer.get(seat);
            if (playerUuid == null) continue;

            boolean isDead = deadPlayers.contains(playerUuid);
            boolean hasUsedGhostVote = DaytimeState.hasUsedGhostVote(playerUuid);

            boolean isBanshee = DaytimeState.hasBansheeDoubleVote(playerUuid);
            if (isDead && isBanshee) {
                // Banshee with ability votes like a living player, so keep the normal OFF block
                BlockPos indicatorPos = ServerConfig.SEAT_VOTE_INDICATOR_POSITIONS.get(seat);
                if (indicatorPos != null) {
                    server.overworld().setBlockAndUpdate(indicatorPos, normalOffBlock.defaultBlockState());
                }
            } else if (isDead && hasUsedGhostVote) {
                // Restore ghost used block for dead player with used ghost vote
                setUsedGhostVoteIndicator(server, seat);
            } else if (isDead) {
                // Dead player without used ghost vote - show ghost OFF
                BlockPos indicatorPos = ServerConfig.SEAT_VOTE_INDICATOR_POSITIONS.get(seat);
                if (indicatorPos != null) {
                    server.overworld().setBlockAndUpdate(indicatorPos, ghostOffBlock.defaultBlockState());
                }
            }
            // Alive players from this set shouldn't happen, but if so, leave as-is
        }

        // Then, handle Voudon-blocked alive players
        Set<UUID> voudonBlockedPlayers = DaytimeState.getVoudonBlockedPlayers();

        for (UUID playerUuid : voudonBlockedPlayers) {
            Integer seat = ServerState.PLAYER_SEAT_NUMBERS.get(playerUuid);
            if (seat == null) continue;

            // Only restore if it's still showing GHOST_USED
            if (!hasUsedGhostVoteIndicator(server, seat)) continue;

            boolean isDead = deadPlayers.contains(playerUuid);

            if (isDead) {
                // Player died while in Voudon mode - check if they have ghost vote
                if (DaytimeState.hasUsedGhostVote(playerUuid)) {
                    // Keep ghost used block, but set indicator to AIR (dead with used ghost vote)
                    // Already has ghost used block, just ensure indicator is AIR
                    BlockPos indicatorPos = ServerConfig.SEAT_VOTE_INDICATOR_POSITIONS.get(seat);
                    if (indicatorPos != null) {
                        server.overworld().setBlockAndUpdate(indicatorPos, Blocks.AIR.defaultBlockState());
                    }
                } else {
                    // Show ghost off indicator (dead, can still vote)
                    removeUsedGhostVoteIndicator(server, seat, ghostOffBlock);
                }
            } else {
                // Clear ghost used indicator for alive players (they go back to normal voting)
                removeUsedGhostVoteIndicator(server, seat, normalOffBlock);
            }
            // Pistons are left in place (never removed)
        }

        // Clear the tracked Voudon-blocked players
        DaytimeState.clearVoudonBlockedPlayers();
    }

    /**
     * Computes the locked-yes vote count using the same eligibility filter that
     * {@link #onVoteResult} applies at vote close, so the live count broadcast to
     * clients matches the count that will actually be tallied.
     *
     * <p>Filters out dead/Beggar voters whose ghost vote has already been used
     * (publicly or secretly via OG mode), and applies Voudon-mode reversed
     * eligibility. Without this filter, a dead voter whose ghost vote was secretly
     * used would inflate the storyteller's live tally even though their vote is
     * dropped at close.
     */
    private static int computeEffectiveLockedVoteCount() {
        Map<UUID, Boolean> lockedVotes = DaytimeState.getLockedVotes();
        int count = 0;
        for (Map.Entry<UUID, Boolean> entry : lockedVotes.entrySet()) {
            if (!Boolean.TRUE.equals(entry.getValue())) continue;
            UUID voter = entry.getKey();

            boolean isDead = cachedDeadPlayers.contains(voter);
            boolean isBansheeAbility = DaytimeState.hasBansheeDoubleVote(voter);
            boolean treatAsDead = isDead && !isBansheeAbility;

            if (cachedVoudonMode) {
                boolean isVoudon = voter.equals(cachedVoudonPlayer);
                if (!isDead && !isVoudon) continue;
            }

            if (!cachedVoudonMode && treatAsDead
                    && (DaytimeState.hasUsedGhostVote(voter) || DaytimeState.hasSecretlyUsedGhostVote(voter))) {
                continue;
            }

            count += DaytimeState.getVoteMultiplier(voter);
        }
        return count;
    }

    /**
     * Broadcasts current vote state to all players (for HUD updates).
     * Only sends updates if a vote is actually in progress.
     * Uses ElectionManager getters for voting order and timing.
     */
    private static void broadcastVoteUpdate(MinecraftServer server) {
        if (!DaytimeState.isVoteInProgress()) {
            return;
        }

        List<UUID> votingOrder = ElectionManager.getElectionOrder();
        long electionStartTime = ElectionManager.getElectionStartTime();

        int lockedCount = computeEffectiveLockedVoteCount();
        Map<UUID, Boolean> lockedVotes = DaytimeState.getLockedVotes();
        long elapsedTime = System.currentTimeMillis() - electionStartTime;
        Set<UUID> bansheePlayers = DaytimeState.getBansheeDoubleVotePlayers();
        Set<UUID> bansheeDoubleActivePlayers = DaytimeState.getBansheeDoubleVoteActivePlayers();

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            UUID playerUuid = player.getUUID();
            int playerIndex = votingOrder.indexOf(playerUuid);
            int secondsUntilLock = 0;
            int votePosition = playerIndex + 1; // Convert from 0-based to 1-based (1st, 2nd, 3rd, etc.)

            if (lockedVotes.containsKey(playerUuid)) {
                secondsUntilLock = 0;
            } else if (playerIndex >= 0) {
                // Time until this player's vote locks = 3 seconds + (playerIndex * time per player)
                // + the announce-to-commit delay, so 0 lands on the actual lock - elapsed time
                long totalTimeUntilLock = 3000 + (playerIndex * ServerConfig.VOTE_TIME_PER_PLAYER)
                        + ElectionManager.PISTON_POWER_DELAY_TICKS * 50L;
                long remainingTime = totalTimeUntilLock - elapsedTime;
                secondsUntilLock = (int) Math.max(0, Math.ceil(remainingTime / 1000.0));
            }

            // Check if this player has secretly used their ghost vote (for OG mode display)
            boolean hasSecretlyUsedGhost = DaytimeState.hasSecretlyUsedGhostVote(playerUuid);

            // Check if this player is Voudon-blocked (alive non-Voudon in Voudon mode)
            boolean isVoudonBlocked = false;
            if (DaytimeState.isVoudonModeActive()) {
                boolean isDead = ServerState.PLAYER_DEATH_STATUS.getOrDefault(playerUuid, false);
                UUID voudonUuid = DaytimeState.getVoudonPlayerUuid();
                boolean isVoudon = playerUuid.equals(voudonUuid);
                isVoudonBlocked = !isDead && !isVoudon;
            }

            VoteStateUpdateS2CPayload updatePayload = new VoteStateUpdateS2CPayload(
                lockedCount,
                secondsUntilLock,
                votePosition,
                lockedVotes,
                DaytimeState.getLeverStates(),
                DaytimeState.isOrganGrinderMode(),
                hasSecretlyUsedGhost,
                false, // isExileSupport - this is regular voting
                isVoudonBlocked,
                bansheePlayers,
                bansheeDoubleActivePlayers
            );

            ServerPlayNetworking.send(player, updatePayload);
        }
    }

    /**
     * Resets the vote - clears nomination and restores piston power.
     * Uses ElectionManager.cancelVotingPhase() to cancel unified timers.
     */
    public static void resetVote(MinecraftServer server) {
        // Cancel voting phase in ElectionManager (handles timers and piston power)
        ElectionManager.cancelVotingPhase(server);

        // End election in ElectionState
        ElectionState.endElection();

        // Restore nominee and nominator eligibility if there's a nomination
        UUID nominee = DaytimeState.getCurrentNominee();
        UUID nominator = DaytimeState.getCurrentNominator();
        if (nominee != null) {
            DaytimeState.setCanBeNominated(nominee, true);
        }
        if (nominator != null) {
            DaytimeState.setCanNominate(nominator, true);

            // Restore nomination for nominator
            // For banshee players: restore to 2 if they had 1, or to 1 if they had 0
            if (DaytimeState.hasBansheeDoubleVote(nominator)) {
                int currentRemaining = DaytimeState.getNominationsRemaining(nominator);
                if (currentRemaining == 1) {
                    // They used 1 of their 2, restore to 2
                    DaytimeState.setNominationsRemaining(nominator, 2);
                } else if (currentRemaining == 0) {
                    // They used both, restore to 1 (usual logic)
                    DaytimeState.setNominationsRemaining(nominator, 1);
                }
            }
            // Non-banshee players: setCanNominate(true) is sufficient
        }

        // Clear nomination
        NominationManager.resetNomination(server);

        // Clear vote state
        DaytimeState.resetVote();

        // Clear cached state
        cachedDeadPlayers.clear();
        cachedOrganGrinderMode = false;
        cachedVoudonMode = false;
        cachedVoudonPlayer = null;
    }

    /**
     * Updates a player's vote indicator when their death status changes.
     * @param server The server instance
     * @param playerUuid The player whose indicator should be updated
     */
    public static void updatePlayerDeathIndicator(MinecraftServer server, UUID playerUuid) {
        if (playerUuid == null) return;

        Integer seat = ServerState.PLAYER_SEAT_NUMBERS.get(playerUuid);
        if (seat == null) return;

        // Get current dead players
        Set<UUID> deadPlayers = ServerState.deadPlayers();

        boolean isDead = deadPlayers.contains(playerUuid);

        // Handle Voudon mode specially
        if (DaytimeState.isVoudonModeActive()) {
            BlockPos indicatorPos = ServerConfig.SEAT_VOTE_INDICATOR_POSITIONS.get(seat);
            if (indicatorPos == null) return;

            UUID voudonUuid = DaytimeState.getVoudonPlayerUuid();
            boolean isVoudon = playerUuid.equals(voudonUuid);

            if (isDead) {
                // Player just died in Voudon mode → show normal OFF (they can now vote),
                // clearing the Voudon-blocked GHOST_USED block below if they had one
                Block normalOffBlock = getBlockFromString(ServerConfig.VOTE_INDICATOR_BLOCK_OFF);
                removeUsedGhostVoteIndicator(server, seat, normalOffBlock);
                // Remove from Voudon-blocked set if they were in it
                DaytimeState.removeVoudonBlockedPlayer(playerUuid);
            } else if (!isVoudon) {
                // Player just revived in Voudon mode (and not Voudon) → GHOST_USED with no indicator
                setUsedGhostVoteIndicator(server, seat, null);

                // Add to Voudon-blocked set
                DaytimeState.addVoudonBlockedPlayer(playerUuid);
            }
            // Voudon player revived keeps normal indicator (handled by normal flow below if needed)
            if (!isVoudon) return;
        }

        // Don't update indicator for dead players with used ghost votes (indicator is frozen)
        // Banshee players with "Has Ability" are NOT treated as dead
        boolean isBansheeAbility = DaytimeState.hasBansheeDoubleVote(playerUuid);
        boolean treatAsDead = isDead && !isBansheeAbility;
        boolean hasUsedGhost = treatAsDead && DaytimeState.hasUsedGhostVote(playerUuid);
        if (hasUsedGhost) {
            return; // Skip updating - their indicator is frozen
        }

        // Get current lever state
        Boolean leverState = DaytimeState.getLeverStates().get(playerUuid);
        if (leverState == null) {
            // If no lever state yet, read it from the world
            BlockPos switchPos = ServerConfig.SEAT_SWITCH_POSITIONS.get(seat);
            if (switchPos != null) {
                BlockState state = server.overworld().getBlockState(switchPos);
                leverState = state.getBlock() instanceof LeverBlock && state.getValue(LeverBlock.POWERED);
                DaytimeState.setLeverState(playerUuid, leverState);
            } else {
                leverState = false;
            }
        }

        // Update indicator block - use exile indicators if exile is active
        if (DaytimeState.hasActiveExile() || DaytimeState.isExileSupportInProgress()) {
            ExileManager.updateExileIndicator(server, seat, leverState);
        } else {
            updateVoteIndicator(server, seat, playerUuid, leverState, deadPlayers);
        }
    }

    /**
     * Event handler for block usage. Detects lever flips at configured vote positions.
     * Call this from UseBlockCallback registration.
     */
    public static InteractionResult onBlockUse(Level world, BlockPos pos, BlockState state) {
        // Check if this is a lever
        if (!(state.getBlock() instanceof LeverBlock)) {
            return InteractionResult.PASS;
        }

        // Only process on server side
        if (world.isClientSide()) {
            return InteractionResult.PASS;
        }

        // Find which seat this lever belongs to (if any)
        Integer matchingSeat = null;
        for (Map.Entry<Integer, BlockPos> entry : ServerConfig.SEAT_SWITCH_POSITIONS.entrySet()) {
            if (entry.getValue().equals(pos)) {
                matchingSeat = entry.getKey();
                break;
            }
        }

        // Not a vote lever
        if (matchingSeat == null) {
            return InteractionResult.PASS;
        }

        // Find the player assigned to this seat
        UUID playerUuid = null;
        for (Map.Entry<UUID, Integer> entry : ServerState.PLAYER_SEAT_NUMBERS.entrySet()) {
            if (entry.getValue().equals(matchingSeat)) {
                playerUuid = entry.getKey();
                break;
            }
        }

        if (playerUuid == null) {
            return InteractionResult.PASS;
        }

        // Organ Grinder mute: while an OG vote is active, blindness is applied
        // to non-operators to hide who's voting. The lever click sound would
        // betray voter identity, so we suppress vanilla's flip (which broadcasts
        // the click sound) and toggle the lever ourselves silently. Returning
        // SUCCESS still plays the arm-swing animation. Scoped to the same
        // window blindness is applied: an active vote in OG mode.
        boolean ogMute = DaytimeState.isVoteInProgress() && DaytimeState.isOrganGrinderMode();
        if (ogMute) {
            world.setBlock(pos, state.cycle(LeverBlock.POWERED), Block.UPDATE_ALL);
        }
        InteractionResult passResult = ogMute ? InteractionResult.SUCCESS : InteractionResult.PASS;

        // Skip processing if this player's vote is already locked. PASS lets
        // the lever physically toggle (so the player sees their click register)
        // but we don't run updateVoteIndicator, so no stray block is placed.
        if (DaytimeState.getLockedVotes().containsKey(playerUuid)) {
            return passResult;
        }

        // Skip processing during the post-vote cleanup window for the same
        // reason. ElectionManager.cleanupInProgress is true for the entire
        // scheduleCleanup → cleanup timer span (covers both vote and exile
        // support since scheduleCleanup is shared).
        if (ElectionManager.isCleanupInProgress()) {
            return passResult;
        }

        // Skip if player is dead and has lost their ghost vote (not Banshee)
        // NOTE: During exile, ghost vote restrictions don't apply - everyone can participate
        // NOTE: During Voudon mode, dead players can vote regardless of ghost vote status
        boolean isDead = ServerState.PLAYER_DEATH_STATUS.getOrDefault(playerUuid, false);
        boolean isBansheeAbility = DaytimeState.hasBansheeDoubleVote(playerUuid);
        boolean treatAsDead = isDead && !isBansheeAbility;
        boolean isExileMode = DaytimeState.hasActiveExile() || DaytimeState.isExileSupportInProgress();
        boolean isVoudonMode = DaytimeState.isVoudonModeActive();
        if (treatAsDead && DaytimeState.hasUsedGhostVote(playerUuid) && !isExileMode && !isVoudonMode) {
            return passResult;
        }

        // Skip if player is Voudon-blocked (alive non-Voudon in Voudon mode)
        // NOTE: During exile, Voudon restrictions don't apply - everyone can participate
        if (isVoudonMode && !isExileMode) {
            UUID voudonUuid = DaytimeState.getVoudonPlayerUuid();
            boolean isVoudon = playerUuid.equals(voudonUuid);
            if (!isDead && !isVoudon) {
                return passResult; // Alive non-Voudon players can't vote in Voudon mode
            }
        }

        // Get server instance
        MinecraftServer server = world.getServer();
        if (server == null) {
            return passResult;
        }

        // Get current dead players
        Set<UUID> deadPlayers = ServerState.deadPlayers();

        // The lever state will change AFTER this event, so we need to schedule an update
        final UUID finalPlayerUuid = playerUuid;
        final int finalSeat = matchingSeat;

        server.execute(() -> {
            // Re-check: the vote-lock sweep may have passed this player between the
            // synchronous locked-votes check above and this deferred execution. If so,
            // drop the lever flip so we don't raise an indicator block after the sweep
            // has already lowered it.
            if (DaytimeState.getLockedVotes().containsKey(finalPlayerUuid)) {
                return;
            }

            // Re-check cleanup window: scheduleCleanup may have started
            // between the synchronous gate above and this deferred execution.
            if (ElectionManager.isCleanupInProgress()) {
                return;
            }

            // Read the NEW lever state (after the flip)
            BlockState newState = world.getBlockState(pos);
            boolean isOn = newState.getBlock() instanceof LeverBlock && newState.getValue(LeverBlock.POWERED);

            // Banshee double vote toggle: when lever turns ON, potentially toggle between 1 and 2 votes
            // First ON keeps default (2 votes), subsequent OFF→ON transitions toggle
            // Works whenever Banshee has double vote ability (not just during vote)
            if (isOn && DaytimeState.hasBansheeDoubleVote(finalPlayerUuid)) {
                Boolean wasOn = DaytimeState.getLeverStates().get(finalPlayerUuid);
                // Only process if lever was previously OFF (true OFF→ON transition)
                if (wasOn == null || !wasOn) {
                    DaytimeState.toggleBansheeDoubleVoteActive(finalPlayerUuid);
                }
            }

            // Update stored lever state
            DaytimeState.setLeverState(finalPlayerUuid, isOn);

            // Update indicator block - use exile indicators if exile is active
            if (DaytimeState.hasActiveExile() || DaytimeState.isExileSupportInProgress()) {
                // During exile call or exile support: use exile indicators
                ExileManager.updateExileIndicator(server, finalSeat, isOn);
                // Also update exile support vote state
                DaytimeState.setExileSupportVote(finalPlayerUuid, isOn);
            } else {
                // Normal vote indicators
                updateVoteIndicator(server, finalSeat, finalPlayerUuid, isOn, deadPlayers);
            }

            // Broadcast updates to clients
            if (DaytimeState.isVoteInProgress()) {
                // During vote: broadcast full vote state
                broadcastVoteUpdate(server);
            } else if (DaytimeState.isExileSupportInProgress()) {
                // During exile support: broadcast exile state
                ExileSupportManager.broadcastExileSupportUpdate(server);
            } else if (DaytimeState.getCurrentNominee() != null) {
                // During nomination: broadcast lever states only
                broadcastLeverStates(server);
            } else if (DaytimeState.hasActiveExile()) {
                // During exile call (before support): broadcast lever states
                broadcastLeverStates(server);
            }
        });

        return passResult;
    }

    // ========== Ghost Vote Block Helper Functions ==========

    /**
     * Sets the used ghost vote indicator for a player's seat.
     * This removes the normal indicator (sets to AIR), places the ghost used block below,
     * and places an upward-facing sticky piston below that.
     *
     * @param server The server instance
     * @param seat The seat number
     */
    public static void setUsedGhostVoteIndicator(MinecraftServer server, int seat) {
        setUsedGhostVoteIndicator(server, seat, null);
    }

    /**
     * Sets the used ghost vote indicator for a player's seat with a custom indicator block.
     * This sets the indicator to the specified block (or AIR if null), places the ghost used block below,
     * and places an upward-facing sticky piston below that.
     *
     * @param server The server instance
     * @param seat The seat number
     * @param indicatorBlock The block to use for the indicator (null for AIR)
     */
    public static void setUsedGhostVoteIndicator(MinecraftServer server, int seat, Block indicatorBlock) {
        BlockPos indicatorPos = ServerConfig.SEAT_VOTE_INDICATOR_POSITIONS.get(seat);
        if (indicatorPos == null) return;

        Level world = server.overworld();

        // Set indicator to specified block or AIR
        if (indicatorBlock != null) {
            world.setBlockAndUpdate(indicatorPos, indicatorBlock.defaultBlockState());
        } else {
            world.setBlockAndUpdate(indicatorPos, Blocks.AIR.defaultBlockState());
        }

        // Write the retracted piston first so an extended head in the middle slot vanishes
        // on its own instead of the base being broken and dropped when the head is overwritten
        BlockPos belowIndicator = indicatorPos.below();
        BlockPos pistonPos = belowIndicator.below();
        BlockState pistonState = Blocks.STICKY_PISTON.defaultBlockState()
                .setValue(PistonBaseBlock.FACING, Direction.UP);
        world.setBlockAndUpdate(pistonPos, pistonState);

        // Place ghost used block below indicator
        Block ghostUsedBlock = getBlockFromString(ServerConfig.VOTE_INDICATOR_BLOCK_GHOST_USED);
        world.setBlockAndUpdate(belowIndicator, ghostUsedBlock.defaultBlockState());
    }

    /**
     * Removes the used ghost vote indicator for a player's seat.
     * This removes the ghost used block below the indicator and sets the indicator
     * to the specified new block. Pistons are left in place (never removed).
     *
     * @param server The server instance
     * @param seat The seat number
     * @param newIndicatorBlock The block to set as the new indicator (e.g., OFF block, GHOST_OFF block)
     */
    public static void removeUsedGhostVoteIndicator(MinecraftServer server, int seat, Block newIndicatorBlock) {
        BlockPos indicatorPos = ServerConfig.SEAT_VOTE_INDICATOR_POSITIONS.get(seat);
        if (indicatorPos == null) return;

        Level world = server.overworld();

        // Remove the ghost used block (set to AIR)
        BlockPos belowIndicator = indicatorPos.below();
        Block ghostUsedBlock = getBlockFromString(ServerConfig.VOTE_INDICATOR_BLOCK_GHOST_USED);
        BlockState belowState = world.getBlockState(belowIndicator);
        if (belowState.getBlock().equals(ghostUsedBlock)) {
            world.setBlockAndUpdate(belowIndicator, Blocks.AIR.defaultBlockState());
        }

        // Set the indicator to the new block
        if (newIndicatorBlock != null) {
            world.setBlockAndUpdate(indicatorPos, newIndicatorBlock.defaultBlockState());
        }
    }

    /**
     * Checks if a seat currently has the used ghost vote indicator block below it.
     *
     * @param server The server instance
     * @param seat The seat number
     * @return true if the ghost used block is present below the indicator
     */
    public static boolean hasUsedGhostVoteIndicator(MinecraftServer server, int seat) {
        BlockPos indicatorPos = ServerConfig.SEAT_VOTE_INDICATOR_POSITIONS.get(seat);
        if (indicatorPos == null) return false;

        Level world = server.overworld();
        BlockPos belowIndicator = indicatorPos.below();
        Block ghostUsedBlock = getBlockFromString(ServerConfig.VOTE_INDICATOR_BLOCK_GHOST_USED);
        BlockState belowState = world.getBlockState(belowIndicator);
        return belowState.getBlock().equals(ghostUsedBlock);
    }

    public static Block getBlockFromString(String blockId) {
        Block block = ElectionManager.getBlockFromString(blockId);
        return block != null ? block : Blocks.STONE; // Default fallback for null
    }

    /**
     * Enum for vote results.
     */
    public enum VoteResult {
        MARKED,
        TIE,
        NOT_ENOUGH
    }
}
