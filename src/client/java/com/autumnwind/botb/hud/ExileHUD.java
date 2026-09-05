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
 * Displays exile information when a traveler is called for exile.
 */
public class ExileHUD {

    private static final int MIN_WIDTH = 220;

    /**
     * Renders the exile HUD if there's an active exile call or support vote in progress.
     */
    public static void render(DrawContext context, MinecraftClient client) {
        // Show if there's an exile target
        UUID exileTargetUuid = ClientState.currentExileTarget;
        if (exileTargetUuid == null) {
            return; // No exile call
        }

        // Get exile target player (supports distant players)
        String targetName = getPlayerName(client, exileTargetUuid);

        // Get caller player (supports distant players)
        UUID callerUuid = ClientState.currentExileCaller;
        String callerName = callerUuid != null ? getPlayerName(client, callerUuid) : "Unknown";

        // Check if the current player is the exile target
        boolean isTarget = client.player.getUuid().equals(exileTargetUuid);

        // Calculate total player count for support threshold
        int totalPlayers = ClientState.playerSeatNumbers.size();
        int supportsRequired = (int) Math.ceil(totalPlayers / 2.0);

        // All three lines are built before anything is drawn, so the box can be sized to
        // whichever is widest, since the title carries two player names.
        int screenWidth = context.getScaledWindowWidth();

        // Line 1: Title - "X calls for exile of Y" or "Exile support for: Y"
        MutableText titleText;
        if (ClientState.exileSupportInProgress) {
            if (isTarget) {
                titleText = Text.literal("Exile support for: ").formatted(Formatting.LIGHT_PURPLE)
                        .append(Text.literal("You").formatted(Formatting.RED, Formatting.BOLD));
            } else {
                titleText = Text.literal("Exile support for: ").formatted(Formatting.LIGHT_PURPLE)
                        .append(Text.literal(targetName).styled(style -> style.withColor(0x9932CC)).formatted(Formatting.BOLD));
            }
        } else {
            // Check if caller is a traveler (only travelers are in the canBeExiled map)
            boolean isCallerTraveler = callerUuid != null && ClientState.canBeExiled.containsKey(callerUuid);
            int callerColor = isCallerTraveler ? 0x9932CC : 0xFFAA00; // Purple for travelers, yellow for non-travelers

            if (isTarget) {
                titleText = Text.literal(callerName).styled(style -> style.withColor(callerColor))
                        .append(Text.literal(" calls for exile of ").formatted(Formatting.WHITE))
                        .append(Text.literal("You").formatted(Formatting.RED, Formatting.BOLD));
            } else {
                // Target is always a traveler (purple)
                titleText = Text.literal(callerName).styled(style -> style.withColor(callerColor))
                        .append(Text.literal(" calls for exile of ").formatted(Formatting.WHITE))
                        .append(Text.literal(targetName).styled(style -> style.withColor(0x9932CC)));
            }
        }

        // Line 2: Support requirements (show current/required during support vote)
        MutableText requirementsText;
        if (ClientState.exileSupportInProgress) {
            // Show as fraction during support vote
            int currentCount = ClientState.exileSupportCount;
            boolean atRequired = currentCount >= supportsRequired;
            MutableText fraction = Text.literal(currentCount + "/" + supportsRequired);
            if (atRequired) {
                fraction = fraction.formatted(Formatting.LIGHT_PURPLE, Formatting.BOLD);
            } else {
                fraction = fraction.formatted(Formatting.WHITE, Formatting.BOLD);
            }
            requirementsText = Text.literal("Support: ").append(fraction);
        } else {
            // Show requirement before support vote
            requirementsText = Text.literal("Support required: ")
                    .append(Text.literal(String.valueOf(supportsRequired)).formatted(Formatting.LIGHT_PURPLE, Formatting.BOLD));
        }

        // Line 3: Vote position or status
        MutableText line3Text;
        if (ClientState.exileSupportInProgress) {
            // Show countdown or status during support vote
            UUID playerUuid = client.player.getUuid();
            // Use exile-specific lever states
            Boolean leverState = ClientState.exileLeverStates.get(playerUuid);

            // Use exile-specific countdown
            if (ClientState.exileSupportLockInTime > 0) {
                line3Text = Text.literal("Support locks in: ")
                        .append(Text.literal(ClientState.exileSupportLockInTime + "s").formatted(Formatting.AQUA));

                if (leverState != null) {
                    String voteStatus = leverState ? "YES" : "NO";
                    Formatting voteColor = leverState ? Formatting.GREEN : Formatting.RED;
                    line3Text.append(Text.literal(" - "))
                            .append(Text.literal(voteStatus).formatted(voteColor, Formatting.BOLD));
                }
            } else {
                if (leverState != null) {
                    String voteStatus = leverState ? "YES" : "NO";
                    Formatting voteColor = leverState ? Formatting.GREEN : Formatting.RED;
                    line3Text = Text.literal("You supported: ")
                            .append(Text.literal(voteStatus).formatted(voteColor));
                } else {
                    boolean isOperator = client.player.hasPermissionLevel(2);
                    if (isOperator) {
                        line3Text = Text.literal(""); // Empty for storyteller
                    } else {
                        line3Text = Text.literal("Check your vote lever").formatted(Formatting.GRAY, Formatting.ITALIC);
                    }
                }
            }
        } else {
            // Before support vote: Show vote position
            if (isTarget) {
                line3Text = Text.literal("You support last").formatted(Formatting.AQUA);
            } else {
                int position = calculateSupportPosition(client.player.getUuid(), exileTargetUuid);
                if (position == 0) {
                    line3Text = Text.empty();
                } else {
                    String positionText = getOrdinalString(position);
                    line3Text = Text.literal("You support " + positionText).formatted(Formatting.AQUA);
                }
            }
        }
        // Purple, double thickness while the support vote runs.
        CenteredHudBox.draw(context, client, MIN_WIDTH, CenteredHudBox.ACTIVE_BORDER, ClientState.exileSupportInProgress,
                List.of(titleText, requirementsText, line3Text));
    }

