package com.autumnwind.botb.gui;

import com.autumnwind.botb.event.KeyInputHandler;
import com.autumnwind.botb.networking.TimerControlC2SPayload;
import com.autumnwind.botb.timer.ClientTimerState;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CheckboxWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

/**
 * Screen for operators to control the storyteller timer
 */
public class TimerScreen extends Screen {
    private TextFieldWidget customTimeField;
    private ButtonWidget pauseResumeButton;
    private ButtonWidget stopButton;
    private CheckboxWidget syncDaylightCheckbox;
    private boolean syncDaylight = false;

    public TimerScreen() {
        super(Text.translatable("gui.blood-on-the-blocktower.timer.title"));
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int startY = this.height / 2 - 100;

        // Quick timer buttons - Row 1: 30s, 1min, 2min
        this.addDrawableChild(ButtonWidget.builder(Text.translatable("gui.blood-on-the-blocktower.timer.seconds_short", 30), button -> {
            startTimer(30);
        }).dimensions(centerX - 115, startY, 70, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.translatable("gui.blood-on-the-blocktower.timer.minutes_short", 1), button -> {
            startTimer(60);
        }).dimensions(centerX - 35, startY, 70, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.translatable("gui.blood-on-the-blocktower.timer.minutes_short", 2), button -> {
            startTimer(120);
        }).dimensions(centerX + 45, startY, 70, 20).build());

        // Quick timer buttons - Row 2: 3min, 5min, 10min
        this.addDrawableChild(ButtonWidget.builder(Text.translatable("gui.blood-on-the-blocktower.timer.minutes_short", 3), button -> {
            startTimer(180);
        }).dimensions(centerX - 115, startY + 30, 70, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.translatable("gui.blood-on-the-blocktower.timer.minutes_short", 5), button -> {
            startTimer(300);
        }).dimensions(centerX - 35, startY + 30, 70, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.translatable("gui.blood-on-the-blocktower.timer.minutes_short", 10), button -> {
            startTimer(600);
        }).dimensions(centerX + 45, startY + 30, 70, 20).build());

        // Custom time input
        customTimeField = new TextFieldWidget(this.textRenderer, centerX - 100, startY + 70, 200, 20, Text.translatable("gui.blood-on-the-blocktower.timer.custom_time"));
        customTimeField.setPlaceholder(Text.translatable("gui.blood-on-the-blocktower.timer.custom_time_hint"));
        customTimeField.setMaxLength(10);
        this.addDrawableChild(customTimeField);

        // Start custom timer button
        this.addDrawableChild(ButtonWidget.builder(Text.translatable("gui.blood-on-the-blocktower.timer.start_custom"), button -> {
            startCustomTimer();
        }).dimensions(centerX - 100, startY + 100, 200, 20).build());

        // Pause/Resume button
        pauseResumeButton = ButtonWidget.builder(
                ClientTimerState.isPaused ? Text.translatable("gui.blood-on-the-blocktower.timer.resume") : Text.translatable("gui.blood-on-the-blocktower.timer.pause"),
                button -> {
                    if (ClientTimerState.isPaused) {
                        resumeTimer();
                    } else {
                        pauseTimer();
                    }
                }
        ).dimensions(centerX - 100, startY + 125, 95, 20).build();
        pauseResumeButton.active = ClientTimerState.isActive;
        this.addDrawableChild(pauseResumeButton);

        // Stop button
        stopButton = ButtonWidget.builder(Text.translatable("gui.blood-on-the-blocktower.timer.stop"), button -> {
            stopTimer();
        }).dimensions(centerX + 5, startY + 125, 95, 20).build();
        stopButton.active = ClientTimerState.isActive;
        this.addDrawableChild(stopButton);

        // Sync daylight checkbox
        syncDaylightCheckbox = CheckboxWidget.builder(Text.translatable("gui.blood-on-the-blocktower.timer.sync_daylight"), this.textRenderer)
                .pos(centerX - 100, startY + 180)
                .callback((checkbox, checked) -> {
                    syncDaylight = checked;
                })
                .build();
        this.addDrawableChild(syncDaylightCheckbox);
    }

    private void startTimer(int seconds) {
        ClientPlayNetworking.send(new TimerControlC2SPayload(TimerControlC2SPayload.Action.START, seconds, syncDaylight));
        this.close();
    }

    private void startCustomTimer() {
        String input = customTimeField.getText().trim();
        if (input.isEmpty()) return;

        int seconds;
        try {
            if (input.contains(":")) {
                // Parse mm:ss format
                String[] parts = input.split(":");
                if (parts.length != 2) {
                    customTimeField.setText("");
                    customTimeField.setPlaceholder(Text.translatable("gui.blood-on-the-blocktower.timer.invalid_format"));
                    return;
                }
                int minutes = Integer.parseInt(parts[0]);
                int secs = Integer.parseInt(parts[1]);
                seconds = minutes * 60 + secs;
            } else {
                // Parse as seconds
                seconds = Integer.parseInt(input);
            }

            if (seconds <= 0 || seconds > 3600) { // Max 1 hour
                customTimeField.setText("");
                customTimeField.setPlaceholder(Text.translatable("gui.blood-on-the-blocktower.timer.range_hint"));
                return;
            }

            startTimer(seconds);
        } catch (NumberFormatException e) {
            customTimeField.setText("");
            customTimeField.setPlaceholder(Text.translatable("gui.blood-on-the-blocktower.timer.invalid_number"));
        }
    }

    private void pauseTimer() {
        ClientPlayNetworking.send(new TimerControlC2SPayload(TimerControlC2SPayload.Action.PAUSE, 0, false));
        this.close();
    }

    private void resumeTimer() {
        ClientPlayNetworking.send(new TimerControlC2SPayload(TimerControlC2SPayload.Action.RESUME, 0, false));
        this.close();
    }

    private void stopTimer() {
        ClientPlayNetworking.send(new TimerControlC2SPayload(TimerControlC2SPayload.Action.STOP, 0, false));
        this.close();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        // Draw title
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 20, 0xFFFFFF);

        // Draw current timer status if active
        if (ClientTimerState.isActive) {
            int minutes = ClientTimerState.remainingSeconds / 60;
            int seconds = ClientTimerState.remainingSeconds % 60;
            String timeText = String.format("%d:%02d", minutes, seconds);
            Text statusText = Text.translatable(ClientTimerState.isPaused
                    ? "gui.blood-on-the-blocktower.timer.paused"
                    : "gui.blood-on-the-blocktower.timer.running");
            context.drawCenteredTextWithShadow(this.textRenderer, Text.translatable("gui.blood-on-the-blocktower.timer.current", timeText, statusText), this.width / 2, this.height / 2 + 60, 0xFFFFFF);
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Don't close screen if typing in search field
        if ((KeyInputHandler.openTimerKey.matchesKey(keyCode, scanCode) || keyCode == GLFW.GLFW_KEY_E)) {
            this.close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }
}
