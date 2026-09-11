package com.autumnwind.botb.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * Small modal over the Script Builder for editing the Bootlegger's special rules.
 */
public class BootleggerRulesScreen extends Screen {

    private static final int PANEL_WIDTH = 280;
    private static final int PAD = 10;
    private static final int FIELD_HEIGHT = 20;
    private static final int ADD_WIDTH = 44;
    private static final int RULE_GAP = 4;

    private final Screen parent;
    private final List<String> rules;

    private TextFieldWidget input;
    private String draft = "";

    private int panelX;
    private int panelY;
    private int panelHeight;
    private int textWidth;
    private int hoveredRule = -1;

    public BootleggerRulesScreen(Screen parent, List<String> rules) {
        super(Text.translatable("gui.blood-on-the-blocktower.bootlegger_rules.title"));
        this.parent = parent;
        this.rules = rules;
    }

    @Override
    protected void init() {
        textWidth = PANEL_WIDTH - PAD * 2;
        int lineHeight = textRenderer.fontHeight + 1;

        int rulesHeight = lineHeight;
        if (!rules.isEmpty()) {
            rulesHeight = 0;
            for (String rule : rules) {
                rulesHeight += wrap(rule).size() * lineHeight + RULE_GAP;
            }
        }

        int headerHeight = textRenderer.fontHeight + 6;
        panelHeight = PAD + headerHeight + rulesHeight + 6 + FIELD_HEIGHT + 6 + FIELD_HEIGHT + PAD;
        panelHeight = Math.min(panelHeight, this.height - 20);
        panelX = (this.width - PANEL_WIDTH) / 2;
        panelY = (this.height - panelHeight) / 2;

        int doneY = panelY + panelHeight - PAD - FIELD_HEIGHT;
        int inputY = doneY - 6 - FIELD_HEIGHT;

        input = new TextFieldWidget(textRenderer, panelX + PAD, inputY,
                textWidth - ADD_WIDTH - 4, FIELD_HEIGHT, Text.empty());
        input.setMaxLength(500);
        input.setPlaceholder(Text.translatable("gui.blood-on-the-blocktower.bootlegger_rules.new_rule").formatted(Formatting.DARK_GRAY));
        input.setText(draft);
        input.setChangedListener(text -> draft = text);
        addDrawableChild(input);

        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.blood-on-the-blocktower.bootlegger_rules.add"), button -> addRule())
                .dimensions(panelX + PAD + textWidth - ADD_WIDTH, inputY, ADD_WIDTH, FIELD_HEIGHT)
                .build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.blood-on-the-blocktower.bootlegger_rules.done"), button -> close())
                .dimensions(panelX + PAD, doneY, textWidth, FIELD_HEIGHT)
                .build());

        setInitialFocus(input);
    }

    private List<OrderedText> wrap(String rule) {
        return textRenderer.wrapLines(Text.literal("- " + rule), textWidth);
    }

    /**
     * The builder is drawn first, then everything of this screen is lifted in z above it.
     */
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        parent.render(context, -1, -1, delta);
        context.getMatrices().push();
        context.getMatrices().translate(0, 0, 50);
        super.render(context, mouseX, mouseY, delta);
        context.getMatrices().pop();
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, 0x90000000);

        context.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + panelHeight, 0xF0101010);
        context.drawBorder(panelX, panelY, PANEL_WIDTH, panelHeight, 0xFFFFAA00);

        int x = panelX + PAD;
        int y = panelY + PAD;
        context.drawCenteredTextWithShadow(textRenderer,
                this.title.copy().formatted(Formatting.GOLD, Formatting.BOLD), this.width / 2, y, 0xFFFFFF);
        y += textRenderer.fontHeight + 6;

        int lineHeight = textRenderer.fontHeight + 1;
        hoveredRule = -1;
        if (rules.isEmpty()) {
            context.drawTextWithShadow(textRenderer,
                    Text.translatable("gui.blood-on-the-blocktower.bootlegger_rules.no_rules").formatted(Formatting.DARK_GRAY, Formatting.ITALIC),
                    x, y, 0xFFFFFF);
            y += lineHeight;
        } else {
            for (int i = 0; i < rules.size(); i++) {
                List<OrderedText> lines = wrap(rules.get(i));
                int rowTop = y - 1;
                int rowBottom = y + lines.size() * lineHeight + 1;
                boolean hovered = mouseX >= x - 2 && mouseX < x + textWidth + 2
                        && mouseY >= rowTop && mouseY < rowBottom;
                if (hovered) {
                    hoveredRule = i;
                    context.fill(x - 2, rowTop, x + textWidth + 2, rowBottom, 0x40FF5555);
                }
                for (OrderedText line : lines) {
                    context.drawTextWithShadow(textRenderer, line, x, y, hovered ? 0xFF5555 : 0x55FFFF);
                    y += lineHeight;
                }
                y += RULE_GAP;
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_1 && hoveredRule >= 0 && hoveredRule < rules.size()) {
            rules.remove(hoveredRule);
            clearAndInit();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if ((keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) && input.isFocused()) {
            addRule();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void addRule() {
        String rule = input.getText().trim();
        if (rule.isEmpty()) {
            return;
        }
        rules.add(rule);
        draft = "";
        clearAndInit();
    }

    @Override
    public void resize(MinecraftClient client, int width, int height) {
        parent.resize(client, width, height);
        super.resize(client, width, height);
    }

    @Override
    public void close() {
        this.client.setScreen(parent);
    }
}
