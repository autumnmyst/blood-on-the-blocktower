package com.autumnwind.botb.gui;

import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;

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

    private EditBox input;
    private String draft = "";

    private int panelX;
    private int panelY;
    private int panelHeight;
    private int textWidth;
    private int hoveredRule = -1;

    public BootleggerRulesScreen(Screen parent, List<String> rules) {
        super(Component.translatable("gui.blood-on-the-blocktower.bootlegger_rules.title"));
        this.parent = parent;
        this.rules = rules;
    }

    @Override
    protected void init() {
        textWidth = PANEL_WIDTH - PAD * 2;
        int lineHeight = font.lineHeight + 1;

        int rulesHeight = lineHeight;
        if (!rules.isEmpty()) {
            rulesHeight = 0;
            for (String rule : rules) {
                rulesHeight += wrap(rule).size() * lineHeight + RULE_GAP;
            }
        }

        int headerHeight = font.lineHeight + 6;
        panelHeight = PAD + headerHeight + rulesHeight + 6 + FIELD_HEIGHT + 6 + FIELD_HEIGHT + PAD;
        panelHeight = Math.min(panelHeight, this.height - 20);
        panelX = (this.width - PANEL_WIDTH) / 2;
        panelY = (this.height - panelHeight) / 2;

        int doneY = panelY + panelHeight - PAD - FIELD_HEIGHT;
        int inputY = doneY - 6 - FIELD_HEIGHT;

        input = new EditBox(font, panelX + PAD, inputY,
                textWidth - ADD_WIDTH - 4, FIELD_HEIGHT, Component.empty());
        input.setMaxLength(500);
        input.setHint(Component.translatable("gui.blood-on-the-blocktower.bootlegger_rules.new_rule").withStyle(ChatFormatting.DARK_GRAY));
        input.setValue(draft);
        input.setResponder(text -> draft = text);
        addRenderableWidget(input);

        addRenderableWidget(Button.builder(Component.translatable("gui.blood-on-the-blocktower.bootlegger_rules.add"), button -> addRule())
                .bounds(panelX + PAD + textWidth - ADD_WIDTH, inputY, ADD_WIDTH, FIELD_HEIGHT)
                .build());
        addRenderableWidget(Button.builder(Component.translatable("gui.blood-on-the-blocktower.bootlegger_rules.done"), button -> onClose())
                .bounds(panelX + PAD, doneY, textWidth, FIELD_HEIGHT)
                .build());

        setInitialFocus(input);
    }

    private List<FormattedCharSequence> wrap(String rule) {
        return font.split(Component.literal("- " + rule), textWidth);
    }

    /**
     * The builder is drawn first, then everything of this screen is lifted in z above it.
     */
    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        parent.extractRenderState(context, -1, -1, delta);
        context.pose().pushMatrix();
        context.pose().translate(0, 0);
        super.extractRenderState(context, mouseX, mouseY, delta);
        context.pose().popMatrix();
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, 0x90000000);

        context.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + panelHeight, 0xF0101010);
        context.outline(panelX, panelY, PANEL_WIDTH, panelHeight, 0xFFFFAA00);

        int x = panelX + PAD;
        int y = panelY + PAD;
        context.centeredText(font,
                this.title.copy().withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), this.width / 2, y, 0xFFFFFFFF);
        y += font.lineHeight + 6;

        int lineHeight = font.lineHeight + 1;
        hoveredRule = -1;
        if (rules.isEmpty()) {
            context.text(font,
                    Component.translatable("gui.blood-on-the-blocktower.bootlegger_rules.no_rules").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC),
                    x, y, 0xFFFFFFFF);
            y += lineHeight;
        } else {
            for (int i = 0; i < rules.size(); i++) {
                List<FormattedCharSequence> lines = wrap(rules.get(i));
                int rowTop = y - 1;
                int rowBottom = y + lines.size() * lineHeight + 1;
                boolean hovered = mouseX >= x - 2 && mouseX < x + textWidth + 2
                        && mouseY >= rowTop && mouseY < rowBottom;
                if (hovered) {
                    hoveredRule = i;
                    context.fill(x - 2, rowTop, x + textWidth + 2, rowBottom, 0x40FF5555);
                }
                for (FormattedCharSequence line : lines) {
                    context.text(font, line, x, y, hovered ? 0xFFFF5555 : 0xFF55FFFF);
                    y += lineHeight;
                }
                y += RULE_GAP;
            }
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();

        if (button == GLFW.GLFW_MOUSE_BUTTON_1 && hoveredRule >= 0 && hoveredRule < rules.size()) {
            rules.remove(hoveredRule);
            rebuildWidgets();
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int keyCode = event.key();
        int scanCode = event.scancode();
        int modifiers = event.modifiers();

        if ((keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) && input.isFocused()) {
            addRule();
            return true;
        }
        return super.keyPressed(event);
    }

    private void addRule() {
        String rule = input.getValue().trim();
        if (rule.isEmpty()) {
            return;
        }
        rules.add(rule);
        draft = "";
        rebuildWidgets();
    }

    @Override
    public void resize(int width, int height) {
        parent.resize(width, height);
        super.resize(width, height);
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(parent);
    }
}
