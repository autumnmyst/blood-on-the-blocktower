package com.autumnwind.botb.util;

import com.autumnwind.botb.states.ClientState;
import com.autumnwind.botb.states.StorytellerState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Utility class for building player counts display text.
 * Used by RoleHUD, AssignRolesQuickHUD, and AssignRolesScreen.
 */
public class PlayerCountsDisplay {

    private PlayerCountsDisplay() {} // Prevent instantiation

    private static boolean isOperator() {
        MinecraftClient client = MinecraftClient.getInstance();
        return client.player != null && client.player.hasPermissionLevel(2);
    }

    /** Storytellers count from their own grimoire; players use what the server sent them. */
    public static int playerCount() {
        return isOperator() ? StorytellerState.PENDING_ROLES.size() : ClientState.activePlayerCount;
    }

    public static int travelerCount() {
        if (!isOperator()) return ClientState.travelerCount;
        return (int) StorytellerState.PENDING_ROLES.keySet().stream().filter(StorytellerState::isTraveler).count();
    }

    /**
     * Builds the player counts text in the appropriate format.
     * @param fullFormat true for expanded format, false for compressed format
     * @return The formatted Text, or null if counts are unavailable
     */
    public static Text buildPlayerCountsText(boolean fullFormat) {
        int playerCount = playerCount();
        int travelerCount = travelerCount();
        int nonTravelerCount = playerCount - travelerCount;

        // Use non-traveler count for role counts lookup (like SetupValidator does)
        RoleCounts.RoleCountInfo counts = RoleCounts.getCounts(nonTravelerCount);
        if (counts == null) return null;

        return buildPlayerCountsText(playerCount, travelerCount, counts, fullFormat);
    }

    /**
     * Builds the player counts text with explicit values.
     * @param playerCount Total player count
     * @param travelerCount Traveler count
     * @param counts Role counts info (based on non-traveler count)
     * @param fullFormat true for expanded format, false for compressed format
     * @return The formatted Text
     */
    public static Text buildPlayerCountsText(int playerCount, int travelerCount,
                                              RoleCounts.RoleCountInfo counts, boolean fullFormat) {
        if (counts == null) return null;

        MutableText countText;
        if (fullFormat) {
            countText = buildFullFormat(playerCount, counts);
            // Add traveler count in purple if there are travelers
            if (travelerCount > 0) {
                countText.append(Text.literal(" Travelers: " + travelerCount).formatted(Formatting.LIGHT_PURPLE));
            }
        } else {
            countText = buildCompressedFormat(counts);
            // Add traveler count in purple if there are travelers
            if (travelerCount > 0) {
                countText.append(Text.literal(" : ").formatted(Formatting.WHITE))
                        .append(Text.literal(String.valueOf(travelerCount)).formatted(Formatting.LIGHT_PURPLE));
            }
        }

        return countText;
    }

    /**
     * Builds the full format: "Players: X | Townsfolk: Y Outsiders: Z Minions: W Demon: V"
     */
    private static MutableText buildFullFormat(int playerCount, RoleCounts.RoleCountInfo counts) {
        return Text.literal("Players: " + playerCount + " | ")
                .append(Text.literal("Townsfolk: " + counts.townsfolk() + " ").formatted(Formatting.BLUE))
                .append(Text.literal("Outsiders: " + counts.outsiders() + " ").formatted(Formatting.DARK_AQUA))
                .append(Text.literal("Minions: " + counts.minions() + " ").formatted(Formatting.RED))
                .append(Text.literal("Demon: " + counts.demon()).formatted(Formatting.DARK_RED));
    }

    /**
     * Builds the compressed format: "Y : Z : W : V" with colored numbers
     */
    private static MutableText buildCompressedFormat(RoleCounts.RoleCountInfo counts) {
        return Text.literal(String.valueOf(counts.townsfolk())).formatted(Formatting.BLUE)
                .append(Text.literal(" : ").formatted(Formatting.WHITE))
                .append(Text.literal(String.valueOf(counts.outsiders())).formatted(Formatting.DARK_AQUA))
                .append(Text.literal(" : ").formatted(Formatting.WHITE))
                .append(Text.literal(String.valueOf(counts.minions())).formatted(Formatting.RED))
                .append(Text.literal(" : ").formatted(Formatting.WHITE))
                .append(Text.literal(String.valueOf(counts.demon())).formatted(Formatting.DARK_RED));
    }
}
