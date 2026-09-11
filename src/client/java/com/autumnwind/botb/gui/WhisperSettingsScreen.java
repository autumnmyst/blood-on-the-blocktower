package com.autumnwind.botb.gui;

import com.autumnwind.botb.config.WhisperSettings;
import com.autumnwind.botb.networking.UpdateWhisperSettingsC2SPayload;
import com.autumnwind.botb.states.ClientWhisperSettings;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.client.input.InputWithModifiers;

/**
 * Whisper rules editor (storyteller) and viewer (player). Same screen used in both
 * modes. When {@code editable} is false, all controls render disabled and the Save
 * button is hidden, so non-storytellers can see what's currently in force.
 *
 * <p>State model:
 * <ul>
 *   <li>The screen edits a local working copy ({@code working}) initialized from
 *       {@link ClientWhisperSettings#current}.</li>
 *   <li>"Save" sends an {@link UpdateWhisperSettingsC2SPayload}, and the server validates,
 *       persists, and broadcasts {@code SyncWhisperSettingsS2CPayload} back. That
 *       broadcast lands on every client (including this one) and updates the displayed
 *       active rules, at which point we close the screen.</li>
 *   <li>"Cancel" / Back simply closes without sending, since nothing was committed yet.</li>
 *   <li>Concurrent storyteller change while we're editing: {@link #onSettingsSync}
 *       updates {@code working} only when {@code !editable} (so a player browsing the
 *       read-only view always reflects current rules). For editors we leave {@code
 *       working} alone, because the editor's pending changes shouldn't be silently
 *       overwritten by another storyteller's save.</li>
 * </ul>
 */
public class WhisperSettingsScreen extends Screen {

    private final Screen parent;
    private final boolean editable;

    /** Working copy, mutated by control callbacks and sent on Save. */
    private boolean allow;
    private boolean broadcast;
    private WhisperSettings.VisualMode visual;
    private boolean audio;
    /** Decoupled from {@link WhisperSettings#range} until parsed at save time. */
    private String rangeText;
    private boolean vcEnforced;

    private EditBox rangeField;

    public WhisperSettingsScreen(Screen parent, boolean editable) {
        super(Component.translatable(editable
                ? "gui.blood-on-the-blocktower.whisper_settings.title"
                : "gui.blood-on-the-blocktower.whisper_settings.title_view"));
        this.parent = parent;
        this.editable = editable;
        loadFromCurrent();
    }

    private void loadFromCurrent() {
        WhisperSettings cur = ClientWhisperSettings.current;
        this.allow = cur.allowWhispering();
        this.broadcast = cur.broadcast();
        this.visual = cur.visual();
        this.audio = cur.audio();
        this.rangeText = cur.rangeUnlimited() ? "" : trimDouble(cur.range());
        this.vcEnforced = cur.vcEnforced();
    }

    /**
     * Called by the network layer when the server pushes a fresh sync. For the
     * read-only player view, refresh the displayed values so the screen reflects
     * the current rules in real time. For editors, ignore it so pending edits stay.
     */
    public void onSettingsSync(WhisperSettings settings) {
        if (!editable) {
            this.allow = settings.allowWhispering();
            this.broadcast = settings.broadcast();
            this.visual = settings.visual();
            this.audio = settings.audio();
            this.rangeText = settings.rangeUnlimited() ? "" : trimDouble(settings.range());
            this.vcEnforced = settings.vcEnforced();
            // Re-init to refresh button labels.
            if (this.minecraft != null) this.minecraft.gui.setScreen(this);
        }
    }

    private static String trimDouble(double d) {
        // 0.0 → "0", 5.5 → "5.5". Nothing fancy, just lose pointless trailing zeros.
        if (d == Math.floor(d) && !Double.isInfinite(d)) {
            return Long.toString((long) d);
        }
        return Double.toString(d);
    }

