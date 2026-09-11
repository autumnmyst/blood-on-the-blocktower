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
        super(Text.translatable("gui.blood-on-the-blocktower.settings.title"));
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
                Text.translatable("gui.blood-on-the-blocktower.settings.volume.nominations"), ClientState.volumeNominations,
                value -> {
                    ClientState.volumeNominations = value;
                    PlayerConfig.save();
                }
        ));
        currentY += sliderHeight + sliderSpacing;

        // Dawn/Dusk Volume
        this.addDrawableChild(new VolumeSlider(
                leftColumnX, currentY, sliderWidth, sliderHeight,
                Text.translatable("gui.blood-on-the-blocktower.settings.volume.dawn_dusk"), ClientState.volumeDawnDusk,
                value -> {
                    ClientState.volumeDawnDusk = value;
                    PlayerConfig.save();
                }
        ));
        currentY += sliderHeight + sliderSpacing;

        // Visit Sound Volume (doorbell/doorknock - storyteller chooses which sound)
        this.addDrawableChild(new VolumeSlider(
                leftColumnX, currentY, sliderWidth, sliderHeight,
                Text.translatable("gui.blood-on-the-blocktower.settings.volume.visit_sound"), ClientState.volumeDoorbell,
                value -> {
                    ClientState.volumeDoorbell = value;
                    PlayerConfig.save();
                }
        ));
        currentY += sliderHeight + sliderSpacing;

        // Role Receive Volume
        this.addDrawableChild(new VolumeSlider(
                leftColumnX, currentY, sliderWidth, sliderHeight,
                Text.translatable("gui.blood-on-the-blocktower.settings.volume.role_receive"), ClientState.volumeRoleReceive,
                value -> {
                    ClientState.volumeRoleReceive = value;
                    PlayerConfig.save();
                }
        ));
        currentY += sliderHeight + sliderSpacing;

        // Final Reveal Volume (game_end sound)
        this.addDrawableChild(new VolumeSlider(
                leftColumnX, currentY, sliderWidth, sliderHeight,
                Text.translatable("gui.blood-on-the-blocktower.settings.volume.final_reveal"), ClientState.volumeFinalReveal,
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
        Text fadeText = Text.translatable("gui.blood-on-the-blocktower.settings.fade_heads", onOff(ClientState.fadeOutOfGroupHeads));
        this.addDrawableChild(ButtonWidget.builder(fadeText, b -> {
            ClientState.fadeOutOfGroupHeads = !ClientState.fadeOutOfGroupHeads;
            PlayerConfig.save();
            this.client.setScreen(this);
        }).dimensions(leftColX, currentY, prefButtonWidth, sliderHeight)
        .tooltip(Tooltip.of(Text.translatable("gui.blood-on-the-blocktower.settings.tooltip.fade_heads")))
        .build());

        Text animationsText = Text.translatable("gui.blood-on-the-blocktower.settings.animations", onOff(ClientState.grimoireAnimationsEnabled));
        this.addDrawableChild(ButtonWidget.builder(animationsText, b -> {
            ClientState.grimoireAnimationsEnabled = !ClientState.grimoireAnimationsEnabled;
            PlayerConfig.save();
            this.client.setScreen(this);
        }).dimensions(rightColX, currentY, prefButtonWidth, sliderHeight)
        .tooltip(Tooltip.of(Text.translatable("gui.blood-on-the-blocktower.settings.tooltip.animations")))
        .build());

        currentY += sliderHeight + sliderSpacing;

        // Row 2: Hints | Floating Role Icons
        Text hintsText = Text.translatable("gui.blood-on-the-blocktower.settings.hints", onOff(ClientState.hintsEnabled));
        this.addDrawableChild(ButtonWidget.builder(hintsText, b -> {
            ClientState.hintsEnabled = !ClientState.hintsEnabled;
            PlayerConfig.save();
            this.client.setScreen(this);
        }).dimensions(leftColX, currentY, prefButtonWidth, sliderHeight)
        .tooltip(Tooltip.of(Text.translatable("gui.blood-on-the-blocktower.settings.tooltip.hints")))
        .build());

        Text iconsText = Text.translatable("gui.blood-on-the-blocktower.settings.role_icons", ClientState.floatingRoleIconMode.displayName());
        this.addDrawableChild(ButtonWidget.builder(iconsText, b -> {
            ClientState.floatingRoleIconMode = ClientState.floatingRoleIconMode.cycle();
            PlayerConfig.save();
            this.client.setScreen(this);
        }).dimensions(rightColX, currentY, prefButtonWidth, sliderHeight)
        .tooltip(Tooltip.of(Text.translatable("gui.blood-on-the-blocktower.settings.tooltip.role_icons")))
        .build());

        // ========================================
        // BOTTOM: Whisper Rules, Glossary, Back Buttons
        // ========================================
        int backButtonWidth = 60;
        int glossaryButtonWidth = 80;
        int whisperButtonWidth = 100;
        int buttonSpacingBottom = 5;

        // Whisper Rules button: read-only view for players. Storytellers edit this elsewhere.
        boolean isStoryteller = this.client != null && this.client.player != null
                && this.client.player.hasPermissionLevel(2);
        if (!isStoryteller) {
            this.addDrawableChild(ButtonWidget.builder(
                    Text.translatable("gui.blood-on-the-blocktower.settings.whisper_rules").formatted(Formatting.LIGHT_PURPLE),
                    button -> this.client.setScreen(new WhisperSettingsScreen(this, false))
            ).dimensions(
                    this.width - backButtonWidth - glossaryButtonWidth - whisperButtonWidth - 2 * buttonSpacingBottom - 10,
                    this.height - 30, whisperButtonWidth, 20)
            .tooltip(Tooltip.of(Text.translatable("gui.blood-on-the-blocktower.settings.tooltip.whisper_rules")))
            .build());
        }

        // Glossary button
        this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("gui.blood-on-the-blocktower.settings.glossary").formatted(Formatting.GOLD),
                button -> this.client.setScreen(new GlossaryScreen(this))
        ).dimensions(this.width - backButtonWidth - glossaryButtonWidth - buttonSpacingBottom - 10, this.height - 30, glossaryButtonWidth, 20)
        .tooltip(Tooltip.of(Text.translatable("gui.blood-on-the-blocktower.settings.tooltip.glossary")))
        .build());

        // Back button
        this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("gui.blood-on-the-blocktower.settings.back").formatted(Formatting.YELLOW),
                button -> this.client.setScreen(this.parent)
        ).dimensions(this.width - backButtonWidth - 10, this.height - 30, backButtonWidth, 20).build());

        // Credits button, bottom-left
        this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("gui.blood-on-the-blocktower.settings.credits").formatted(Formatting.AQUA),
                button -> this.client.setScreen(new CreditsScreen(this))
        ).dimensions(10, this.height - 30, 70, 20)
        .tooltip(Tooltip.of(Text.translatable("gui.blood-on-the-blocktower.settings.tooltip.credits")))
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
        context.drawTextWithShadow(this.textRenderer, Text.translatable("gui.blood-on-the-blocktower.settings.category.volume").formatted(Formatting.GOLD), leftColumnX, 25, 0xFFFFFF);

        // Calculate preferences category Y position (after 5 volume sliders)
        int grimoireCategoryY = 25 + 12 + 2 + (5 * (20 + 5)) + 15;
        context.drawTextWithShadow(this.textRenderer, Text.translatable("gui.blood-on-the-blocktower.settings.category.preferences").formatted(Formatting.GOLD), leftColumnX, grimoireCategoryY, 0xFFFFFF);
    }

    private static Text onOff(boolean on) {
        return Text.translatable(on
                ? "gui.blood-on-the-blocktower.settings.on"
                : "gui.blood-on-the-blocktower.settings.off");
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
        private final Text label;
        private final VolumeCallback callback;

        @FunctionalInterface
        interface VolumeCallback {
            void apply(float value);
        }

        public VolumeSlider(int x, int y, int width, int height, Text label, float initialValue, VolumeCallback callback) {
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
            this.setMessage(Text.translatable("gui.blood-on-the-blocktower.settings.volume_format", label, percent));
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
            this.addEntry(DocumentEntry.title(textRenderer, Text.translatable("gui.blood-on-the-blocktower.settings.doc.hotkeys").formatted(Formatting.GOLD, Formatting.BOLD)));
            this.addEntry(DocumentEntry.spacer());

            addHotkeyEntry(Text.translatable("gui.blood-on-the-blocktower.settings.hotkey.open_grimoire"), KeyInputHandler.openAssignGui);
            addHotkeyEntry(Text.translatable("gui.blood-on-the-blocktower.settings.hotkey.open_script"), KeyInputHandler.openScriptKey);
            addHotkeyEntry(Text.translatable("gui.blood-on-the-blocktower.settings.hotkey.open_role_details"), KeyInputHandler.openMyRoleDetailsKey);
            addHotkeyEntry(Text.translatable("gui.blood-on-the-blocktower.settings.hotkey.open_role_catalog"), KeyInputHandler.openCatalogKey);
            addHotkeyEntry(Text.translatable("gui.blood-on-the-blocktower.settings.hotkey.toggle_role_hud"), KeyInputHandler.toggleShowRole);
            addHotkeyEntry(Text.translatable("gui.blood-on-the-blocktower.settings.hotkey.grimoire_quick_view"), KeyInputHandler.quickRoleViewKey);
            addHotkeyEntry(Text.translatable("gui.blood-on-the-blocktower.settings.hotkey.toggle_sidebar"), KeyInputHandler.toggleSidebarKey);
            this.addEntry(DocumentEntry.spacer());
            this.addEntry(DocumentEntry.spacer());

            // --- Grimoire Section ---
            this.addEntry(DocumentEntry.title(textRenderer, Text.translatable("gui.blood-on-the-blocktower.settings.doc.grimoire").formatted(Formatting.GOLD, Formatting.BOLD)));
            this.addEntry(DocumentEntry.spacer());

            this.addEntry(DocumentEntry.title(textRenderer, Text.translatable("gui.blood-on-the-blocktower.settings.doc.reminders").formatted(Formatting.GRAY, Formatting.ITALIC)));
            addWrappedText(Text.translatable("gui.blood-on-the-blocktower.settings.doc.reminders_body"), textWidth);
            this.addEntry(DocumentEntry.spacer());

            this.addEntry(DocumentEntry.title(textRenderer, Text.translatable("gui.blood-on-the-blocktower.settings.doc.role_assignment").formatted(Formatting.GRAY, Formatting.ITALIC)));
            addWrappedText(Text.translatable("gui.blood-on-the-blocktower.settings.doc.role_assignment_body"), textWidth);
            this.addEntry(DocumentEntry.spacer());

            this.addEntry(DocumentEntry.title(textRenderer, Text.translatable("gui.blood-on-the-blocktower.settings.doc.viewing_role_details").formatted(Formatting.GRAY, Formatting.ITALIC)));
            addColoredModifierText(Text.empty(), "Shift", Text.translatable("gui.blood-on-the-blocktower.settings.doc.shift_click_role"), textWidth);
            this.addEntry(DocumentEntry.spacer());
            addColoredModifierText(Text.translatable("gui.blood-on-the-blocktower.settings.doc.hold"), "Shift", Text.translatable("gui.blood-on-the-blocktower.settings.doc.shift_hover_role"), textWidth);
            this.addEntry(DocumentEntry.spacer());

            // --- Reference Section ---
            this.addEntry(DocumentEntry.title(textRenderer, Text.translatable("gui.blood-on-the-blocktower.settings.doc.reference").formatted(Formatting.GOLD, Formatting.BOLD)));
            this.addEntry(DocumentEntry.spacer());

            this.addEntry(DocumentEntry.title(textRenderer, Text.translatable("gui.blood-on-the-blocktower.settings.doc.script_reference").formatted(Formatting.GRAY, Formatting.ITALIC)));
            addWrappedText(Text.translatable("gui.blood-on-the-blocktower.settings.doc.script_reference_body"), textWidth);
            this.addEntry(DocumentEntry.spacer());
            addColoredTabEntry(Text.translatable("gui.blood-on-the-blocktower.settings.doc.tab.roles"), Text.translatable("gui.blood-on-the-blocktower.settings.doc.tab.roles_desc"), textWidth);
            this.addEntry(DocumentEntry.spacer());
            addColoredTabEntry(Text.translatable("gui.blood-on-the-blocktower.settings.doc.tab.night_order"), Text.translatable("gui.blood-on-the-blocktower.settings.doc.tab.night_order_desc"), textWidth);
            this.addEntry(DocumentEntry.spacer());
            addColoredTabEntry(Text.translatable("gui.blood-on-the-blocktower.settings.doc.tab.jinxes"), Text.translatable("gui.blood-on-the-blocktower.settings.doc.tab.jinxes_desc"), textWidth);
            this.addEntry(DocumentEntry.spacer());
            addWrappedText(Text.translatable("gui.blood-on-the-blocktower.settings.doc.click_role_details"), textWidth);
            this.addEntry(DocumentEntry.spacer());
            addColoredModifierText(Text.empty(), "Ctrl", Text.translatable("gui.blood-on-the-blocktower.settings.doc.ctrl_click_cross_out"), textWidth);
            this.addEntry(DocumentEntry.spacer());

            this.addEntry(DocumentEntry.title(textRenderer, Text.translatable("gui.blood-on-the-blocktower.settings.doc.role_catalog").formatted(Formatting.GRAY, Formatting.ITALIC)));
            addWrappedText(Text.translatable("gui.blood-on-the-blocktower.settings.doc.role_catalog_body"), textWidth);
        }

        private void addWrappedText(Text text, int width) {
            for (OrderedText line : textRenderer.wrapLines(text, width)) {
                this.addEntry(DocumentEntry.text(textRenderer, line, 0xFFFFFF));
            }
        }

        private void addColoredTabEntry(Text tabName, Text description, int width) {
            MutableText text = Text.literal("\u2022 ").formatted(Formatting.WHITE)
                    .append(tabName.copy().formatted(Formatting.GOLD))
                    .append(Text.translatable("gui.blood-on-the-blocktower.settings.doc.tab_desc", description).formatted(Formatting.WHITE));
            for (OrderedText line : textRenderer.wrapLines(text, width)) {
                this.addEntry(DocumentEntry.text(textRenderer, line, 0xFFFFFF));
            }
        }

        private void addColoredModifierText(Text prefix, String modifier, Text suffix, int width) {
            // Color modifiers: Shift = aqua, Ctrl = yellow
            Formatting modColor = modifier.equals("Shift") ? Formatting.AQUA : Formatting.YELLOW;
            MutableText text = prefix.copy().formatted(Formatting.WHITE)
                    .append(Text.literal(modifier).formatted(modColor))
                    .append(suffix.copy().formatted(Formatting.WHITE));
            for (OrderedText line : textRenderer.wrapLines(text, width)) {
                this.addEntry(DocumentEntry.text(textRenderer, line, 0xFFFFFF));
            }
        }

        private void addHotkeyEntry(Text action, KeyBinding keyBinding) {
            Text keyName = keyBinding != null ? keyBinding.getBoundKeyLocalizedText() : Text.translatable("gui.blood-on-the-blocktower.settings.not_bound");
            this.addEntry(DocumentEntry.text(textRenderer, Text.translatable("gui.blood-on-the-blocktower.settings.hotkey_line", action).formatted(Formatting.WHITE)
                    .append(Text.literal("[").append(keyName).append("]").formatted(Formatting.YELLOW)).asOrderedText(), 0xFFFFFF));
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
