package com.autumnwind.botb.gui.widget;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.widget.ElementListWidget;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;

import java.util.Collections;
import java.util.List;

/**
 * Read-only rows for the documentation-style lists (settings help, storyteller guide,
 * glossary, character details): a shadowed heading, a plain wrapped text line, or a blank
 * spacer. Screens choose the heading style and text color; the row only draws.
 */
public abstract class DocumentEntry extends ElementListWidget.Entry<DocumentEntry> {

    @Override
    public List<? extends Element> children() {
        return Collections.emptyList();
    }

    @Override
    public List<? extends Selectable> selectableChildren() {
        return Collections.emptyList();
    }

    /** A heading drawn with a shadow in white; pass the text already formatted. */
    public static DocumentEntry title(TextRenderer textRenderer, Text text) {
        return new DocumentEntry() {
            @Override
            public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
                context.drawTextWithShadow(textRenderer, text, x, y, 0xFFFFFF);
            }
        };
    }

    /** One wrapped line of body text, drawn without a shadow. */
    public static DocumentEntry text(TextRenderer textRenderer, OrderedText text, int color) {
        return text(textRenderer, text, color, 0);
    }

    /** Same, shifted right by {@code indent} pixels, for the continuation lines of a bullet. */
    public static DocumentEntry text(TextRenderer textRenderer, OrderedText text, int color, int indent) {
        return new DocumentEntry() {
            @Override
            public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
                context.drawText(textRenderer, text, x + indent, y, color, false);
            }
        };
    }

    /** An empty row. */
    public static DocumentEntry spacer() {
        return new DocumentEntry() {
            @Override
            public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            }
        };
    }
}