    @Override
    protected void init() {
        int rowH = 22;
        int gap = 4;
        int labelW = 130;
        int controlW = 110;
        int leftX = this.width / 2 - (labelW + gap + controlW) / 2;
        int controlX = leftX + labelW + gap;
        int y = 40;

        // 1. Allow whispering
        addRow(y, leftX, controlX, controlW, Component.translatable("gui.blood-on-the-blocktower.whisper_settings.allow_whispering"),
                onOff(allow).withStyle(allow ? ChatFormatting.GREEN : ChatFormatting.RED),
                editable, b -> { allow = !allow; refresh(); });
        y += rowH;

        // 2. Broadcast
        addRow(y, leftX, controlX, controlW, Component.translatable("gui.blood-on-the-blocktower.whisper_settings.broadcast_whispers"),
                onOff(broadcast).withStyle(broadcast ? ChatFormatting.GREEN : ChatFormatting.RED),
                editable, b -> { broadcast = !broadcast; refresh(); });
        y += rowH;

        // 3. Visual
        addRow(y, leftX, controlX, controlW, Component.translatable("gui.blood-on-the-blocktower.whisper_settings.visual_effect"),
                Component.translatable(visual.translationKey()).withStyle(colorFor(visual)),
                editable, b -> { visual = visual.cycle(); refresh(); });
        y += rowH;

        // 4. Audio
        addRow(y, leftX, controlX, controlW, Component.translatable("gui.blood-on-the-blocktower.whisper_settings.audio_cue"),
                onOff(audio).withStyle(audio ? ChatFormatting.GREEN : ChatFormatting.RED),
                editable, b -> { audio = !audio; refresh(); });
        y += rowH;

        // 5. Range, a text field with placeholder. Empty → unlimited.
        this.addRenderableWidget(new LabelWidget(leftX, y, labelW, rowH - 2, Component.translatable("gui.blood-on-the-blocktower.whisper_settings.whisper_range")));
        rangeField = new EditBox(this.font, controlX, y, controlW, rowH - 2,
                Component.literal(""));
        rangeField.setMaxLength(10);
        rangeField.setValue(rangeText);
        rangeField.setHint(Component.translatable("gui.blood-on-the-blocktower.whisper_settings.range_unlimited").withStyle(ChatFormatting.DARK_GRAY));
        rangeField.setEditable(editable);
        rangeField.setResponder(v -> rangeText = v);
        this.addRenderableWidget(rangeField);
        y += rowH;

        // 6. VC enforced
        addRow(y, leftX, controlX, controlW, Component.translatable("gui.blood-on-the-blocktower.whisper_settings.vc_enforced"),
                onOff(vcEnforced).withStyle(vcEnforced ? ChatFormatting.GREEN : ChatFormatting.RED),
                editable, b -> { vcEnforced = !vcEnforced; refresh(); });
        y += rowH + 8;

        // Bottom: Save (storyteller) + Cancel/Back
        int bottomY = this.height - 30;
        if (editable) {
            this.addRenderableWidget(Button.builder(
                    Component.translatable("gui.blood-on-the-blocktower.whisper_settings.save").withStyle(ChatFormatting.GREEN),
                    b -> save()
            ).bounds(this.width / 2 - 65, bottomY, 60, 20).build());
            this.addRenderableWidget(Button.builder(
                    Component.translatable("gui.blood-on-the-blocktower.whisper_settings.cancel").withStyle(ChatFormatting.YELLOW),
                    b -> onClose()
            ).bounds(this.width / 2 + 5, bottomY, 60, 20).build());
        } else {
            this.addRenderableWidget(Button.builder(
                    Component.translatable("gui.blood-on-the-blocktower.whisper_settings.back").withStyle(ChatFormatting.YELLOW),
                    b -> onClose()
            ).bounds(this.width / 2 - 30, bottomY, 60, 20).build());
        }
    }

    private void refresh() {
        if (this.minecraft != null) this.minecraft.gui.setScreen(this);
    }

    private static MutableComponent onOff(boolean on) {
        return Component.translatable(on
                ? "gui.blood-on-the-blocktower.whisper_settings.on"
                : "gui.blood-on-the-blocktower.whisper_settings.off");
    }

    private void addRow(int y, int leftX, int controlX, int controlW,
                         Component labelText, Component valueText,
                         boolean enabled, Button.OnPress onPress) {
        this.addRenderableWidget(new LabelWidget(leftX, y, controlX - leftX - 4, 18, labelText));
        Button btn = Button.builder(valueText, onPress)
                .bounds(controlX, y, controlW, 18)
                .build();
        btn.active = enabled;
        this.addRenderableWidget(btn);
    }

    private static ChatFormatting colorFor(WhisperSettings.VisualMode m) {
        return m == WhisperSettings.VisualMode.OFF ? ChatFormatting.RED : ChatFormatting.GREEN;
    }

    private void save() {
        // Parse range: blank or non-numeric → unlimited (0). Negative → unlimited too.
        double parsedRange = 0.0;
        String trimmed = rangeText == null ? "" : rangeText.trim();
        if (!trimmed.isEmpty()) {
            try {
                double v = Double.parseDouble(trimmed);
                if (v > 0) parsedRange = v;
            } catch (NumberFormatException ignored) {
                // Fall back to unlimited rather than silently clamping garbage.
            }
        }

        WhisperSettings updated = new WhisperSettings(
                allow, broadcast, visual, audio, parsedRange, vcEnforced
        );
        ClientPlayNetworking.send(new UpdateWhisperSettingsC2SPayload(updated));
        // Server will broadcast a sync, so close immediately so the user sees the parent
        // screen rather than waiting for the round-trip.
        onClose();
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) this.minecraft.gui.setScreen(parent);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractRenderState(context, mouseX, mouseY, delta);
        context.centeredText(this.font, this.title, this.width / 2, 12, 0xFFFFFFFF);
        if (!editable) {
            context.centeredText(
                    this.font,
                    Component.translatable("gui.blood-on-the-blocktower.whisper_settings.set_by_storyteller").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC),
                    this.width / 2, this.height - 50, 0xFFFFFFFF);
        }
    }

    /**
     * Tiny static-text widget so labels share the same render-loop layer as the
     * other controls. Inline rather than spinning up a whole helper file.
     */
    private class LabelWidget extends AbstractButton {
        private final Component text;

        LabelWidget(int x, int y, int width, int height, Component text) {
            super(x, y, width, height, text);
            this.text = text;
            this.active = false;
        }

        @Override
        public void onPress(InputWithModifiers input) {}

        @Override
        protected void extractContents(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
            int color = (editable ? 0xFFFFFFFF : 0xFFAAAAAA);
            context.text(
                    WhisperSettingsScreen.this.font,
                    text,
                    this.getX(),
                    this.getY() + (this.getHeight() - WhisperSettingsScreen.this.font.lineHeight) / 2,
                    color
            );
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput builder) {
            builder.add(NarratedElementType.TITLE, text);
        }
    }
}
