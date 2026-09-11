package com.autumnwind.botb.hud;

import com.autumnwind.botb.states.ClientState;
import com.autumnwind.botb.states.StorytellerState;
import com.autumnwind.botb.timer.ClientTimerState;
import com.autumnwind.botb.util.*;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.FormattedCharSequence;
import com.autumnwind.botb.event.KeyInputHandler;

/**
 * Renders the role HUD overlay showing player counts and role information.
 * Has two modes:
 * - Full HUD: Detailed player counts + role box with icon, name, type, and description
 * - Minimal HUD: Compact player counts + small role icon
 *
 * For operators with night info enabled, shows current visit instructions instead of their role.
 */
public class RoleHUD {

    /**
     * Renders the role HUD.
     * @param drawContext The draw context
     * @param client The Minecraft client
     */
    public static void render(GuiGraphics drawContext, Minecraft client) {
        if (client.player == null) return;

        // Get player count and role info
        int playerCount = PlayerCountsDisplay.playerCount();
        int travelerCount = PlayerCountsDisplay.travelerCount();
        int nonTravelerCount = playerCount - travelerCount;
        // Use non-traveler count for role counts lookup (like SetupValidator does)
        RoleCounts.RoleCountInfo counts = RoleCounts.getCounts(nonTravelerCount);
        Role role = ClientState.myRole;
        Boolean isGood = ClientState.myAlignment;

        // Position player counts higher when no timer, below timer when timer is active
        // Boss bar is at the top, with text above it
        // Timer text renders at y ~30-35, so we render below it at ~20 when timer is active
        final int playerCountsHeight = ClientTimerState.hasActiveTimer() ? 20 : 10;

        if (ClientState.isRoleHudVisible) {
            renderFullHUD(drawContext, client, counts, role, isGood, playerCount, travelerCount, playerCountsHeight);
        } else {
            renderMinimalHUD(drawContext, client, counts, role, travelerCount, playerCountsHeight);
        }
    }

    /**
     * Renders the full HUD with detailed player counts and role box.
     */
    private static void renderFullHUD(GuiGraphics drawContext, Minecraft client,
                                       RoleCounts.RoleCountInfo counts, Role role, Boolean isGood,
                                       int playerCount, int travelerCount, int playerCountsHeight) {
        // 1. Render detailed player counts
        Component countText = PlayerCountsDisplay.buildPlayerCountsText(playerCount, travelerCount, counts, true);
        if (countText != null) {
            int screenWidth = drawContext.guiWidth();
            drawContext.drawCenteredString(client.font, countText, screenWidth / 2, playerCountsHeight, 0xFFFFFF);
        }

        // 2. Render full role box OR night order instructions box
        // For operators with night info enabled and current visit, show instructions HUD instead
        boolean isOperator = client.player.hasPermissions(2);
        boolean isActuallyNight = ClientState.currentNight > ClientState.currentDay;
        boolean isOperatorWithVisits = isOperator &&
                StorytellerState.sendTeleportInfo &&
                ClientState.isNightHudVisible;

        // Show the instructions HUD whenever the selected visit has content (icon + instruction),
        // day or night: daytime visits (Nominations and its modifiers, the Leviathan and Vizier
        // announcements) need it as much as the night ones. currentVisitScriptRole is null for
        // static visits, and we still want their instruction shown, so don't gate on it here.
        boolean showInstructionsHud = isOperatorWithVisits &&
                StorytellerState.currentVisitInstructions != null &&
                !StorytellerState.currentVisitInstructions.isBlank() &&
                StorytellerState.currentVisitIcon != null;

        // At night with the order open but no visit to show, hide the role box entirely. During
        // the day a visit with nothing to say (Dawn, Dusk) falls back to the storyteller's own role.
        if (isOperatorWithVisits && isActuallyNight && !showInstructionsHud) {
            return;
        }

        // Determine what to display
        if (showInstructionsHud) {
            // Storyteller instructions HUD - use visit state
            ResourceLocation displayIcon = StorytellerState.currentVisitIcon;
            String displayText = StorytellerState.currentVisitInstructions;
            boolean displayIsGoodVal = StorytellerState.currentVisitIsGood;

            // Determine role type / default alignment / display name from the script role
            // when present. Static-only visits have no script role, so fall back to NONE
            // type and an empty display name (currentVisitRoleText, set by VisitNavigation,
            // overrides the displayName at render time when present).
            ScriptRole visitScriptRole = StorytellerState.currentVisitScriptRole;
            RoleType roleType = visitScriptRole != null && visitScriptRole.getTeam() != null
                    ? visitScriptRole.getTeam() : RoleType.NONE;
            boolean isDefaultGood = visitScriptRole == null || visitScriptRole.isDefaultGood();
            String displayName = visitScriptRole != null ? visitScriptRole.getDisplayName() : "";

            renderRoleBox(drawContext, client, displayIcon, displayName, roleType,
                    isDefaultGood, displayIsGoodVal, displayText, true, null);
        } else {
            // Normal player HUD
            if (ClientState.myAssignment != null && ClientState.myAssignment.isCustomRole()) {
                // Custom role from assignment
                ScriptRole scriptRole = ClientState.myAssignment.getScriptRole();
                if (scriptRole != null) {
                    boolean displayIsGoodVal = isGood != null ? isGood : true;
                    renderRoleBox(drawContext, client, scriptRole.getIcon(),
                            ClientState.myAssignment.getDisplayName(),
                            ClientState.myAssignment.getRoleType(),
                            ClientState.myAssignment.isRoleDefaultGood(),
                            displayIsGoodVal,
                            scriptRole.getAbility(), false, ClientState.myAssignment.override());
                }
            } else if (role != null && role != Role.NO_ROLE && isGood != null) {
                // Official role
                AlignmentOverride override = ClientState.myAssignment != null ? ClientState.myAssignment.override() : AlignmentOverride.DEFAULT;
                renderRoleBox(drawContext, client, role.getIcon(), role.getDisplayName(),
                        role.getType(), role.isDefaultGood(), isGood, role.getDescription(), false, override);
            }
        }
    }

