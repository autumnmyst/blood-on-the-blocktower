package com.autumnwind.botb.gui.assignroles.widgets;

import com.autumnwind.botb.util.Reminder;

import java.util.UUID;

public class ClickableReminder {
    public final int x, y, size;
    public final Reminder reminder;
    public final UUID playerUuid;
    public final int index; // Index in the player's reminder list

    public ClickableReminder(int x, int y, int size, Reminder reminder, UUID playerUuid, int index) {
        this.x = x;
        this.y = y;
        this.size = size;
        this.reminder = reminder;
        this.playerUuid = playerUuid;
        this.index = index;
    }

    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= x && mouseX < x + size &&
                mouseY >= y && mouseY < y + size;
    }
}
