package com.autumnwind.botb.hud;

import com.autumnwind.botb.states.ClientState;
import com.autumnwind.botb.util.PlayerListUtil;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.server.permissions.Permissions;

/**
 * Displays voting information during an active vote.
 */
public class VoteHUD {

    private static final int MIN_WIDTH = 150;

    /**
     * Renders the vote HUD if there's an active nomination or vote in progress.
     */
    public static void render(GuiGraphicsExtractor context, Minecraft client) {
        // Show if there's a nomination OR a vote in progress
        UUID nomineeUuid = ClientState.currentNominee;
        if (nomineeUuid == null) {
            return; // No nomination
        }

        // Get nominee player (supports distant players)
        String nomineeName;
        AbstractClientPlayer nominee = (AbstractClientPlayer) client.level.getPlayerByUUID(nomineeUuid);
        if (nominee != null) {
            nomineeName = nominee.getName().getString();
        } else {
            PlayerListUtil.PlayerInfo info = PlayerListUtil.getPlayer(client, nomineeUuid);
            nomineeName = info != null ? info.name() : Component.translatable("gui.blood-on-the-blocktower.common.unknown_player").getString();
        }

        // Check if the current player is the nominee
        boolean isNominee = client.player.getUUID().equals(nomineeUuid);

        // Check if player is an operator (for Organ Grinder mode visibility)
        boolean isOperator = client.player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);

        // Organ Grinder hides vote info from everyone but the storyteller. The day-scoped flag
        // is the one that matters here: it is set when the day's first OG vote starts and holds
        // for the rest of the day, so real "Tie: 4 | Execute: 5" thresholds from an OG-marked
        // nominee can't surface in the next nomination's pre-vote display.
        boolean hideVoteInfo = ClientState.organGrinderModeActiveToday && !isOperator;

        // Calculate alive player count
        int aliveCount = (int) ClientState.playerSeatNumbers.keySet().stream()
                .filter(uuid -> !ClientState.playerDeathStatus.getOrDefault(uuid, false))
                .count();

        // Calculate votes required
        // Voudon mode: any vote marks the nominee, as long as it beats the current mark
        int votesRequired = ClientState.voudonModeActive ? 1 : (int) Math.ceil(aliveCount / 2.0);

        // Check if there's a tie scenario (hidden in Organ Grinder mode for non-operators)
        int votesForTie = hideVoteInfo ? 0 : ClientState.votesForMarkedPlayer;
        if (votesForTie > 0) {
            votesRequired = votesForTie + 1;
        }
        boolean hasMFE = ClientState.markedForExecution != null && !hideVoteInfo;

        // Check if player is dead and lost ghost vote (either publicly or secretly in OG mode)
        // In Voudon mode dead players vote freely, so a used ghost vote doesn't block them
        boolean isDead = ClientState.playerDeathStatus.getOrDefault(client.player.getUUID(), false);
        boolean hasLostGhostVote = isDead && !ClientState.voudonModeActive
                && (ClientState.hasUsedGhostVote.getOrDefault(client.player.getUUID(), false) || ClientState.hasSecretlyUsedGhostVote);

        // Voudon-blocked: alive seated non-Voudon players have no vote while Voudon mode
        // is active. Computed from synced state so it already shows during the nomination,
        // before the vote itself starts.
        boolean isVoudonBlocked = ClientState.isVoudonBlocked
                || (ClientState.voudonModeActive && !isDead
                        && ClientState.playerSeatNumbers.containsKey(client.player.getUUID())
                        && !client.player.getUUID().equals(ClientState.voudonPlayerUuid));

        // All three lines are built before anything is drawn, so the box can be sized to
        // whichever is widest, since nominee names have no length limit.
        int screenWidth = context.guiWidth();