    /**
     * Calculates what position this player will support in based on seat numbers.
     * Support starts one seat higher than the exile target, wrapping around.
     */
    private static int calculateSupportPosition(UUID playerUuid, UUID targetUuid) {
        Integer playerSeat = ClientState.playerSeatNumbers.get(playerUuid);
        Integer targetSeat = ClientState.playerSeatNumbers.get(targetUuid);

        // Player must have a seat to vote
        if (playerSeat == null) {
            return 0;
        }

        // Build sorted list of all seated players
        List<Integer> sortedSeats = new ArrayList<>(ClientState.playerSeatNumbers.values());
        Collections.sort(sortedSeats);

        if (sortedSeats.isEmpty()) {
            return 0;
        }

        int startIndex;
        if (targetSeat == null) {
            startIndex = 0;
        } else {
            int targetIndex = sortedSeats.indexOf(targetSeat);
            if (targetIndex == -1) {
                return 0;
            }
            startIndex = (targetIndex + 1) % sortedSeats.size();
        }

        for (int i = 0; i < sortedSeats.size(); i++) {
            int index = (startIndex + i) % sortedSeats.size();
            if (sortedSeats.get(index).equals(playerSeat)) {
                return i + 1;
            }
        }

        return 0;
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

    /**
     * Gets player name with fallback for distant players.
     */
    private static String getPlayerName(MinecraftClient client, UUID playerUuid) {
        AbstractClientPlayerEntity player = (AbstractClientPlayerEntity) client.world.getPlayerByUuid(playerUuid);
        if (player != null) return player.getName().getString();
        PlayerListUtil.PlayerInfo info = PlayerListUtil.getPlayer(client, playerUuid);
        return info != null ? info.name() : "Unknown";
    }
}
