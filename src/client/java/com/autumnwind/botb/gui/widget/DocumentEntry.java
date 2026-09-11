package com.autumnwind.botb.gui.widget;

import java.util.Collections;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/**
 * Read-only rows for the documentation-style lists (settings help, storyteller guide,
 * glossary, character details): a shadowed heading, a plain wrapped text line, or a blank
 * spacer. Screens choose the heading style and text color; the row only draws.
 */
public abstract class DocumentEntry extends ContainerObjectSelectionList.Entry<DocumentEntry> {

    @Override
    public List<? extends GuiEventListener> children() {
        return Collections.emptyList();
    }

    @Override
    public List<? extends NarratableEntry> narratables() {
        return Collections.emptyList();
    }

    /** A heading drawn with a shadow in white; pass the text already formatted. */
    public static DocumentEntry title(Font textRenderer, Component text) {
        return new DocumentEntry() {
            @Override
            public void render(GuiGraphics context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
                context.drawString(textRenderer, text, x, y, 0xFFFFFF);
            }
        };
    }

    /** One wrapped line of body text, drawn without a shadow. */
    public static DocumentEntry text(Font textRenderer, FormattedCharSequence text, int color) {
        return new DocumentEntry() {
            @Override
            public void render(GuiGraphics context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
                context.drawString(textRenderer, text, x, y, color, false);
            }
        };
    }

    /** An empty row. */
    public static DocumentEntry spacer() {
        return new DocumentEntry() {
            @Override
            public void render(GuiGraphics context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            }
        };
    }
}