        // Line 1: Title
        MutableComponent titleText;
        if (ClientState.voteInProgress) {
            // Show "Now voting for: " in yellow
            if (isNominee) {
                titleText = Component.translatable("hud.blood-on-the-blocktower.vote.now_voting_for").withStyle(ChatFormatting.YELLOW)
                        .append(Component.translatable("hud.blood-on-the-blocktower.common.you").withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
            } else {
                titleText = Component.translatable("hud.blood-on-the-blocktower.vote.now_voting_for").withStyle(ChatFormatting.YELLOW)
                        .append(Component.literal(nomineeName).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
            }
        } else {
            if (isNominee) {
                titleText = Component.translatable("hud.blood-on-the-blocktower.vote.nominated").withStyle(ChatFormatting.YELLOW)
                        .append(Component.translatable("hud.blood-on-the-blocktower.common.you").withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
            } else {
                titleText = Component.translatable("hud.blood-on-the-blocktower.vote.nominated").withStyle(ChatFormatting.YELLOW)
                        .append(Component.literal(nomineeName).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
            }
        }
        // Line 2: Vote requirements (with current count if voting)
        MutableComponent requirementsText;
        if (hideVoteInfo) {
            // Organ Grinder mode: hide all vote info with purple "?", but show the
            // static "≥ ceil(alive/2)" baseline threshold, which is public info and
            // doesn't leak how many votes any prior OG nominee received.
            requirementsText = Component.translatable("hud.blood-on-the-blocktower.vote.votes").withStyle(ChatFormatting.WHITE)
                    .append(Component.literal("?").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD))
                    .append(Component.literal(" (≥" + votesRequired + ")").withStyle(ChatFormatting.GRAY));
        } else if (ClientState.voteInProgress) {
            // During vote: Show as fraction
            int currentCount = ClientState.currentVoteCount;

            if (hasMFE) {
                // With MFE: show both tie and execute as fractions
                // Tie: yellow if exactly at tie count (and not at execute), otherwise white - always bold
                boolean atTie = (currentCount == votesForTie) && (currentCount < votesForTie + 1);
                MutableComponent tieFraction = Component.literal(currentCount + "/" + votesForTie);
                if (atTie) {
                    tieFraction = tieFraction.withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD);
                } else {
                    tieFraction = tieFraction.withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD);
                }

                // Execute: red if at or above execute count, otherwise white - always bold
                boolean atExecute = currentCount >= votesForTie + 1;
                MutableComponent execFraction = Component.literal(currentCount + "/" + (votesForTie + 1));
                if (atExecute) {
                    execFraction = execFraction.withStyle(ChatFormatting.RED, ChatFormatting.BOLD);
                } else {
                    execFraction = execFraction.withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD);
                }

                requirementsText = Component.translatable("hud.blood-on-the-blocktower.vote.tie")
                        .append(tieFraction)
                        .append(Component.translatable("hud.blood-on-the-blocktower.vote.execute").withStyle(ChatFormatting.WHITE))
                        .append(execFraction);
            } else {
                // Without MFE: show votes required as fraction
                // Red if at or above required, otherwise white - always bold
                boolean atRequired = currentCount >= votesRequired;
                MutableComponent requiredFraction = Component.literal(currentCount + "/" + votesRequired);
                if (atRequired) {
                    requiredFraction = requiredFraction.withStyle(ChatFormatting.RED, ChatFormatting.BOLD);
                } else {
                    requiredFraction = requiredFraction.withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD);
                }

                requirementsText = Component.translatable("hud.blood-on-the-blocktower.vote.votes_required")
                        .append(requiredFraction);
            }
        } else {
            // Before vote: Show requirements without current count
            if (hasMFE) {
                requirementsText = Component.translatable("hud.blood-on-the-blocktower.vote.tie")
                        .append(Component.literal(String.valueOf(votesForTie)).withStyle(ChatFormatting.YELLOW))
                        .append(Component.translatable("hud.blood-on-the-blocktower.vote.execute").withStyle(ChatFormatting.WHITE))
                        .append(Component.literal(String.valueOf(votesForTie + 1)).withStyle(ChatFormatting.RED));
            } else {
                requirementsText = Component.translatable("hud.blood-on-the-blocktower.vote.votes_required")
                        .append(Component.literal(String.valueOf(votesRequired)).withStyle(ChatFormatting.YELLOW));
            }
        }
        // Line 3: Vote position / countdown / locked status / cannot vote. Unseated viewers
        // (the storyteller, spectators) have no lever and no place in the voting order, so
        // they get no line at all: the server sends them a zero countdown and, in Voudon mode,
        // a blocked flag, and an Atheist-nominated storyteller would otherwise read "You vote Last".
        MutableComponent line3Text;
        boolean seated = ClientState.playerSeatNumbers.containsKey(client.player.getUUID());

        if (!seated) {
            line3Text = Component.empty();
        } else if (hasLostGhostVote) {
            // Player has lost their ghost vote
            line3Text = Component.translatable("hud.blood-on-the-blocktower.vote.cannot_vote").withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC);
        } else if (isVoudonBlocked) {
            // Player is Voudon-blocked (alive non-Voudon in Voudon mode)
            line3Text = Component.translatable("hud.blood-on-the-blocktower.vote.cannot_vote").withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC);
        } else if (ClientState.voteInProgress) {
            // During vote: Show countdown or locked status
            UUID playerUuid = client.player.getUUID();
            Boolean leverState = ClientState.leverStates.get(playerUuid);

            if (ClientState.myVoteLockInTime > 0) {
                // Show countdown - YES/NO is bold while unlocked
                line3Text = Component.translatable("hud.blood-on-the-blocktower.vote.locks_in")
                        .append(Component.literal(ClientState.myVoteLockInTime + "s").withStyle(ChatFormatting.AQUA));

                if (leverState != null) {
                    Component voteStatus = leverState ? Component.translatable("hud.blood-on-the-blocktower.common.yes") : Component.translatable("hud.blood-on-the-blocktower.common.no");
                    ChatFormatting voteColor = leverState ? ChatFormatting.GREEN : ChatFormatting.RED;
                    line3Text.append(Component.literal(" - "))
                            .append(voteStatus.copy().withStyle(voteColor, ChatFormatting.BOLD));
                }
            } else {
                // Vote is locked - YES/NO is not bold once locked
                if (leverState != null) {
                    Component voteStatus = leverState ? Component.translatable("hud.blood-on-the-blocktower.common.yes") : Component.translatable("hud.blood-on-the-blocktower.common.no");
                    ChatFormatting voteColor = leverState ? ChatFormatting.GREEN : ChatFormatting.RED;
                    line3Text = Component.translatable("hud.blood-on-the-blocktower.vote.you_voted")
                            .append(voteStatus.copy().withStyle(voteColor));
                } else {
                    // Storytellers don't have levers and don't vote - show nothing
                    if (isOperator) {
                        line3Text = Component.literal(""); // Empty for storyteller
                    } else {
                        line3Text = Component.translatable("hud.blood-on-the-blocktower.common.check_lever").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC);
                    }
                }
            }
        } else {
            // Before vote: Show vote position
            if (isNominee) {
                line3Text = Component.translatable("hud.blood-on-the-blocktower.vote.you_vote_last").withStyle(ChatFormatting.AQUA);
            } else {
                // Calculate vote position based on seat numbers
                int position = calculateVotePosition(client.player.getUUID(), nomineeUuid);
                if (position == 0) {
                    // Player is unseated (e.g., storyteller) - don't show vote position
                    line3Text = Component.empty();
                } else {
                    line3Text = Component.translatable("hud.blood-on-the-blocktower.vote.you_vote", getOrdinalText(position)).withStyle(ChatFormatting.AQUA);
                }
            }
        }

        // Spectators and the storyteller have no vote position or lever line, so the box
        // shrinks to two lines instead of keeping a blank third one
        boolean hasLine3 = !line3Text.getString().isEmpty();
        List<Component> lines = hasLine3 ? List.of(titleText, requirementsText, line3Text) : List.of(titleText, requirementsText);

        // Purple and double thickness while a vote is running, yellow otherwise.
        if (ClientState.voteInProgress) {
            CenteredHudBox.draw(context, client, MIN_WIDTH, CenteredHudBox.ACTIVE_BORDER, true, lines);
        } else {
            CenteredHudBox.draw(context, client, MIN_WIDTH, CenteredHudBox.IDLE_BORDER, false, lines);
        }
    }

    /**
     * Calculates what position this player will vote in based on seat numbers.
     * Voting starts one seat higher than the nominee, wrapping around.
     * For unseated nominees (like storyteller in Atheist), voting starts from seat 1.
     */
    private static int calculateVotePosition(UUID playerUuid, UUID nomineeUuid) {
        Integer playerSeat = ClientState.playerSeatNumbers.get(playerUuid);
        Integer nomineeSeat = ClientState.playerSeatNumbers.get(nomineeUuid);

        // Player must have a seat to vote
        if (playerSeat == null) {
            return 0; // Player is unseated, can't calculate position
        }

        // Build sorted list of all seated players
        List<Integer> sortedSeats = new ArrayList<>(ClientState.playerSeatNumbers.values());
        Collections.sort(sortedSeats);

        if (sortedSeats.isEmpty()) {
            return 0; // Fallback
        }

        int startIndex;
        if (nomineeSeat == null) {
            // Unseated nominee (e.g., storyteller in Atheist) - voting starts from seat 1
            startIndex = 0; // First seat in the sorted list
        } else {
            // Find nominee's index
            int nomineeIndex = sortedSeats.indexOf(nomineeSeat);
            if (nomineeIndex == -1) {
                return 0; // Fallback
            }
            // Voting starts at the next seat after nominee
            startIndex = (nomineeIndex + 1) % sortedSeats.size();
        }

        // Find player's position in the voting order
        for (int i = 0; i < sortedSeats.size(); i++) {
            int index = (startIndex + i) % sortedSeats.size();
            if (sortedSeats.get(index).equals(playerSeat)) {
                return i + 1; // Return 1-based position
            }
        }

        return 0; // Fallback
    }

    /**
     * Converts a number to its ordinal string representation (1st, 2nd, 3rd, etc.)
     */
    private static Component getOrdinalText(int number) {
        if (number % 100 >= 11 && number % 100 <= 13) {
            return Component.translatable("hud.blood-on-the-blocktower.common.ordinal_other", number);
        }

        return switch (number % 10) {
            case 1 -> Component.translatable("hud.blood-on-the-blocktower.common.ordinal_1", number);
            case 2 -> Component.translatable("hud.blood-on-the-blocktower.common.ordinal_2", number);
            case 3 -> Component.translatable("hud.blood-on-the-blocktower.common.ordinal_3", number);
            default -> Component.translatable("hud.blood-on-the-blocktower.common.ordinal_other", number);
        };
    }
}
