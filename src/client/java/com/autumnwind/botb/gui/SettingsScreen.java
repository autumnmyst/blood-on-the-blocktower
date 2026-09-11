package com.autumnwind.botb.gui;

import com.autumnwind.botb.config.PlayerConfig;
import com.autumnwind.botb.event.KeyInputHandler;
import com.autumnwind.botb.states.ClientState;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;
import org.lwjgl.glfw.GLFW;
import com.autumnwind.botb.gui.widget.DocumentEntry;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.client.input.KeyEvent;

/**
 * Settings screen - volume controls and player documentation.
 * Available to all players (not just operators).
 */
public class SettingsScreen extends Screen {

    private final Screen parent;
    private DocumentationListWidget documentationWidget;

    public SettingsScreen(Screen parent) {
        super(Component.translatable("gui.blood-on-the-blocktower.settings.title"));
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
        this.addRenderableWidget(new VolumeSlider(
                leftColumnX, currentY, sliderWidth, sliderHeight,
                Component.translatable("gui.blood-on-the-blocktower.settings.volume.nominations"), ClientState.volumeNominations,
                value -> {
                    ClientState.volumeNominations = value;
                    PlayerConfig.save();
                }
        ));
        currentY += sliderHeight + sliderSpacing;

        // Dawn/Dusk Volume
        this.addRenderableWidget(new VolumeSlider(
                leftColumnX, currentY, sliderWidth, sliderHeight,
                Component.translatable("gui.blood-on-the-blocktower.settings.volume.dawn_dusk"), ClientState.volumeDawnDusk,
                value -> {
                    ClientState.volumeDawnDusk = value;
                    PlayerConfig.save();
                }
        ));
        currentY += sliderHeight + sliderSpacing;

        // Visit Sound Volume (doorbell/doorknock - storyteller chooses which sound)
        this.addRenderableWidget(new VolumeSlider(
                leftColumnX, currentY, sliderWidth, sliderHeight,
                Component.translatable("gui.blood-on-the-blocktower.settings.volume.visit_sound"), ClientState.volumeDoorbell,
                value -> {
                    ClientState.volumeDoorbell = value;
                    PlayerConfig.save();
                }
        ));
        currentY += sliderHeight + sliderSpacing;

        // Role Receive Volume
        this.addRenderableWidget(new VolumeSlider(
                leftColumnX, currentY, sliderWidth, sliderHeight,
                Component.translatable("gui.blood-on-the-blocktower.settings.volume.role_receive"), ClientState.volumeRoleReceive,
                value -> {
                    ClientState.volumeRoleReceive = value;
                    PlayerConfig.save();
                }
        ));
        currentY += sliderHeight + sliderSpacing;

        // Final Reveal Volume (game_end sound)
        this.addRenderableWidget(new VolumeSlider(
                leftColumnX, currentY, sliderWidth, sliderHeight,
                Component.translatable("gui.blood-on-the-blocktower.settings.volume.final_reveal"), ClientState.volumeFinalReveal,
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
        Component fadeText = Component.translatable("gui.blood-on-the-blocktower.settings.fade_heads", onOff(ClientState.fadeOutOfGroupHeads));
        this.addRenderableWidget(Button.builder(fadeText, b -> {
            ClientState.fadeOutOfGroupHeads = !ClientState.fadeOutOfGroupHeads;
            PlayerConfig.save();
            this.minecraft.gui.setScreen(this);
        }).bounds(leftColX, currentY, prefButtonWidth, sliderHeight)
        .tooltip(Tooltip.create(Component.translatable("gui.blood-on-the-blocktower.settings.tooltip.fade_heads")))
        .build());

        Component animationsText = Component.translatable("gui.blood-on-the-blocktower.settings.animations", onOff(ClientState.grimoireAnimationsEnabled));
        this.addRenderableWidget(Button.builder(animationsText, b -> {
            ClientState.grimoireAnimationsEnabled = !ClientState.grimoireAnimationsEnabled;
            PlayerConfig.save();
            this.minecraft.gui.setScreen(this);
        }).bounds(rightColX, currentY, prefButtonWidth, sliderHeight)
        .tooltip(Tooltip.create(Component.translatable("gui.blood-on-the-blocktower.settings.tooltip.animations")))
        .build());

        currentY += sliderHeight + sliderSpacing;

        // Row 2: Hints | Floating Role Icons
        Component hintsText = Component.translatable("gui.blood-on-the-blocktower.settings.hints", onOff(ClientState.hintsEnabled));
        this.addRenderableWidget(Button.builder(hintsText, b -> {
            ClientState.hintsEnabled = !ClientState.hintsEnabled;
            PlayerConfig.save();
            this.minecraft.gui.setScreen(this);
        }).bounds(leftColX, currentY, prefButtonWidth, sliderHeight)
        .tooltip(Tooltip.create(Component.translatable("gui.blood-on-the-blocktower.settings.tooltip.hints")))
        .build());

        Component iconsText = Component.translatable("gui.blood-on-the-blocktower.settings.role_icons", ClientState.floatingRoleIconMode.displayName());
        this.addRenderableWidget(Button.builder(iconsText, b -> {
            ClientState.floatingRoleIconMode = ClientState.floatingRoleIconMode.cycle();
            PlayerConfig.save();
            this.minecraft.gui.setScreen(this);
        }).bounds(rightColX, currentY, prefButtonWidth, sliderHeight)
        .tooltip(Tooltip.create(Component.translatable("gui.blood-on-the-blocktower.settings.tooltip.role_icons")))
        .build());

        // ========================================
        // BOTTOM: Whisper Rules, Glossary, Back Buttons
        // ========================================
        int backButtonWidth = 60;
        int glossaryButtonWidth = 80;
        int whisperButtonWidth = 100;
        int buttonSpacingBottom = 5;

        // Whisper Rules button: read-only view for players. Storytellers edit this elsewhere.
        boolean isStoryteller = this.minecraft != null && this.minecraft.player != null
                && this.minecraft.player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
        if (!isStoryteller) {
            this.addRenderableWidget(Button.builder(
                    Component.translatable("gui.blood-on-the-blocktower.settings.whisper_rules").withStyle(ChatFormatting.LIGHT_PURPLE),
                    button -> this.minecraft.gui.setScreen(new WhisperSettingsScreen(this, false))
            ).bounds(
                    this.width - backButtonWidth - glossaryButtonWidth - whisperButtonWidth - 2 * buttonSpacingBottom - 10,
                    this.height - 30, whisperButtonWidth, 20)
            .tooltip(Tooltip.create(Component.translatable("gui.blood-on-the-blocktower.settings.tooltip.whisper_rules")))
            .build());
        }

        // Glossary button
        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.blood-on-the-blocktower.settings.glossary").withStyle(ChatFormatting.GOLD),
                button -> this.minecraft.gui.setScreen(new GlossaryScreen(this))
        ).bounds(this.width - backButtonWidth - glossaryButtonWidth - buttonSpacingBottom - 10, this.height - 30, glossaryButtonWidth, 20)
        .tooltip(Tooltip.create(Component.translatable("gui.blood-on-the-blocktower.settings.tooltip.glossary")))
        .build());

        // Back button
        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.blood-on-the-blocktower.settings.back").withStyle(ChatFormatting.YELLOW),
                button -> this.minecraft.gui.setScreen(this.parent)
        ).bounds(this.width - backButtonWidth - 10, this.height - 30, backButtonWidth, 20).build());

        // Credits button, bottom-left
        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.blood-on-the-blocktower.settings.credits").withStyle(ChatFormatting.AQUA),
                button -> this.minecraft.gui.setScreen(new CreditsScreen(this))
        ).bounds(10, this.height - 30, 70, 20)
        .tooltip(Tooltip.create(Component.translatable("gui.blood-on-the-blocktower.settings.tooltip.credits")))
        .build());

        // ========================================
        // RIGHT SIDE: DOCUMENTATION WIDGET
        // ========================================
        int docX = this.width / 2 + 10;
        int docY = 30;
        int docWidth = this.width / 2 - 20;
        int docHeight = this.height - docY - 40;

        this.documentationWidget = new DocumentationListWidget(this.minecraft, docWidth, docHeight, docY);
        this.documentationWidget.updateSizeAndPosition(docWidth, docHeight, docX, docY);
        this.addRenderableWidget(this.documentationWidget);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractRenderState(context, mouseX, mouseY, delta);

        // Draw title
        context.centeredText(this.font, this.title, this.width / 2, 10, 0xFFFFFFFF);

        // Draw category labels
        int leftColumnX = 20;
        context.text(this.font, Component.translatable("gui.blood-on-the-blocktower.settings.category.volume").withStyle(ChatFormatting.GOLD), leftColumnX, 25, 0xFFFFFFFF);

        // Calculate preferences category Y position (after 5 volume sliders)
        int grimoireCategoryY = 25 + 12 + 2 + (5 * (20 + 5)) + 15;
        context.text(this.font, Component.translatable("gui.blood-on-the-blocktower.settings.category.preferences").withStyle(ChatFormatting.GOLD), leftColumnX, grimoireCategoryY, 0xFFFFFFFF);
    }

    private static Component onOff(boolean on) {
        return Component.translatable(on
                ? "gui.blood-on-the-blocktower.settings.on"
                : "gui.blood-on-the-blocktower.settings.off");
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int keyCode = event.key();
        int scanCode = event.scancode();
        int modifiers = event.modifiers();

        boolean exitKeyPressed = keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_E;
        boolean openAssignGuiPressed = KeyInputHandler.openAssignGui != null && KeyInputHandler.openAssignGui.matches(event);
        if (exitKeyPressed || openAssignGuiPressed) {
            this.minecraft.gui.setScreen(this.parent);
            return true;
        }
        return super.keyPressed(event);
    }

    /**
     * Custom slider widget for volume control.
     * Range: 0% to 100% (stored as 0.0 to 1.0 multiplier)
     */
    private static class VolumeSlider extends AbstractSliderButton {
        private final Component label;
        private final VolumeCallback callback;

        @FunctionalInterface
        interface VolumeCallback {
            void apply(float value);
        }

        public VolumeSlider(int x, int y, int width, int height, Component label, float initialValue, VolumeCallback callback) {
            // Slider value is 0.0-1.0, same as volume multiplier
            super(x, y, width, height, Component.empty(), initialValue);
            this.label = label;
            this.callback = callback;
            this.updateMessage();
        }

        @Override
        protected void updateMessage() {
            // value is 0.0-1.0, multiply by 100 to get percentage
            int percent = (int) (this.value * 100);
            this.setMessage(Component.translatable("gui.blood-on-the-blocktower.settings.volume_format", label, percent));
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
    private class DocumentationListWidget extends ContainerObjectSelectionList<DocumentEntry> {

        public DocumentationListWidget(Minecraft client, int width, int height, int y) {
            super(client, width, height, y, client.font.lineHeight + 2);

            int textWidth = this.getRowWidth() - 10;

            // --- Hotkeys Section ---
            this.addEntry(DocumentEntry.title(font, Component.translatable("gui.blood-on-the-blocktower.settings.doc.hotkeys").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)));
            this.addEntry(DocumentEntry.spacer());

            addHotkeyEntry(Component.translatable("gui.blood-on-the-blocktower.settings.hotkey.open_grimoire"), KeyInputHandler.openAssignGui);
            addHotkeyEntry(Component.translatable("gui.blood-on-the-blocktower.settings.hotkey.open_script"), KeyInputHandler.openScriptKey);
            addHotkeyEntry(Component.translatable("gui.blood-on-the-blocktower.settings.hotkey.open_role_details"), KeyInputHandler.openMyRoleDetailsKey);
            addHotkeyEntry(Component.translatable("gui.blood-on-the-blocktower.settings.hotkey.open_role_catalog"), KeyInputHandler.openCatalogKey);
            addHotkeyEntry(Component.translatable("gui.blood-on-the-blocktower.settings.hotkey.toggle_role_hud"), KeyInputHandler.toggleShowRole);
            addHotkeyEntry(Component.translatable("gui.blood-on-the-blocktower.settings.hotkey.grimoire_quick_view"), KeyInputHandler.quickRoleViewKey);
            addHotkeyEntry(Component.translatable("gui.blood-on-the-blocktower.settings.hotkey.toggle_sidebar"), KeyInputHandler.toggleSidebarKey);
            this.addEntry(DocumentEntry.spacer());
            this.addEntry(DocumentEntry.spacer());

            // --- Grimoire Section ---
            this.addEntry(DocumentEntry.title(font, Component.translatable("gui.blood-on-the-blocktower.settings.doc.grimoire").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)));
            this.addEntry(DocumentEntry.spacer());

            this.addEntry(DocumentEntry.title(font, Component.translatable("gui.blood-on-the-blocktower.settings.doc.reminders").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC)));
            addWrappedText(Component.translatable("gui.blood-on-the-blocktower.settings.doc.reminders_body"), textWidth);
            this.addEntry(DocumentEntry.spacer());

            this.addEntry(DocumentEntry.title(font, Component.translatable("gui.blood-on-the-blocktower.settings.doc.role_assignment").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC)));
            addWrappedText(Component.translatable("gui.blood-on-the-blocktower.settings.doc.role_assignment_body"), textWidth);
            this.addEntry(DocumentEntry.spacer());

            this.addEntry(DocumentEntry.title(font, Component.translatable("gui.blood-on-the-blocktower.settings.doc.viewing_role_details").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC)));
            addColoredModifierText(Component.empty(), "Shift", Component.translatable("gui.blood-on-the-blocktower.settings.doc.shift_click_role"), textWidth);
            this.addEntry(DocumentEntry.spacer());
            addColoredModifierText(Component.translatable("gui.blood-on-the-blocktower.settings.doc.hold"), "Shift", Component.translatable("gui.blood-on-the-blocktower.settings.doc.shift_hover_role"), textWidth);
            this.addEntry(DocumentEntry.spacer());

            // --- Reference Section ---
            this.addEntry(DocumentEntry.title(font, Component.translatable("gui.blood-on-the-blocktower.settings.doc.reference").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)));
            this.addEntry(DocumentEntry.spacer());

            this.addEntry(DocumentEntry.title(font, Component.translatable("gui.blood-on-the-blocktower.settings.doc.script_reference").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC)));
            addWrappedText(Component.translatable("gui.blood-on-the-blocktower.settings.doc.script_reference_body"), textWidth);
            this.addEntry(DocumentEntry.spacer());
            addColoredTabEntry(Component.translatable("gui.blood-on-the-blocktower.settings.doc.tab.roles"), Component.translatable("gui.blood-on-the-blocktower.settings.doc.tab.roles_desc"), textWidth);
            this.addEntry(DocumentEntry.spacer());
            addColoredTabEntry(Component.translatable("gui.blood-on-the-blocktower.settings.doc.tab.night_order"), Component.translatable("gui.blood-on-the-blocktower.settings.doc.tab.night_order_desc"), textWidth);
            this.addEntry(DocumentEntry.spacer());
            addColoredTabEntry(Component.translatable("gui.blood-on-the-blocktower.settings.doc.tab.jinxes"), Component.translatable("gui.blood-on-the-blocktower.settings.doc.tab.jinxes_desc"), textWidth);
            this.addEntry(DocumentEntry.spacer());
            addWrappedText(Component.translatable("gui.blood-on-the-blocktower.settings.doc.click_role_details"), textWidth);
            this.addEntry(DocumentEntry.spacer());
            addColoredModifierText(Component.empty(), "Ctrl", Component.translatable("gui.blood-on-the-blocktower.settings.doc.ctrl_click_cross_out"), textWidth);
            this.addEntry(DocumentEntry.spacer());

            this.addEntry(DocumentEntry.title(font, Component.translatable("gui.blood-on-the-blocktower.settings.doc.role_catalog").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC)));
            addWrappedText(Component.translatable("gui.blood-on-the-blocktower.settings.doc.role_catalog_body"), textWidth);
        }

        private void addWrappedText(Component text, int width) {
            for (FormattedCharSequence line : font.split(text, width)) {
                this.addEntry(DocumentEntry.text(font, line, 0xFFFFFFFF));
            }
        }

        private void addColoredTabEntry(Component tabName, Component description, int width) {
            MutableComponent text = Component.literal("\u2022 ").withStyle(ChatFormatting.WHITE)
                    .append(tabName.copy().withStyle(ChatFormatting.GOLD))
                    .append(Component.translatable("gui.blood-on-the-blocktower.settings.doc.tab_desc", description).withStyle(ChatFormatting.WHITE));
            for (FormattedCharSequence line : font.split(text, width)) {
                this.addEntry(DocumentEntry.text(font, line, 0xFFFFFFFF));
            }
        }

        private void addColoredModifierText(Component prefix, String modifier, Component suffix, int width) {
            // Color modifiers: Shift = aqua, Ctrl = yellow
            ChatFormatting modColor = modifier.equals("Shift") ? ChatFormatting.AQUA : ChatFormatting.YELLOW;
            MutableComponent text = prefix.copy().withStyle(ChatFormatting.WHITE)
                    .append(Component.literal(modifier).withStyle(modColor))
                    .append(suffix.copy().withStyle(ChatFormatting.WHITE));
            for (FormattedCharSequence line : font.split(text, width)) {
                this.addEntry(DocumentEntry.text(font, line, 0xFFFFFFFF));
            }
        }

        private void addHotkeyEntry(Component action, KeyMapping keyBinding) {
            Component keyName = keyBinding != null ? keyBinding.getTranslatedKeyMessage() : Component.translatable("gui.blood-on-the-blocktower.settings.not_bound");
            this.addEntry(DocumentEntry.text(font, Component.translatable("gui.blood-on-the-blocktower.settings.hotkey_line", action).withStyle(ChatFormatting.WHITE)
                    .append(Component.literal("[").append(keyName).append("]").withStyle(ChatFormatting.YELLOW)).getVisualOrderText(), 0xFFFFFFFF));
        }

        @Override
        public int getRowWidth() {
            return this.width - 20;
        }

        @Override
        protected int scrollBarX() {
            return this.getX() + this.width - 6;
        }

    }
}
