package com.autumnwind.botb.hud;

import com.autumnwind.botb.util.Role;
import com.autumnwind.botb.states.ClientState;
import com.autumnwind.botb.states.StorytellerState;
import com.autumnwind.botb.util.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.util.ARGB;

/**
 * Manages the madness HUD overlay for both storyteller and player views.
 */
public class MadnessHUD {

    private static final int ICON_SIZE = 20;
    private static final int ICON_PADDING = 4;
    private static final int START_X = 10;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int MAX_TEXT_WIDTH = 250;
    private static final int EXPANDED_LIST_BOTTOM_PADDING = 40;

    /**
     * Detects all active madnesses from storyteller's reminder state.
     * This is called storyteller-side to build the madness list for each player.
     */
    public static Map<UUID, List<Madness>> detectMadnessesFromReminders() {
        Map<UUID, List<Madness>> playerMadnesses = new HashMap<>();

        for (Map.Entry<UUID, PendingRoleAssignment> entry : StorytellerState.PENDING_ROLES.entrySet()) {
            UUID playerUuid = entry.getKey();
            PendingRoleAssignment assignment = entry.getValue();
            Role assignedRole = assignment.role();
            List<Reminder> reminders = StorytellerState.REMINDERS.getOrDefault(playerUuid, Collections.emptyList());
            List<Madness> madnesses = new ArrayList<>();

            // Check for Pixie madness
            if (assignedRole == Role.PIXIE || hasAssociatedRole(reminders, Role.PIXIE)) {
                // Count how many PIXIE specifically associated role reminders exist
                long pixieReminderCount = reminders.stream()
                    .filter(r -> r.role().isPresent())
                    .filter(r -> {
                        Role role = r.role().get();
                        return role == Role.PIXIE &&
                               Reminders.isRoleMarker(r.text(), role);
                    })
                    .count();

                // Look for Pixie's associated role (the townsfolk they're mad about)
                for (Reminder reminder : reminders) {
                    if (reminder.role().isPresent()) {
                        Role role = reminder.role().get();
                        // Skip Pixie roles if they have less than 2 pixie reminders specifically
                        if (role == Role.PIXIE && pixieReminderCount < 2) {
                            continue;
                        }
                        if (role.getType() == RoleType.TOWNSFOLK &&
                            Reminders.isRoleMarker(reminder.text(), role)) {
                            // Player is mad they are this townsfolk role
                            // Note: "Has Ability" reminder is for storyteller tracking only
                            madnesses.add(new Madness.PixieMadness(role));
                            break;
                        }
                    }
                }
            }

            // Check for Harpy madness (any player with player reminders)
            for (Reminder reminder : reminders) {
                if (reminder.isPlayerReminder()) {
                    madnesses.add(new Madness.HarpyMadness(reminder.playerUuid().get()));
                }
            }

            // Check for Cerenovus madness (mad role reminders)
            for (Reminder reminder : reminders) {
                if (reminder.isMadRoleReminder()) {
                    madnesses.add(new Madness.CerenovusMadness(reminder.role().get()));
                }
            }

            // Check for Mutant madness (storyteller only, not sent to player)
            if (assignedRole == Role.MUTANT || hasAssociatedRole(reminders, Role.MUTANT)) {
                madnesses.add(new Madness.MutantMadness());
            }

            if (!madnesses.isEmpty()) {
                playerMadnesses.put(playerUuid, madnesses);
            }
        }

        return playerMadnesses;
    }

    /**
     * Checks if a player has a specific role as an associated role reminder.
     */
    private static boolean hasAssociatedRole(List<Reminder> reminders, Role role) {
        return reminders.stream().anyMatch(r ->
            r.role().isPresent() &&
            r.role().get() == role &&
            Reminders.isRoleMarker(r.text(), role));
    }

    /**
     * Renders the madness HUD.
     * For storyteller: shows all players' madnesses
     * For players: shows their own madnesses
     */
    public static void render(GuiGraphicsExtractor context, Minecraft client) {
        if (client == null || client.player == null) return;

        boolean isOperator = client.player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);