    /**
     * Renders the role box with icon, name, type, and description.
     * Works for both official and custom roles by accepting extracted values.
     * @param icon The role icon identifier
     * @param displayName The role display name
     * @param roleType The role type (Townsfolk, Outsider, Minion, Demon)
     * @param isDefaultGood Whether the role is good by default
     * @param displayIsGood The actual alignment after overrides
     * @param displayText The role description/instructions
     * @param showExtraInfo If true, show extra info from NightOrderInfoGenerator (operators only)
     * @param override The alignment override (null for storyteller HUD)
     */
    private static void renderRoleBox(GuiGraphics drawContext, Minecraft client,
                                       ResourceLocation icon, String displayName, RoleType roleType,
                                       boolean isDefaultGood, boolean displayIsGood, String displayText,
                                       boolean showExtraInfo, AlignmentOverride override) {
        // Text Preparation
        // For storyteller instructions HUD, use the role/associated role text and player names
        Component roleNameText;
        MutableComponent roleTypeText;

        if (showExtraInfo && StorytellerState.currentVisitRoleText != null) {
            // Show "ASSIGNED / ASSOCIATED (Drunk)" format for storyteller with proper colors
            roleNameText = StorytellerState.currentVisitRoleText;
            // Show player name(s) in yellow instead of role type (no italics)
            roleTypeText = StorytellerState.currentVisitPlayerNames != null
                ? StorytellerState.currentVisitPlayerNames.copy() : Component.empty();
        } else {
            // Normal player HUD - show role name and type
            roleNameText = Component.literal(displayName);
            boolean isTraveler = roleType == RoleType.TRAVELER;
            boolean alignmentMismatched = displayIsGood != isDefaultGood;

            if (isTraveler) {
                // Travelers show alignment indicator based on override:
                // FORCE_GOOD -> (GOOD), FORCE_BAD -> (EVIL), DEFAULT -> (---)
                roleTypeText = Component.literal(roleType.getDisplayName());
                if (override == AlignmentOverride.FORCE_GOOD) {
                    roleTypeText.append(Component.translatable("hud.blood-on-the-blocktower.role.good").withStyle(ChatFormatting.BOLD, ChatFormatting.BLUE));
                } else if (override == AlignmentOverride.FORCE_BAD) {
                    roleTypeText.append(Component.translatable("hud.blood-on-the-blocktower.role.evil").withStyle(ChatFormatting.BOLD, ChatFormatting.RED));
                } else {
                    // Default alignment - show neutral indicator
                    roleTypeText.append(Component.literal(" (---)").withStyle(ChatFormatting.ITALIC, ChatFormatting.GRAY));
                }
            } else {
                roleTypeText = Component.literal(roleType.getDisplayName());
                if (alignmentMismatched) {
                    roleTypeText.append(Component.translatable(displayIsGood ? "hud.blood-on-the-blocktower.role.good" : "hud.blood-on-the-blocktower.role.evil").withStyle(ChatFormatting.BOLD));
                }
            }
            roleTypeText.withStyle(ChatFormatting.ITALIC);
        }

        MutableComponent descText = Component.literal(displayText);
        if (!showExtraInfo && ClientState.hintsEnabled) {
            String keyName = KeyInputHandler.openMyRoleDetailsKey
                    .getTranslatedKeyMessage().getString();
            String hideKeyName = KeyInputHandler.toggleShowRole
                    .getTranslatedKeyMessage().getString();
            descText.append(Component.literal("\n\n").append(Component.translatable("hud.blood-on-the-blocktower.role.hint_keys",
                            Component.literal(keyName).withStyle(ChatFormatting.YELLOW),
                            Component.literal(hideKeyName).withStyle(ChatFormatting.YELLOW))
                    .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC)));
        }
        List<FormattedCharSequence> wrappedDesc = client.font.split(descText, 200);

