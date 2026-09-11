package com.autumnwind.botb.hud;

import com.autumnwind.botb.states.ClientState;
import com.autumnwind.botb.states.StorytellerState;
import com.autumnwind.botb.util.PendingRoleAssignment;
import com.autumnwind.botb.util.Role;
import com.autumnwind.botb.util.RoleType;
import com.autumnwind.botb.hud.nightorderhud.RoleHelpers;
import com.autumnwind.botb.util.CustomRole;
import com.autumnwind.botb.util.Reminder;
import com.autumnwind.botb.util.Script;
import com.autumnwind.botb.util.ScriptRole;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import com.autumnwind.botb.gui.GameEndHoldingScreen;
import com.autumnwind.botb.util.AlignmentOverride;
import com.autumnwind.botb.util.PlayerListUtil;
import com.autumnwind.botb.util.UrlTextureLoader;

/**
 * Displays the game end animation with victory/defeat screen and role reveals.
 */
public class GameEndAnimationHUD {

    private static boolean isAnimating = false;
    private static boolean goodWins = false;
    private static long animationStartTime = 0;
    private static final int ROLE_ICON_SIZE = 32;
    private static final int HEAD_ICON_SIZE = 24;
    private static final int PLAYERS_PER_ROW = 6;

    // Screen and HUD management flags
    private static boolean hasClosedCurrentScreen = false;
    private static boolean hasOpenedHoldingScreen = false;
    private static boolean hasClosedHoldingScreen = false;

    // Animation timing (in milliseconds)
    private static final int FADE_TO_BLACK_DURATION = 1000;  // 0-1s: Fade to black
    private static final int BLACK_SCREEN_DELAY = 400;        // 1-1.4s: Black screen before title
    private static final int SUBTITLE_DELAY = 1100;           // 1.4-2.5s: Delay between title and subtitle
    private static final int TITLE_WAIT_DURATION = 3600;      // 1.4-5s: Wait before player entries (from title)
    private static final int PLAYER_FADE_DURATION = 1000;
    private static final int END_WAIT_DURATION = 5000;
    private static final int FADE_OUT_DURATION = 3000;

    /**
     * Starts the game end animation.
     */
    public static void startAnimation(boolean goodTeamWins) {
        isAnimating = true;
        goodWins = goodTeamWins;
        animationStartTime = System.currentTimeMillis();

        // Reset screen management flags
        hasClosedCurrentScreen = false;
        hasOpenedHoldingScreen = false;
        hasClosedHoldingScreen = false;
    }

    /**
     * Checks if the animation is currently running.
     */
    public static boolean isAnimating() {
        return isAnimating;
    }

    /**
     * Returns true while the animation is in its initial fade-to-black phase.
     * The world is still partially visible during this phase.
     */
    public static boolean isInFadeToBlack() {
        if (!isAnimating) return false;
        long elapsed = System.currentTimeMillis() - animationStartTime;
        return elapsed < FADE_TO_BLACK_DURATION;
    }

    /**
     * Resets the animation state (called on disconnect/world leave).
     * Ensures holding screen is closed and state is clean.
     */
    public static void reset() {
        isAnimating = false;
        hasClosedCurrentScreen = false;
        hasOpenedHoldingScreen = false;
        hasClosedHoldingScreen = false;
        ClientState.gameEnding = false;

        // Close holding screen if it's open
        Minecraft client = Minecraft.getInstance();
        if (client != null && client.screen instanceof GameEndHoldingScreen) {
            client.setScreen(null);
        }
    }

