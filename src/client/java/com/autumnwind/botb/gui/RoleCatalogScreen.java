package com.autumnwind.botb.gui;

import com.autumnwind.botb.event.KeyInputHandler;
import com.autumnwind.botb.util.Role;
import com.autumnwind.botb.util.RoleType;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import java.util.Locale;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;

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
    private EditBox searchField;
    private RoleCatalogListWidget roleListWidget;
    private Button toggleButton;

    /** Saved scroll amounts for each view */
    private double savedMainScrollAmount = 0.0;
    private double savedExtraScrollAmount = 0.0;

    public RoleCatalogScreen(Component title) {
        super(title);
        this.filteredRoles = new ArrayList<>(MAIN_ROLES);
    }

    @Override
    protected void init() {
        int searchWidth = 200;
        int toggleWidth = 80;
        int gap = 5;

        // Search field - shifted left to make room for toggle button
        this.searchField = new EditBox(this.font, this.width / 2 - searchWidth / 2 - toggleWidth / 2 - gap, 20, searchWidth, 20, Component.empty());
        this.searchField.setResponder(this::filterRoles);
        this.addRenderableWidget(this.searchField);

        // Toggle button next to search field
        this.toggleButton = Button.builder(getToggleButtonText(), this::onTogglePressed)
                .bounds(this.width / 2 + searchWidth / 2 - toggleWidth / 2 + gap, 20, toggleWidth, 20)
                .build();
        this.addRenderableWidget(this.toggleButton);

        int listTopY = 50;
        int footerHeight = 10;
        this.roleListWidget = new RoleCatalogListWidget(this.minecraft, this.width, this.height - listTopY - footerHeight, listTopY);

        // Apply search filter and populate
        filterRoles(this.searchField.getValue());

        // Restore scroll position for current view
        double savedScroll = showingExtraRoles ? savedExtraScrollAmount : savedMainScrollAmount;
        this.roleListWidget.setScrollAmount(savedScroll);
        this.addRenderableWidget(this.roleListWidget);
    }

    private Component getToggleButtonText() {
        return Component.translatable(showingExtraRoles
                ? "gui.blood-on-the-blocktower.role_catalog.main"
                : "gui.blood-on-the-blocktower.role_catalog.extra");
    }

    private void onTogglePressed(Button button) {
        // Save current scroll position
        if (showingExtraRoles) {
            savedExtraScrollAmount = this.roleListWidget.scrollAmount();
        } else {
            savedMainScrollAmount = this.roleListWidget.scrollAmount();
        }

        // Toggle view
        showingExtraRoles = !showingExtraRoles;

        // Update button text
        this.toggleButton.setMessage(getToggleButtonText());

        // Re-filter with current search text
        filterRoles(this.searchField.getValue());

        // Restore scroll position for new view
        double savedScroll = showingExtraRoles ? savedExtraScrollAmount : savedMainScrollAmount;
        this.roleListWidget.setScrollAmount(savedScroll);
    }

    private void filterRoles(String searchText) {
        String lowerCaseText = searchText.toLowerCase(Locale.ROOT);
        List<Role> sourceRoles = showingExtraRoles ? EXTRA_ROLES : MAIN_ROLES;

        this.filteredRoles = sourceRoles.stream()
                .filter(role -> role.getDisplayName().toLowerCase(Locale.ROOT).contains(lowerCaseText) ||
                        role.getType().getDisplayName().toLowerCase(Locale.ROOT).contains(lowerCaseText))
                .collect(Collectors.toList());
        this.roleListWidget.populateRoles(this.filteredRoles);

        // Only reset scroll if search text changed (not on toggle)
        if (!searchText.isEmpty()) {
            this.roleListWidget.setScrollAmount(0);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractRenderState(context, mouseX, mouseY, delta);
        context.centeredText(this.font, this.title, this.width / 2, 8, 0xFFFFFFFF);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int keyCode = event.key();
        int scanCode = event.scancode();
        int modifiers = event.modifiers();

        // Don't close screen if typing in search field
        if ((KeyInputHandler.openCatalogKey.matches(event) || keyCode == GLFW.GLFW_KEY_E)
                && !this.searchField.isFocused()) {
            this.onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    private class RoleCatalogListWidget extends ContainerObjectSelectionList<RoleCatalogListWidget.RoleCatalogEntry> {

        public RoleCatalogListWidget(Minecraft client, int width, int height, int y) {
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
        @Override protected int scrollBarX() { return super.scrollBarX() + 30; }

        public class RoleCatalogEntry extends ContainerObjectSelectionList.Entry<RoleCatalogEntry> {
            private final List<Role> rolesInRow;
            private int entryY;

            public RoleCatalogEntry(List<Role> roles) {
                this.rolesInRow = roles;
            }

            @Override
            public void extractContent(GuiGraphicsExtractor context, int mouseX, int mouseY, boolean hovered, float tickDelta) {
                int x = getContentX();
                int y = getContentY();

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
                    context.outline(borderX, y + 5, borderWidth, 40, borderColor);
                    // MODIFIED: Drawing a smaller 38x38 texture to fit inside.
                    context.blit(RenderPipelines.GUI_TEXTURED, role.getIcon(), borderX + 1, y + 6, 0, 0, 38, 38, 38, 38);

                    int textCenterX = borderX + (borderWidth / 2);
                    String roleNameString = role.getDisplayName();

                    // Use wider margin for single words, narrower for multi-word names
                    int wrapWidth = roleNameString.contains(" ") ? itemWidth - 4 : itemWidth + 1;

                    List<Component> textLines = minecraft.font.getSplitter()
                            .splitLines(roleNameString, wrapWidth, Style.EMPTY)
                            .stream()
                            .map(line -> Component.literal(line.getString()))
                            .collect(Collectors.toList());

                    // MODIFIED: Adjusted Y position to be closer to the smaller icon.
                    int startY = y + 50;

                    for (int j = 0; j < textLines.size(); j++) {
                        Component line = textLines.get(j);
                        int currentLineY = startY + (j * minecraft.font.lineHeight);
                        context.centeredText(minecraft.font, line, textCenterX, currentLineY, 0xFFFFFFFF);
                    }

                    if (isMouseOverRole) {
                        int tooltipMaxWidth = 170;
                        List<FormattedText> wrappedLines = minecraft.font.getSplitter()
                                .splitLines(role.getDescription(), tooltipMaxWidth, Style.EMPTY);
                        List<Component> tooltipTextLines = wrappedLines.stream()
                                .map(line -> Component.literal(line.getString()).withStyle(ChatFormatting.YELLOW))
                                .collect(Collectors.toList());
                        context.setComponentTooltipForNextFrame(minecraft.font, tooltipTextLines, mouseX, mouseY);
                    }
                }
            }

            @Override
            public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
                double mouseX = event.x();
                double mouseY = event.y();
                int button = event.button();

                if (button == GLFW.GLFW_MOUSE_BUTTON_1) {
                    // MODIFIED: Use the same updated itemWidth for click detection.
                    int itemWidth = 75;
                    int rowX = RoleCatalogListWidget.this.getRowLeft();

                    for (int i = 0; i < this.rolesInRow.size(); i++) {
                        int roleX = rowX + i * itemWidth;
                        int roleY = this.entryY;
                        int roleHeight = RoleCatalogListWidget.this.defaultEntryHeight;

                        if (mouseX >= roleX && mouseX < roleX + itemWidth && mouseY >= roleY && mouseY < roleY + roleHeight) {
                            // Save scroll position for current view
                            if (RoleCatalogScreen.this.showingExtraRoles) {
                                RoleCatalogScreen.this.savedExtraScrollAmount = RoleCatalogScreen.this.roleListWidget.scrollAmount();
                            } else {
                                RoleCatalogScreen.this.savedMainScrollAmount = RoleCatalogScreen.this.roleListWidget.scrollAmount();
                            }

                            Role selectedRole = this.rolesInRow.get(i);
                            // Get the full list of roles from the parent screen
                            List<Role> fullCatalogList = RoleCatalogScreen.this.filteredRoles;

                            // Call the new constructor with the full list
                            minecraft.gui.setScreen(new CharacterDetailsScreen(selectedRole, RoleCatalogScreen.this, fullCatalogList));

                            return true;
                        }
                    }
                }
                return false;
            }

            @Override public List<? extends GuiEventListener> children() { return Collections.emptyList(); }
            @Override public List<? extends NarratableEntry> narratables() { return Collections.emptyList(); }
        }
    }
}