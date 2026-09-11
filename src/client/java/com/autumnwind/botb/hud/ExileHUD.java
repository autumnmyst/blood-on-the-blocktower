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
 * Displays exile information when a traveler is called for exile.
 */
public class ExileHUD {

    private static final int MIN_WIDTH = 220;

    /**
     * Renders the exile HUD if there's an active exile call or support vote in progress.
     */
    public static void render(GuiGraphicsExtractor context, Minecraft client) {
        // Show if there's an exile target
        UUID exileTargetUuid = ClientState.currentExileTarget;
        if (exileTargetUuid == null) {
            return; // No exile call
        }

        // Get exile target player (supports distant players)
        String targetName = getPlayerName(client, exileTargetUuid);

        // Get caller player (supports distant players)
        UUID callerUuid = ClientState.currentExileCaller;
        String callerName = callerUuid != null ? getPlayerName(client, callerUuid) : Component.translatable("gui.blood-on-the-blocktower.common.unknown_player").getString();

        // Check if the current player is the exile target
        boolean isTarget = client.player.getUUID().equals(exileTargetUuid);

        // Calculate total player count for support threshold
        int totalPlayers = ClientState.playerSeatNumbers.size();
        int supportsRequired = (int) Math.ceil(totalPlayers / 2.0);

        // All three lines are built before anything is drawn, so the box can be sized to
        // whichever is widest, since the title carries two player names.
        int screenWidth = context.guiWidth();

        // Line 1: Title - "X calls for exile of Y" or "Exile support for: Y"
        MutableComponent titleText;
        if (ClientState.exileSupportInProgress) {
            if (isTarget) {
                titleText = Component.translatable("hud.blood-on-the-blocktower.exile.support_for").withStyle(ChatFormatting.LIGHT_PURPLE)
                        .append(Component.translatable("hud.blood-on-the-blocktower.common.you").withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
            } else {
                titleText = Component.translatable("hud.blood-on-the-blocktower.exile.support_for").withStyle(ChatFormatting.LIGHT_PURPLE)
                        .append(Component.literal(targetName).withStyle(style -> style.withColor(0x9932CC)).withStyle(ChatFormatting.BOLD));
            }
        } else {
            // Check if caller is a traveler (only travelers are in the canBeExiled map)
            boolean isCallerTraveler = callerUuid != null && ClientState.canBeExiled.containsKey(callerUuid);
            int callerColor = isCallerTraveler ? 0x9932CC : 0xFFAA00; // Purple for travelers, yellow for non-travelers

            if (isTarget) {
                titleText = Component.literal(callerName).withStyle(style -> style.withColor(callerColor))
                        .append(Component.translatable("hud.blood-on-the-blocktower.exile.calls_for_exile_of").withStyle(ChatFormatting.WHITE))
                        .append(Component.translatable("hud.blood-on-the-blocktower.common.you").withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
            } else {
                // Target is always a traveler (purple)
                titleText = Component.literal(callerName).withStyle(style -> style.withColor(callerColor))
                        .append(Component.translatable("hud.blood-on-the-blocktower.exile.calls_for_exile_of").withStyle(ChatFormatting.WHITE))
                        .append(Component.literal(targetName).withStyle(style -> style.withColor(0x9932CC)));
            }
        }

        // Line 2: Support requirements (show current/required during support vote)
        MutableComponent requirementsText;
        if (ClientState.exileSupportInProgress) {
            // Show as fraction during support vote
            int currentCount = ClientState.exileSupportCount;
            boolean atRequired = currentCount >= supportsRequired;
            MutableComponent fraction = Component.literal(currentCount + "/" + supportsRequired);
            if (atRequired) {
                fraction = fraction.withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD);
            } else {
                fraction = fraction.withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD);
            }
            requirementsText = Component.translatable("hud.blood-on-the-blocktower.exile.support").append(fraction);
        } else {
            // Show requirement before support vote
            requirementsText = Component.translatable("hud.blood-on-the-blocktower.exile.support_required")
                    .append(Component.literal(String.valueOf(supportsRequired)).withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));
        }

        // Line 3: Vote position or status
        MutableComponent line3Text;
        if (ClientState.exileSupportInProgress) {
            // Show countdown or status during support vote
            UUID playerUuid = client.player.getUUID();
            // Use exile-specific lever states
            Boolean leverState = ClientState.exileLeverStates.get(playerUuid);

            // Use exile-specific countdown
            if (ClientState.exileSupportLockInTime > 0) {
                line3Text = Component.translatable("hud.blood-on-the-blocktower.exile.locks_in")
                        .append(Component.literal(ClientState.exileSupportLockInTime + "s").withStyle(ChatFormatting.AQUA));

                if (leverState != null) {
                    Component voteStatus = leverState ? Component.translatable("hud.blood-on-the-blocktower.common.yes") : Component.translatable("hud.blood-on-the-blocktower.common.no");
                    ChatFormatting voteColor = leverState ? ChatFormatting.GREEN : ChatFormatting.RED;
                    line3Text.append(Component.literal(" - "))
                            .append(voteStatus.copy().withStyle(voteColor, ChatFormatting.BOLD));
                }
            } else {
                if (leverState != null) {
                    Component voteStatus = leverState ? Component.translatable("hud.blood-on-the-blocktower.common.yes") : Component.translatable("hud.blood-on-the-blocktower.common.no");
                    ChatFormatting voteColor = leverState ? ChatFormatting.GREEN : ChatFormatting.RED;
                    line3Text = Component.translatable("hud.blood-on-the-blocktower.exile.you_supported")
                            .append(voteStatus.copy().withStyle(voteColor));
                } else {
                    boolean isOperator = client.player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
                    if (isOperator) {
                        line3Text = Component.literal(""); // Empty for storyteller
                    } else {
                        line3Text = Component.translatable("hud.blood-on-the-blocktower.common.check_lever").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC);
                    }
                }
            }
        } else {
            // Before support vote: Show vote position
            if (isTarget) {
                line3Text = Component.translatable("hud.blood-on-the-blocktower.exile.you_support_last").withStyle(ChatFormatting.AQUA);
            } else {
                int position = calculateSupportPosition(client.player.getUUID(), exileTargetUuid);
                if (position == 0) {
                    line3Text = Component.empty();
                } else {
                    line3Text = Component.translatable("hud.blood-on-the-blocktower.exile.you_support", getOrdinalText(position)).withStyle(ChatFormatting.AQUA);
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

    /**
     * Gets player name with fallback for distant players.
     */
    private static String getPlayerName(Minecraft client, UUID playerUuid) {
        AbstractClientPlayer player = (AbstractClientPlayer) client.level.getPlayerByUUID(playerUuid);
        if (player != null) return player.getName().getString();
        PlayerListUtil.PlayerInfo info = PlayerListUtil.getPlayer(client, playerUuid);
        return info != null ? info.name() : Component.translatable("gui.blood-on-the-blocktower.common.unknown_player").getString();
    }
}
