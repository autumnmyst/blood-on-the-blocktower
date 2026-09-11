package com.autumnwind.botb.networking;

import com.autumnwind.botb.clockhands.ClockHandsState;
import com.autumnwind.botb.daytime.VotingManager;
import com.autumnwind.botb.gui.AssignRolesScreen;
import com.autumnwind.botb.states.ClientState;
import com.autumnwind.botb.states.StorytellerState;
import com.autumnwind.botb.timer.ClientTimerState;
import com.autumnwind.botb.util.PendingRoleAssignment;
import com.autumnwind.botb.util.PlayerListUtil;
import com.autumnwind.botb.util.RoleType;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/** Client-bound packet receivers for nominations, votes, results, the timer, and the clock hands. */
final class DaytimeReceivers {

    private DaytimeReceivers() {}

    static void register() {
        // Client receives daytime state sync
        ClientPlayNetworking.registerGlobalReceiver(SyncDaytimeStateS2CPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                // Update storyteller MFE from server (authoritative source)
                // This handles Legion cases where storyteller MFE differs from player MFE
                StorytellerState.storytellerMFE = payload.storytellerMFE();
                StorytellerState.storytellerMFEVotes = payload.storytellerMFEVotes();

                // Clear Legion protection when both MFEs are null (hard reset or new day)
                if (payload.markedForExecution() == null && payload.storytellerMFE() == null) {
                    StorytellerState.legionProtectedPlayers.clear();
                }

                // Track minion nomination for Town Crier (only for operators)
                // Check if this is a new nomination (current is null, new is not)
                boolean isOperator = context.client().player != null && context.client().player.hasPermissionLevel(2);
                if (isOperator && ClientState.currentNominator == null && payload.currentNominator() != null) {
                    // New nomination - check if nominator is a minion
                    PendingRoleAssignment nominatorAssignment =
                            StorytellerState.PENDING_ROLES.get(payload.currentNominator());
                    if (nominatorAssignment != null) {
                        RoleType nomType = nominatorAssignment.isCustomRole() && nominatorAssignment.customRole().isPresent()
                                ? nominatorAssignment.customRole().get().team()
                                : nominatorAssignment.role().getType();
                        if (nomType == RoleType.MINION) {
                            StorytellerState.minionNominatedToday = true;
                        }
                    }
                }

                // Check if a nomination has started or changed
                boolean nominationStateChanged =
                    (ClientState.currentNominator == null && payload.currentNominator() != null) ||
                    (ClientState.currentNominator != null && !ClientState.currentNominator.equals(payload.currentNominator()));

                // Check if exile state has changed
                boolean exileStateChanged =
                    (ClientState.currentExileTarget == null && payload.currentExileTarget() != null) ||
                    (ClientState.currentExileTarget != null && !ClientState.currentExileTarget.equals(payload.currentExileTarget())) ||
                    (ClientState.exileSupportInProgress != payload.exileSupportInProgress());

                ClientState.updateDaytimeState(
                    payload.canNominate(),
                    payload.canBeNominated(),
                    payload.hasUsedGhostVote(),
                    payload.nominationsRemaining(),
                    payload.currentNominator(),
                    payload.currentNominee(),
                    payload.markedForExecution(),
                    payload.votesForMarkedPlayer(),
                    payload.nominationsOpen(),
                    payload.organGrinderModeActiveToday(),
                    payload.storytellerCanBeNominated(),
                    payload.canBeExiled(),
                    payload.currentExileCaller(),
                    payload.currentExileTarget(),
                    payload.exileSupportInProgress(),
                    payload.exileSupportCount(),
                    payload.voudonModeActive(),
                    payload.voudonPlayerUuid()
                );

                // Refresh AssignRolesScreen if it's open and nomination or exile state changed
                if ((nominationStateChanged || exileStateChanged) && context.client().currentScreen instanceof AssignRolesScreen assignRolesScreen) {
                    assignRolesScreen.init(context.client(), context.client().getWindow().getScaledWidth(), context.client().getWindow().getScaledHeight());
                }
            });
        });

        // Client receives lever state updates during nomination phase (before vote starts)
        ClientPlayNetworking.registerGlobalReceiver(LeverStateUpdateS2CPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                // Only update lever states, don't set voteInProgress=true
                ClientState.leverStates = new HashMap<>(payload.leverStates());
                ClientState.bansheePlayers = new HashSet<>(payload.bansheePlayers());
                ClientState.bansheeDoubleActivePlayers = new HashSet<>(payload.bansheeDoubleActivePlayers());
            });
        });

        // Client receives vote state updates during active vote or exile support
        ClientPlayNetworking.registerGlobalReceiver(VoteStateUpdateS2CPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                ClientState.bansheePlayers = new HashSet<>(payload.bansheePlayers());
                ClientState.bansheeDoubleActivePlayers = new HashSet<>(payload.bansheeDoubleActivePlayers());
                if (payload.isExileSupport()) {
                    // This is an exile support update
                    ClientState.updateExileSupportState(
                        payload.lockedVoteCount(),
                        payload.secondsUntilMyVoteLocks(),
                        payload.myVotePosition(),
                        payload.lockedVotes(),
                        payload.leverStates()
                    );
                } else {
                    // This is a regular vote update
                    ClientState.updateVoteState(
                        payload.lockedVoteCount(),
                        payload.secondsUntilMyVoteLocks(),
                        payload.myVotePosition(),
                        payload.lockedVotes(),
                        payload.leverStates(),
                        payload.organGrinderMode(),
                        payload.hasSecretlyUsedGhostVote(),
                        payload.isVoudonBlocked()
                    );
                }
            });
        });

        // Client receives vote result
        ClientPlayNetworking.registerGlobalReceiver(VoteResultS2CPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                // Capture nominee UUID before clearing vote state (for Legion tracking)
                UUID nomineeUuid = ClientState.currentNominee;

                // Legion Vote Hiding: use server-provided storyteller view (authoritative)
                boolean isOperator = context.client().player != null && context.client().player.hasPermissionLevel(2);
                if (isOperator) {
                    // Update storyteller's MFE view (may differ from ClientState.markedForExecution)
                    StorytellerState.storytellerMFE = payload.storytellerMFE();
                    StorytellerState.storytellerMFEVotes = payload.storytellerMFEVotes();

                    // Track Legion-protected players (those who appear marked but shouldn't be)
                    if (nomineeUuid != null) {
                        if (payload.legionProtectedVote()) {
                            // This nominee appears marked to players but not to storyteller
                            StorytellerState.legionProtectedPlayers.add(nomineeUuid);
                        } else if (payload.result() == VotingManager.VoteResult.MARKED) {
                            // Not Legion-protected but still marked: remove from protected set
                            StorytellerState.legionProtectedPlayers.remove(nomineeUuid);
                        }
                    }

                    // Track demon voting for Flowergirl
                    // Check if any demon voted in this vote
                    for (UUID voter : payload.voters()) {
                        PendingRoleAssignment assignment = StorytellerState.PENDING_ROLES.get(voter);
                        if (assignment != null) {
                            RoleType voterType = assignment.isCustomRole() && assignment.customRole().isPresent()
                                    ? assignment.customRole().get().team()
                                    : assignment.role().getType();
                            if (voterType == RoleType.DEMON) {
                                StorytellerState.demonVotedToday = true;
                                break;
                            }
                        }
                    }
                }

                // Clear vote state
                ClientState.clearVoteState();

                // Check for Organ Grinder mode visibility (isOperator already calculated above)
                boolean hideVoteInfo = payload.organGrinderMode() && !isOperator;

                // Display vote result in chat
                List<UUID> voters = payload.voters();

                // For operators in Legion games: use storytellerResult instead of player result
                VotingManager.VoteResult displayResult =
                        isOperator ? payload.storytellerResult() : payload.result();
                int displayVoteCount = isOperator ? payload.storytellerMFEVotes() : payload.voteCount();

                Text resultMessage;
                if (hideVoteInfo) {
                    // Organ Grinder mode: hide details from non-operators
                    resultMessage = Text.translatable("message.blood-on-the-blocktower.client.vote_counted")
                            .formatted(Formatting.LIGHT_PURPLE);
                } else {
                    // Get the name to display for marked player (for operators, may be different than nominee)
                    String markedPlayerName = payload.nomineeName();
                    if (isOperator && payload.storytellerMFE() != null && displayResult == VotingManager.VoteResult.MARKED) {
                        // For operators: show the storyteller's MFE name if different (supports distant players)
                        AbstractClientPlayerEntity stMFEPlayer =
                                (AbstractClientPlayerEntity)
                                context.client().world.getPlayerByUuid(payload.storytellerMFE());
                        if (stMFEPlayer != null) {
                            markedPlayerName = stMFEPlayer.getName().getString();
                        } else {
                            PlayerListUtil.PlayerInfo info = PlayerListUtil.getPlayer(context.client(), payload.storytellerMFE());
                            if (info != null) markedPlayerName = info.name();
                        }
                    }

                    final String finalMarkedPlayerName = markedPlayerName;
                    resultMessage = switch (displayResult) {
                        case MARKED -> Text.literal(finalMarkedPlayerName)
                                .styled(style -> style.withColor(0xFF8C00)) // Orange (canBeNominated color)
                                .append(Text.translatable("message.blood-on-the-blocktower.client.has_been").formatted(Formatting.WHITE))
                                .append(Text.translatable("message.blood-on-the-blocktower.client.marked_for_execution").formatted(Formatting.RED))
                                .append(Text.translatable("message.blood-on-the-blocktower.client.with_votes", displayVoteCount).formatted(Formatting.WHITE));
                        case TIE -> Text.translatable("message.blood-on-the-blocktower.client.tie").formatted(Formatting.YELLOW)
                                .append(Text.translatable("message.blood-on-the-blocktower.client.all_pardoned_with_votes", payload.voteCount()).formatted(Formatting.WHITE));
                        case NOT_ENOUGH -> Text.translatable("message.blood-on-the-blocktower.client.not_enough_votes", payload.voteCount())
                                .formatted(Formatting.WHITE);
                    };
                }

                if (context.client().player != null) {
                    context.client().player.sendMessage(resultMessage, false);

                    // For operators: explain when it's a Legion-protected vote (evil-only)
                    if (isOperator && payload.legionProtectedVote()) {
                        context.client().player.sendMessage(
                            Text.translatable("message.blood-on-the-blocktower.client.legion_evil_only_vote")
                                .formatted(Formatting.DARK_PURPLE),
                            false
                        );
                    }

                    // Only show voters list if not hiding vote info (supports distant players)
                    if (!hideVoteInfo && !voters.isEmpty()) {
                        context.client().player.sendMessage(Text.translatable("message.blood-on-the-blocktower.client.voted"), false);
                        for (UUID voterUuid : voters) {
                            String voterName = null;
                            AbstractClientPlayerEntity voter =
                                (AbstractClientPlayerEntity)
                                context.client().world.getPlayerByUuid(voterUuid);

                            if (voter != null) {
                                voterName = voter.getName().getString();
                            } else {
                                PlayerListUtil.PlayerInfo info = PlayerListUtil.getPlayer(context.client(), voterUuid);
                                if (info != null) voterName = info.name();
                            }

                            if (voterName != null) {
                                context.client().player.sendMessage(
                                    Text.translatable("message.blood-on-the-blocktower.client.voter_entry",
                                        Text.literal(voterName).formatted(Formatting.YELLOW)).formatted(Formatting.WHITE),
                                    false
                                );
                            }
                        }
                    }
                }
            });
        });

        // Client receives timer state updates
        ClientPlayNetworking.registerGlobalReceiver(TimerStateS2CPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                ClientTimerState.updateTimerState(
                    payload.isActive(),
                    payload.isPaused(),
                    payload.remainingSeconds(),
                    payload.totalSeconds()
                );
            });
        });

        // Client receives clock hands state update
        ClientPlayNetworking.registerGlobalReceiver(ClockHandsStateS2CPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                ClockHandsState.onStateReceived(payload);
            });
        });
    }
}
