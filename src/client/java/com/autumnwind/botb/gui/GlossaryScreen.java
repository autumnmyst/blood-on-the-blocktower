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

    private final Screen parent;
    private GlossaryListWidget glossaryWidget;

    public GlossaryScreen(Screen parent) {
        super(Text.literal("Glossary"));
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
                Text.literal("Back").formatted(Formatting.YELLOW),
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
            this.addEntry(DocumentEntry.title(textRenderer, Text.literal("Alignment").formatted(Formatting.GOLD, Formatting.BOLD)));
            this.addEntry(DocumentEntry.spacer());
            addWrappedText("Good (blue) vs Evil (red). Good players want to find and eliminate the Demon. Evil players want to keep the Demon alive and kill the town.", textWidth);
            this.addEntry(DocumentEntry.spacer());
            this.addEntry(DocumentEntry.spacer());

            // --- Role Types ---
            this.addEntry(DocumentEntry.title(textRenderer, Text.literal("Role Types").formatted(Formatting.GOLD, Formatting.BOLD)));
            this.addEntry(DocumentEntry.spacer());
            addColoredRoleType("Townsfolk", RoleType.TOWNSFOLK.getColor(), ": Good characters with helpful abilities.", textWidth);
            addColoredRoleType("Outsiders", RoleType.OUTSIDER.getColor(), ": Good characters whose abilities help evil.", textWidth);
            addColoredRoleType("Minions", RoleType.MINION.getColor(), ": Evil characters who support the Demon.", textWidth);
            addColoredRoleType("Demons", RoleType.DEMON.getColor(), ": Evil characters who (usually) kill at night.", textWidth);
            this.addEntry(DocumentEntry.spacer());
            this.addEntry(DocumentEntry.title(textRenderer, Text.literal("Extra:").formatted(Formatting.GRAY, Formatting.ITALIC)));
            addColoredRoleType("Travelers", RoleType.TRAVELER.getColor(), ": Good or evil players who may join or leave mid-game.", textWidth);
            addColoredRoleType("Fabled", RoleType.FABLED.getColor(), ": Extra rules to run more balanced and inclusive games.", textWidth);
            addColoredRoleType("Loric", RoleType.LORIC.getColor(), ": Extra rules to make the game feel fresh and interesting.", textWidth);
            this.addEntry(DocumentEntry.spacer());
            this.addEntry(DocumentEntry.spacer());

            // --- Nominations ---
            this.addEntry(DocumentEntry.title(textRenderer, Text.literal("Nominations").formatted(Formatting.GOLD, Formatting.BOLD)));
            this.addEntry(DocumentEntry.spacer());
            addWrappedText("During the day, living players can nominate others for execution. Each player can only nominate once per day, and each player can only be nominated once per day.", textWidth);
            this.addEntry(DocumentEntry.spacer());
            this.addEntry(DocumentEntry.spacer());

            // --- Voting ---
            this.addEntry(DocumentEntry.title(textRenderer, Text.literal("Voting").formatted(Formatting.GOLD, Formatting.BOLD)));
            this.addEntry(DocumentEntry.spacer());
            addWrappedText("After a nomination, all players vote. Votes are locked in order around the circle. You need at least half of living players to vote yes, plus more than any previous vote that day.", textWidth);
            this.addEntry(DocumentEntry.spacer());
            this.addEntry(DocumentEntry.spacer());

            // --- Ghost Votes ---
            this.addEntry(DocumentEntry.title(textRenderer, Text.literal("Ghost Votes").formatted(Formatting.GOLD, Formatting.BOLD)));
            this.addEntry(DocumentEntry.spacer());
            addWrappedText("Dead players get one ghost vote to use for the rest of the game. Choose wisely when to use it!", textWidth);
            this.addEntry(DocumentEntry.spacer());
            this.addEntry(DocumentEntry.spacer());

            // --- Execution ---
            this.addEntry(DocumentEntry.title(textRenderer, Text.literal("Execution").formatted(Formatting.GOLD, Formatting.BOLD)));
            this.addEntry(DocumentEntry.spacer());
            addWrappedText("If a player receives enough votes, they are marked for execution. At the end of the day, the player with the most votes is executed (if any). Some characters may survive execution.", textWidth);
            this.addEntry(DocumentEntry.spacer());
            this.addEntry(DocumentEntry.spacer());

            // --- Madness ---
            this.addEntry(DocumentEntry.title(textRenderer, Text.literal("Madness").formatted(Formatting.GOLD, Formatting.BOLD)));
            this.addEntry(DocumentEntry.spacer());
            addWrappedText("Some abilities require you to be 'mad' about something - you must try to convince others it's true, or face consequences.", textWidth);
            this.addEntry(DocumentEntry.spacer());
            this.addEntry(DocumentEntry.spacer());

            // --- Poisoning ---
            this.addEntry(DocumentEntry.title(textRenderer, Text.literal("Drunkenness and Poisoning").formatted(Formatting.GOLD, Formatting.BOLD)));
            this.addEntry(DocumentEntry.spacer());
            addWrappedText("A drunk or poisoned player's ability does not work correctly. They may receive false information or their ability may have no effect. The player does not know they are drunk or poisoned. The effects of 'drunk' and 'poisoned' are functionally identical.", textWidth);
            this.addEntry(DocumentEntry.spacer());
            this.addEntry(DocumentEntry.spacer());

            // --- Registration ---
            this.addEntry(DocumentEntry.title(textRenderer, Text.literal("Registration").formatted(Formatting.GOLD, Formatting.BOLD)));
            this.addEntry(DocumentEntry.spacer());
            addWrappedText("Some abilities detect character types or alignments. A player 'registers' as whatever they appear to be to these abilities - which may differ from their actual role due to other abilities.", textWidth);
            this.addEntry(DocumentEntry.spacer());
            this.addEntry(DocumentEntry.spacer());

            // --- Protection ---
            this.addEntry(DocumentEntry.title(textRenderer, Text.literal("Protection").formatted(Formatting.GOLD, Formatting.BOLD)));
            this.addEntry(DocumentEntry.spacer());
            addWrappedText("Some characters can protect others from the Demon. This protection not only prevents the player from being killed by the Demon, but also makes that player immune to the Demon's other effects.", textWidth);
            this.addEntry(DocumentEntry.spacer());
            this.addEntry(DocumentEntry.spacer());

            // --- Bluffs ---
            this.addEntry(DocumentEntry.title(textRenderer, Text.literal("Bluffs").formatted(Formatting.GOLD, Formatting.BOLD)));
            this.addEntry(DocumentEntry.spacer());
            addWrappedText("At the start of the game, the Demon learns three 'bluff' roles - good roles that are not in play. Evil players can safely claim to be these roles without conflicting with a real player.", textWidth);
        }

        private void addWrappedText(String text, int width) {
            for (OrderedText line : textRenderer.wrapLines(Text.literal(text), width)) {
                this.addEntry(DocumentEntry.text(textRenderer, line, 0xFFFFFF));
            }
        }

        private void addColoredRoleType(String roleTypeName, int color, String description, int width) {
            MutableText text = Text.literal(roleTypeName).withColor(color)
                    .append(Text.literal(description).formatted(Formatting.WHITE));
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