        // Extra info for storyteller only (from NightOrderInfoGenerator)
        List<FormattedCharSequence> wrappedExtraInfo = List.of();
        if (showExtraInfo && StorytellerState.currentVisitExtraInfo != null) {
            wrappedExtraInfo = client.font.split(StorytellerState.currentVisitExtraInfo, 200);
        }
        int extraInfoGap = wrappedExtraInfo.isEmpty() ? 0 : 4; // Gap before extra info

        // Static-info bundles (MINION_INFO / DEMON_INFO) have no specific role to label,
        // so VisitNavigation puts the player names into roleNameText and leaves
        // roleTypeText empty. Collapse the empty line rather than rendering blank space.
        boolean hasRoleType = !roleTypeText.getString().isEmpty();

        // Layout Calculations
        int x_padding = 10, y_padding = 10, icon_size = 48, text_x_padding = 5;
        int text_x_start = x_padding + icon_size + text_x_padding;
        int font_height = client.font.lineHeight;
        int text_lines = 1 + (hasRoleType ? 1 : 0) + wrappedDesc.size() + wrappedExtraInfo.size();
        int text_height = font_height * text_lines + 2 + extraInfoGap;
        int box_inner_height = Math.max(icon_size, text_height);
        int box_width = icon_size + text_x_padding + 200 + 2 * x_padding;
        int box_height = box_inner_height + 2 * y_padding;
        int x = 10, y = 50;

        int text_area_x1 = x + text_x_start - 3, text_area_y1 = y + y_padding - 2;
        int text_area_x2 = x + box_width - x_padding + 3, text_area_y2 = y + y_padding + text_height + 2;

        // Rendering
        int alpha = 128;
        int background_color = displayIsGood ? FastColor.ARGB32.color(alpha, 135, 206, 235) : FastColor.ARGB32.color(alpha, 240, 128, 128);
        drawContext.fill(x, y, x + box_width, y + box_height, background_color);

        int icon_x = x + x_padding, icon_y = y + y_padding;
        drawContext.blit(icon, icon_x, icon_y, 0, 0, icon_size, icon_size, icon_size, icon_size);
        drawContext.renderOutline(icon_x - 1, icon_y - 1, icon_size + 2, icon_size + 2, 0xFFFFFFFF);

