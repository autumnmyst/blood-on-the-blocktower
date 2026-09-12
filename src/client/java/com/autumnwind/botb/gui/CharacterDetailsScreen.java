package com.autumnwind.botb.gui;

import com.autumnwind.botb.event.KeyInputHandler;
import com.autumnwind.botb.states.ClientState;
import com.autumnwind.botb.util.*;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import com.autumnwind.botb.util.ScriptRole;
import com.autumnwind.botb.config.CustomRoleLibrary;
import java.util.ArrayList;
import com.autumnwind.botb.gui.widget.DocumentEntry;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Style;

public class CharacterDetailsScreen extends Screen {
    private final ScriptRole scriptRole;
    /** Null when opened by the details keybind with no screen already open. */
    @Nullable
    private final Screen parent;
    @Nullable
    private final RoleDetails.RoleDetailData details; // null for custom roles
    private DetailsListWidget listWidget;

    private List<ScriptRole> catalogList;
    private int catalogIndex;

    // Almanac data for custom roles
    @Nullable
    private AlmanacData.RoleAlmanacData almanacRoleData;
    private boolean almanacLoading = false;

    /**
     * Constructor for ScriptRole (works with both official and custom roles).
     */
    public CharacterDetailsScreen(ScriptRole scriptRole, Screen parent) {
        super(Component.literal(scriptRole.getDisplayName()));
        this.scriptRole = scriptRole;
        this.parent = parent;

        // Only load details for official roles
        if (!scriptRole.isCustom()) {
            this.details = RoleDetails.get(((ScriptRole.Official) scriptRole).role());
        } else {
            this.details = null;
            // Try to fetch almanac data for custom roles
            fetchAlmanacData();
        }

        this.catalogList = null;
        this.catalogIndex = -1;
    }

    /**
     * Fetch almanac data for the current custom role.
     * Always calls fetchAlmanacs to properly merge main + extra almanacs (uses cached data if available).
     */
    private void fetchAlmanacData() {
        Script script = ClientState.currentScript;
        String main = script == null ? null : script.almanac();

        List<String> extras = new ArrayList<>();
        if (script != null && script.extraAlmanacs() != null) {
            extras.addAll(script.extraAlmanacs());
        }

        // A character can be in the custom role library without being on the active script. The
        // Script Builder's palette opens details for exactly that case. The library is then the
        // only record of which almanac documents it.
        CustomRoleLibrary.get(scriptRole.getId()).ifPresent(entry -> {
            for (String url : entry.almanacs()) {
                if (url != null && !url.isBlank() && !url.equals(main) && !extras.contains(url)) {
                    extras.add(url);
                }
            }
        });

        if ((main == null || main.isEmpty()) && extras.isEmpty()) {
            return;
        }

        // Always fetch via fetchAlmanacs to properly merge main + extra almanacs
        // This uses cached data for individual URLs but ensures extras are included
        this.almanacLoading = true;
        AlmanacParser.fetchAlmanacs(main, extras)
            .thenAccept(data -> {
                this.almanacRoleData = data.getRoleData(scriptRole.getId()).orElse(null);
                this.almanacLoading = false;
                // Refresh the screen if we're still showing it
                Minecraft.getInstance().execute(() -> {
                    if (Minecraft.getInstance().gui.screen() == this) {
                        this.rebuildWidgets();
                    }
                });
            });
    }

    /**
     * Constructor for ScriptRole with list navigation.
     */
    public CharacterDetailsScreen(ScriptRole scriptRole, Screen parent, List<ScriptRole> catalogList) {
        this(scriptRole, parent);
        this.catalogList = catalogList;
        if (this.catalogList != null) {
            this.catalogIndex = this.catalogList.indexOf(scriptRole);
        }
    }

    /**
     * Legacy constructor for Role (wraps in ScriptRole.Official).
     */
    public CharacterDetailsScreen(Role role, Screen parent) {
        this(new ScriptRole.Official(role), parent);
    }

