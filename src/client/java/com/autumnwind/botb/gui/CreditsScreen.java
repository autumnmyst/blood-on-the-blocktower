package com.autumnwind.botb.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ElementListWidget;
import net.minecraft.text.MutableText;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

import java.util.Collections;
import java.util.List;

/**
 * Full-screen credits, reached from the Settings screen.
 *
 * <p>Content lives in {@link CreditsListWidget#populate()}. Add sections there with
 * {@code section}, {@code entry}, {@code body} and {@code link}.
 */
public class CreditsScreen extends Screen {

    /** Title artwork across the top, standing in for a written heading. */
    private static final int TITLE_ART_Y = 6;
    private static final int TITLE_ART_WIDTH = 200;

    private final Screen parent;
    private CreditsListWidget creditsWidget;

    public CreditsScreen(Screen parent) {
        super(Text.translatable("gui.blood-on-the-blocktower.credits.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int listY = TITLE_ART_Y + TitleArt.heightFor(TITLE_ART_WIDTH) + 6;
        int listHeight = this.height - listY - 40;

        this.creditsWidget = new CreditsListWidget(this.client, this.width - 40, listHeight, listY);
        this.creditsWidget.setX(20);
        this.addDrawableChild(this.creditsWidget);

        int backButtonWidth = 60;
        this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("gui.blood-on-the-blocktower.credits.back").formatted(Formatting.YELLOW),
                button -> this.client.setScreen(this.parent)
        ).dimensions(this.width - backButtonWidth - 10, this.height - 30, backButtonWidth, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        TitleArt.drawCentered(context, this.width / 2, TITLE_ART_Y, TITLE_ART_WIDTH);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_E) {
            this.client.setScreen(this.parent);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private class CreditsListWidget extends ElementListWidget<CreditsListWidget.Entry> {

        private int textWidth;

        public CreditsListWidget(MinecraftClient client, int width, int height, int y) {
            super(client, width, height, y, client.textRenderer.fontHeight + 2);
            this.textWidth = this.getRowWidth() - 10;
            populate();
        }

        /** The credits themselves. */
        private void populate() {
            section(Text.literal("Blood on the Blocktower"));
            entry(Text.translatable("gui.blood-on-the-blocktower.credits.entry.by"), Text.literal("Autumn Wind (autumnmyst)"));
            body(Text.translatable("gui.blood-on-the-blocktower.credits.body.blocktower"));
            spacer();

            section(Text.literal("Blood on the Clocktower"));
            entry(Text.translatable("gui.blood-on-the-blocktower.credits.entry.game_design"), Text.literal("Steven Medway"));
            entry(Text.translatable("gui.blood-on-the-blocktower.credits.entry.published_by"), Text.literal("The Pandemonium Institute"));
            body(Text.translatable("gui.blood-on-the-blocktower.credits.body.clocktower"));
            spacer();

            section(Text.literal("The Yogscast"));
            body(Text.translatable("gui.blood-on-the-blocktower.credits.body.yogscast"));
            spacer();

            section(Text.translatable("gui.blood-on-the-blocktower.credits.section.art"));
            entry(Text.translatable("gui.blood-on-the-blocktower.credits.entry.role_icons"), Text.translatable("gui.blood-on-the-blocktower.credits.entry.role_icons_value"));
            entry(Text.translatable("gui.blood-on-the-blocktower.credits.entry.other_icons"), Text.translatable("gui.blood-on-the-blocktower.credits.entry.other_icons_value"));
            entry(Text.translatable("gui.blood-on-the-blocktower.credits.entry.clock_hands"), Text.translatable("gui.blood-on-the-blocktower.credits.entry.clock_hands_value"));
            spacer();

            section(Text.translatable("gui.blood-on-the-blocktower.credits.section.sounds"));
            entry(Text.translatable("gui.blood-on-the-blocktower.credits.entry.receive_role"), Text.translatable("gui.blood-on-the-blocktower.credits.entry.receive_role_value"));
            entry(Text.translatable("gui.blood-on-the-blocktower.credits.entry.dawn"), Text.literal("Edvard Grieg - Morning Mood (Au Matin)"));
            entry(Text.translatable("gui.blood-on-the-blocktower.credits.entry.dusk"), Text.translatable("gui.blood-on-the-blocktower.credits.entry.dusk_value"));
            entry(Text.translatable("gui.blood-on-the-blocktower.credits.entry.game_end"), Text.translatable("gui.blood-on-the-blocktower.credits.entry.game_end_value"));
            entry(Text.translatable("gui.blood-on-the-blocktower.credits.entry.everything_else"), Text.translatable("gui.blood-on-the-blocktower.credits.entry.everything_else_value"));
            spacer();

            section(Text.translatable("gui.blood-on-the-blocktower.credits.section.thanks"));
            body(Text.translatable("gui.blood-on-the-blocktower.credits.body.thanks"));
            spacer();

            section(Text.translatable("gui.blood-on-the-blocktower.credits.section.links"));
            link(Text.literal("Blood on the Clocktower"), "https://bloodontheclocktower.com");
            link(Text.literal("The Yogscast"), "https://www.youtube.com/@yogscast");
            link(Text.translatable("gui.blood-on-the-blocktower.credits.link.custom_role_creator"), "https://bloodstar.clocktica.com/");
            spacer();

            section(Text.translatable("gui.blood-on-the-blocktower.credits.section.all_rights_reserved"));
            body(Text.translatable("gui.blood-on-the-blocktower.credits.body.all_rights_reserved"));
        }

        // ---- Content helpers -------------------------------------------------

        /** A gold, bold section heading. */
        private void section(Text title) {
            this.addEntry(new TextEntry(title.copy().formatted(Formatting.GOLD, Formatting.BOLD)));
        }

        /** A "Role: Name" line with the role in grey and the name in white. */
        private void entry(Text role, Text name) {
            this.addEntry(new TextEntry(Text.translatable("gui.blood-on-the-blocktower.credits.entry_label", role).formatted(Formatting.GRAY)
                    .append(name.copy().formatted(Formatting.WHITE))));
        }

        /** A paragraph, wrapped to the list width. */
        private void body(Text text) {
            wrap(text.copy().formatted(Formatting.WHITE));
        }

        /** A label with a URL beneath it, aqua so it reads as a link. */
        private void link(Text label, String url) {
            this.addEntry(new TextEntry(label.copy().formatted(Formatting.WHITE)));
            wrap(Text.literal("  " + url).formatted(Formatting.AQUA));
        }

        private void spacer() {
            this.addEntry(new TextEntry(Text.empty()));
        }

        private void wrap(MutableText text) {
            for (OrderedText line : textRenderer.wrapLines(text, textWidth)) {
                this.addEntry(new OrderedTextEntry(line));
            }
        }

        // ---- Widget plumbing -------------------------------------------------

        @Override
        public int getRowWidth() {
            return this.width - 20;
        }

        @Override
        protected int getScrollbarX() {
            return this.getX() + this.width - 6;
        }

        public abstract class Entry extends ElementListWidget.Entry<Entry> {
            @Override
            public List<? extends Element> children() {
                return Collections.emptyList();
            }

            @Override
            public List<? extends Selectable> selectableChildren() {
                return Collections.emptyList();
            }
        }

        public class TextEntry extends Entry {
            private final Text text;

            public TextEntry(Text text) {
                this.text = text;
            }

            @Override
            public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight,
                               int mouseX, int mouseY, boolean hovered, float tickDelta) {
                context.drawTextWithShadow(textRenderer, text, x, y, 0xFFFFFF);
            }
        }

        public class OrderedTextEntry extends Entry {
            private final OrderedText text;

            public OrderedTextEntry(OrderedText text) {
                this.text = text;
            }

            @Override
            public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight,
                               int mouseX, int mouseY, boolean hovered, float tickDelta) {
                context.drawTextWithShadow(textRenderer, text, x, y, 0xFFFFFF);
            }
        }
    }
}
