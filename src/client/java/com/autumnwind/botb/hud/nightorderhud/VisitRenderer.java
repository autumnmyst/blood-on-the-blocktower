package com.autumnwind.botb.hud.nightorderhud;

import com.autumnwind.botb.states.ClientState;
import com.autumnwind.botb.states.StorytellerState;
import com.autumnwind.botb.util.Reminder;
import com.autumnwind.botb.util.RoleType;
import com.autumnwind.botb.util.RoleVisit;
import com.autumnwind.botb.util.Script;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

import java.util.List;

/**
 * Handles rendering of the Night Order HUD.
 */
public class VisitRenderer {

    private static final int ICON_SIZE = 24;
    private static final int ICON_SPACING = 4;
    private static final int REMINDER_ICON_SIZE = 14;
    private static final int REMINDER_SPACING = 2;

    /**
     * Renders the Night Order HUD at the top of the screen.
     */
    public static void render(DrawContext context, MinecraftClient client) {
        if (StorytellerState.activeNightOrder.isEmpty()) {
            NightOrderBuilder.rebuildActiveNightOrder();
        }

        Script script = ClientState.currentScript;
        int totalWidth = StorytellerState.activeNightOrder.size() * (ICON_SIZE + ICON_SPACING) - ICON_SPACING;
        int startX = (context.getScaledWindowWidth() - totalWidth) / 2;
        int y = 30;

        for (int i = 0; i < StorytellerState.activeNightOrder.size(); i++) {
            RoleVisit visit = StorytellerState.activeNightOrder.get(i);
            Identifier icon = visit.getIcon();
            int x = startX + i * (ICON_SIZE + ICON_SPACING);

            context.drawTexture(icon, x, y, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);

            if (i == StorytellerState.currentNightVisitIndex) {
                context.drawBorder(x - 1, y - 1, ICON_SIZE + 2, ICON_SIZE + 2, 0xFFFFFFFF);
            }

            // Draw icon reminders under the visit
            List<Reminder> iconReminders = visit.iconReminders();
            if (iconReminders != null && !iconReminders.isEmpty()) {
                int reminderY = y + ICON_SIZE + REMINDER_SPACING;
                int reminderX = x + (ICON_SIZE - REMINDER_ICON_SIZE) / 2;

                for (Reminder reminder : iconReminders) {
                    Identifier reminderIcon = reminder.getIcon();
                    context.drawTexture(reminderIcon, reminderX, reminderY, 0, 0, REMINDER_ICON_SIZE, REMINDER_ICON_SIZE, REMINDER_ICON_SIZE, REMINDER_ICON_SIZE);

                    // Draw alignment border (pass script for custom role lookup)
                    int borderColor = reminder.getAlignmentColor(script);
                    if (borderColor != RoleType.NONE.getColor()) {
                        context.drawBorder(reminderX - 1, reminderY - 1, REMINDER_ICON_SIZE + 2, REMINDER_ICON_SIZE + 2, borderColor | 0xFF000000);
                    }

                    reminderY += REMINDER_ICON_SIZE + REMINDER_SPACING;
                }
            }
        }
    }
}
