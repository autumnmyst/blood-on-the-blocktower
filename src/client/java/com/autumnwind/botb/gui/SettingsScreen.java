package com.autumnwind.botb.gui;

import com.autumnwind.botb.config.PlayerConfig;
import com.autumnwind.botb.event.KeyInputHandler;
import com.autumnwind.botb.states.ClientState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ElementListWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.MutableText;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.option.KeyBinding;
import com.autumnwind.botb.gui.widget.DocumentEntry;

/**
 * Settings screen - volume controls and player documentation.
 * Available to all players (not just operators).
 */
public class SettingsScreen extends Screen {

    private final Screen parent;
    private DocumentationListWidget documentationWidget;

    public SettingsScreen(Screen parent) {
        super(Text.literal("Settings"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int sliderWidth = 150;
        int sliderHeight = 20;
        int sliderSpacing = 5;
        int leftColumnX = 20;
        int labelHeight = 12;
        int categorySpacing = 2;

        // ========================================
        // LEFT SIDE: VOLUME CONTROLS
        // Volume multipliers: 0% to 100% (stored as 0.0 to 1.0)
        // ========================================

        int volumeCategoryY = 25;
        int currentY = volumeCategoryY + labelHeight + categorySpacing;

        // Nominations Volume
        this.addDrawableChild(new VolumeSlider(
                leftColumnX, currentY, sliderWidth, sliderHeight,
                "Nominations", ClientState.volumeNominations,
                value -> {
                    ClientState.volumeNominations = value;
                    PlayerConfig.save();
                }
        ));
        currentY += sliderHeight + sliderSpacing;

        // Dawn/Dusk Volume
        this.addDrawableChild(new VolumeSlider(
                leftColumnX, currentY, sliderWidth, sliderHeight,
                "Dawn/Dusk", ClientState.volumeDawnDusk,
                value -> {
                    ClientState.volumeDawnDusk = value;
                    PlayerConfig.save();
                }
        ));
        currentY += sliderHeight + sliderSpacing;

        // Visit Sound Volume (doorbell/doorknock - storyteller chooses which sound)
        this.addDrawableChild(new VolumeSlider(
                leftColumnX, currentY, sliderWidth, sliderHeight,
                "Visit Sound", ClientState.volumeDoorbell,
                value -> {
                    ClientState.volumeDoorbell = value;
                    PlayerConfig.save();
                }
        ));
        currentY += sliderHeight + sliderSpacing;

        // Role Receive Volume
        this.addDrawableChild(new VolumeSlider(
                leftColumnX, currentY, sliderWidth, sliderHeight,
                "Role Receive", ClientState.volumeRoleReceive,
                value -> {
                    ClientState.volumeRoleReceive = value;
                    PlayerConfig.save();
                }
        ));
        currentY += sliderHeight + sliderSpacing;

        // Final Reveal Volume (game_end sound)
        this.addDrawableChild(new VolumeSlider(
                leftColumnX, currentY, sliderWidth, sliderHeight,
                "Final Reveal", ClientState.volumeFinalReveal,
                value -> {
                    ClientState.volumeFinalReveal = value;
                    PlayerConfig.save();
                }
        ));
        currentY += sliderHeight + sliderSpacing + 15;

        // ========================================
        // PREFERENCES SETTINGS (2x2 grid)
        // ========================================

        int grimoireCategoryY = currentY;
        currentY += labelHeight + categorySpacing;

        // Preferences buttons are wider than the volume sliders
        int prefButtonWidth = 110;
        int leftColX = leftColumnX;
        int rightColX = leftColumnX + prefButtonWidth + sliderSpacing;

        // Row 1: Fade Heads | Animations
        Text fadeText = Text.literal("Fade Heads: " + (ClientState.fadeOutOfGroupHeads ? "ON" : "OFF"));
        this.addDrawableChild(ButtonWidget.builder(fadeText, b -> {
            ClientState.fadeOutOfGroupHeads = !ClientState.fadeOutOfGroupHeads;
            PlayerConfig.save();
            this.client.setScreen(this);
        }).dimensions(leftColX, currentY, prefButtonWidth, sliderHeight)
        .tooltip(Tooltip.of(Text.literal("Fade player heads in grimoire when not in your voice chat group")))
        .build());

        Text animationsText = Text.literal("Animations: " + (ClientState.grimoireAnimationsEnabled ? "ON" : "OFF"));
        this.addDrawableChild(ButtonWidget.builder(animationsText, b -> {
            ClientState.grimoireAnimationsEnabled = !ClientState.grimoireAnimationsEnabled;
            PlayerConfig.save();
            this.client.setScreen(this);
        }).dimensions(rightColX, currentY, prefButtonWidth, sliderHeight)
        .tooltip(Tooltip.of(Text.literal("Enable or disable grimoire entry fade-in animations")))
        .build());

        currentY += sliderHeight + sliderSpacing;

        // Row 2: Hints | Floating Role Icons
        Text hintsText = Text.literal("Hints: " + (ClientState.hintsEnabled ? "ON" : "OFF"));
        this.addDrawableChild(ButtonWidget.builder(hintsText, b -> {
            ClientState.hintsEnabled = !ClientState.hintsEnabled;
            PlayerConfig.save();
            this.client.setScreen(this);
        }).dimensions(leftColX, currentY, prefButtonWidth, sliderHeight)
        .tooltip(Tooltip.of(Text.literal("Show helpful hints and tips")))
        .build());

        Text iconsText = Text.literal("Role Icons: " + ClientState.floatingRoleIconMode.displayName());
        this.addDrawableChild(ButtonWidget.builder(iconsText, b -> {
            ClientState.floatingRoleIconMode = ClientState.floatingRoleIconMode.cycle();
            PlayerConfig.save();
            this.client.setScreen(this);
        }).dimensions(rightColX, currentY, prefButtonWidth, sliderHeight)
        .tooltip(Tooltip.of(Text.literal("Floating role icons above player heads: OFF / ALWAYS / GAME END (after game end only, cleared on reset)")))
        .build());

        // ========================================
        // BOTTOM: Whisper Rules, Glossary, Back Buttons
        // ========================================
        int backButtonWidth = 60;
        int glossaryButtonWidth = 80;
        int whisperButtonWidth = 100;
        int buttonSpacingBottom = 5;

        // Whisper Rules button: read-only view for non-storytellers, editable for ops
        boolean canEditWhispers = this.client != null && this.client.player != null
                && this.client.player.hasPermissionLevel(2);
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Whisper Rules").formatted(Formatting.LIGHT_PURPLE),
                button -> this.client.setScreen(new WhisperSettingsScreen(this, canEditWhispers))
        ).dimensions(
                this.width - backButtonWidth - glossaryButtonWidth - whisperButtonWidth - 2 * buttonSpacingBottom - 10,
                this.height - 30, whisperButtonWidth, 20)
        .tooltip(Tooltip.of(Text.literal(canEditWhispers
                ? "Configure whisper rules"
                : "View the active whisper rules (set by the storyteller)")))
        .build());

        // Glossary button
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Glossary").formatted(Formatting.GOLD),
                button -> this.client.setScreen(new GlossaryScreen(this))
        ).dimensions(this.width - backButtonWidth - glossaryButtonWidth - buttonSpacingBottom - 10, this.height - 30, glossaryButtonWidth, 20)
        .tooltip(Tooltip.of(Text.literal("View game terminology and rules")))
        .build());

        // Back button
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Back").formatted(Formatting.YELLOW),
                button -> this.client.setScreen(this.parent)
        ).dimensions(this.width - backButtonWidth - 10, this.height - 30, backButtonWidth, 20).build());

        // Credits button, bottom-left
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Credits").formatted(Formatting.AQUA),
                button -> this.client.setScreen(new CreditsScreen(this))
        ).dimensions(10, this.height - 30, 70, 20)
        .tooltip(Tooltip.of(Text.literal("Who made this")))
        .build());

        // ========================================
        // RIGHT SIDE: DOCUMENTATION WIDGET
        // ========================================
        int docX = this.width / 2 + 10;
        int docY = 30;
        int docWidth = this.width / 2 - 20;
        int docHeight = this.height - docY - 40;

        this.documentationWidget = new DocumentationListWidget(this.client, docWidth, docHeight, docY);
        this.documentationWidget.setX(docX);
        this.addDrawableChild(this.documentationWidget);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        // Draw title
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 10, 0xFFFFFF);

        // Draw category labels
        int leftColumnX = 20;
        context.drawTextWithShadow(this.textRenderer, Text.literal("Volume").formatted(Formatting.GOLD), leftColumnX, 25, 0xFFFFFF);

        // Calculate preferences category Y position (after 5 volume sliders)
        int grimoireCategoryY = 25 + 12 + 2 + (5 * (20 + 5)) + 15;
        context.drawTextWithShadow(this.textRenderer, Text.literal("Preferences").formatted(Formatting.GOLD), leftColumnX, grimoireCategoryY, 0xFFFFFF);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        boolean exitKeyPressed = keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_E;
        boolean openAssignGuiPressed = KeyInputHandler.openAssignGui != null && KeyInputHandler.openAssignGui.matchesKey(keyCode, scanCode);
        if (exitKeyPressed || openAssignGuiPressed) {
            this.client.setScreen(this.parent);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    /**
     * Custom slider widget for volume control.
     * Range: 0% to 100% (stored as 0.0 to 1.0 multiplier)
     */
    private static class VolumeSlider extends SliderWidget {
        private final String label;
        private final VolumeCallback callback;

        @FunctionalInterface
        interface VolumeCallback {
            void apply(float value);
        }

        public VolumeSlider(int x, int y, int width, int height, String label, float initialValue, VolumeCallback callback) {
            // Slider value is 0.0-1.0, same as volume multiplier
            super(x, y, width, height, Text.empty(), initialValue);
            this.label = label;
            this.callback = callback;
            this.updateMessage();
        }

        @Override
        protected void updateMessage() {
            // value is 0.0-1.0, multiply by 100 to get percentage
            int percent = (int) (this.value * 100);
            this.setMessage(Text.literal(label + ": " + percent + "%"));
        }

        @Override
        protected void applyValue() {
            // Slider value (0.0-1.0) is the volume multiplier directly
            callback.apply((float) this.value);
        }
    }

    /**
     * Scrollable documentation widget showing player hotkeys, grimoire help, and reference info.
     */
    private class DocumentationListWidget extends ElementListWidget<DocumentEntry> {

        public DocumentationListWidget(MinecraftClient client, int width, int height, int y) {
            super(client, width, height, y, client.textRenderer.fontHeight + 2);

            int textWidth = this.getRowWidth() - 10;

            // --- Hotkeys Section ---
            this.addEntry(DocumentEntry.title(textRenderer, Text.literal("Hotkeys").formatted(Formatting.GOLD, Formatting.BOLD)));
            this.addEntry(DocumentEntry.spacer());

            addHotkeyEntry("Open Grimoire", KeyInputHandler.openAssignGui);
            addHotkeyEntry("Open Script", KeyInputHandler.openScriptKey);
            addHotkeyEntry("Open Role Details", KeyInputHandler.openMyRoleDetailsKey);
            addHotkeyEntry("Open Role Catalog", KeyInputHandler.openCatalogKey);
            addHotkeyEntry("Toggle Role HUD", KeyInputHandler.toggleShowRole);
            addHotkeyEntry("Grimoire Quick View", KeyInputHandler.quickRoleViewKey);
            addHotkeyEntry("Toggle Sidebar", KeyInputHandler.toggleSidebarKey);
            this.addEntry(DocumentEntry.spacer());
            this.addEntry(DocumentEntry.spacer());

            // --- Grimoire Section ---
            this.addEntry(DocumentEntry.title(textRenderer, Text.literal("Grimoire").formatted(Formatting.GOLD, Formatting.BOLD)));
            this.addEntry(DocumentEntry.spacer());

            this.addEntry(DocumentEntry.title(textRenderer, Text.literal("Reminders").formatted(Formatting.GRAY, Formatting.ITALIC)));
            addWrappedText("Click on a player's head to add a reminder token. Reminders help track ability effects, poisoning, protection, and other game states.", textWidth);
            this.addEntry(DocumentEntry.spacer());

            this.addEntry(DocumentEntry.title(textRenderer, Text.literal("Role Assignment").formatted(Formatting.GRAY, Formatting.ITALIC)));
            addWrappedText("Click on a player's role icon to assign them a role. Use this to track who you think each player is, or as the Storyteller to assign actual roles.", textWidth);
            this.addEntry(DocumentEntry.spacer());

            this.addEntry(DocumentEntry.title(textRenderer, Text.literal("Viewing Role Details").formatted(Formatting.GRAY, Formatting.ITALIC)));
            addColoredModifierText("", "Shift", "+Click on a role icon to view its full details and ability description.", textWidth);
            this.addEntry(DocumentEntry.spacer());
            addColoredModifierText("Hold ", "Shift", " while hovering over an assigned role or demon bluff to see its details in a tooltip.", textWidth);
            this.addEntry(DocumentEntry.spacer());

            // --- Reference Section ---
            this.addEntry(DocumentEntry.title(textRenderer, Text.literal("Reference").formatted(Formatting.GOLD, Formatting.BOLD)));
            this.addEntry(DocumentEntry.spacer());

            this.addEntry(DocumentEntry.title(textRenderer, Text.literal("Script Reference").formatted(Formatting.GRAY, Formatting.ITALIC)));
            addWrappedText("Open the Script Reference screen to view all roles in the current script. The screen has tabs for:", textWidth);
            this.addEntry(DocumentEntry.spacer());
            addColoredTabEntry("Roles", "All roles in the current script by type", textWidth);
            this.addEntry(DocumentEntry.spacer());
            addColoredTabEntry("Night Order", "The order abilities activate at night", textWidth);
            this.addEntry(DocumentEntry.spacer());
            addColoredTabEntry("Jinxes", "Special interactions between roles", textWidth);
            this.addEntry(DocumentEntry.spacer());
            addWrappedText("Click any role to view its full details.", textWidth);
            this.addEntry(DocumentEntry.spacer());
            addColoredModifierText("", "Ctrl", "+Click on a role to cross it out, used for tracking which roles you believe are not in play.", textWidth);
            this.addEntry(DocumentEntry.spacer());

            this.addEntry(DocumentEntry.title(textRenderer, Text.literal("Role Catalog").formatted(Formatting.GRAY, Formatting.ITALIC)));
            addWrappedText("The Role Catalog shows ALL roles in the game, not just those in the current script.", textWidth);
        }

        private void addWrappedText(String text, int width) {
            for (OrderedText line : textRenderer.wrapLines(Text.literal(text), width)) {
                this.addEntry(DocumentEntry.text(textRenderer, line, 0xFFFFFF));
            }
        }

        private void addColoredTabEntry(String tabName, String description, int width) {
            MutableText text = Text.literal("\u2022 ").formatted(Formatting.WHITE)
                    .append(Text.literal(tabName).formatted(Formatting.GOLD))
                    .append(Text.literal(" - " + description).formatted(Formatting.WHITE));
            for (OrderedText line : textRenderer.wrapLines(text, width)) {
                this.addEntry(DocumentEntry.text(textRenderer, line, 0xFFFFFF));
            }
        }

        private void addColoredModifierText(String prefix, String modifier, String suffix, int width) {
            // Color modifiers: Shift = aqua, Ctrl = yellow
            Formatting modColor = modifier.equals("Shift") ? Formatting.AQUA : Formatting.YELLOW;
            MutableText text = Text.literal(prefix).formatted(Formatting.WHITE)
                    .append(Text.literal(modifier).formatted(modColor))
                    .append(Text.literal(suffix).formatted(Formatting.WHITE));
            for (OrderedText line : textRenderer.wrapLines(text, width)) {
                this.addEntry(DocumentEntry.text(textRenderer, line, 0xFFFFFF));
            }
        }

        private void addHotkeyEntry(String action, KeyBinding keyBinding) {
            String keyName = keyBinding != null ? keyBinding.getBoundKeyLocalizedText().getString() : "Not bound";
            this.addEntry(DocumentEntry.text(textRenderer, Text.literal("\u2022 " + action + ": ").formatted(Formatting.WHITE)
                    .append(Text.literal("[" + keyName + "]").formatted(Formatting.YELLOW)).asOrderedText(), 0xFFFFFF));
        }

        @Override
        public int getRowWidth() {
            return this.width - 20;
        }

        @Override
        protected int getScrollbarX() {
            return this.getX() + this.width - 6;
        }

    }
}
