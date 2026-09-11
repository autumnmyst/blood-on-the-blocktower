package com.autumnwind.botb.gui;

import com.autumnwind.botb.util.RoleType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ElementListWidget;
import net.minecraft.text.MutableText;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

import com.autumnwind.botb.gui.widget.DocumentEntry;

/**
 * Full-screen glossary with game terminology and explanations.
 */
public class GlossaryScreen extends Screen {

    private static final String KEY_PREFIX = "gui.blood-on-the-blocktower.glossary.";

    private final Screen parent;
    private GlossaryListWidget glossaryWidget;

    public GlossaryScreen(Screen parent) {
        super(Text.translatable(KEY_PREFIX + "title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        // Full-screen scrollable glossary
        int listY = 30;
        int listHeight = this.height - listY - 40;

        this.glossaryWidget = new GlossaryListWidget(this.client, this.width - 40, listHeight, listY);
        this.glossaryWidget.setX(20);
        this.addDrawableChild(this.glossaryWidget);

        // Back button
        int backButtonWidth = 60;
        this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("gui.blood-on-the-blocktower.back").formatted(Formatting.YELLOW),
                button -> this.client.setScreen(this.parent)
        ).dimensions(this.width - backButtonWidth - 10, this.height - 30, backButtonWidth, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        // Draw title
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 10, 0xFFFFFF);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_E) {
            this.client.setScreen(this.parent);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    /**
     * Full-screen scrollable glossary widget.
     */
    private class GlossaryListWidget extends ElementListWidget<DocumentEntry> {

        public GlossaryListWidget(MinecraftClient client, int width, int height, int y) {
            super(client, width, height, y, client.textRenderer.fontHeight + 2);

            int textWidth = this.getRowWidth() - 10;

            // --- Alignment ---
            addTitle("alignment.title");
            this.addEntry(DocumentEntry.spacer());
            addWrappedText("alignment.body", textWidth);
            this.addEntry(DocumentEntry.spacer());
            this.addEntry(DocumentEntry.spacer());

            // --- Role Types ---
            addTitle("role_types.title");
            this.addEntry(DocumentEntry.spacer());
            addColoredRoleType("townsfolk", RoleType.TOWNSFOLK.getColor(), "townsfolk.description", textWidth);
            addColoredRoleType("outsiders", RoleType.OUTSIDER.getColor(), "outsiders.description", textWidth);
            addColoredRoleType("minions", RoleType.MINION.getColor(), "minions.description", textWidth);
            addColoredRoleType("demons", RoleType.DEMON.getColor(), "demons.description", textWidth);
            this.addEntry(DocumentEntry.spacer());
            this.addEntry(DocumentEntry.title(textRenderer, Text.translatable(KEY_PREFIX + "extra").formatted(Formatting.GRAY, Formatting.ITALIC)));
            addColoredRoleType("travelers", RoleType.TRAVELER.getColor(), "travelers.description", textWidth);
            addColoredRoleType("fabled", RoleType.FABLED.getColor(), "fabled.description", textWidth);
            addColoredRoleType("loric", RoleType.LORIC.getColor(), "loric.description", textWidth);
            this.addEntry(DocumentEntry.spacer());
            this.addEntry(DocumentEntry.spacer());

            // --- Nominations ---
            addTitle("nominations.title");
            this.addEntry(DocumentEntry.spacer());
            addWrappedText("nominations.body", textWidth);
            this.addEntry(DocumentEntry.spacer());
            this.addEntry(DocumentEntry.spacer());

            // --- Voting ---
            addTitle("voting.title");
            this.addEntry(DocumentEntry.spacer());
            addWrappedText("voting.body", textWidth);
            this.addEntry(DocumentEntry.spacer());
            this.addEntry(DocumentEntry.spacer());

            // --- Ghost Votes ---
            addTitle("ghost_votes.title");
            this.addEntry(DocumentEntry.spacer());
            addWrappedText("ghost_votes.body", textWidth);
            this.addEntry(DocumentEntry.spacer());
            this.addEntry(DocumentEntry.spacer());

            // --- Execution ---
            addTitle("execution.title");
            this.addEntry(DocumentEntry.spacer());
            addWrappedText("execution.body", textWidth);
            this.addEntry(DocumentEntry.spacer());
            this.addEntry(DocumentEntry.spacer());

            // --- Madness ---
            addTitle("madness.title");
            this.addEntry(DocumentEntry.spacer());
            addWrappedText("madness.body", textWidth);
            this.addEntry(DocumentEntry.spacer());
            this.addEntry(DocumentEntry.spacer());

            // --- Poisoning ---
            addTitle("droisoning.title");
            this.addEntry(DocumentEntry.spacer());
            addWrappedText("droisoning.body", textWidth);
            this.addEntry(DocumentEntry.spacer());
            this.addEntry(DocumentEntry.spacer());

            // --- Registration ---
            addTitle("registration.title");
            this.addEntry(DocumentEntry.spacer());
            addWrappedText("registration.body", textWidth);
            this.addEntry(DocumentEntry.spacer());
            this.addEntry(DocumentEntry.spacer());

            // --- Protection ---
            addTitle("protection.title");
            this.addEntry(DocumentEntry.spacer());
            addWrappedText("protection.body", textWidth);
            this.addEntry(DocumentEntry.spacer());
            this.addEntry(DocumentEntry.spacer());

            // --- Bluffs ---
            addTitle("bluffs.title");
            this.addEntry(DocumentEntry.spacer());
            addWrappedText("bluffs.body", textWidth);
        }

        private void addTitle(String key) {
            this.addEntry(DocumentEntry.title(textRenderer, Text.translatable(KEY_PREFIX + key).formatted(Formatting.GOLD, Formatting.BOLD)));
        }

        private void addWrappedText(String key, int width) {
            for (OrderedText line : textRenderer.wrapLines(Text.translatable(KEY_PREFIX + key), width)) {
                this.addEntry(DocumentEntry.text(textRenderer, line, 0xFFFFFF));
            }
        }

        private void addColoredRoleType(String roleTypeKey, int color, String descriptionKey, int width) {
            MutableText text = Text.translatable(KEY_PREFIX + roleTypeKey).withColor(color)
                    .append(Text.translatable(KEY_PREFIX + descriptionKey).formatted(Formatting.WHITE));
            for (OrderedText line : textRenderer.wrapLines(text, width)) {
                this.addEntry(DocumentEntry.text(textRenderer, line, 0xFFFFFF));
            }
        }

        @Override
        public int getRowWidth() {
            return this.width - 20;
        }

        @Override
        protected int getScrollbarX() {
            return this.getX() + this.width - 6;
        }

    }
}
