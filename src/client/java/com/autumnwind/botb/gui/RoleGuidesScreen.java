package com.autumnwind.botb.gui;

import com.autumnwind.botb.event.KeyInputHandler;
import com.autumnwind.botb.util.Role;
import com.autumnwind.botb.util.RoleGuides;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;

/** Catalog-style grid of every role the mod special-cases; click one for how to run it. */
public class RoleGuidesScreen extends Screen {

    private final Screen parent;
    private List<Role> filteredRoles;
    private EditBox searchField;
    private RoleGuideListWidget roleListWidget;
    private double savedScrollAmount = 0.0;

    public RoleGuidesScreen(Screen parent) {
        super(Component.translatable("gui.blood-on-the-blocktower.role_guides.title"));
        this.parent = parent;
        this.filteredRoles = new ArrayList<>(RoleGuides.roles());
    }

    @Override
    protected void init() {
        int searchWidth = 200;
        this.searchField = new EditBox(this.font, this.width / 2 - searchWidth / 2, 20, searchWidth, 20, Component.empty());
        this.searchField.setResponder(this::filterRoles);
        this.addRenderableWidget(this.searchField);

        int listTopY = 50;
        int footerHeight = 40;
        this.roleListWidget = new RoleGuideListWidget(this.minecraft, this.width, this.height - listTopY - footerHeight, listTopY);
        filterRoles(this.searchField.getValue());
        this.roleListWidget.setScrollAmount(savedScrollAmount);
        this.addRenderableWidget(this.roleListWidget);

        int backButtonWidth = 60;
        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.blood-on-the-blocktower.role_guides.back").withStyle(ChatFormatting.YELLOW),
                button -> this.minecraft.setScreen(this.parent)
        ).bounds(this.width - backButtonWidth - 10, this.height - 30, backButtonWidth, 20).build());
    }

    private void filterRoles(String searchText) {
        String lowerCaseText = searchText.toLowerCase(Locale.ROOT);
        this.filteredRoles = RoleGuides.roles().stream()
                .filter(role -> role.getDisplayName().toLowerCase(Locale.ROOT).contains(lowerCaseText) ||
                        role.getType().getDisplayName().toLowerCase(Locale.ROOT).contains(lowerCaseText))
                .collect(Collectors.toList());
        this.roleListWidget.populateRoles(this.filteredRoles);
        if (!searchText.isEmpty()) {
            this.roleListWidget.setScrollAmount(0);
        }
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredString(this.font, this.title, this.width / 2, 8, 0xFFFFFF);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        boolean exitKey = keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_E
                || KeyInputHandler.openAssignGui.matches(keyCode, scanCode);
        if (exitKey && !this.searchField.isFocused()) {
            this.minecraft.setScreen(this.parent);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private class RoleGuideListWidget extends ContainerObjectSelectionList<RoleGuideListWidget.RoleGuideEntry> {

        private static final int COLUMNS = 5;
        private static final int ITEM_WIDTH = 75;

        public RoleGuideListWidget(Minecraft client, int width, int height, int y) {
            super(client, width, height, y, 70);
        }

        public void populateRoles(List<Role> roles) {
            this.clearEntries();
            for (int i = 0; i < roles.size(); i += COLUMNS) {
                this.addEntry(new RoleGuideEntry(roles.subList(i, Math.min(i + COLUMNS, roles.size()))));
            }
        }

        @Override public int getRowWidth() { return ITEM_WIDTH * COLUMNS; }
        @Override protected int getScrollbarPosition() { return super.getScrollbarPosition() + 30; }

        public class RoleGuideEntry extends ContainerObjectSelectionList.Entry<RoleGuideEntry> {
            private final List<Role> rolesInRow;
            private int entryY;

            public RoleGuideEntry(List<Role> roles) {
                this.rolesInRow = roles;
            }

            @Override
            public void render(GuiGraphics context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
                this.entryY = y;
                for (int i = 0; i < this.rolesInRow.size(); i++) {
                    Role role = this.rolesInRow.get(i);
                    int roleX = x + i * ITEM_WIDTH;
                    int borderWidth = 40;
                    int borderX = roleX + (ITEM_WIDTH - borderWidth) / 2;
                    boolean isMouseOverRole = mouseX >= borderX && mouseX < borderX + borderWidth && mouseY >= y + 5 && mouseY < y + 5 + borderWidth;

                    context.renderOutline(borderX, y + 5, borderWidth, 40, role.getType().getColor());
                    context.blit(role.getIcon(), borderX + 1, y + 6, 0, 0, 38, 38, 38, 38);

                    String roleNameString = role.getDisplayName();
                    int wrapWidth = roleNameString.contains(" ") ? ITEM_WIDTH - 4 : ITEM_WIDTH + 1;
                    List<Component> textLines = minecraft.font.getSplitter()
                            .splitLines(roleNameString, wrapWidth, Style.EMPTY)
                            .stream()
                            .map(line -> Component.literal(line.getString()))
                            .collect(Collectors.toList());
                    int textCenterX = borderX + borderWidth / 2;
                    for (int j = 0; j < textLines.size(); j++) {
                        context.drawCenteredString(minecraft.font, textLines.get(j), textCenterX, y + 50 + j * minecraft.font.lineHeight, 0xFFFFFF);
                    }

                    if (isMouseOverRole) {
                        List<FormattedText> wrappedLines = minecraft.font.getSplitter()
                                .splitLines(role.getDescription(), 170, Style.EMPTY);
                        List<Component> tooltipTextLines = wrappedLines.stream()
                                .map(line -> Component.literal(line.getString()).withStyle(ChatFormatting.YELLOW))
                                .collect(Collectors.toList());
                        context.renderComponentTooltip(minecraft.font, tooltipTextLines, mouseX, mouseY);
                    }
                }
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                if (button != GLFW.GLFW_MOUSE_BUTTON_1) return false;
                int rowX = RoleGuideListWidget.this.getRowLeft();
                for (int i = 0; i < this.rolesInRow.size(); i++) {
                    int roleX = rowX + i * ITEM_WIDTH;
                    if (mouseX >= roleX && mouseX < roleX + ITEM_WIDTH && mouseY >= this.entryY && mouseY < this.entryY + RoleGuideListWidget.this.itemHeight) {
                        RoleGuidesScreen.this.savedScrollAmount = RoleGuidesScreen.this.roleListWidget.getScrollAmount();
                        minecraft.setScreen(new RoleGuideDetailsScreen(this.rolesInRow.get(i), RoleGuidesScreen.this, RoleGuidesScreen.this.filteredRoles));
                        return true;
                    }
                }
                return false;
            }

            @Override public List<? extends GuiEventListener> children() { return Collections.emptyList(); }
            @Override public List<? extends NarratableEntry> narratables() { return Collections.emptyList(); }
        }
    }
}