        int text_backdrop_color = FastColor.ARGB32.color(191, 0, 0, 0);
        drawContext.fill(text_area_x1, text_area_y1, text_area_x2, text_area_y2, text_backdrop_color);

        int text_start_x = icon_x + icon_size + text_x_padding;
        int current_y = icon_y;

        // Color role name based on role type, with alignment override handling
        // Travelers always use purple, regardless of alignment override
        // Forced-evil good roles should be minion red, forced-good evil roles should be townsfolk blue
        int name_color;
        boolean isTravelerRole = roleType == RoleType.TRAVELER;
        if (isTravelerRole) {
            // Travelers always show purple name, alignment is shown via the indicator
            name_color = roleType.getColor() | 0xFF000000;
        } else if (displayIsGood && !isDefaultGood) {
            // Forced good (evil role made good) - townsfolk blue
            name_color = RoleType.TOWNSFOLK.getColor() | 0xFF000000;
        } else if (!displayIsGood && isDefaultGood) {
            // Forced evil (good role made evil) - minion red
            name_color = RoleType.MINION.getColor() | 0xFF000000;
        } else {
            // No alignment override - use role's type color
            name_color = roleType.getColor() | 0xFF000000;
        }
        drawContext.drawString(client.font, roleNameText, text_start_x, current_y, name_color);
        current_y += font_height;

        if (hasRoleType) {
            drawContext.drawString(client.font, roleTypeText, text_start_x, current_y, 0xFFAAAAAA);
            current_y += font_height + 2;
        } else {
            current_y += 2; // small gap before description, matching the spaced-out look
        }

        for (FormattedCharSequence line : wrappedDesc) {
            drawContext.drawString(client.font, line, text_start_x, current_y, 0xFFFFFFFF, false);
            current_y += font_height;
        }

        // Render extra info if present (for storyteller HUD)
        if (!wrappedExtraInfo.isEmpty()) {
            current_y += extraInfoGap;
            for (FormattedCharSequence line : wrappedExtraInfo) {
                drawContext.drawString(client.font, line, text_start_x, current_y, 0xFFFFFFFF, false);
                current_y += font_height;
            }
        }
    }

    /**
     * Renders the minimal HUD with compact player counts and small role icon.
     */
    private static void renderMinimalHUD(GuiGraphics drawContext, Minecraft client,
                                          RoleCounts.RoleCountInfo counts, Role role, int travelerCount, int playerCountsHeight) {
        // 1. Render minimal player counts
        // Note: travelerCount is already available, but we need playerCount for the utility
        int playerCount = ClientState.activePlayerCount;
        Component countText = PlayerCountsDisplay.buildPlayerCountsText(playerCount, travelerCount, counts, false);
        if (countText != null) {
            int screenWidth = drawContext.guiWidth();
            drawContext.drawCenteredString(client.font, countText, screenWidth / 2, playerCountsHeight, 0xFFFFFF);
        }

        // 2. Render small role icon - either night order visit role or player's role
        // For operators with night info enabled, show the current visit icon instead
        boolean isOperatorInNightMode = client.player.hasPermissions(2) &&
                StorytellerState.sendTeleportInfo &&
                ClientState.isNightHudVisible;

        ResourceLocation iconToRender = null;

        if (isOperatorInNightMode && StorytellerState.currentVisitIcon != null) {
            // Storyteller visit icon (works for both official and custom roles)
            iconToRender = StorytellerState.currentVisitIcon;
        } else if (ClientState.myAssignment != null && ClientState.myAssignment.isCustomRole()) {
            // Player has custom role - get icon from assignment
            ScriptRole scriptRole = ClientState.myAssignment.getScriptRole();
            if (scriptRole != null) {
                iconToRender = scriptRole.getIcon();
            }
        } else if (role != null && role != Role.NO_ROLE) {
            // Player has official role
            iconToRender = role.getIcon();
        }

        if (iconToRender != null) {
            int iconSize = 32;
            int x = 10, y = 10;
            drawContext.blit(iconToRender, x, y, 0, 0, iconSize, iconSize, iconSize, iconSize);
        }
    }
}
