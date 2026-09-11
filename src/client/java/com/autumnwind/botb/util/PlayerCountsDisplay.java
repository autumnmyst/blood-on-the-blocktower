package com.autumnwind.botb.util;

import com.autumnwind.botb.states.ClientState;
import com.autumnwind.botb.states.StorytellerState;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.permissions.Permissions;

/**
 * Utility class for building player counts display text.
 * Used by RoleHUD, AssignRolesQuickHUD, and AssignRolesScreen.
 */
public class PlayerCountsDisplay {

    private PlayerCountsDisplay() {} // Prevent instantiation

    private static boolean isOperator() {
        Minecraft client = Minecraft.getInstance();
        return client.player != null && client.player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
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
    public static Component buildPlayerCountsText(boolean fullFormat) {
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
    public static Component buildPlayerCountsText(int playerCount, int travelerCount,
                                              RoleCounts.RoleCountInfo counts, boolean fullFormat) {
        if (counts == null) return null;

        MutableComponent countText;
        if (fullFormat) {
            countText = buildFullFormat(playerCount, counts);
            // Add traveler count in purple if there are travelers
            if (travelerCount > 0) {
                countText.append(Component.translatable("hud.blood-on-the-blocktower.player_counts.travelers", travelerCount).withStyle(ChatFormatting.LIGHT_PURPLE));
            }
        } else {
            countText = buildCompressedFormat(counts);
            // Add traveler count in purple if there are travelers
            if (travelerCount > 0) {
                countText.append(Component.literal(" : ").withStyle(ChatFormatting.WHITE))
                        .append(Component.literal(String.valueOf(travelerCount)).withStyle(ChatFormatting.LIGHT_PURPLE));
            }
        }

        return countText;
    }

    /**
     * Builds the full format: "Players: X | Townsfolk: Y Outsiders: Z Minions: W Demon: V"
     */
    private static MutableComponent buildFullFormat(int playerCount, RoleCounts.RoleCountInfo counts) {
        return Component.translatable("hud.blood-on-the-blocktower.player_counts.players", playerCount)
                .append(Component.translatable("hud.blood-on-the-blocktower.player_counts.townsfolk", counts.townsfolk()).withStyle(ChatFormatting.BLUE))
                .append(Component.translatable("hud.blood-on-the-blocktower.player_counts.outsiders", counts.outsiders()).withStyle(ChatFormatting.DARK_AQUA))
                .append(Component.translatable("hud.blood-on-the-blocktower.player_counts.minions", counts.minions()).withStyle(ChatFormatting.RED))
                .append(Component.translatable("hud.blood-on-the-blocktower.player_counts.demon", counts.demon()).withStyle(ChatFormatting.DARK_RED));
    }

    /**
     * Builds the compressed format: "Y : Z : W : V" with colored numbers
     */
    private static MutableComponent buildCompressedFormat(RoleCounts.RoleCountInfo counts) {
        return Component.literal(String.valueOf(counts.townsfolk())).withStyle(ChatFormatting.BLUE)
                .append(Component.literal(" : ").withStyle(ChatFormatting.WHITE))
                .append(Component.literal(String.valueOf(counts.outsiders())).withStyle(ChatFormatting.DARK_AQUA))
                .append(Component.literal(" : ").withStyle(ChatFormatting.WHITE))
                .append(Component.literal(String.valueOf(counts.minions())).withStyle(ChatFormatting.RED))
                .append(Component.literal(" : ").withStyle(ChatFormatting.WHITE))
                .append(Component.literal(String.valueOf(counts.demon())).withStyle(ChatFormatting.DARK_RED));
    }
}