    /**
     * Renders the game end animation.
     */
    public static void render(GuiGraphics context, Minecraft client) {
        if (!isAnimating) {
            return;
        }

        long elapsed = System.currentTimeMillis() - animationStartTime;

        // Calculate current animation phase
        int phase1End = FADE_TO_BLACK_DURATION;
        int phase2End = phase1End + BLACK_SCREEN_DELAY;
        int phase3Start = phase2End + TITLE_WAIT_DURATION;

        // Get all player data
        List<PlayerData> players = getPlayerDataSorted();
        int totalPlayers = players.size();
        int totalFadeTime = totalPlayers * PLAYER_FADE_DURATION;

        int phase3End = phase3Start + totalFadeTime;
        int phase4End = phase3End + END_WAIT_DURATION;
        int phase5End = phase4End + FADE_OUT_DURATION;

        // Screen management based on animation phase
        manageScreenTransitions(client, elapsed, phase4End);

        // Phase 1: Fade to black (0-1s)
        float blackAlpha = 0;
        if (elapsed < phase1End) {
            blackAlpha = (float) elapsed / FADE_TO_BLACK_DURATION;
        }
        // Phase 1-4: Fully black (1s-end)
        else if (elapsed < phase4End) {
            blackAlpha = 1.0f;

            // After black screen delay (at 1.5s), re-enable HUD so players can see their role after animation
            if (elapsed >= phase2End && !ClientState.isHudEnabled) {
                ClientState.isHudEnabled = true;
                ClientState.isRoleHudVisible = true;
            }
        }
        // Phase 5: Fade out (everything fades away)
        else if (elapsed < phase5End) {
            float fadeOutProgress = (float) (elapsed - phase4End) / FADE_OUT_DURATION;
            blackAlpha = 1.0f - fadeOutProgress;
        }
        // Animation complete
        else {
            // Safety: ensure holding screen is closed if animation ends
            if (client.screen instanceof GameEndHoldingScreen) {
                client.setScreen(null);
            }

            isAnimating = false;
            ClientState.gameEnding = false;
            return;
        }

        int screenWidth = context.guiWidth();
        int screenHeight = context.guiHeight();

        // Draw black background with high z-level to cover text with shadows
        int blackColor = FastColor.ARGB32.color((int) (blackAlpha * 255), 0, 0, 0);

        // Push matrices and translate to a higher z-level to ensure it renders above text
        context.pose().pushPose();
        context.pose().translate(0, 0, 400); // High z-level to render above text layers
        context.fill(0, 0, screenWidth, screenHeight, blackColor);
        context.pose().popPose();

        // Phase 2+: Show title and content (after black screen delay)
        if (elapsed >= phase2End) {
            // Calculate content alpha for fade out phase
            float contentAlpha = 1.0f;
            if (elapsed >= phase4End) {
                if (elapsed < phase5End) {
                    float fadeOutProgress = (float) (elapsed - phase4End) / FADE_OUT_DURATION;
                    contentAlpha = 1.0f - fadeOutProgress;
                } else {
                    // Past fade out - set alpha to 0 to prevent flicker
                    contentAlpha = 0.0f;
                }
            }

            // Determine what to show based on timing
            // Stop rendering 100ms before animation ends to prevent flicker
            boolean showTitle = elapsed >= phase2End && elapsed < (phase5End - 100);  // Show title at 1.4s, hide before end
            boolean showSubtitle = elapsed >= (phase2End + SUBTITLE_DELAY) && elapsed < (phase5End - 100);  // Show subtitle at 2.5s, hide before end

            // Render title (and subtitle if time)
            renderTitle(context, client, screenWidth, screenHeight, contentAlpha, showTitle, showSubtitle);

            // Phase 3+: Render player entries (after title wait)
            if (elapsed >= phase3Start) {
                int timeSincePlayersStart = (int) (elapsed - phase3Start);
                renderPlayerEntries(context, client, screenWidth, screenHeight, players, timeSincePlayersStart, contentAlpha);
            }
        }
    }

    /**
     * Manages screen transitions during the animation.
     * - Close any open screen when animation starts
     * - Open holding screen when fully black (for all players including storyteller)
     * - Close holding screen when fade out begins
     */
    private static void manageScreenTransitions(Minecraft client, long elapsed, int phase4End) {
        // 1. Close any open screen when animation starts (once)
        if (!hasClosedCurrentScreen && elapsed > 0) {
            if (client.screen != null) {
                client.execute(() -> client.setScreen(null));
            }
            hasClosedCurrentScreen = true;
        }

        // 2. Open holding screen when fully black (elapsed >= FADE_TO_BLACK_DURATION)
        if (!hasOpenedHoldingScreen && elapsed >= FADE_TO_BLACK_DURATION) {
            client.execute(() -> client.setScreen(new GameEndHoldingScreen()));
            hasOpenedHoldingScreen = true;
        }

        // 3. Close holding screen when fade out begins (elapsed >= phase4End)
        if (!hasClosedHoldingScreen && elapsed >= phase4End) {
            if (client.screen instanceof GameEndHoldingScreen) {
                client.execute(() -> client.setScreen(null));
            }
            hasClosedHoldingScreen = true;
        }
    }

