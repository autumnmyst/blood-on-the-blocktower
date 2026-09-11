package com.autumnwind.botb.gui;

import com.autumnwind.botb.event.KeyInputHandler;
import com.autumnwind.botb.networking.TimerControlC2SPayload;
import com.autumnwind.botb.timer.ClientTimerState;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/**
 * Screen for operators to control the storyteller timer
 */
public class TimerScreen extends Screen {
    private EditBox customTimeField;
    private Button pauseResumeButton;
    private Button stopButton;
    private Checkbox syncDaylightCheckbox;
    private boolean syncDaylight = false;

    public TimerScreen() {
        super(Component.translatable("gui.blood-on-the-blocktower.timer.title"));
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int startY = this.height / 2 - 100;

        // Quick timer buttons - Row 1: 30s, 1min, 2min
        this.addRenderableWidget(Button.builder(Component.translatable("gui.blood-on-the-blocktower.timer.seconds_short", 30), button -> {
            startTimer(30);
        }).bounds(centerX - 115, startY, 70, 20).build());

        this.addRenderableWidget(Button.builder(Component.translatable("gui.blood-on-the-blocktower.timer.minutes_short", 1), button -> {
            startTimer(60);
        }).bounds(centerX - 35, startY, 70, 20).build());

        this.addRenderableWidget(Button.builder(Component.translatable("gui.blood-on-the-blocktower.timer.minutes_short", 2), button -> {
            startTimer(120);
        }).bounds(centerX + 45, startY, 70, 20).build());

        // Quick timer buttons - Row 2: 3min, 5min, 10min
        this.addRenderableWidget(Button.builder(Component.translatable("gui.blood-on-the-blocktower.timer.minutes_short", 3), button -> {
            startTimer(180);
        }).bounds(centerX - 115, startY + 30, 70, 20).build());

        this.addRenderableWidget(Button.builder(Component.translatable("gui.blood-on-the-blocktower.timer.minutes_short", 5), button -> {
            startTimer(300);
        }).bounds(centerX - 35, startY + 30, 70, 20).build());

        this.addRenderableWidget(Button.builder(Component.translatable("gui.blood-on-the-blocktower.timer.minutes_short", 10), button -> {
            startTimer(600);
        }).bounds(centerX + 45, startY + 30, 70, 20).build());

        // Custom time input
        customTimeField = new EditBox(this.font, centerX - 100, startY + 70, 200, 20, Component.translatable("gui.blood-on-the-blocktower.timer.custom_time"));
        customTimeField.setHint(Component.translatable("gui.blood-on-the-blocktower.timer.custom_time_hint"));
        customTimeField.setMaxLength(10);
        this.addRenderableWidget(customTimeField);

        // Start custom timer button
        this.addRenderableWidget(Button.builder(Component.translatable("gui.blood-on-the-blocktower.timer.start_custom"), button -> {
            startCustomTimer();
        }).bounds(centerX - 100, startY + 100, 200, 20).build());

        // Pause/Resume button
        pauseResumeButton = Button.builder(
                ClientTimerState.isPaused ? Component.translatable("gui.blood-on-the-blocktower.timer.resume") : Component.translatable("gui.blood-on-the-blocktower.timer.pause"),
                button -> {
                    if (ClientTimerState.isPaused) {
                        resumeTimer();
                    } else {
                        pauseTimer();
                    }
                }
        ).bounds(centerX - 100, startY + 125, 95, 20).build();
        pauseResumeButton.active = ClientTimerState.isActive;
        this.addRenderableWidget(pauseResumeButton);

        // Stop button
        stopButton = Button.builder(Component.translatable("gui.blood-on-the-blocktower.timer.stop"), button -> {
            stopTimer();
        }).bounds(centerX + 5, startY + 125, 95, 20).build();
        stopButton.active = ClientTimerState.isActive;
        this.addRenderableWidget(stopButton);

        // Sync daylight checkbox
        syncDaylightCheckbox = Checkbox.builder(Component.translatable("gui.blood-on-the-blocktower.timer.sync_daylight"), this.font)
                .pos(centerX - 100, startY + 180)
                .onValueChange((checkbox, checked) -> {
                    syncDaylight = checked;
                })
                .build();
        this.addRenderableWidget(syncDaylightCheckbox);
    }

    private void startTimer(int seconds) {
        ClientPlayNetworking.send(new TimerControlC2SPayload(TimerControlC2SPayload.Action.START, seconds, syncDaylight));
        this.onClose();
    }

    private void startCustomTimer() {
        String input = customTimeField.getValue().trim();
        if (input.isEmpty()) return;

        int seconds;
        try {
            if (input.contains(":")) {
                // Parse mm:ss format
                String[] parts = input.split(":");
                if (parts.length != 2) {
                    customTimeField.setValue("");
                    customTimeField.setHint(Component.translatable("gui.blood-on-the-blocktower.timer.invalid_format"));
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
                customTimeField.setValue("");
                customTimeField.setHint(Component.translatable("gui.blood-on-the-blocktower.timer.range_hint"));
                return;
            }

            startTimer(seconds);
        } catch (NumberFormatException e) {
            customTimeField.setValue("");
            customTimeField.setHint(Component.translatable("gui.blood-on-the-blocktower.timer.invalid_number"));
        }
    }

    private void pauseTimer() {
        ClientPlayNetworking.send(new TimerControlC2SPayload(TimerControlC2SPayload.Action.PAUSE, 0, false));
        this.onClose();
    }

    private void resumeTimer() {
        ClientPlayNetworking.send(new TimerControlC2SPayload(TimerControlC2SPayload.Action.RESUME, 0, false));
        this.onClose();
    }

    private void stopTimer() {
        ClientPlayNetworking.send(new TimerControlC2SPayload(TimerControlC2SPayload.Action.STOP, 0, false));
        this.onClose();
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        // Draw title
        context.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);

        // Draw current timer status if active
        if (ClientTimerState.isActive) {
            int minutes = ClientTimerState.remainingSeconds / 60;
            int seconds = ClientTimerState.remainingSeconds % 60;
            String timeText = String.format("%d:%02d", minutes, seconds);
            Component statusText = Component.translatable(ClientTimerState.isPaused
                    ? "gui.blood-on-the-blocktower.timer.paused"
                    : "gui.blood-on-the-blocktower.timer.running");
            context.drawCenteredString(this.font, Component.translatable("gui.blood-on-the-blocktower.timer.current", timeText, statusText), this.width / 2, this.height / 2 + 60, 0xFFFFFF);
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Don't close screen if typing in search field
        if ((KeyInputHandler.openTimerKey.matches(keyCode, scanCode) || keyCode == GLFW.GLFW_KEY_E)) {
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }
}
