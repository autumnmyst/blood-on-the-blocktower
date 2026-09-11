package com.autumnwind.botb.gui;

import com.autumnwind.botb.event.KeyInputHandler;
import com.autumnwind.botb.states.ClientState;
import com.autumnwind.botb.util.AlmanacData;
import com.autumnwind.botb.util.AlmanacParser;
import com.autumnwind.botb.util.Script;
import com.autumnwind.botb.util.UrlTextureLoader;
import com.autumnwind.botb.util.UrlTextureLoaderImpl;
import org.lwjgl.glfw.GLFW;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.client.input.KeyEvent;

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
        super(Component.translatable("gui.blood-on-the-blocktower.custom_script_details.title"));
        this.script = ClientState.currentScript;
        this.parent = parent;

        // Start fetching almanac data
        if (script != null && script.hasAlmanac()) {
            AlmanacParser.fetchAlmanacs(script.almanac(), script.extraAlmanacs())
                .thenAccept(data -> {
                    this.almanacData = data;
                    this.isLoading = false;
                    // Refresh the current page if we're still on screen
                    Minecraft.getInstance().execute(() -> {
                        if (Minecraft.getInstance().gui.screen() == this) {
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

        this.addRenderableWidget(Button.builder(pageTitle(Page.SYNOPSIS), b -> switchPage(Page.SYNOPSIS)).bounds(synopsisX, topY, buttonWidth, 20).build());
        this.addRenderableWidget(Button.builder(pageTitle(Page.OVERVIEW), b -> switchPage(Page.OVERVIEW)).bounds(overviewX, topY, buttonWidth, 20).build());
        this.addRenderableWidget(Button.builder(pageTitle(Page.CHANGELOG), b -> switchPage(Page.CHANGELOG)).bounds(changelogX, topY, buttonWidth, 20).build());

        this.switchPage(this.currentPage, true);

        this.addRenderableWidget(Button.builder(Component.translatable("gui.back"), (button) -> this.minecraft.gui.setScreen(this.parent)).bounds(this.width / 2 - 100, this.height - 28, 200, 20).build());
    }

    private void switchPage(Page newPage, boolean isInitial) {
        if (!isInitial && this.currentPage == newPage) return;
        if (this.listWidget != null) {
            this.savedScrollAmounts.put(this.currentPage, this.listWidget.scrollAmount());
            this.removeWidget(this.listWidget);
        }
        this.currentPage = newPage;

        int buttonTopY = hasAuthor() ? 27 : 25;
        int gapBelowButtons = hasAuthor() ? 8 : 10;
        int listTopY = buttonTopY + 20 + gapBelowButtons;
        int footerHeight = 40;

        this.listWidget = new DetailsListWidget(this.minecraft, this.width, this.height - listTopY - footerHeight, listTopY);
        this.listWidget.buildPage(newPage);
        this.listWidget.setScrollAmount(this.savedScrollAmounts.getOrDefault(newPage, 0.0));
        this.addRenderableWidget(this.listWidget);
    }

    private void switchPage(Page newPage) {
        this.switchPage(newPage, false);
    }

    private static Component pageTitle(Page page) {
        return switch (page) {
            case SYNOPSIS -> Component.translatable("gui.blood-on-the-blocktower.custom_script_details.synopsis");
            case OVERVIEW -> Component.translatable("gui.blood-on-the-blocktower.custom_script_details.overview");
            case CHANGELOG -> Component.translatable("gui.blood-on-the-blocktower.custom_script_details.changelog");
        };
    }

    private static Component noContentMessage(Page page) {
        return switch (page) {
            case SYNOPSIS -> Component.translatable("gui.blood-on-the-blocktower.custom_script_details.no_synopsis");
            case OVERVIEW -> Component.translatable("gui.blood-on-the-blocktower.custom_script_details.no_overview");
            case CHANGELOG -> Component.translatable("gui.blood-on-the-blocktower.custom_script_details.no_changelog");
        };
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractRenderState(context, mouseX, mouseY, delta);

        if (script != null) {
            if (hasAuthor()) {
                context.centeredText(this.font, script.name(), this.width / 2, 4, 0xFFFFFFFF);
                Component authorText = Component.translatable("gui.blood-on-the-blocktower.custom_script_details.by_author", script.author()).withStyle(ChatFormatting.GRAY);
                context.centeredText(this.font, authorText, this.width / 2, 15, 0xFFAAAAAA);
            } else {
                context.centeredText(this.font, script.name(), this.width / 2, 8, 0xFFFFFFFF);
            }
        }

        // Show loading indicator if still fetching
        if (isLoading) {
            context.centeredText(this.font, Component.translatable("gui.blood-on-the-blocktower.custom_script_details.loading_almanac").withStyle(ChatFormatting.YELLOW), this.width / 2, this.height / 2, 0xFFFFFFFF);
        }
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int keyCode = event.key();
        int scanCode = event.scancode();
        int modifiers = event.modifiers();

        if (KeyInputHandler.openScriptKey.matches(event) || keyCode == GLFW.GLFW_KEY_E) {
            this.minecraft.gui.setScreen(this.parent);
            return true;
        }
        return super.keyPressed(event);
    }

    private class DetailsListWidget extends ContainerObjectSelectionList<DetailsListWidget.Entry> {
        public DetailsListWidget(Minecraft client, int width, int height, int y) {
            super(client, width, height, y, client.font.lineHeight + 2);
        }

        public void buildPage(Page page) {
            this.clearEntries();

            if (isLoading) {
                this.addEntry(new TextEntry(Component.translatable("gui.blood-on-the-blocktower.custom_script_details.loading").withStyle(ChatFormatting.ITALIC, ChatFormatting.GRAY)));
                return;
            }

            // Check for errors
            Component lastError = AlmanacParser.getLastError();
            if (lastError != null && (almanacData == null || !almanacData.hasScriptData())) {
                this.addEntry(new TextEntry(Component.translatable("gui.blood-on-the-blocktower.custom_script_details.load_failed").withStyle(ChatFormatting.RED)));
                this.addEntry(new TextEntry(lastError.copy().withStyle(ChatFormatting.GRAY)));
                return;
            }

            if (almanacData == null || almanacData.scriptData() == null) {
                this.addEntry(new TextEntry(Component.translatable("gui.blood-on-the-blocktower.custom_script_details.no_almanac_data").withStyle(ChatFormatting.ITALIC, ChatFormatting.GRAY)));
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
                int entryHeight = font.lineHeight + 2;
                int logoEntries = (64 / entryHeight) + 1;
                for (int i = 0; i < logoEntries; i++) {
                    this.addEntry(new LogoEntry(i));
                }
                this.addEntry(new SpacerEntry());
            }

            // Add a title
            this.addEntry(new TitleEntry(pageTitle(page)));
            this.addEntry(new SpacerEntry());

            if (content == null || content.isEmpty()) {
                this.addEntry(new TextEntry(noContentMessage(page).copy().withStyle(ChatFormatting.ITALIC, ChatFormatting.GRAY)));
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
                List<FormattedCharSequence> wrappedLines = font.split(Component.literal(paragraph), textWidth);
                for (FormattedCharSequence line : wrappedLines) {
                    this.addEntry(new TextEntry(line));
                }
            }
        }

        @Override
        public int getRowWidth() {
            return this.width - 40;
        }

        @Override
        protected int scrollBarX() {
            return this.getX() + this.width - 10;
        }

        public abstract class Entry extends ContainerObjectSelectionList.Entry<com.autumnwind.botb.gui.CustomScriptDetailsScreen.DetailsListWidget.Entry> {}

        public class TitleEntry extends com.autumnwind.botb.gui.CustomScriptDetailsScreen.DetailsListWidget.Entry {
            private final Component text;
            public TitleEntry(Component title) {
                this.text = title.copy().withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
            }

            @Override
            public void extractContent(GuiGraphicsExtractor context, int mouseX, int mouseY, boolean hovered, float tickDelta) {
                int y = getContentY();

                context.centeredText(font, text, DetailsListWidget.this.width / 2, y, 0xFFFFFFFF);
            }
            @Override public List<? extends GuiEventListener> children() { return Collections.emptyList(); }
            @Override public List<? extends NarratableEntry> narratables() { return Collections.emptyList(); }
        }

        public class TextEntry extends com.autumnwind.botb.gui.CustomScriptDetailsScreen.DetailsListWidget.Entry {
            private final FormattedCharSequence text;
            public TextEntry(FormattedCharSequence text) { this.text = text; }
            public TextEntry(Component text) { this.text = text.getVisualOrderText(); }

            @Override
            public void extractContent(GuiGraphicsExtractor context, int mouseX, int mouseY, boolean hovered, float tickDelta) {
                int x = getContentX();
                int y = getContentY();

                context.text(font, text, x + 10, y, 0xFFFFFFFF, false);
            }
            @Override public List<? extends GuiEventListener> children() { return Collections.emptyList(); }
            @Override public List<? extends NarratableEntry> narratables() { return Collections.emptyList(); }
        }

        public class SpacerEntry extends com.autumnwind.botb.gui.CustomScriptDetailsScreen.DetailsListWidget.Entry {
            public SpacerEntry() {}
            @Override
            public void extractContent(GuiGraphicsExtractor context, int mouseX, int mouseY, boolean hovered, float tickDelta) {
}
            @Override public List<? extends GuiEventListener> children() { return Collections.emptyList(); }
            @Override public List<? extends NarratableEntry> narratables() { return Collections.emptyList(); }
        }

        public class LogoEntry extends com.autumnwind.botb.gui.CustomScriptDetailsScreen.DetailsListWidget.Entry {
            private static final int MAX_LOGO_HEIGHT = 64;
            private static final int MAX_LOGO_WIDTH = 200;
            private final int entryIndex;

            public LogoEntry(int entryIndex) {
                this.entryIndex = entryIndex;
            }

            @Override
            public void extractContent(GuiGraphicsExtractor context, int mouseX, int mouseY, boolean hovered, float tickDelta) {
                int x = getContentX();
                int y = getContentY();

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
                    context.blit(RenderPipelines.GUI_TEXTURED, logoTexture, logoX, y, 0, 0, renderWidth, renderHeight, textureWidth, textureHeight, textureWidth, textureHeight);
                }
            }

            @Override public List<? extends GuiEventListener> children() { return Collections.emptyList(); }
            @Override public List<? extends NarratableEntry> narratables() { return Collections.emptyList(); }
        }
    }
}