    /**
     * Legacy constructor for Role with list navigation.
     */
    public CharacterDetailsScreen(Role role, Screen parent, List<Role> roleList) {
        this(new ScriptRole.Official(role), parent);
        // Convert Role list to ScriptRole list for navigation
        if (roleList != null) {
            this.catalogList = roleList.stream()
                    .map(r -> (ScriptRole) new ScriptRole.Official(r))
                    .toList();
            this.catalogIndex = roleList.indexOf(role);
        }
    }

    @Override
    protected void init() {
        int buttonY = this.height - 28;
        int buttonHeight = 20;

        // If we have a list, create the 3-button layout
        if (this.catalogList != null && this.catalogIndex != -1) {
            int backButtonWidth = 150;
            int arrowButtonWidth = 20;
            int spacing = 5;

            int backButtonX = this.width / 2 - backButtonWidth / 2;
            int prevButtonX = backButtonX - arrowButtonWidth - spacing;
            int nextButtonX = backButtonX + backButtonWidth + spacing;

            // "Back" button (goes back to catalog screen)
            this.addRenderableWidget(Button.builder(Component.translatable("gui.back"), (button) -> this.minecraft.gui.setScreen(this.parent))
                    .bounds(backButtonX, buttonY, backButtonWidth, buttonHeight)
                    .build());

            // "Previous" button
            if (this.catalogIndex > 0) {
                this.addRenderableWidget(Button.builder(Component.literal("<"), (button) -> {
                    ScriptRole prevRole = this.catalogList.get(this.catalogIndex - 1);
                    this.minecraft.gui.setScreen(new CharacterDetailsScreen(prevRole, this.parent, this.catalogList));
                }).bounds(prevButtonX, buttonY, arrowButtonWidth, buttonHeight).build());
            }

            // "Next" button
            if (this.catalogIndex < this.catalogList.size() - 1) {
                this.addRenderableWidget(Button.builder(Component.literal(">"), (button) -> {
                    ScriptRole nextRole = this.catalogList.get(this.catalogIndex + 1);
                    this.minecraft.gui.setScreen(new CharacterDetailsScreen(nextRole, this.parent, this.catalogList));
                }).bounds(nextButtonX, buttonY, arrowButtonWidth, buttonHeight).build());
            }

        } else {
            // Otherwise, show the default full-width "Back" button
            this.addRenderableWidget(Button.builder(Component.translatable("gui.back"), (button) -> this.minecraft.gui.setScreen(this.parent))
                    .bounds(this.width / 2 - 100, buttonY, 200, buttonHeight)
                    .build());
        }

        // Show details list for official roles OR custom roles with almanac data
        if (hasDetailsToShow()) {
            int listX = this.width / 2 + 5;
            int listY = 20;
            int listWidth = this.width / 2 - 10;
            int listHeight = this.height - listY - 35;

            this.listWidget = new DetailsListWidget(this.minecraft, listWidth, listHeight, listY);
            this.listWidget.updateSizeAndPosition(listWidth, listHeight, listX, listY);
            this.addRenderableWidget(this.listWidget);
        }
    }

