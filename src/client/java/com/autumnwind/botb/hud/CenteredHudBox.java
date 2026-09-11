package com.autumnwind.botb.hud;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;

/**
 * The centered box under the player counts that the vote, exile, and setup HUDs share: a
 * translucent black fill, a one- or two-pixel border, and centered lines of text.
 */
final class CenteredHudBox {

    private CenteredHudBox() {}

    static final int PADDING = 6;
    static final int LINE_HEIGHT = 12;
    static final int TOP_Y = 32; // just below the role type counts

    /** Purple, for a running vote or support count. */
    static final int ACTIVE_BORDER = ARGB.color(255, 138, 43, 226);
    /** Yellow, for a nomination or the setup wizard. */
    static final int IDLE_BORDER = ARGB.color(255, 200, 200, 0);

    /** Draws the box sized to the widest line, with a double border when asked. */
    static void draw(GuiGraphicsExtractor context, Minecraft client, int minWidth, int borderColor, boolean doubleBorder, List<Component> lines) {
        int screenWidth = context.guiWidth();
        int hudWidth = boxWidth(client, screenWidth, minWidth, lines.toArray(new Component[0]));
        int hudHeight = PADDING * 2 + (LINE_HEIGHT + 2) * lines.size();
        int hudX = (screenWidth - hudWidth) / 2;
        int hudY = TOP_Y;

        context.fill(hudX, hudY, hudX + hudWidth, hudY + hudHeight, ARGB.color(200, 0, 0, 0));
        context.outline(hudX, hudY, hudWidth, hudHeight, borderColor);
        if (doubleBorder) {
            context.outline(hudX + 1, hudY + 1, hudWidth - 2, hudHeight - 2, borderColor);
        }

        int centerX = hudX + hudWidth / 2;
        int currentY = hudY + PADDING;
        for (Component line : lines) {
            context.centeredText(client.font, line, centerX, currentY, 0xFFFFFFFF);
            currentY += LINE_HEIGHT + 2;
        }
    }

    /**
     * Width that fits the widest line, never below {@code minWidth} and never wider than the
     * screen allows.
     */
    static int boxWidth(Minecraft client, int screenWidth, int minWidth, Component... lines) {
        int widest = 0;
        for (Component line : lines) {
            widest = Math.max(widest, client.font.width(line));
        }
        return Math.min(Math.max(minWidth, widest + PADDING * 2), Math.max(minWidth, screenWidth - 8));
    }
}
