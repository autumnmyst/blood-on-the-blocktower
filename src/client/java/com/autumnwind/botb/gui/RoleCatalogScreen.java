package com.autumnwind.botb.gui;

import com.autumnwind.botb.event.KeyInputHandler;
import com.autumnwind.botb.util.Role;
import com.autumnwind.botb.util.RoleType;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Locale;

public class RoleCatalogScreen extends Screen {

    /** If true, showing extra roles (travelers, fabled, loric). If false, showing main roles. */
    private boolean showingExtraRoles = false;

    /** Main roles: townsfolk, outsiders, minions, demons */
    private static final List<Role> MAIN_ROLES = Arrays.stream(Role.values())
            .filter(r -> r != Role.NO_ROLE)
            .filter(r -> r.getType() == RoleType.TOWNSFOLK ||
                         r.getType() == RoleType.OUTSIDER ||
                         r.getType() == RoleType.MINION ||
                         r.getType() == RoleType.DEMON)
            .toList();

    /** Extra roles: travelers, fabled, loric */
    private static final List<Role> EXTRA_ROLES = Arrays.stream(Role.values())
            .filter(r -> r != Role.NO_ROLE)
            .filter(r -> r.getType() == RoleType.TRAVELER ||
                         r.getType() == RoleType.FABLED ||
                         r.getType() == RoleType.LORIC)
            .toList();

    private List<Role> filteredRoles;
    private TextFieldWidget searchField;
    private RoleCatalogListWidget roleListWidget;
    private ButtonWidget toggleButton;

    /** Saved scroll amounts for each view */
    private double savedMainScrollAmount = 0.0;
    private double savedExtraScrollAmount = 0.0;

    public RoleCatalogScreen(Text title) {
        super(title);
        this.filteredRoles = new ArrayList<>(MAIN_ROLES);
    }

    @Override
    protected void init() {
        int searchWidth = 200;
        int toggleWidth = 80;
        int gap = 5;

        // Search field - shifted left to make room for toggle button
        this.searchField = new TextFieldWidget(this.textRenderer, this.width / 2 - searchWidth / 2 - toggleWidth / 2 - gap, 20, searchWidth, 20, Text.empty());
        this.searchField.setChangedListener(this::filterRoles);
        this.addDrawableChild(this.searchField);

        // Toggle button next to search field
        this.toggleButton = ButtonWidget.builder(getToggleButtonText(), this::onTogglePressed)
                .dimensions(this.width / 2 + searchWidth / 2 - toggleWidth / 2 + gap, 20, toggleWidth, 20)
                .build();
        this.addDrawableChild(this.toggleButton);

        int listTopY = 50;
        int footerHeight = 10;
        this.roleListWidget = new RoleCatalogListWidget(this.client, this.width, this.height - listTopY - footerHeight, listTopY);

        // Apply search filter and populate
        filterRoles(this.searchField.getText());

        // Restore scroll position for current view
        double savedScroll = showingExtraRoles ? savedExtraScrollAmount : savedMainScrollAmount;
        this.roleListWidget.setScrollAmount(savedScroll);
        this.addDrawableChild(this.roleListWidget);
    }

    private Text getToggleButtonText() {
        return Text.literal(showingExtraRoles ? "Main" : "Extra");
    }

    private void onTogglePressed(ButtonWidget button) {
        // Save current scroll position
        if (showingExtraRoles) {
            savedExtraScrollAmount = this.roleListWidget.getScrollAmount();
        } else {
            savedMainScrollAmount = this.roleListWidget.getScrollAmount();
        }

        // Toggle view
        showingExtraRoles = !showingExtraRoles;

        // Update button text
        this.toggleButton.setMessage(getToggleButtonText());

        // Re-filter with current search text
        filterRoles(this.searchField.getText());

        // Restore scroll position for new view
        double savedScroll = showingExtraRoles ? savedExtraScrollAmount : savedMainScrollAmount;
        this.roleListWidget.setScrollAmount(savedScroll);
    }

