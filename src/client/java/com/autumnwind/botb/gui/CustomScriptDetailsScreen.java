package com.autumnwind.botb.gui;

import com.autumnwind.botb.event.KeyInputHandler;
import com.autumnwind.botb.states.ClientState;
import com.autumnwind.botb.util.AlmanacData;
import com.autumnwind.botb.util.AlmanacParser;
import com.autumnwind.botb.util.Script;
import com.autumnwind.botb.util.UrlTextureLoader;
import com.autumnwind.botb.util.UrlTextureLoaderImpl;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ElementListWidget;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Screen for displaying custom script details from the almanac.
 * Shows Synopsis, Overview, and Changelog sections.
 */
public class CustomScriptDetailsScreen extends Screen {

    private enum Page { SYNOPSIS, OVERVIEW, CHANGELOG }
    private Page currentPage = Page.SYNOPSIS;
    private DetailsListWidget listWidget;
    private final Script script;
    private final Screen parent;
    private AlmanacData almanacData;
    private boolean isLoading = true;
    private final Map<Page, Double> savedScrollAmounts = new EnumMap<>(Page.class);

    public CustomScriptDetailsScreen(Screen parent) {
        super(Text.literal("Script Details"));
        this.script = ClientState.currentScript;
        this.parent = parent;

        // Start fetching almanac data
        if (script != null && script.hasAlmanac()) {
            AlmanacParser.fetchAlmanacs(script.almanac(), script.extraAlmanacs())
                .thenAccept(data -> {
                    this.almanacData = data;
                    this.isLoading = false;
                    // Refresh the current page if we're still on screen
                    MinecraftClient.getInstance().execute(() -> {
                        if (MinecraftClient.getInstance().currentScreen == this) {
                            switchPage(currentPage, true);
                        }
                    });
                });
        } else {
            this.almanacData = AlmanacData.empty();
            this.isLoading = false;
        }
    }

    private boolean hasAuthor() {
        return script != null && script.author() != null && !script.author().isEmpty();
    }

    @Override
    protected void init() {
        int buttonWidth = 90;
        int spacing = 10;
        int topY = hasAuthor() ? 27 : 25;
        int synopsisX = this.width / 2 - buttonWidth / 2 - spacing - buttonWidth;
        int overviewX = this.width / 2 - buttonWidth / 2;
        int changelogX = this.width / 2 + buttonWidth / 2 + spacing;

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Synopsis"), b -> switchPage(Page.SYNOPSIS)).dimensions(synopsisX, topY, buttonWidth, 20).build());
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Overview"), b -> switchPage(Page.OVERVIEW)).dimensions(overviewX, topY, buttonWidth, 20).build());
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Changelog"), b -> switchPage(Page.CHANGELOG)).dimensions(changelogX, topY, buttonWidth, 20).build());

        this.switchPage(this.currentPage, true);

        this.addDrawableChild(ButtonWidget.builder(Text.translatable("gui.back"), (button) -> this.client.setScreen(this.parent)).dimensions(this.width / 2 - 100, this.height - 28, 200, 20).build());
    }

    private void switchPage(Page newPage, boolean isInitial) {
        if (!isInitial && this.currentPage == newPage) return;
        if (this.listWidget != null) {
            this.savedScrollAmounts.put(this.currentPage, this.listWidget.getScrollAmount());
            this.remove(this.listWidget);
        }
        this.currentPage = newPage;

        int buttonTopY = hasAuthor() ? 27 : 25;
        int gapBelowButtons = hasAuthor() ? 8 : 10;
        int listTopY = buttonTopY + 20 + gapBelowButtons;
        int footerHeight = 40;

        this.listWidget = new DetailsListWidget(this.client, this.width, this.height - listTopY - footerHeight, listTopY);
        this.listWidget.buildPage(newPage);
        this.listWidget.setScrollAmount(this.savedScrollAmounts.getOrDefault(newPage, 0.0));
        this.addDrawableChild(this.listWidget);
    }