        if (isOperator) {
            renderStorytellerView(context, client);
        } else {
            renderPlayerView(context, client);
        }
    }

    /**
     * Renders the storyteller's view: all active madnesses.
     */
    private static void renderStorytellerView(GuiGraphicsExtractor context, Minecraft client) {
        Map<UUID, List<Madness>> allMadnesses = detectMadnessesFromReminders();
        if (allMadnesses.isEmpty()) return;

        int screenHeight = context.guiHeight();
        boolean isExpanded = ClientState.isRoleHudVisible;
        int currentY = screenHeight - (isExpanded ? EXPANDED_LIST_BOTTOM_PADDING : 50);

        // Iterate through all players with madnesses (from bottom to top)
        List<Map.Entry<UUID, List<Madness>>> entries = new ArrayList<>(allMadnesses.entrySet());

        for (int i = entries.size() - 1; i >= 0; i--) {
            Map.Entry<UUID, List<Madness>> entry = entries.get(i);
            UUID playerUuid = entry.getKey();
            List<Madness> madnesses = entry.getValue();

            for (int j = madnesses.size() - 1; j >= 0; j--) {
                Madness madness = madnesses.get(j);

                // Don't render PixieMadness if the player has "Has Ability" reminder (storyteller-side only)
                if (madness instanceof Madness.PixieMadness) {
                    List<Reminder> playerReminders = StorytellerState.REMINDERS.getOrDefault(playerUuid, Collections.emptyList());
                    boolean hasAbility = playerReminders.stream()
                        .anyMatch(r -> r.text().equals(Reminders.HAS_ABILITY) &&
                                     r.role().isPresent() && r.role().get() == Role.PIXIE);
                    if (hasAbility) {
                        continue; // Skip rendering this madness
                    }
                }

                int rowHeight = renderStorytellerMadnessRow(context, client, madness, playerUuid, START_X, currentY, isExpanded);
                currentY -= rowHeight + ICON_PADDING;
            }
        }
    }

    /**
     * Renders a single madness row in storyteller view.
     * Returns the height of the rendered row.
     */
    private static int renderStorytellerMadnessRow(GuiGraphicsExtractor context, Minecraft client,
                                                    Madness madness, UUID playerUuid, int x, int y, boolean isExpanded) {
        int currentX = x;

        // Get player info (supports distant players)
        AbstractClientPlayer player = (AbstractClientPlayer) client.level.getPlayerByUUID(playerUuid);
        String playerName;
        if (player != null) {
            playerName = player.getName().getString();
        } else {
            PlayerListUtil.PlayerInfo info = PlayerListUtil.getPlayer(client, playerUuid);
            playerName = info != null ? info.name() : Component.translatable("gui.blood-on-the-blocktower.common.unknown_player").getString();
        }

        if (isExpanded) {
            // Expanded view: 3 icons horizontally + madness text
            int textWidth = MAX_TEXT_WIDTH;
            int padding = 6;
            int iconRowWidth = ICON_SIZE * 3 + ICON_PADDING * 2;

            // Calculate content
            String madnessText = getMadnessTextForStoryteller(madness, client, playerName);
            List<Component> wrappedLines = wrapText(client, madnessText, textWidth - padding * 2);
            int textHeight = wrappedLines.size() * (client.font.lineHeight + 2);
            int contentHeight = Math.max(ICON_SIZE, textHeight);
            int boxHeight = contentHeight + padding * 2;
            int boxWidth = iconRowWidth + padding + textWidth;

            // Draw backdrop
            int bgColor = ARGB.color(191, 0, 0, 0);
            context.fill(x, y - boxHeight, x + boxWidth, y, bgColor);

            // Draw 3 icons in a row
            int iconY = y - boxHeight + padding;
            renderStorytellerIconsExpanded(context, client, madness, playerUuid, x + padding, iconY);

            // Draw text
            int textX = x + iconRowWidth + padding * 2;
            int textY = y - boxHeight + padding;
            for (Component line : wrappedLines) {
                context.text(client.font, line, textX, textY, TEXT_COLOR);
                textY += client.font.lineHeight + 2;
            }

            return boxHeight;
        } else {
            // Compact view: 3 icons horizontally
            renderStorytellerIconsCompact(context, client, madness, playerUuid, currentX, y);
            return ICON_SIZE;
        }
    }

    /**
     * Renders storyteller icons in expanded mode (3 icons in a row).
     */
    private static void renderStorytellerIconsExpanded(GuiGraphicsExtractor context, Minecraft client,
                                                        Madness madness, UUID playerUuid, int x, int y) {
        int currentX = x;

        // Icon 1: Madness type
        Role madnessRole = getMadnessRole(madness);
        Identifier madnessIcon = madnessRole.getIcon();
        context.blit(RenderPipelines.GUI_TEXTURED, madnessIcon, currentX, y, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
        currentX += ICON_SIZE + ICON_PADDING;

        // Icon 2: Player head (supports distant players + disconnect fallback)
        PlayerListUtil.drawPlayerHead(context, client, playerUuid, currentX, y, ICON_SIZE);
        currentX += ICON_SIZE + ICON_PADDING;

        // Icon 3: Target
        switch (madness) {
            case Madness.PixieMadness pixie -> {
                Identifier roleIcon = pixie.townsfolkRole().getIcon();
                context.blit(RenderPipelines.GUI_TEXTURED, roleIcon, currentX, y, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
            }
            case Madness.HarpyMadness harpy -> {
                PlayerListUtil.drawPlayerHead(context, client, harpy.targetPlayerUuid(), currentX, y, ICON_SIZE);
            }
            case Madness.CerenovusMadness cerenovus -> {
                Identifier roleIcon = cerenovus.madRole().getIcon();
                context.blit(RenderPipelines.GUI_TEXTURED, roleIcon, currentX, y, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
            }
            case Madness.MutantMadness mutant -> {
                // No target icon for mutant
            }
        }
    }

    /**
     * Renders storyteller icons in compact mode (3 icons horizontally).
     */
    private static void renderStorytellerIconsCompact(GuiGraphicsExtractor context, Minecraft client,
                                                       Madness madness, UUID playerUuid, int x, int y) {
        int currentX = x;

        // Icon 1: Madness type
        Role madnessRole = getMadnessRole(madness);
        Identifier madnessIcon = madnessRole.getIcon();
        context.blit(RenderPipelines.GUI_TEXTURED, madnessIcon, currentX, y, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
        currentX += ICON_SIZE + ICON_PADDING;

        // Icon 2: Player head (supports distant players + disconnect fallback)
        PlayerListUtil.drawPlayerHead(context, client, playerUuid, currentX, y, ICON_SIZE);
        currentX += ICON_SIZE + ICON_PADDING;

        // Icon 3: Target
        switch (madness) {
            case Madness.PixieMadness pixie -> {
                Identifier roleIcon = pixie.townsfolkRole().getIcon();
                context.blit(RenderPipelines.GUI_TEXTURED, roleIcon, currentX, y, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
            }
            case Madness.HarpyMadness harpy -> {
                PlayerListUtil.drawPlayerHead(context, client, harpy.targetPlayerUuid(), currentX, y, ICON_SIZE);
            }
            case Madness.CerenovusMadness cerenovus -> {
                Identifier roleIcon = cerenovus.madRole().getIcon();
                context.blit(RenderPipelines.GUI_TEXTURED, roleIcon, currentX, y, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
            }
            case Madness.MutantMadness mutant -> {
                // No third icon for mutant
            }
        }
    }

    /**
     * Gets the madness text for storyteller expanded view.
     */
    private static String getMadnessTextForStoryteller(Madness madness, Minecraft client, String playerName) {
        return switch (madness) {
            case Madness.PixieMadness pixie -> Component.translatable("hud.blood-on-the-blocktower.madness.storyteller.pixie", playerName, pixie.townsfolkRole().getDisplayName()).getString();
            case Madness.HarpyMadness harpy -> {
                String targetName = getPlayerName(client, harpy.targetPlayerUuid());
                yield Component.translatable("hud.blood-on-the-blocktower.madness.storyteller.harpy", playerName, targetName).getString();
            }
            case Madness.CerenovusMadness cerenovus -> Component.translatable("hud.blood-on-the-blocktower.madness.storyteller.cerenovus", playerName, cerenovus.madRole().getDisplayName()).getString();
            case Madness.MutantMadness mutant -> Component.translatable("hud.blood-on-the-blocktower.madness.storyteller.mutant", playerName).getString();
        };
    }

    /**
     * Gets player name with fallback for distant players.
     */
    private static String getPlayerName(Minecraft client, UUID playerUuid) {
        AbstractClientPlayer player = (AbstractClientPlayer) client.level.getPlayerByUUID(playerUuid);
        if (player != null) return player.getName().getString();
        PlayerListUtil.PlayerInfo info = PlayerListUtil.getPlayerOrCached(client, playerUuid);
        return info != null ? info.name() : Component.translatable("gui.blood-on-the-blocktower.common.unknown_player").getString();
    }

    /**
     * Gets the role associated with this madness type.
     */
    private static Role getMadnessRole(Madness madness) {
        return switch (madness.getType()) {
            case PIXIE -> Role.PIXIE;
            case HARPY -> Role.HARPY;
            case CERENOVUS -> Role.CERENOVUS;
            case MUTANT -> Role.MUTANT;
        };
    }

    /**
     * Renders the player's view: their own madnesses.
     */
    private static void renderPlayerView(GuiGraphicsExtractor context, Minecraft client) {
        List<Madness> myMadnesses = ClientState.myMadnesses;
        if (myMadnesses == null || myMadnesses.isEmpty()) return;

        int screenHeight = context.guiHeight();
        boolean isExpanded = ClientState.isRoleHudVisible;
        int currentY = screenHeight - (isExpanded ? EXPANDED_LIST_BOTTOM_PADDING : 50);

        for (int i = myMadnesses.size() - 1; i >= 0; i--) {
            Madness madness = myMadnesses.get(i);
            int rowHeight = renderPlayerMadnessRow(context, client, madness, START_X, currentY, isExpanded);
            currentY -= rowHeight + ICON_PADDING;
        }
    }

    /**
     * Renders a single madness row in player view.
     * Returns the height of the rendered row.
     */
    private static int renderPlayerMadnessRow(GuiGraphicsExtractor context, Minecraft client,
                                               Madness madness, int x, int y, boolean isExpanded) {
        if (isExpanded) {
            // Expanded view: 2 icons horizontally + text with backdrop
            int textWidth = MAX_TEXT_WIDTH;
            int padding = 6;
            int iconRowWidth = ICON_SIZE * 2 + ICON_PADDING;

            // Calculate content
            String madnessText = getMadnessTextForPlayer(madness, client);
            List<Component> wrappedLines = wrapText(client, madnessText, textWidth - padding * 2);
            int textHeight = wrappedLines.size() * (client.font.lineHeight + 2);
            int contentHeight = Math.max(ICON_SIZE, textHeight);
            int boxHeight = contentHeight + padding * 2;
            int boxWidth = iconRowWidth + padding + textWidth;

            // Draw backdrop
            int bgColor = ARGB.color(191, 0, 0, 0);
            context.fill(x, y - boxHeight, x + boxWidth, y, bgColor);

            // Draw 2 icons in a row
            int iconY = y - boxHeight + padding;
            renderPlayerIconsExpanded(context, client, madness, x + padding, iconY);

            // Draw text
            int textX = x + iconRowWidth + padding * 2;
            int textY = y - boxHeight + padding;
            for (Component line : wrappedLines) {
                context.text(client.font, line, textX, textY, TEXT_COLOR);
                textY += client.font.lineHeight + 2;
            }

            return boxHeight;
        } else {
            // Compact view: 2 icons horizontally
            renderPlayerIconsCompact(context, client, madness, x, y);
            return ICON_SIZE;
        }
    }

    /**
     * Renders player icons in expanded mode (2 icons horizontally).
     */
    private static void renderPlayerIconsExpanded(GuiGraphicsExtractor context, Minecraft client,
                                                   Madness madness, int x, int y) {
        int currentX = x;

        // Icon 1: Madness-causing role
        Role madnessRole = getMadnessRole(madness);
        Identifier madnessIcon = madnessRole.getIcon();
        context.blit(RenderPipelines.GUI_TEXTURED, madnessIcon, currentX, y, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
        currentX += ICON_SIZE + ICON_PADDING;

        // Icon 2: Target
        switch (madness) {
            case Madness.PixieMadness pixie -> {
                Identifier roleIcon = pixie.townsfolkRole().getIcon();
                context.blit(RenderPipelines.GUI_TEXTURED, roleIcon, currentX, y, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
            }
            case Madness.HarpyMadness harpy -> {
                PlayerListUtil.drawPlayerHead(context, client, harpy.targetPlayerUuid(), currentX, y, ICON_SIZE);
            }
            case Madness.CerenovusMadness cerenovus -> {
                Identifier roleIcon = cerenovus.madRole().getIcon();
                context.blit(RenderPipelines.GUI_TEXTURED, roleIcon, currentX, y, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
            }
            case Madness.MutantMadness mutant -> {
                // Should not be sent to player
            }
        }
    }

    /**
     * Renders player icons in compact mode (2 icons horizontally).
     */
    private static void renderPlayerIconsCompact(GuiGraphicsExtractor context, Minecraft client,
                                                   Madness madness, int x, int y) {
        int currentX = x;

        // Icon 1: Madness-causing role
        Role madnessRole = getMadnessRole(madness);
        Identifier madnessIcon = madnessRole.getIcon();
        context.blit(RenderPipelines.GUI_TEXTURED, madnessIcon, currentX, y, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
        currentX += ICON_SIZE + ICON_PADDING;

        // Icon 2: Target
        switch (madness) {
            case Madness.PixieMadness pixie -> {
                Identifier roleIcon = pixie.townsfolkRole().getIcon();
                context.blit(RenderPipelines.GUI_TEXTURED, roleIcon, currentX, y, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
            }
            case Madness.HarpyMadness harpy -> {
                PlayerListUtil.drawPlayerHead(context, client, harpy.targetPlayerUuid(), currentX, y, ICON_SIZE);
            }
            case Madness.CerenovusMadness cerenovus -> {
                Identifier roleIcon = cerenovus.madRole().getIcon();
                context.blit(RenderPipelines.GUI_TEXTURED, roleIcon, currentX, y, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
            }
            case Madness.MutantMadness mutant -> {
                // Should not be sent to player
            }
        }
    }

    /**
     * Gets the madness text for player view.
     */
    private static String getMadnessTextForPlayer(Madness madness, Minecraft client) {
        return switch (madness) {
            case Madness.PixieMadness pixie -> pixie.getPlayerText();
            case Madness.HarpyMadness harpy -> {
                String targetName = getPlayerName(client, harpy.targetPlayerUuid());
                yield harpy.getPlayerText(targetName);
            }
            case Madness.CerenovusMadness cerenovus -> cerenovus.getPlayerText();
            case Madness.MutantMadness mutant -> ""; // Never sent to players
        };
    }

    /**
     * Wraps text to fit within a given width.
     */
    private static List<Component> wrapText(Minecraft client, String text, int maxWidth) {
        List<Component> lines = new ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();

        for (String word : words) {
            String testLine = currentLine.length() == 0 ? word : currentLine + " " + word;
            if (client.font.width(testLine) <= maxWidth) {
                if (currentLine.length() > 0) {
                    currentLine.append(" ");
                }
                currentLine.append(word);
            } else {
                if (currentLine.length() > 0) {
                    lines.add(Component.literal(currentLine.toString()));
                    currentLine = new StringBuilder(word);
                } else {
                    // Single word is too long, just add it anyway
                    lines.add(Component.literal(word));
                }
            }
        }

        if (currentLine.length() > 0) {
            lines.add(Component.literal(currentLine.toString()));
        }

        return lines;
    }
}