    /**
     * Whether the local player is on the winning team, judged by their true alignment from the
     * revealed grimoire. Anyone not in the grimoire, such as the storyteller, counts as winning.
     */
    public static boolean localPlayerWon(Minecraft client, boolean goodWins) {
        PendingRoleAssignment playerAssignment = StorytellerState.PENDING_ROLES.get(client.player.getUUID());
        if (playerAssignment == null) return true;
        return goodWins == playerAssignment.isFinalGood();
    }

    /**
     * Renders the Victory/Defeat title and subtitle.
     */
    private static void renderTitle(GuiGraphics context, Minecraft client, int screenWidth, int screenHeight,
                                     float alpha, boolean showTitle, boolean showSubtitle) {
        boolean playerWon = localPlayerWon(client, goodWins);

        // Calculate positions (moved higher to make room for player entries)
        int titleY = screenHeight / 4 - 30;
        int subtitleY = titleY + 20;

        // Push to higher z-level so title renders above black screen
        context.pose().pushPose();
        context.pose().translate(0, 0, 500);

        // Render title if it's time
        if (showTitle) {
            Component titleText = Component.translatable(playerWon ? "hud.blood-on-the-blocktower.game_end.victory" : "hud.blood-on-the-blocktower.game_end.defeat")
                    .withStyle(style -> style.withBold(true));
            int titleColor = FastColor.ARGB32.color((int) (alpha * 255), playerWon ? 0 : 255, playerWon ? 255 : 0, 0);

            // Render title (scaled 3x for larger text)
            float titleScale = 3.0f;
            context.pose().pushPose();
            context.pose().scale(titleScale, titleScale, 1.0f);
            // Center horizontally: divide by 2 for center, then by scale
            int scaledX = (int) (screenWidth / (2 * titleScale));
            // Adjust Y to keep bottom in same position (move up by the additional height)
            int textHeight = client.font.lineHeight;
            int scaledY = (int) ((titleY - textHeight) / titleScale);
            context.drawCenteredString(client.font, titleText, scaledX, scaledY, titleColor);
            context.pose().popPose();
        }

        // Render subtitle if it's time (storytellers see subtitle too)
        if (showSubtitle) {
            // Build colored subtitle text (RGB only, alpha applied via draw call)
            int townsfolkBlue = 0x00AAFF;  // Townsfolk blue (RGB)
            int minionRed = 0xFF5555;      // Minion red (RGB)
            int white = 0xFFFFFF;          // White (RGB)

            Component subtitleText;
            if (goodWins) {
                // "The Good Team Wins" with "Good" in blue
                subtitleText = Component.translatable("hud.blood-on-the-blocktower.game_end.team_wins",
                                Component.translatable("hud.blood-on-the-blocktower.game_end.good").withStyle(style -> style.withColor(townsfolkBlue)))
                        .withStyle(style -> style.withColor(white));
            } else {
                // "The Evil Team Wins" with "Evil" in red
                subtitleText = Component.translatable("hud.blood-on-the-blocktower.game_end.team_wins",
                                Component.translatable("hud.blood-on-the-blocktower.game_end.evil").withStyle(style -> style.withColor(minionRed)))
                        .withStyle(style -> style.withColor(white));
            }

            // Apply alpha uniformly to all text via the color parameter
            int subtitleColor = FastColor.ARGB32.color((int) (alpha * 255), 255, 255, 255);
            context.drawCenteredString(client.font, subtitleText, screenWidth / 2, subtitleY, subtitleColor);
        }

        context.pose().popPose();
    }

