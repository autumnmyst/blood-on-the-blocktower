package com.autumnwind.botb.hud;

import com.autumnwind.botb.states.ClientState;
import com.autumnwind.botb.util.PlayerListUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.UUID;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Displays voting information during an active vote.
 */
public class VoteHUD {

    private static final int MIN_WIDTH = 150;

    /**
     * Renders the vote HUD if there's an active nomination or vote in progress.
     */
    public static void render(DrawContext context, MinecraftClient client) {
        // Show if there's a nomination OR a vote in progress
        UUID nomineeUuid = ClientState.currentNominee;
        if (nomineeUuid == null) {
            return; // No nomination
        }

        // Get nominee player (supports distant players)
        String nomineeName;
        AbstractClientPlayerEntity nominee = (AbstractClientPlayerEntity) client.world.getPlayerByUuid(nomineeUuid);
        if (nominee != null) {
            nomineeName = nominee.getName().getString();
        } else {
            PlayerListUtil.PlayerInfo info = PlayerListUtil.getPlayer(client, nomineeUuid);
            nomineeName = info != null ? info.name() : "Unknown";
        }

        // Check if the current player is the nominee
        boolean isNominee = client.player.getUuid().equals(nomineeUuid);

        // Check if player is an operator (for Organ Grinder mode visibility)
        boolean isOperator = client.player.hasPermissionLevel(2);

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
        boolean isDead = ClientState.playerDeathStatus.getOrDefault(client.player.getUuid(), false);
        boolean hasLostGhostVote = isDead && !ClientState.voudonModeActive
                && (ClientState.hasUsedGhostVote.getOrDefault(client.player.getUuid(), false) || ClientState.hasSecretlyUsedGhostVote);

        // Voudon-blocked: alive seated non-Voudon players have no vote while Voudon mode
        // is active. Computed from synced state so it already shows during the nomination,
        // before the vote itself starts.
        boolean isVoudonBlocked = ClientState.isVoudonBlocked
                || (ClientState.voudonModeActive && !isDead
                        && ClientState.playerSeatNumbers.containsKey(client.player.getUuid())
                        && !client.player.getUuid().equals(ClientState.voudonPlayerUuid));

        // All three lines are built before anything is drawn, so the box can be sized to
        // whichever is widest, since nominee names have no length limit.
        int screenWidth = context.getScaledWindowWidth();

        // Line 1: Title
        MutableText titleText;
        if (ClientState.voteInProgress) {
            // Show "Now voting for: " in yellow
            if (isNominee) {
                titleText = Text.literal("Now voting for: ").formatted(Formatting.YELLOW)
                        .append(Text.literal("You").formatted(Formatting.RED, Formatting.BOLD));
            } else {
                titleText = Text.literal("Now voting for: ").formatted(Formatting.YELLOW)
                        .append(Text.literal(nomineeName).formatted(Formatting.GOLD, Formatting.BOLD));
            }
        } else {
            if (isNominee) {
                titleText = Text.literal("Nominated: ").formatted(Formatting.YELLOW)
                        .append(Text.literal("You").formatted(Formatting.RED, Formatting.BOLD));
            } else {
                titleText = Text.literal("Nominated: ").formatted(Formatting.YELLOW)
                        .append(Text.literal(nomineeName).formatted(Formatting.GOLD, Formatting.BOLD));
            }
        }
        // Line 2: Vote requirements (with current count if voting)
        MutableText requirementsText;
        if (hideVoteInfo) {
            // Organ Grinder mode: hide all vote info with purple "?", but show the
            // static "≥ ceil(alive/2)" baseline threshold, which is public info and
            // doesn't leak how many votes any prior OG nominee received.
            requirementsText = Text.literal("Votes: ").formatted(Formatting.WHITE)
                    .append(Text.literal("?").formatted(Formatting.LIGHT_PURPLE, Formatting.BOLD))
                    .append(Text.literal(" (≥" + votesRequired + ")").formatted(Formatting.GRAY));
        } else if (ClientState.voteInProgress) {
            // During vote: Show as fraction
            int currentCount = ClientState.currentVoteCount;

            if (hasMFE) {
                // With MFE: show both tie and execute as fractions
                // Tie: yellow if exactly at tie count (and not at execute), otherwise white - always bold
                boolean atTie = (currentCount == votesForTie) && (currentCount < votesForTie + 1);
                MutableText tieFraction = Text.literal(currentCount + "/" + votesForTie);
                if (atTie) {
                    tieFraction = tieFraction.formatted(Formatting.YELLOW, Formatting.BOLD);
                } else {
                    tieFraction = tieFraction.formatted(Formatting.WHITE, Formatting.BOLD);
                }

                // Execute: red if at or above execute count, otherwise white - always bold
                boolean atExecute = currentCount >= votesForTie + 1;
                MutableText execFraction = Text.literal(currentCount + "/" + (votesForTie + 1));
                if (atExecute) {
                    execFraction = execFraction.formatted(Formatting.RED, Formatting.BOLD);
                } else {
                    execFraction = execFraction.formatted(Formatting.WHITE, Formatting.BOLD);
                }

                requirementsText = Text.literal("Tie: ")
                        .append(tieFraction)
                        .append(Text.literal(" | Execute: ").formatted(Formatting.WHITE))
                        .append(execFraction);
            } else {
                // Without MFE: show votes required as fraction
                // Red if at or above required, otherwise white - always bold
                boolean atRequired = currentCount >= votesRequired;
                MutableText requiredFraction = Text.literal(currentCount + "/" + votesRequired);
                if (atRequired) {
                    requiredFraction = requiredFraction.formatted(Formatting.RED, Formatting.BOLD);
                } else {
                    requiredFraction = requiredFraction.formatted(Formatting.WHITE, Formatting.BOLD);
                }

                requirementsText = Text.literal("Votes required: ")
                        .append(requiredFraction);
            }
        } else {
            // Before vote: Show requirements without current count
            if (hasMFE) {
                requirementsText = Text.literal("Tie: ")
                        .append(Text.literal(String.valueOf(votesForTie)).formatted(Formatting.YELLOW))
                        .append(Text.literal(" | Execute: ").formatted(Formatting.WHITE))
                        .append(Text.literal(String.valueOf(votesForTie + 1)).formatted(Formatting.RED));
            } else {
                requirementsText = Text.literal("Votes required: ")
                        .append(Text.literal(String.valueOf(votesRequired)).formatted(Formatting.YELLOW));
            }
        }
        // Line 3: Vote position / countdown / locked status / cannot vote. Unseated viewers
        // (the storyteller, spectators) have no lever and no place in the voting order, so
        // they get no line at all: the server sends them a zero countdown and, in Voudon mode,
        // a blocked flag, and an Atheist-nominated storyteller would otherwise read "You vote Last".
        MutableText line3Text;
        boolean seated = ClientState.playerSeatNumbers.containsKey(client.player.getUuid());

        if (!seated) {
            line3Text = Text.empty();
        } else if (hasLostGhostVote) {
            // Player has lost their ghost vote
            line3Text = Text.literal("you cannot vote").formatted(Formatting.DARK_RED, Formatting.ITALIC);
        } else if (isVoudonBlocked) {
            // Player is Voudon-blocked (alive non-Voudon in Voudon mode)
            line3Text = Text.literal("you cannot vote").formatted(Formatting.DARK_RED, Formatting.ITALIC);
        } else if (ClientState.voteInProgress) {
            // During vote: Show countdown or locked status
            UUID playerUuid = client.player.getUuid();
            Boolean leverState = ClientState.leverStates.get(playerUuid);

            if (ClientState.myVoteLockInTime > 0) {
                // Show countdown - YES/NO is bold while unlocked
                line3Text = Text.literal("Vote locks in: ")
                        .append(Text.literal(ClientState.myVoteLockInTime + "s").formatted(Formatting.AQUA));

                if (leverState != null) {
                    String voteStatus = leverState ? "YES" : "NO";
                    Formatting voteColor = leverState ? Formatting.GREEN : Formatting.RED;
                    line3Text.append(Text.literal(" - "))
                            .append(Text.literal(voteStatus).formatted(voteColor, Formatting.BOLD));
                }
            } else {
                // Vote is locked - YES/NO is not bold once locked
                if (leverState != null) {
                    String voteStatus = leverState ? "YES" : "NO";
                    Formatting voteColor = leverState ? Formatting.GREEN : Formatting.RED;
                    line3Text = Text.literal("You voted: ")
                            .append(Text.literal(voteStatus).formatted(voteColor));
                } else {
                    // Storytellers don't have levers and don't vote - show nothing
                    if (isOperator) {
                        line3Text = Text.literal(""); // Empty for storyteller
                    } else {
                        line3Text = Text.literal("Check your vote lever").formatted(Formatting.GRAY, Formatting.ITALIC);
                    }
                }
            }
        } else {
            // Before vote: Show vote position
            String positionText;
            if (isNominee) {
                positionText = "Last";
                line3Text = Text.literal("You vote " + positionText).formatted(Formatting.AQUA);
            } else {
                // Calculate vote position based on seat numbers
                int position = calculateVotePosition(client.player.getUuid(), nomineeUuid);
                if (position == 0) {
                    // Player is unseated (e.g., storyteller) - don't show vote position
                    line3Text = Text.empty();
                } else {
                    positionText = getOrdinalString(position);
                    line3Text = Text.literal("You vote " + positionText).formatted(Formatting.AQUA);
                }
            }
        }

        // Spectators and the storyteller have no vote position or lever line, so the box
        // shrinks to two lines instead of keeping a blank third one
        boolean hasLine3 = !line3Text.getString().isEmpty();
        List<Text> lines = hasLine3 ? List.of(titleText, requirementsText, line3Text) : List.of(titleText, requirementsText);

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
    private static String getOrdinalString(int number) {
        if (number % 100 >= 11 && number % 100 <= 13) {
            return number + "th";
        }

        return switch (number % 10) {
            case 1 -> number + "st";
            case 2 -> number + "nd";
            case 3 -> number + "rd";
            default -> number + "th";
        };
    }
}
