package com.autumnwind.botb.gui;

import com.autumnwind.botb.config.WhisperSettings;
import com.autumnwind.botb.networking.UpdateWhisperSettingsC2SPayload;
import com.autumnwind.botb.states.ClientWhisperSettings;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.screen.narration.NarrationPart;
import net.minecraft.client.gui.widget.PressableWidget;

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

    private TextFieldWidget rangeField;

    public WhisperSettingsScreen(Screen parent, boolean editable) {
        super(Text.translatable(editable
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
            if (this.client != null) this.client.setScreen(this);
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
        addRow(y, leftX, controlX, controlW, Text.translatable("gui.blood-on-the-blocktower.whisper_settings.allow_whispering"),
                onOff(allow).formatted(allow ? Formatting.GREEN : Formatting.RED),
                editable, b -> { allow = !allow; refresh(); });
        y += rowH;

        // 2. Broadcast
        addRow(y, leftX, controlX, controlW, Text.translatable("gui.blood-on-the-blocktower.whisper_settings.broadcast_whispers"),
                onOff(broadcast).formatted(broadcast ? Formatting.GREEN : Formatting.RED),
                editable, b -> { broadcast = !broadcast; refresh(); });
        y += rowH;

        // 3. Visual
        addRow(y, leftX, controlX, controlW, Text.translatable("gui.blood-on-the-blocktower.whisper_settings.visual_effect"),
                Text.literal(visual.displayName()).formatted(colorFor(visual)),
                editable, b -> { visual = visual.cycle(); refresh(); });
        y += rowH;

        // 4. Audio
        addRow(y, leftX, controlX, controlW, Text.translatable("gui.blood-on-the-blocktower.whisper_settings.audio_cue"),
                onOff(audio).formatted(audio ? Formatting.GREEN : Formatting.RED),
                editable, b -> { audio = !audio; refresh(); });
        y += rowH;

        // 5. Range, a text field with placeholder. Empty → unlimited.
        this.addDrawableChild(new LabelWidget(leftX, y, labelW, rowH - 2, Text.translatable("gui.blood-on-the-blocktower.whisper_settings.whisper_range")));
        rangeField = new TextFieldWidget(this.textRenderer, controlX, y, controlW, rowH - 2,
                Text.literal(""));
        rangeField.setMaxLength(10);
        rangeField.setText(rangeText);
        rangeField.setPlaceholder(Text.translatable("gui.blood-on-the-blocktower.whisper_settings.range_unlimited").formatted(Formatting.DARK_GRAY));
        rangeField.setEditable(editable);
        rangeField.setChangedListener(v -> rangeText = v);
        this.addDrawableChild(rangeField);
        y += rowH;

        // 6. VC enforced
        addRow(y, leftX, controlX, controlW, Text.translatable("gui.blood-on-the-blocktower.whisper_settings.vc_enforced"),
                onOff(vcEnforced).formatted(vcEnforced ? Formatting.GREEN : Formatting.RED),
                editable, b -> { vcEnforced = !vcEnforced; refresh(); });
        y += rowH + 8;

        // Bottom: Save (storyteller) + Cancel/Back
        int bottomY = this.height - 30;
        if (editable) {
            this.addDrawableChild(ButtonWidget.builder(
                    Text.translatable("gui.blood-on-the-blocktower.whisper_settings.save").formatted(Formatting.GREEN),
                    b -> save()
            ).dimensions(this.width / 2 - 65, bottomY, 60, 20).build());
            this.addDrawableChild(ButtonWidget.builder(
                    Text.translatable("gui.blood-on-the-blocktower.whisper_settings.cancel").formatted(Formatting.YELLOW),
                    b -> close()
            ).dimensions(this.width / 2 + 5, bottomY, 60, 20).build());
        } else {
            this.addDrawableChild(ButtonWidget.builder(
                    Text.translatable("gui.blood-on-the-blocktower.whisper_settings.back").formatted(Formatting.YELLOW),
                    b -> close()
            ).dimensions(this.width / 2 - 30, bottomY, 60, 20).build());
        }
    }

    private void refresh() {
        if (this.client != null) this.client.setScreen(this);
    }

    private static MutableText onOff(boolean on) {
        return Text.translatable(on
                ? "gui.blood-on-the-blocktower.whisper_settings.on"
                : "gui.blood-on-the-blocktower.whisper_settings.off");
    }

    private void addRow(int y, int leftX, int controlX, int controlW,
                         Text labelText, Text valueText,
                         boolean enabled, ButtonWidget.PressAction onPress) {
        this.addDrawableChild(new LabelWidget(leftX, y, controlX - leftX - 4, 18, labelText));
        ButtonWidget btn = ButtonWidget.builder(valueText, onPress)
                .dimensions(controlX, y, controlW, 18)
                .build();
        btn.active = enabled;
        this.addDrawableChild(btn);
    }

    private static Formatting colorFor(WhisperSettings.VisualMode m) {
        return m == WhisperSettings.VisualMode.OFF ? Formatting.RED : Formatting.GREEN;
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
        close();
    }

    @Override
    public void close() {
        if (this.client != null) this.client.setScreen(parent);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 12, 0xFFFFFF);
        if (!editable) {
            context.drawCenteredTextWithShadow(
                    this.textRenderer,
                    Text.translatable("gui.blood-on-the-blocktower.whisper_settings.set_by_storyteller").formatted(Formatting.GRAY, Formatting.ITALIC),
                    this.width / 2, this.height - 50, 0xFFFFFF);
        }
    }

    /**
     * Tiny static-text widget so labels share the same render-loop layer as the
     * other controls. Inline rather than spinning up a whole helper file.
     */
    private class LabelWidget extends PressableWidget {
        private final Text text;

        LabelWidget(int x, int y, int width, int height, Text text) {
            super(x, y, width, height, text);
            this.text = text;
            this.active = false;
        }

        @Override
        public void onPress() {}

        @Override
        protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
            int color = (editable ? 0xFFFFFF : 0xAAAAAA);
            context.drawTextWithShadow(
                    WhisperSettingsScreen.this.textRenderer,
                    text,
                    this.getX(),
                    this.getY() + (this.getHeight() - WhisperSettingsScreen.this.textRenderer.fontHeight) / 2,
                    color
            );
        }

        @Override
        protected void appendClickableNarrations(NarrationMessageBuilder builder) {
            builder.put(NarrationPart.TITLE, text);
        }
    }
}