    /**
     * Renders player entries (head, name, role) in rows.
     */
    private static void renderPlayerEntries(GuiGraphics context, Minecraft client, int screenWidth, int screenHeight,
                                            List<PlayerData> players, int timeSinceStart, float globalAlpha) {
        int entriesStartY = screenHeight / 2 - 60;  // Raised higher to prevent second row cutoff
        int entryWidth = 60;
        int entryHeight = 80;
        int entrySpacing = 10;

        // Calculate number of rows needed
        int totalPlayers = players.size();
        int rowsNeeded = (int) Math.ceil((double) totalPlayers / PLAYERS_PER_ROW);

        // If only 1 row, center it vertically between where 2 rows would be
        int verticalOffset = 0;
        if (rowsNeeded == 1) {
            verticalOffset = 50; // Half of the row spacing (entryHeight + 20) / 2 = 100 / 2 = 50
        }

        for (int i = 0; i < players.size(); i++) {
            // Calculate when this player should start fading in
            int playerFadeStartTime = i * PLAYER_FADE_DURATION;
            int playerFadeEndTime = playerFadeStartTime + PLAYER_FADE_DURATION;

            // Calculate player fade alpha
            float playerAlpha = 0;
            if (timeSinceStart >= playerFadeStartTime) {
                if (timeSinceStart >= playerFadeEndTime) {
                    playerAlpha = 1.0f;
                } else {
                    playerAlpha = (float) (timeSinceStart - playerFadeStartTime) / PLAYER_FADE_DURATION;
                }
            }

            // Apply global alpha (for fade out phase)
            playerAlpha *= globalAlpha;

            // Skip if not visible yet (use small threshold to avoid flicker)
            if (playerAlpha < 0.02f) {
                continue;
            }

            PlayerData player = players.get(i);

            // Calculate row and column
            int row = i / PLAYERS_PER_ROW;
            int col = i % PLAYERS_PER_ROW;

            // Calculate total players in this row
            int playersInRow = Math.min(PLAYERS_PER_ROW, players.size() - row * PLAYERS_PER_ROW);

            // Calculate centering offset for this row
            int rowWidth = playersInRow * entryWidth + (playersInRow - 1) * entrySpacing;
            int rowStartX = (screenWidth - rowWidth) / 2;

            // Calculate position
            int x = rowStartX + col * (entryWidth + entrySpacing);
            int y = entriesStartY + verticalOffset + row * (entryHeight + 20);

            // Render this player entry
            renderPlayerEntry(context, client, player, x, y, playerAlpha);
        }
    }

    private static final int ASSOCIATED_ICON_SIZE = 16;
    private static final int ASSOCIATED_SPACING = 2;

    /**
     * Renders a single player entry (head, name, role, and associated roles).
     */
    private static void renderPlayerEntry(GuiGraphics context, Minecraft client, PlayerData player,
                                          int x, int y, float alpha) {
        int alphaInt = (int) (alpha * 255);

        // Push to higher z-level so player entries render above black screen
        context.pose().pushPose();
        context.pose().translate(0, 0, 500);

        // Render player head with alpha blending
        int headX = x + (60 - HEAD_ICON_SIZE) / 2;
        int headY = y;

        if (player.skinTexture != null) {
            // Enable blending and set shader color for alpha
            com.mojang.blaze3d.systems.RenderSystem.enableBlend();
            com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, alpha);

            // Draw head base layer
            context.blit(player.skinTexture, headX, headY, HEAD_ICON_SIZE, HEAD_ICON_SIZE,
                    8, 8, 8, 8, 64, 64);
            // Draw head overlay
            context.blit(player.skinTexture, headX, headY, HEAD_ICON_SIZE, HEAD_ICON_SIZE,
                    40, 8, 8, 8, 64, 64);

            // Reset shader color
            com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            com.mojang.blaze3d.systems.RenderSystem.disableBlend();
        }

        // Render player name
        int nameY = headY + HEAD_ICON_SIZE + 2;
        int nameColor = FastColor.ARGB32.color(alphaInt, 255, 255, 255);
        int nameX = x + 30; // Center of entry width
        context.drawCenteredString(client.font, Component.literal(player.name), nameX, nameY, nameColor);