    /**
     * Check if we have details to show in the right panel.
     * True for official roles (with RoleDetails) or custom roles with almanac data.
     */
    private boolean hasDetailsToShow() {
        if (details != null) return true;
        if (almanacRoleData != null && almanacRoleData.hasAnyData()) return true;
        return false;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractRenderState(context, mouseX, mouseY, delta);

        // Determine layout:
        // - Official roles always use half width with details on right
        // - Custom roles with almanac data also use half width with details on right
        // - Custom roles without almanac data use full width and center content
        boolean hasRightPanel = hasDetailsToShow();
        boolean shouldCenter = scriptRole.isCustom() && !hasRightPanel;
        int contentWidth = hasRightPanel ? this.width / 2 : this.width;
        int topMargin = 30;
        int leftX = shouldCenter ? 40 : 20; // Give more margin for centered content
        int currentY = topMargin;

        // Icon
        int iconSize = 64;
        int iconX = shouldCenter ? (this.width - iconSize) / 2 : leftX;
        context.blit(RenderPipelines.GUI_TEXTURED, scriptRole.getIcon(), iconX, currentY, 0, 0, iconSize, iconSize, iconSize, iconSize);
        currentY += iconSize + 5;

        // Role Name and Type
        Component roleName = Component.literal(scriptRole.getDisplayName()).withStyle(ChatFormatting.BOLD);
        if (shouldCenter) {
            context.centeredText(this.font, roleName, this.width / 2, currentY, 0xFFFFFFFF);
        } else {
            context.text(this.font, roleName, leftX, currentY, 0xFFFFFFFF);
        }
        currentY += this.font.lineHeight + 2;

        // Handle team display
        RoleType team = scriptRole.getTeam();
        String teamName = team != null ? team.getDisplayName() : RoleType.NONE.getDisplayName();
        int teamColor = team != null ? team.getColor() : 0xFFAAAAAA;
        Component roleType = Component.literal(teamName).withStyle(ChatFormatting.ITALIC);
        if (shouldCenter) {
            context.centeredText(this.font, roleType, this.width / 2, currentY, teamColor);
        } else {
            context.text(this.font, roleType, leftX, currentY, teamColor);
        }
        currentY += this.font.lineHeight + 10;

        // Role Ability (wrapped)
        int textWidth = shouldCenter ? (this.width - leftX * 2) : (contentWidth - leftX * 2);
        String abilityString = "\"" + scriptRole.getAbility() + "\"";
        if (shouldCenter) {
            // Center each line for custom roles without almanac data
            currentY = drawCenteredWrappedText(context, Component.literal(abilityString), textWidth, currentY, 0xFFFFFFFF);
        } else {
            Component abilityText = Component.literal(abilityString).withStyle(ChatFormatting.WHITE);
            context.textWithWordWrap(this.font, abilityText, leftX, currentY, textWidth, 0xFFFFFFFF);
            currentY += this.font.wordWrapHeight(abilityText, textWidth);
        }
        currentY += 10;

        // Flavor Text
        if (scriptRole instanceof ScriptRole.Fabled fabled) {
            // Fabled/Loric - get flavor from NonPlayerCharacter, or fallback to almanac
            String flavor = fabled.fabledCharacter().flavor();

            // Use almanac flavor as backup if role doesn't have flavor text
            if ((flavor == null || flavor.isEmpty()) && almanacRoleData != null && almanacRoleData.hasFlavor()) {
                flavor = almanacRoleData.flavor();
            }

            if (flavor != null && !flavor.isEmpty()) {
                if (shouldCenter) {
                    drawCenteredWrappedText(context, Component.literal(flavor).withStyle(ChatFormatting.ITALIC), textWidth, currentY, 0xFFCCCCCC);
                } else {
                    Component flavorText = Component.literal(flavor).withStyle(ChatFormatting.ITALIC);
                    context.textWithWordWrap(this.font, flavorText, leftX, currentY, textWidth, 0xFFCCCCCC);
                }
            }
        } else if (scriptRole instanceof ScriptRole.Custom custom) {
            // Custom role - get flavor from CustomRole, or fallback to almanac
            CustomRole customRole = custom.customRole();
            String flavor = customRole.flavor();

            // Use almanac flavor as backup if role doesn't have flavor text
            if ((flavor == null || flavor.isEmpty()) && almanacRoleData != null && almanacRoleData.hasFlavor()) {
                flavor = almanacRoleData.flavor();
            }

            if (flavor != null && !flavor.isEmpty()) {
                if (shouldCenter) {
                    // Center each line for custom roles without almanac details
                    drawCenteredWrappedText(context, Component.literal(flavor).withStyle(ChatFormatting.ITALIC), textWidth, currentY, 0xFFCCCCCC);
                } else {
                    // Left-align for custom roles with almanac details (like official roles)
                    Component flavorText = Component.literal(flavor).withStyle(ChatFormatting.ITALIC);
                    context.textWithWordWrap(this.font, flavorText, leftX, currentY, textWidth, 0xFFCCCCCC);
                }
            }
        } else if (details != null) {
            // Official role - get flavor from RoleDetails
            Component flavorText = Component.literal(details.flavorText()).withStyle(ChatFormatting.ITALIC);
            context.textWithWordWrap(this.font, flavorText, leftX, currentY, textWidth, 0xFFCCCCCC);

            // Artist credit (only for official roles)
            int halfWidth = this.width / 2;
            Component artistText = Component.translatable("gui.blood-on-the-blocktower.character_details.artist", details.artist()).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC);
            int artistX = halfWidth + 110;
            int artistMaxWidth = this.width - artistX - 10;
            int artistY = this.height - 30 + (20 - this.font.lineHeight * 2) / 2;
            context.textWithWordWrap(this.font, artistText, artistX, artistY, artistMaxWidth, 0xFF888888);
        }
    }

    /**
     * Draws text wrapped to the specified width, with each line centered horizontally.
     * @return The Y position after the last line
     */
    private int drawCenteredWrappedText(GuiGraphicsExtractor context, Component text, int maxWidth, int startY, int color) {
        List<FormattedCharSequence> lines = this.font.split(text, maxWidth);
        int currentY = startY;
        for (FormattedCharSequence line : lines) {
            int lineWidth = this.font.width(line);
            int lineX = (this.width - lineWidth) / 2;
            context.text(this.font, line, lineX, currentY, color, true);
            currentY += this.font.lineHeight;
        }
        return currentY;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int keyCode = event.key();
        int scanCode = event.scancode();
        int modifiers = event.modifiers();

        if (KeyInputHandler.openMyRoleDetailsKey.matches(event)) {
            this.minecraft.gui.setScreen(this.parent);
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_E) {
            this.minecraft.gui.setScreen(this.parent);
            return true;
        }

        // Arrow key navigation
        if (this.catalogList != null && this.catalogIndex != -1) {
            if (keyCode == GLFW.GLFW_KEY_RIGHT && this.catalogIndex < this.catalogList.size() - 1) {
                ScriptRole nextRole = this.catalogList.get(this.catalogIndex + 1);
                this.minecraft.gui.setScreen(new CharacterDetailsScreen(nextRole, this.parent, this.catalogList));
                return true;
            }

            if (keyCode == GLFW.GLFW_KEY_LEFT && this.catalogIndex > 0) {
                ScriptRole prevRole = this.catalogList.get(this.catalogIndex - 1);
                this.minecraft.gui.setScreen(new CharacterDetailsScreen(prevRole, this.parent, this.catalogList));
                return true;
            }
        }

        return super.keyPressed(event);
    }

    /**
     * Escape stays a quick way straight back to the game, except when the parent is a
     * {@link ReturnOnClose} screen holding editing state, in which case it hands control back to that
     * parent and leaving goes through the parent's own exit.
     */
    @Override
    public void onClose() {
        if (this.parent instanceof ReturnOnClose) {
            this.minecraft.gui.setScreen(this.parent);
            return;
        }
        super.onClose();
    }

    private class DetailsListWidget extends ContainerObjectSelectionList<DocumentEntry> {
        public DetailsListWidget(Minecraft client, int width, int height, int y) {
            super(client, width, height, y, client.font.lineHeight + 1);

            int textWidth = this.getRowWidth() - 10; // Padding inside the list

            List<String> specialRules = AbilityText.bootleggerRules(scriptRole, ClientState.currentScript);
            if (specialRules != null) {
                this.addEntry(DocumentEntry.title(font, Component.translatable("gui.blood-on-the-blocktower.character_details.special_rules").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)));
                String prefix = specialRules.size() > 1 ? "- " : "";
                for (String rule : specialRules) {
                    for (FormattedCharSequence line : font.split(Component.literal(prefix + rule), textWidth)) {
                        this.addEntry(DocumentEntry.text(font, line, 0xFF55FFFF));
                    }
                }
                this.addEntry(DocumentEntry.spacer());
                this.addEntry(DocumentEntry.spacer());
            }

            if (details != null) {
                // Official role - show Summary and Examples from RoleDetails
                this.addEntry(DocumentEntry.title(font, Component.translatable("gui.blood-on-the-blocktower.character_details.summary").withStyle(ChatFormatting.GOLD)));
                addMultilineText(details.summary(), textWidth);

                // Spacer
                this.addEntry(DocumentEntry.spacer());
                this.addEntry(DocumentEntry.spacer());

                // Examples Section
                this.addEntry(DocumentEntry.title(font, Component.translatable("gui.blood-on-the-blocktower.character_details.examples").withStyle(ChatFormatting.GOLD)));
                addMultilineText(details.examples(), textWidth);
            } else if (almanacRoleData != null) {
                // Custom role - show sections from almanac data
                boolean needsSpacer = false;

                // Overview section
                if (almanacRoleData.hasOverview()) {
                    if (needsSpacer) { this.addEntry(DocumentEntry.spacer()); }
                    this.addEntry(DocumentEntry.title(font, Component.translatable("gui.blood-on-the-blocktower.character_details.overview").withStyle(ChatFormatting.GOLD)));
                    addMultilineText(almanacRoleData.overview(), textWidth);
                    needsSpacer = true;
                }

                // Examples section
                if (almanacRoleData.hasExamples()) {
                    if (needsSpacer) { this.addEntry(DocumentEntry.spacer()); }
                    this.addEntry(DocumentEntry.title(font, Component.translatable("gui.blood-on-the-blocktower.character_details.examples").withStyle(ChatFormatting.GOLD)));
                    addMultilineText(almanacRoleData.examples(), textWidth);
                    needsSpacer = true;
                }

                // How To Run section
                if (almanacRoleData.hasHowToRun()) {
                    if (needsSpacer) { this.addEntry(DocumentEntry.spacer()); }
                    this.addEntry(DocumentEntry.title(font, Component.translatable("gui.blood-on-the-blocktower.character_details.how_to_run").withStyle(ChatFormatting.GOLD)));
                    addMultilineText(almanacRoleData.howToRun(), textWidth);
                    needsSpacer = true;
                }

                // Tip section
                if (almanacRoleData.hasTip()) {
                    if (needsSpacer) { this.addEntry(DocumentEntry.spacer()); }
                    this.addEntry(DocumentEntry.title(font, Component.translatable("gui.blood-on-the-blocktower.character_details.tip").withStyle(ChatFormatting.GOLD)));
                    addMultilineText(almanacRoleData.tip(), textWidth);
                }
            }
        }

        private static final String BULLET = "\u2022 ";

        private void addMultilineText(String content, int textWidth) {
            // Split by line breaks first, then wrap each paragraph
            String[] paragraphs = content.split("\n");
            boolean needsGap = false;
            for (String paragraph : paragraphs) {
                if (paragraph.isEmpty()) {
                    // Empty line = paragraph break
                    this.addEntry(DocumentEntry.spacer());
                    needsGap = false;
                    continue;
                }
                if (needsGap) {
                    // Add gap between paragraphs
                    this.addEntry(DocumentEntry.spacer());
                }
                needsGap = true;
                boolean bulleted = paragraph.startsWith("- ");
                String body = bulleted ? paragraph.substring(2) : paragraph;
                int indent = bulleted ? font.width(BULLET) : 0;
                List<FormattedCharSequence> lines = font.split(Component.literal(body), textWidth - indent);
                for (int i = 0; i < lines.size(); i++) {
                    FormattedCharSequence line = lines.get(i);
                    if (bulleted && i == 0) {
                        line = FormattedCharSequence.composite(FormattedCharSequence.forward(BULLET, Style.EMPTY), line);
                        this.addEntry(DocumentEntry.text(font, line, 0xFFFFFFFF));
                    } else {
                        this.addEntry(DocumentEntry.text(font, line, 0xFFFFFFFF, indent));
                    }
                }
            }
        }

        @Override
        public int getRowWidth() {
            return this.width;
        }

        @Override
        protected int scrollBarX() {
            return this.getX() + this.width - 6;
        }

    }
}