package com.autumnwind.botb.gui;

import com.autumnwind.botb.event.KeyInputHandler;
import com.autumnwind.botb.util.Role;
import com.autumnwind.botb.util.RoleGuides;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ElementListWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/** Catalog-style grid of every role the mod special-cases; click one for how to run it. */
public class RoleGuidesScreen extends Screen {

    private final Screen parent;
    private List<Role> filteredRoles;
    private TextFieldWidget searchField;
    private RoleGuideListWidget roleListWidget;
    private double savedScrollAmount = 0.0;

    public RoleGuidesScreen(Screen parent) {
        super(Text.translatable("gui.blood-on-the-blocktower.role_guides.title"));
        this.parent = parent;
        this.filteredRoles = new ArrayList<>(RoleGuides.roles());
    }

    @Override
    protected void init() {
        int searchWidth = 200;
        this.searchField = new TextFieldWidget(this.textRenderer, this.width / 2 - searchWidth / 2, 20, searchWidth, 20, Text.empty());
        this.searchField.setChangedListener(this::filterRoles);
        this.addDrawableChild(this.searchField);

        int listTopY = 50;
        int footerHeight = 40;
        this.roleListWidget = new RoleGuideListWidget(this.client, this.width, this.height - listTopY - footerHeight, listTopY);
        filterRoles(this.searchField.getText());
        this.roleListWidget.setScrollAmount(savedScrollAmount);
        this.addDrawableChild(this.roleListWidget);

        int backButtonWidth = 60;
        this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("gui.blood-on-the-blocktower.role_guides.back").formatted(Formatting.YELLOW),
                button -> this.client.setScreen(this.parent)
        ).dimensions(this.width - backButtonWidth - 10, this.height - 30, backButtonWidth, 20).build());
    }

    private void filterRoles(String searchText) {
        String lowerCaseText = searchText.toLowerCase(Locale.ROOT);
        this.filteredRoles = RoleGuides.roles().stream()
                .filter(role -> role.getDisplayName().toLowerCase(Locale.ROOT).contains(lowerCaseText) ||
                        role.getType().name().toLowerCase(Locale.ROOT).contains(lowerCaseText))
                .collect(Collectors.toList());
        this.roleListWidget.populateRoles(this.filteredRoles);
        if (!searchText.isEmpty()) {
            this.roleListWidget.setScrollAmount(0);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 8, 0xFFFFFF);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        boolean exitKey = keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_E
                || KeyInputHandler.openAssignGui.matchesKey(keyCode, scanCode);
        if (exitKey && !this.searchField.isFocused()) {
            this.client.setScreen(this.parent);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private class RoleGuideListWidget extends ElementListWidget<RoleGuideListWidget.RoleGuideEntry> {

        private static final int COLUMNS = 5;
        private static final int ITEM_WIDTH = 75;

        public RoleGuideListWidget(MinecraftClient client, int width, int height, int y) {
            super(client, width, height, y, 70);
        }

        public void populateRoles(List<Role> roles) {
            this.clearEntries();
            for (int i = 0; i < roles.size(); i += COLUMNS) {
                this.addEntry(new RoleGuideEntry(roles.subList(i, Math.min(i + COLUMNS, roles.size()))));
            }
        }

        @Override public int getRowWidth() { return ITEM_WIDTH * COLUMNS; }
        @Override protected int getScrollbarX() { return super.getScrollbarX() + 30; }

        public class RoleGuideEntry extends ElementListWidget.Entry<RoleGuideEntry> {
            private final List<Role> rolesInRow;
            private int entryY;

            public RoleGuideEntry(List<Role> roles) {
                this.rolesInRow = roles;
            }

            @Override
            public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
                this.entryY = y;
                for (int i = 0; i < this.rolesInRow.size(); i++) {
                    Role role = this.rolesInRow.get(i);
                    int roleX = x + i * ITEM_WIDTH;
                    int borderWidth = 40;
                    int borderX = roleX + (ITEM_WIDTH - borderWidth) / 2;
                    boolean isMouseOverRole = mouseX >= borderX && mouseX < borderX + borderWidth && mouseY >= y + 5 && mouseY < y + 5 + borderWidth;

                    context.drawBorder(borderX, y + 5, borderWidth, 40, role.getType().getColor());
                    context.drawTexture(role.getIcon(), borderX + 1, y + 6, 0, 0, 38, 38, 38, 38);

                    String roleNameString = role.getDisplayName();
                    int wrapWidth = roleNameString.contains(" ") ? ITEM_WIDTH - 4 : ITEM_WIDTH + 1;
                    List<Text> textLines = client.textRenderer.getTextHandler()
                            .wrapLines(roleNameString, wrapWidth, Style.EMPTY)
                            .stream()
                            .map(line -> Text.literal(line.getString()))
                            .collect(Collectors.toList());
                    int textCenterX = borderX + borderWidth / 2;
                    for (int j = 0; j < textLines.size(); j++) {
                        context.drawCenteredTextWithShadow(client.textRenderer, textLines.get(j), textCenterX, y + 50 + j * client.textRenderer.fontHeight, 0xFFFFFF);
                    }

                    if (isMouseOverRole) {
                        List<StringVisitable> wrappedLines = client.textRenderer.getTextHandler()
                                .wrapLines(role.getDescription(), 170, Style.EMPTY);
                        List<Text> tooltipTextLines = wrappedLines.stream()
                                .map(line -> Text.literal(line.getString()).formatted(Formatting.YELLOW))
                                .collect(Collectors.toList());
                        context.drawTooltip(client.textRenderer, tooltipTextLines, mouseX, mouseY);
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
                        client.setScreen(new RoleGuideDetailsScreen(this.rolesInRow.get(i), RoleGuidesScreen.this, RoleGuidesScreen.this.filteredRoles));
                        return true;
                    }
                }
                return false;
            }

            @Override public List<? extends Element> children() { return Collections.emptyList(); }
            @Override public List<? extends Selectable> selectableChildren() { return Collections.emptyList(); }
        }
    }
}
