package com.autumnwind.botb.gui;

import com.autumnwind.botb.BloodOnTheBlocktower;
import com.mojang.blaze3d.systems.RenderSystem;
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
import net.minecraft.util.Identifier;
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
    private static final Identifier YOGSCAST_LOGO =
            Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/thirdpartylogos/the_yogscast_logo.png");

    private final Screen parent;
    private CreditsListWidget creditsWidget;

    public CreditsScreen(Screen parent) {
        super(Text.literal("Credits"));
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
                Text.literal("Back").formatted(Formatting.YELLOW),
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
            section("Blood on the Blocktower");
            entry("By", "Autumn Wind (autumnmyst)");
            body("""
                    A Minecraft adaptation of Blood on the Clocktower,
                    the social deduction game by The Pandemonium Institute.""");
            spacer();

            section("Blood on the Clocktower");
            entry("Game design", "Steven Medway");
            entry("Published by", "The Pandemonium Institute");
            body("""
                    Names, abilities, and almanac text belong to The Pandemonium Institute.
                    This mod is an UNOFFICIAL and UNAFFILIATED fan project.""");
            spacer();

            image(YOGSCAST_LOGO, 1500, 436, 28);
            section("The Yogscast");
            body("Almost all of the role icons in this mod are by the fantastic folks at The Yogscast! They were a huge inspiration for this mod, and their role icons really bring the whole aesthetic together.");
            spacer();

            section("Art");
            entry("Role icons", "Artist credit is on the bottom right of each role's details page");
            entry("Other icons", "Dawn, Dusk, Minion, and Demon icons by The Yogscast");
            entry("Clock hands", "Retextured stock images");
            spacer();

            section("Sounds");
            entry("Receive Role", "\"Role Reveal\" sound from Innersloth's \"Among Us\"");
            entry("Dawn", "Edvard Grieg - Morning Mood (Au Matin)");
            entry("Dusk", "\"Secret Area Discovered\" sound from Team Cherry's \"Hollow Knight\"");
            entry("Game End", "\"Boss Defeat\" sound from Team Cherry's \"Hollow Knight\"");
            entry("Everything Else", "Royalty free sounds");
            spacer();

            section("Thanks");
            body("An enormous thank you to Steven Medway & The Pandemonium Institute for making such an amazing game and fostering this great community, to The Yogscast for their fabulous icons, and to all the friends who came together each week to play and test the mod with me. Seriously, I can't thank you enough.");
            spacer();

            section("Links");
            link("Blood on the Clocktower", "https://bloodontheclocktower.com");
            link("The Yogscast", "https://www.youtube.com/@yogscast");
            link("Custom Role Creator", "https://bloodstar.clocktica.com/");
            spacer();

            section("All Rights Reserved");
            body("This applies to the code and my own assets. I claim no rights to third-party works used and credited, such as Blood on the Clocktower (owned by Steven Medway and The Pandemonium Institute) and assets by The Yogscast; refer to the respective owners for their licensing terms.");
        }

        // ---- Content helpers -------------------------------------------------

        /** A gold, bold section heading. */
        private void section(String title) {
            this.addEntry(new TextEntry(Text.literal(title).formatted(Formatting.GOLD, Formatting.BOLD)));
        }

        /** A "Role: Name" line with the role in grey and the name in white. */
        private void entry(String role, String name) {
            this.addEntry(new TextEntry(Text.literal(role + ": ").formatted(Formatting.GRAY)
                    .append(Text.literal(name).formatted(Formatting.WHITE))));
        }

        /** A paragraph, wrapped to the list width. */
        private void body(String text) {
            wrap(Text.literal(text).formatted(Formatting.WHITE));
        }

        /** A label with a URL beneath it, aqua so it reads as a link. */
        private void link(String label, String url) {
            this.addEntry(new TextEntry(Text.literal(label).formatted(Formatting.WHITE)));
            wrap(Text.literal("  " + url).formatted(Formatting.AQUA));
        }

        /** A centered image; blank rows after it reserve the rest of its height. */
        private void image(Identifier texture, int textureWidth, int textureHeight, int drawHeight) {
            int drawWidth = Math.round((float) drawHeight * textureWidth / textureHeight);
            this.addEntry(new ImageEntry(texture, drawWidth, drawHeight, textureWidth, textureHeight));
            for (int reserved = itemHeight; reserved < drawHeight; reserved += itemHeight) {
                spacer();
            }
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

        public class ImageEntry extends Entry {
            private final Identifier texture;
            private final int drawWidth;
            private final int drawHeight;
            private final int textureWidth;
            private final int textureHeight;

            public ImageEntry(Identifier texture, int drawWidth, int drawHeight, int textureWidth, int textureHeight) {
                this.texture = texture;
                this.drawWidth = drawWidth;
                this.drawHeight = drawHeight;
                this.textureWidth = textureWidth;
                this.textureHeight = textureHeight;
            }

            @Override
            public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight,
                               int mouseX, int mouseY, boolean hovered, float tickDelta) {
                // Linear sampling, reapplied each draw because a resource reload rebuilds the
                // texture object with the default nearest filter.
                MinecraftClient.getInstance().getTextureManager().getTexture(texture).setFilter(true, false);
                RenderSystem.enableBlend();
                context.drawTexture(texture, x + (entryWidth - drawWidth) / 2, y, drawWidth, drawHeight,
                        0, 0, textureWidth, textureHeight, textureWidth, textureHeight);
                RenderSystem.disableBlend();
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
