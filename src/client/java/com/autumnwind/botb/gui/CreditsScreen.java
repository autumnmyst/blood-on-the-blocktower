package com.autumnwind.botb.gui;

import org.lwjgl.glfw.GLFW;

import java.util.Collections;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.client.input.KeyEvent;

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
        super(Component.translatable("gui.blood-on-the-blocktower.credits.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int listY = TITLE_ART_Y + TitleArt.heightFor(TITLE_ART_WIDTH) + 6;
        int listHeight = this.height - listY - 40;

        this.creditsWidget = new CreditsListWidget(this.minecraft, this.width - 40, listHeight, listY);
        this.creditsWidget.updateSizeAndPosition(this.width - 40, listHeight, 20, listY);
        this.addRenderableWidget(this.creditsWidget);

        int backButtonWidth = 60;
        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.blood-on-the-blocktower.credits.back").withStyle(ChatFormatting.YELLOW),
                button -> this.minecraft.gui.setScreen(this.parent)
        ).bounds(this.width - backButtonWidth - 10, this.height - 30, backButtonWidth, 20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractRenderState(context, mouseX, mouseY, delta);
        TitleArt.drawCentered(context, this.width / 2, TITLE_ART_Y, TITLE_ART_WIDTH);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int keyCode = event.key();
        int scanCode = event.scancode();
        int modifiers = event.modifiers();

        if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_E) {
            this.minecraft.gui.setScreen(this.parent);
            return true;
        }
        return super.keyPressed(event);
    }

    private class CreditsListWidget extends ContainerObjectSelectionList<CreditsListWidget.Entry> {

        private int textWidth;

        public CreditsListWidget(Minecraft client, int width, int height, int y) {
            super(client, width, height, y, client.font.lineHeight + 2);
            this.textWidth = this.getRowWidth() - 10;
            populate();
        }

        /** The credits themselves. */
        private void populate() {
            section(Component.literal("Blood on the Blocktower"));
            entry(Component.translatable("gui.blood-on-the-blocktower.credits.entry.by"), Component.literal("Autumn Wind (autumnmyst)"));
            body(Component.translatable("gui.blood-on-the-blocktower.credits.body.blocktower"));
            spacer();

            section(Component.literal("Blood on the Clocktower"));
            entry(Component.translatable("gui.blood-on-the-blocktower.credits.entry.game_design"), Component.literal("Steven Medway"));
            entry(Component.translatable("gui.blood-on-the-blocktower.credits.entry.published_by"), Component.literal("The Pandemonium Institute"));
            body(Component.translatable("gui.blood-on-the-blocktower.credits.body.clocktower"));
            spacer();

            section(Component.literal("The Yogscast"));
            body(Component.translatable("gui.blood-on-the-blocktower.credits.body.yogscast"));
            spacer();

            section(Component.translatable("gui.blood-on-the-blocktower.credits.section.art"));
            entry(Component.translatable("gui.blood-on-the-blocktower.credits.entry.role_icons"), Component.translatable("gui.blood-on-the-blocktower.credits.entry.role_icons_value"));
            entry(Component.translatable("gui.blood-on-the-blocktower.credits.entry.other_icons"), Component.translatable("gui.blood-on-the-blocktower.credits.entry.other_icons_value"));
            entry(Component.translatable("gui.blood-on-the-blocktower.credits.entry.clock_hands"), Component.translatable("gui.blood-on-the-blocktower.credits.entry.clock_hands_value"));
            spacer();

            section(Component.translatable("gui.blood-on-the-blocktower.credits.section.sounds"));
            entry(Component.translatable("gui.blood-on-the-blocktower.credits.entry.receive_role"), Component.translatable("gui.blood-on-the-blocktower.credits.entry.receive_role_value"));
            entry(Component.translatable("gui.blood-on-the-blocktower.credits.entry.dawn"), Component.literal("Edvard Grieg - Morning Mood (Au Matin)"));
            entry(Component.translatable("gui.blood-on-the-blocktower.credits.entry.dusk"), Component.translatable("gui.blood-on-the-blocktower.credits.entry.dusk_value"));
            entry(Component.translatable("gui.blood-on-the-blocktower.credits.entry.game_end"), Component.translatable("gui.blood-on-the-blocktower.credits.entry.game_end_value"));
            entry(Component.translatable("gui.blood-on-the-blocktower.credits.entry.everything_else"), Component.translatable("gui.blood-on-the-blocktower.credits.entry.everything_else_value"));
            spacer();

            section(Component.translatable("gui.blood-on-the-blocktower.credits.section.thanks"));
            body(Component.translatable("gui.blood-on-the-blocktower.credits.body.thanks"));
            spacer();

            section(Component.translatable("gui.blood-on-the-blocktower.credits.section.links"));
            link(Component.literal("Blood on the Clocktower"), "https://bloodontheclocktower.com");
            link(Component.literal("The Yogscast"), "https://www.youtube.com/@yogscast");
            link(Component.translatable("gui.blood-on-the-blocktower.credits.link.custom_role_creator"), "https://bloodstar.clocktica.com/");
            spacer();

            section(Component.translatable("gui.blood-on-the-blocktower.credits.section.all_rights_reserved"));
            body(Component.translatable("gui.blood-on-the-blocktower.credits.body.all_rights_reserved"));
        }

        // ---- Content helpers -------------------------------------------------

        /** A gold, bold section heading. */
        private void section(Component title) {
            this.addEntry(new TextEntry(title.copy().withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)));
        }

        /** A "Role: Name" line with the role in grey and the name in white. */
        private void entry(Component role, Component name) {
            this.addEntry(new TextEntry(Component.translatable("gui.blood-on-the-blocktower.credits.entry_label", role).withStyle(ChatFormatting.GRAY)
                    .append(name.copy().withStyle(ChatFormatting.WHITE))));
        }

        /** A paragraph, wrapped to the list width. */
        private void body(Component text) {
            wrap(text.copy().withStyle(ChatFormatting.WHITE));
        }

        /** A label with a URL beneath it, aqua so it reads as a link. */
        private void link(Component label, String url) {
            this.addEntry(new TextEntry(label.copy().withStyle(ChatFormatting.WHITE)));
            wrap(Component.literal("  " + url).withStyle(ChatFormatting.AQUA));
        }

        private void spacer() {
            this.addEntry(new TextEntry(Component.empty()));
        }

        private void wrap(MutableComponent text) {
            for (FormattedCharSequence line : font.split(text, textWidth)) {
                this.addEntry(new OrderedTextEntry(line));
            }
        }

        // ---- Widget plumbing -------------------------------------------------

        @Override
        public int getRowWidth() {
            return this.width - 20;
        }

        @Override
        protected int scrollBarX() {
            return this.getX() + this.width - 6;
        }

        public abstract class Entry extends ContainerObjectSelectionList.Entry<com.autumnwind.botb.gui.CreditsScreen.CreditsListWidget.Entry> {
            @Override
            public List<? extends GuiEventListener> children() {
                return Collections.emptyList();
            }

            @Override
            public List<? extends NarratableEntry> narratables() {
                return Collections.emptyList();
            }
        }

        public class TextEntry extends com.autumnwind.botb.gui.CreditsScreen.CreditsListWidget.Entry {
            private final Component text;

            public TextEntry(Component text) {
                this.text = text;
            }

            @Override
            public void extractContent(GuiGraphicsExtractor context, int mouseX, int mouseY, boolean hovered, float tickDelta) {
                int x = getContentX();
                int y = getContentY();

                context.text(font, text, x, y, 0xFFFFFFFF);
            }
        }

        public class OrderedTextEntry extends com.autumnwind.botb.gui.CreditsScreen.CreditsListWidget.Entry {
            private final FormattedCharSequence text;

            public OrderedTextEntry(FormattedCharSequence text) {
                this.text = text;
            }

            @Override
            public void extractContent(GuiGraphicsExtractor context, int mouseX, int mouseY, boolean hovered, float tickDelta) {
                int x = getContentX();
                int y = getContentY();

                context.text(font, text, x, y, 0xFFFFFFFF);
            }
        }
    }
}
