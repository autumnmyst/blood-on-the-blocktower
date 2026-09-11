package com.autumnwind.botb.gui;

import com.autumnwind.botb.event.KeyInputHandler;
import com.autumnwind.botb.gui.widget.DocumentEntry;
import com.autumnwind.botb.util.Role;
import com.autumnwind.botb.util.RoleGuides;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ElementListWidget;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/** One role's guide: centered icon, name, team and ability, then a scrollable how-to-run box. */
public class RoleGuideDetailsScreen extends Screen {

    private static final int ICON_SIZE = 64;
    private static final int TOP_MARGIN = 30;
    private static final int SIDE_MARGIN = 40;

    private final Role role;
    private final Screen parent;
    private final List<Role> roleList;
    private final int index;

    public RoleGuideDetailsScreen(Role role, Screen parent, List<Role> roleList) {
        super(Text.literal(role.getDisplayName()));
        this.role = role;
        this.parent = parent;
        this.roleList = roleList;
        this.index = roleList.indexOf(role);
    }

    @Override
    protected void init() {
        int buttonY = this.height - 28;
        int backButtonWidth = 150;
        int arrowButtonWidth = 20;
        int spacing = 5;
        int backButtonX = this.width / 2 - backButtonWidth / 2;

        this.addDrawableChild(ButtonWidget.builder(Text.translatable("gui.back"), button -> this.client.setScreen(this.parent))
                .dimensions(backButtonX, buttonY, backButtonWidth, 20)
                .build());
        if (index > 0) {
            this.addDrawableChild(ButtonWidget.builder(Text.literal("<"), button -> open(index - 1))
                    .dimensions(backButtonX - arrowButtonWidth - spacing, buttonY, arrowButtonWidth, 20)
                    .build());
        }
        if (index >= 0 && index < roleList.size() - 1) {
            this.addDrawableChild(ButtonWidget.builder(Text.literal(">"), button -> open(index + 1))
                    .dimensions(backButtonX + backButtonWidth + spacing, buttonY, arrowButtonWidth, 20)
                    .build());
        }

        int listY = headerHeight() + 10;
        int listWidth = this.width - SIDE_MARGIN * 2;
        int listHeight = buttonY - listY - 8;
        GuideListWidget listWidget = new GuideListWidget(this.client, listWidth, listHeight, listY);
        listWidget.setX(SIDE_MARGIN);
        this.addDrawableChild(listWidget);
    }

    private void open(int newIndex) {
        this.client.setScreen(new RoleGuideDetailsScreen(roleList.get(newIndex), parent, roleList));
    }

    /** Y just below the ability text; the header is drawn in render with the same measurements. */
    private int headerHeight() {
        int y = TOP_MARGIN + ICON_SIZE + 5;
        y += this.textRenderer.fontHeight + 2;
        y += this.textRenderer.fontHeight + 10;
        y += this.textRenderer.getWrappedLinesHeight(abilityString(), this.width - SIDE_MARGIN * 2);
        return y;
    }

    private String abilityString() {
        return "\"" + role.getDescription() + "\"";
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        int currentY = TOP_MARGIN;
        context.drawTexture(role.getIcon(), (this.width - ICON_SIZE) / 2, currentY, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
        currentY += ICON_SIZE + 5;

        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(role.getDisplayName()).formatted(Formatting.BOLD), this.width / 2, currentY, 0xFFFFFF);
        currentY += this.textRenderer.fontHeight + 2;

        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(role.getType().name()).formatted(Formatting.ITALIC), this.width / 2, currentY, role.getType().getColor());
        currentY += this.textRenderer.fontHeight + 10;

        for (OrderedText line : this.textRenderer.wrapLines(Text.literal(abilityString()), this.width - SIDE_MARGIN * 2)) {
            context.drawText(this.textRenderer, line, (this.width - this.textRenderer.getWidth(line)) / 2, currentY, 0xFFFFFF, true);
            currentY += this.textRenderer.fontHeight;
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_E || KeyInputHandler.openMyRoleDetailsKey.matchesKey(keyCode, scanCode)) {
            this.client.setScreen(this.parent);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_RIGHT && index >= 0 && index < roleList.size() - 1) {
            open(index + 1);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_LEFT && index > 0) {
            open(index - 1);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private class GuideListWidget extends ElementListWidget<DocumentEntry> {
        public GuideListWidget(MinecraftClient client, int width, int height, int y) {
            super(client, width, height, y, client.textRenderer.fontHeight + 1);
            int textWidth = this.getRowWidth() - 10;

            this.addEntry(DocumentEntry.title(textRenderer, Text.translatable("gui.blood-on-the-blocktower.role_guide_details.how_to_run").formatted(Formatting.GOLD)));
            boolean first = true;
            for (String paragraph : RoleGuides.get(role).split("\n")) {
                if (!first) this.addEntry(DocumentEntry.spacer());
                first = false;
                for (OrderedText line : textRenderer.wrapLines(Text.literal(paragraph), textWidth)) {
                    this.addEntry(DocumentEntry.text(textRenderer, line, 0xFFFFFF));
                }
            }
        }

        @Override
        public int getRowWidth() {
            return this.width;
        }

        @Override
        protected int getScrollbarX() {
            return this.getX() + this.width - 6;
        }
    }
}
