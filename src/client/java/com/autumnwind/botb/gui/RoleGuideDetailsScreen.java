package com.autumnwind.botb.gui;

import com.autumnwind.botb.event.KeyInputHandler;
import com.autumnwind.botb.gui.widget.DocumentEntry;
import com.autumnwind.botb.util.Role;
import com.autumnwind.botb.util.RoleGuides;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

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
        super(Component.literal(role.getDisplayName()));
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

        this.addRenderableWidget(Button.builder(Component.translatable("gui.back"), button -> this.minecraft.setScreen(this.parent))
                .bounds(backButtonX, buttonY, backButtonWidth, 20)
                .build());
        if (index > 0) {
            this.addRenderableWidget(Button.builder(Component.literal("<"), button -> open(index - 1))
                    .bounds(backButtonX - arrowButtonWidth - spacing, buttonY, arrowButtonWidth, 20)
                    .build());
        }
        if (index >= 0 && index < roleList.size() - 1) {
            this.addRenderableWidget(Button.builder(Component.literal(">"), button -> open(index + 1))
                    .bounds(backButtonX + backButtonWidth + spacing, buttonY, arrowButtonWidth, 20)
                    .build());
        }

        int listY = headerHeight() + 10;
        int listWidth = this.width - SIDE_MARGIN * 2;
        int listHeight = buttonY - listY - 8;
        GuideListWidget listWidget = new GuideListWidget(this.minecraft, listWidth, listHeight, listY);
        listWidget.setX(SIDE_MARGIN);
        this.addRenderableWidget(listWidget);
    }

    private void open(int newIndex) {
        this.minecraft.setScreen(new RoleGuideDetailsScreen(roleList.get(newIndex), parent, roleList));
    }

    /** Y just below the ability text; the header is drawn in render with the same measurements. */
    private int headerHeight() {
        int y = TOP_MARGIN + ICON_SIZE + 5;
        y += this.font.lineHeight + 2;
        y += this.font.lineHeight + 10;
        y += this.font.wordWrapHeight(abilityString(), this.width - SIDE_MARGIN * 2);
        return y;
    }

    private String abilityString() {
        return "\"" + role.getDescription() + "\"";
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        int currentY = TOP_MARGIN;
        context.blit(role.getIcon(), (this.width - ICON_SIZE) / 2, currentY, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
        currentY += ICON_SIZE + 5;

        context.drawCenteredString(this.font, Component.literal(role.getDisplayName()).withStyle(ChatFormatting.BOLD), this.width / 2, currentY, 0xFFFFFF);
        currentY += this.font.lineHeight + 2;

        context.drawCenteredString(this.font, Component.literal(role.getType().getDisplayName()).withStyle(ChatFormatting.ITALIC), this.width / 2, currentY, role.getType().getColor());
        currentY += this.font.lineHeight + 10;

        for (FormattedCharSequence line : this.font.split(Component.literal(abilityString()), this.width - SIDE_MARGIN * 2)) {
            context.drawString(this.font, line, (this.width - this.font.width(line)) / 2, currentY, 0xFFFFFF, true);
            currentY += this.font.lineHeight;
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_E || KeyInputHandler.openMyRoleDetailsKey.matches(keyCode, scanCode)) {
            this.minecraft.setScreen(this.parent);
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

    private class GuideListWidget extends ContainerObjectSelectionList<DocumentEntry> {
        public GuideListWidget(Minecraft client, int width, int height, int y) {
            super(client, width, height, y, client.font.lineHeight + 1);
            int textWidth = this.getRowWidth() - 10;

            this.addEntry(DocumentEntry.title(font, Component.translatable("gui.blood-on-the-blocktower.role_guide_details.how_to_run").withStyle(ChatFormatting.GOLD)));
            boolean first = true;
            for (String paragraph : RoleGuides.get(role).split("\n")) {
                if (!first) this.addEntry(DocumentEntry.spacer());
                first = false;
                for (FormattedCharSequence line : font.split(Component.literal(paragraph), textWidth)) {
                    this.addEntry(DocumentEntry.text(font, line, 0xFFFFFF));
                }
            }
        }

        @Override
        public int getRowWidth() {
            return this.width;
        }

        @Override
        protected int getScrollbarPosition() {
            return this.getX() + this.width - 6;
        }
    }
}