    private void filterRoles(String searchText) {
        String lowerCaseText = searchText.toLowerCase(Locale.ROOT);
        List<Role> sourceRoles = showingExtraRoles ? EXTRA_ROLES : MAIN_ROLES;

        this.filteredRoles = sourceRoles.stream()
                .filter(role -> role.getDisplayName().toLowerCase(Locale.ROOT).contains(lowerCaseText) ||
                        role.getType().name().toLowerCase(Locale.ROOT).contains(lowerCaseText))
                .collect(Collectors.toList());
        this.roleListWidget.populateRoles(this.filteredRoles);

        // Only reset scroll if search text changed (not on toggle)
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
        // Don't close screen if typing in search field
        if ((KeyInputHandler.openCatalogKey.matchesKey(keyCode, scanCode) || keyCode == GLFW.GLFW_KEY_E)
                && !this.searchField.isFocused()) {
            this.close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private class RoleCatalogListWidget extends ElementListWidget<RoleCatalogListWidget.RoleCatalogEntry> {

        public RoleCatalogListWidget(MinecraftClient client, int width, int height, int y) {
            // MODIFIED: Decreased height of each row.
            super(client, width, height, y, 70);
        }

        public void populateRoles(List<Role> roles) {
            this.clearEntries();
            final int columns = 5;
            for (int i = 0; i < roles.size(); i += columns) {
                List<Role> rowRoles = roles.subList(i, Math.min(i + columns, roles.size()));
                if (!rowRoles.isEmpty()) {
                    this.addEntry(new RoleCatalogEntry(rowRoles));
                }
            }
        }

        // MODIFIED: Adjusted row width to match new item width.
        @Override public int getRowWidth() { return 75 * 5; }
        @Override protected int getScrollbarX() { return super.getScrollbarX() + 30; }

        public class RoleCatalogEntry extends ElementListWidget.Entry<RoleCatalogEntry> {
            private final List<Role> rolesInRow;
            private int entryY;

            public RoleCatalogEntry(List<Role> roles) {
                this.rolesInRow = roles;
            }

            @Override
            public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
                this.entryY = y;
                // MODIFIED: Widened item width slightly to prevent text cutoff.
                int itemWidth = 75;

                for (int i = 0; i < this.rolesInRow.size(); i++) {
                    Role role = this.rolesInRow.get(i);
                    int roleX = x + i * itemWidth;

                    // MODIFIED: Reduced border size.
                    int borderWidth = 40;
                    // MODIFIED: Centered the icon within the item's horizontal space.
                    int borderX = roleX + (itemWidth - borderWidth) / 2;

                    boolean isMouseOverRole = mouseX >= borderX && mouseX < borderX + borderWidth && mouseY >= y + 5 && mouseY < y + 5 + borderWidth;

                    int borderColor = role.getType().getColor();

                    // MODIFIED: Drawing a smaller 40x40 border.
                    context.drawBorder(borderX, y + 5, borderWidth, 40, borderColor);
                    // MODIFIED: Drawing a smaller 38x38 texture to fit inside.
                    context.drawTexture(role.getIcon(), borderX + 1, y + 6, 0, 0, 38, 38, 38, 38);

                    int textCenterX = borderX + (borderWidth / 2);
                    String roleNameString = role.getDisplayName();

                    // Use wider margin for single words, narrower for multi-word names
                    int wrapWidth = roleNameString.contains(" ") ? itemWidth - 4 : itemWidth + 1;

                    List<Text> textLines = client.textRenderer.getTextHandler()
                            .wrapLines(roleNameString, wrapWidth, Style.EMPTY)
                            .stream()
                            .map(line -> Text.literal(line.getString()))
                            .collect(Collectors.toList());

                    // MODIFIED: Adjusted Y position to be closer to the smaller icon.
                    int startY = y + 50;

                    for (int j = 0; j < textLines.size(); j++) {
                        Text line = textLines.get(j);
                        int currentLineY = startY + (j * client.textRenderer.fontHeight);
                        context.drawCenteredTextWithShadow(client.textRenderer, line, textCenterX, currentLineY, 0xFFFFFF);
                    }

                    if (isMouseOverRole) {
                        int tooltipMaxWidth = 170;
                        List<StringVisitable> wrappedLines = client.textRenderer.getTextHandler()
                                .wrapLines(role.getDescription(), tooltipMaxWidth, Style.EMPTY);
                        List<Text> tooltipTextLines = wrappedLines.stream()
                                .map(line -> Text.literal(line.getString()).formatted(Formatting.YELLOW))
                                .collect(Collectors.toList());
                        context.drawTooltip(client.textRenderer, tooltipTextLines, mouseX, mouseY);
                    }
                }
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                if (button == GLFW.GLFW_MOUSE_BUTTON_1) {
                    // MODIFIED: Use the same updated itemWidth for click detection.
                    int itemWidth = 75;
                    int rowX = RoleCatalogListWidget.this.getRowLeft();

                    for (int i = 0; i < this.rolesInRow.size(); i++) {
                        int roleX = rowX + i * itemWidth;
                        int roleY = this.entryY;
                        int roleHeight = RoleCatalogListWidget.this.itemHeight;

                        if (mouseX >= roleX && mouseX < roleX + itemWidth && mouseY >= roleY && mouseY < roleY + roleHeight) {
                            // Save scroll position for current view
                            if (RoleCatalogScreen.this.showingExtraRoles) {
                                RoleCatalogScreen.this.savedExtraScrollAmount = RoleCatalogScreen.this.roleListWidget.getScrollAmount();
                            } else {
                                RoleCatalogScreen.this.savedMainScrollAmount = RoleCatalogScreen.this.roleListWidget.getScrollAmount();
                            }

                            Role selectedRole = this.rolesInRow.get(i);
                            // Get the full list of roles from the parent screen
                            List<Role> fullCatalogList = RoleCatalogScreen.this.filteredRoles;

                            // Call the new constructor with the full list
                            client.setScreen(new CharacterDetailsScreen(selectedRole, RoleCatalogScreen.this, fullCatalogList));

                            return true;
                        }
                    }
                }
                return false;
            }

            @Override public List<? extends Element> children() { return Collections.emptyList(); }
            @Override public List<? extends Selectable> selectableChildren() { return Collections.emptyList(); }
        }
    }
}