        // Render role icon with role type border
        int roleY = nameY + client.font.lineHeight + 2;
        int roleX = x + (60 - ROLE_ICON_SIZE) / 2;

        // Draw role type border
        int borderColor = FastColor.ARGB32.color(alphaInt,
                FastColor.ARGB32.red(player.alignmentColor),
                FastColor.ARGB32.green(player.alignmentColor),
                FastColor.ARGB32.blue(player.alignmentColor));
        context.renderOutline(roleX - 1, roleY - 1, ROLE_ICON_SIZE + 2, ROLE_ICON_SIZE + 2, borderColor);

        // Draw role icon
        if (player.roleIcon != null) {
            // Apply alpha to texture rendering using RenderSystem
            com.mojang.blaze3d.systems.RenderSystem.enableBlend();
            com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, alpha);
            context.blit(player.roleIcon, roleX, roleY, 0, 0, ROLE_ICON_SIZE, ROLE_ICON_SIZE,
                    ROLE_ICON_SIZE, ROLE_ICON_SIZE);
            com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            com.mojang.blaze3d.systems.RenderSystem.disableBlend();
        }

        // Render associated roles below the main role (like night order HUD)
        if (player.associatedRoles != null && !player.associatedRoles.isEmpty()) {
            int associatedY = roleY + ROLE_ICON_SIZE + ASSOCIATED_SPACING;
            int associatedX = x + (60 - ASSOCIATED_ICON_SIZE) / 2;

            for (Reminder reminder : player.associatedRoles) {
                ResourceLocation associatedIcon = reminder.getIcon();

                // Draw associated role icon with alpha
                com.mojang.blaze3d.systems.RenderSystem.enableBlend();
                com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, alpha);
                context.blit(associatedIcon, associatedX, associatedY, 0, 0,
                        ASSOCIATED_ICON_SIZE, ASSOCIATED_ICON_SIZE, ASSOCIATED_ICON_SIZE, ASSOCIATED_ICON_SIZE);
                com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
                com.mojang.blaze3d.systems.RenderSystem.disableBlend();

                // Draw alignment border for associated role (pass script for custom role color lookup)
                Script script = ClientState.currentScript;
                int associatedColor = reminder.getAlignmentColor(script);
                if (associatedColor != RoleType.NONE.getColor()) {
                    int associatedBorderColor = FastColor.ARGB32.color(alphaInt,
                            FastColor.ARGB32.red(associatedColor),
                            FastColor.ARGB32.green(associatedColor),
                            FastColor.ARGB32.blue(associatedColor));
                    context.renderOutline(associatedX - 1, associatedY - 1,
                            ASSOCIATED_ICON_SIZE + 2, ASSOCIATED_ICON_SIZE + 2, associatedBorderColor);
                }

                associatedY += ASSOCIATED_ICON_SIZE + ASSOCIATED_SPACING;
            }
        }

        context.pose().popPose();
    }

    /**
     * Gets all player data sorted by seat number.
     */
    private static List<PlayerData> getPlayerDataSorted() {
        List<PlayerData> players = new ArrayList<>();
        Minecraft client = Minecraft.getInstance();

        // Get all players from StorytellerState
        for (Map.Entry<UUID, PendingRoleAssignment> entry : StorytellerState.PENDING_ROLES.entrySet()) {
            UUID playerUuid = entry.getKey();
            PendingRoleAssignment assignment = entry.getValue();
            Integer seatNumber = StorytellerState.PENDING_SEAT_NUMBERS.get(playerUuid);

            if (seatNumber == null) {
                continue; // Skip unseated players
            }

            // Get player name and skin
            String playerName = Component.translatable("gui.blood-on-the-blocktower.common.unknown_player").getString();
            ResourceLocation skinTexture = null;

            AbstractClientPlayer playerEntity = (AbstractClientPlayer) client.level.getPlayerByUUID(playerUuid);
            if (playerEntity != null) {
                playerName = playerEntity.getName().getString();
                skinTexture = playerEntity.getSkin().texture();
            } else {
                // Try to get from player list (prefer custom display name over raw profile)
                PlayerInfo playerListEntry = client.getConnection().getPlayerInfo(playerUuid);
                if (playerListEntry != null) {
                    playerName = PlayerListUtil.resolveDisplayName(playerListEntry, client, playerUuid);
                    skinTexture = playerListEntry.getSkin().texture();
                }
            }

            // Get role and alignment (handle custom roles)
            Role role = assignment.role();
            boolean isGood = assignment.isFinalGood();

            // Get role type, icon, and color (handling custom roles)
            RoleType roleType;
            ResourceLocation roleIcon;
            int borderColor;

            if (assignment.isCustomRole() && assignment.customRole().isPresent()) {
                CustomRole customRole = assignment.customRole().get();
                roleType = customRole.team();
                roleIcon = UrlTextureLoader.getTexture(customRole);
                borderColor = roleType.getColor();
            } else {
                roleType = role.getType();
                roleIcon = role.getIcon();
                borderColor = roleType.getColor();
            }

            // Special handling for travelers: show blue/red/purple based on alignment override
            if (roleType == RoleType.TRAVELER) {
                AlignmentOverride override = assignment.override();
                borderColor = switch (override) {
                    case FORCE_GOOD -> RoleType.TOWNSFOLK.getColor(); // Blue
                    case FORCE_BAD -> RoleType.MINION.getColor(); // Red
                    default -> RoleType.TRAVELER.getColor(); // Purple
                };
            } else if (isGood != roleType.isDefaultGood()) {
                // Alignment override flipped the player's side, so recolor to match actual alignment
                borderColor = isGood ? RoleType.TOWNSFOLK.getColor() : RoleType.MINION.getColor();
            }

            // Get associated roles (e.g., from Pixie, Cannibal, etc.)
            List<Reminder> associatedRoles = new ArrayList<>(RoleHelpers.getAssociatedRoleReminders(playerUuid));

            // Also add custom role associated reminders (reminders where text matches custom role display name)
            Script script = ClientState.currentScript;
            if (script != null) {
                List<Reminder> playerReminders = StorytellerState.REMINDERS.getOrDefault(playerUuid, Collections.emptyList());
                for (Reminder r : playerReminders) {
                    if (r.isCustomRoleReminder() && r.customRoleId().isPresent() && !r.isFabled()) {
                        String customRoleId = r.customRoleId().get();
                        String reminderText = r.text();

                        // Check customRoles for matching display name
                        boolean foundMatch = false;
                        if (script.customRoles() != null) {
                            for (CustomRole cr : script.customRoles()) {
                                if (cr.id().equals(customRoleId) && reminderText.equalsIgnoreCase(cr.getDisplayName())) {
                                    associatedRoles.add(r);
                                    foundMatch = true;
                                    break;
                                }
                            }
                        }
                        // Also check travelers
                        if (!foundMatch && script.travelers() != null) {
                            for (ScriptRole sr : script.travelers()) {
                                if (sr.getId().equals(customRoleId) && reminderText.equalsIgnoreCase(sr.getDisplayName())) {
                                    associatedRoles.add(r);
                                    break;
                                }
                            }
                        }
                    }
                }
            }

            players.add(new PlayerData(playerUuid, playerName, seatNumber, role, isGood,
                    roleIcon, skinTexture, borderColor, associatedRoles));
        }

        // Sort by seat number
        players.sort(Comparator.comparingInt(p -> p.seatNumber));

        return players;
    }

    /**
     * Data class for player information.
     */
    private static class PlayerData {
        UUID uuid;
        String name;
        int seatNumber;
        Role role;
        boolean isGood;
        ResourceLocation roleIcon;
        ResourceLocation skinTexture;
        int alignmentColor;
        List<Reminder> associatedRoles;

        PlayerData(UUID uuid, String name, int seatNumber, Role role, boolean isGood,
                   ResourceLocation roleIcon, ResourceLocation skinTexture, int alignmentColor, List<Reminder> associatedRoles) {
            this.uuid = uuid;
            this.name = name;
            this.seatNumber = seatNumber;
            this.role = role;
            this.isGood = isGood;
            this.roleIcon = roleIcon;
            this.skinTexture = skinTexture;
            this.alignmentColor = alignmentColor;
            this.associatedRoles = associatedRoles;
        }
    }
}