    private void switchPage(Page newPage) {
        this.switchPage(newPage, false);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        if (script != null) {
            if (hasAuthor()) {
                context.drawCenteredTextWithShadow(this.textRenderer, script.name(), this.width / 2, 4, 0xFFFFFF);
                Text authorText = Text.literal("by " + script.author()).formatted(Formatting.GRAY);
                context.drawCenteredTextWithShadow(this.textRenderer, authorText, this.width / 2, 15, 0xAAAAAA);
            } else {
                context.drawCenteredTextWithShadow(this.textRenderer, script.name(), this.width / 2, 8, 0xFFFFFF);
            }
        }

        // Show loading indicator if still fetching
        if (isLoading) {
            context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Loading almanac...").formatted(Formatting.YELLOW), this.width / 2, this.height / 2, 0xFFFFFF);
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (KeyInputHandler.openScriptKey.matchesKey(keyCode, scanCode) || keyCode == GLFW.GLFW_KEY_E) {
            this.client.setScreen(this.parent);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private class DetailsListWidget extends ElementListWidget<DetailsListWidget.Entry> {
        public DetailsListWidget(MinecraftClient client, int width, int height, int y) {
            super(client, width, height, y, client.textRenderer.fontHeight + 2);
        }

        public void buildPage(Page page) {
            this.clearEntries();

            if (isLoading) {
                this.addEntry(new TextEntry(Text.literal("Loading...").formatted(Formatting.ITALIC, Formatting.GRAY)));
                return;
            }

            // Check for errors
            String lastError = AlmanacParser.getLastError();
            if (lastError != null && (almanacData == null || !almanacData.hasScriptData())) {
                this.addEntry(new TextEntry(Text.literal("Failed to load almanac:").formatted(Formatting.RED)));
                this.addEntry(new TextEntry(Text.literal(lastError).formatted(Formatting.GRAY)));
                return;
            }

            if (almanacData == null || almanacData.scriptData() == null) {
                this.addEntry(new TextEntry(Text.literal("No almanac data available.").formatted(Formatting.ITALIC, Formatting.GRAY)));
                return;
            }

            AlmanacData.ScriptAlmanacData scriptData = almanacData.scriptData();
            String content = switch (page) {
                case SYNOPSIS -> scriptData.synopsis();
                case OVERVIEW -> scriptData.overview();
                case CHANGELOG -> scriptData.changelog();
            };

            // For Synopsis page, add the logo centered at the top if present
            if (page == Page.SYNOPSIS && script != null && script.hasLogo()) {
                // Logo is 64px tall, each entry is ~fontHeight+2, so we need multiple entries
                int entryHeight = textRenderer.fontHeight + 2;
                int logoEntries = (64 / entryHeight) + 1;
                for (int i = 0; i < logoEntries; i++) {
                    this.addEntry(new LogoEntry(i));
                }
                this.addEntry(new SpacerEntry());
            }

            // Add a title
            String title = page.name().charAt(0) + page.name().substring(1).toLowerCase();
            this.addEntry(new TitleEntry(title));
            this.addEntry(new SpacerEntry());

            if (content == null || content.isEmpty()) {
                this.addEntry(new TextEntry(Text.literal("No " + page.name().toLowerCase() + " available.").formatted(Formatting.ITALIC, Formatting.GRAY)));
                return;
            }

            // Process content - split by line breaks first, then wrap each paragraph
            int textWidth = this.getRowWidth() - 20;
            String[] paragraphs = content.split("\n");
            boolean firstParagraph = true;
            for (String paragraph : paragraphs) {
                if (paragraph.isEmpty()) {
                    // Empty line = paragraph break
                    this.addEntry(new SpacerEntry());
                    continue;
                }
                if (!firstParagraph) {
                    // Add gap between paragraphs
                    this.addEntry(new SpacerEntry());
                }
                firstParagraph = false;
                List<OrderedText> wrappedLines = textRenderer.wrapLines(Text.literal(paragraph), textWidth);
                for (OrderedText line : wrappedLines) {
                    this.addEntry(new TextEntry(line));
                }
            }
        }

        @Override
        public int getRowWidth() {
            return this.width - 40;
        }

        @Override
        protected int getScrollbarX() {
            return this.getX() + this.width - 10;
        }

        public abstract class Entry extends ElementListWidget.Entry<Entry> {}

        public class TitleEntry extends Entry {
            private final Text text;
            public TitleEntry(String title) {
                this.text = Text.literal(title).formatted(Formatting.GOLD, Formatting.BOLD);
            }

            @Override
            public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
                context.drawCenteredTextWithShadow(textRenderer, text, DetailsListWidget.this.width / 2, y, 0xFFFFFF);
            }
            @Override public List<? extends Element> children() { return Collections.emptyList(); }
            @Override public List<? extends Selectable> selectableChildren() { return Collections.emptyList(); }
        }

        public class TextEntry extends Entry {
            private final OrderedText text;
            public TextEntry(OrderedText text) { this.text = text; }
            public TextEntry(Text text) { this.text = text.asOrderedText(); }

            @Override
            public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
                context.drawText(textRenderer, text, x + 10, y, 0xFFFFFF, false);
            }
            @Override public List<? extends Element> children() { return Collections.emptyList(); }
            @Override public List<? extends Selectable> selectableChildren() { return Collections.emptyList(); }
        }

        public class SpacerEntry extends Entry {
            public SpacerEntry() {}
            @Override
            public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {}
            @Override public List<? extends Element> children() { return Collections.emptyList(); }
            @Override public List<? extends Selectable> selectableChildren() { return Collections.emptyList(); }
        }

        public class LogoEntry extends Entry {
            private static final int MAX_LOGO_HEIGHT = 64;
            private static final int MAX_LOGO_WIDTH = 200;
            private final int entryIndex;

            public LogoEntry(int entryIndex) {
                this.entryIndex = entryIndex;
            }

            @Override
            public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
                // Only render on the first logo entry (we use multiple entries for height)
                if (entryIndex == 0 && script != null && script.hasLogo()) {
                    Identifier logoTexture = UrlTextureLoader.getTexture(script.logo());

                    // Get actual dimensions if available
                    int[] dims = UrlTextureLoaderImpl.getDimensions(script.logo());
                    int renderWidth, renderHeight;
                    int textureWidth, textureHeight;

                    if (dims != null) {
                        // Use actual dimensions with aspect ratio
                        textureWidth = dims[0];
                        textureHeight = dims[1];
                        float aspectRatio = (float) textureWidth / textureHeight;

                        // Scale to fit within max bounds while maintaining aspect ratio
                        if (aspectRatio > 1.0f) {
                            // Wider than tall
                            renderWidth = Math.min(MAX_LOGO_WIDTH, textureWidth);
                            renderHeight = (int) (renderWidth / aspectRatio);
                            if (renderHeight > MAX_LOGO_HEIGHT) {
                                renderHeight = MAX_LOGO_HEIGHT;
                                renderWidth = (int) (renderHeight * aspectRatio);
                            }
                        } else {
                            // Taller than wide or square
                            renderHeight = Math.min(MAX_LOGO_HEIGHT, textureHeight);
                            renderWidth = (int) (renderHeight * aspectRatio);
                            if (renderWidth > MAX_LOGO_WIDTH) {
                                renderWidth = MAX_LOGO_WIDTH;
                                renderHeight = (int) (renderWidth / aspectRatio);
                            }
                        }
                    } else {
                        // Not loaded yet, use square placeholder
                        renderWidth = MAX_LOGO_HEIGHT;
                        renderHeight = MAX_LOGO_HEIGHT;
                        textureWidth = MAX_LOGO_HEIGHT;
                        textureHeight = MAX_LOGO_HEIGHT;
                    }

                    int logoX = (DetailsListWidget.this.width - renderWidth) / 2;
                    // Draw the texture scaled to renderWidth x renderHeight
                    // The last two params are the texture size for UV mapping
                    context.drawTexture(logoTexture, logoX, y, renderWidth, renderHeight, 0, 0, textureWidth, textureHeight, textureWidth, textureHeight);
                }
            }

            @Override public List<? extends Element> children() { return Collections.emptyList(); }
            @Override public List<? extends Selectable> selectableChildren() { return Collections.emptyList(); }
        }
    }
}
