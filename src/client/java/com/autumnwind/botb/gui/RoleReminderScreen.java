package com.autumnwind.botb.gui;

import com.autumnwind.botb.event.KeyInputHandler;
import com.autumnwind.botb.hud.NightOrderHudManager;
import com.autumnwind.botb.states.ClientState;
import com.autumnwind.botb.states.StorytellerState;
import com.autumnwind.botb.util.*;
import org.lwjgl.glfw.GLFW;

import java.util.*;
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
import net.minecraft.resources.Identifier;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;

/**
 * Screen for selecting role-based associated reminders.
 * Shows all roles on the script (or all selectable roles if no script), including custom roles.
 * Clicking a role adds an associated role reminder for that role.
 */
public class RoleReminderScreen extends Screen {

    private final UUID targetPlayerUUID;
    private final Screen parentScreen;
    private EditBox searchField;
    private RoleGridWidget roleGridWidget;
    private List<ScriptRole> sourceRoles;
    private List<ScriptRole> filteredRoles;
    private double savedScrollAmount = 0.0;

    public RoleReminderScreen(Component title, UUID targetPlayerUUID, Screen parentScreen) {
        super(title);
        this.targetPlayerUUID = targetPlayerUUID;
        this.parentScreen = parentScreen;
    }

    @Override
    protected void init() {
        // Source the role list via Script.allRoles, which sorts by team
        // (Townsfolk → Outsider → Minion → Demon → Traveler) regardless of how the
        // script JSON declared them. Matches RoleSelectionScreen's ordering so the
        // user sees the same arrangement in both places.
        this.sourceRoles = new ArrayList<>();
        if (ClientState.currentScript != null) {
            this.sourceRoles.addAll(ClientState.currentScript.allRoles());
        }
        if (this.sourceRoles.isEmpty()) {
            // Fallback to all selectable roles (already in Role enum / SELECTABLE_ROLES order)
            for (Role role : Role.SELECTABLE_ROLES) {
                this.sourceRoles.add(new ScriptRole.Official(role));
            }
        }

        this.filteredRoles = new ArrayList<>(this.sourceRoles);

        // Layout
        int topBarY = 30;
        int searchWidth = 200;
        int startX = this.width / 2 - searchWidth / 2;

        // Search field
        this.searchField = new EditBox(this.font, startX, topBarY, searchWidth, 20, Component.translatable("gui.blood-on-the-blocktower.role_reminder.search"));
        this.searchField.setResponder(this::filterRoles);
        this.addRenderableWidget(this.searchField);

        // Role grid widget
        int listTopY = topBarY + 20 + 10;
        int footerHeight = 40;
        this.roleGridWidget = new RoleGridWidget(this.minecraft, this.width, this.height - listTopY - footerHeight, listTopY);
        this.roleGridWidget.populateRoles(this.filteredRoles);
        this.roleGridWidget.setScrollAmount(this.savedScrollAmount);
        this.addRenderableWidget(this.roleGridWidget);

        // Done button
        this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), (button) -> this.minecraft.gui.setScreen(this.parentScreen))
                .bounds(this.width / 2 - 100, this.height - 28, 200, 20)
                .build());
    }

    private void filterRoles(String searchText) {
        String lowerCaseText = searchText.toLowerCase(Locale.ROOT);
        this.filteredRoles = this.sourceRoles.stream()
                .filter(role -> role.getDisplayName().toLowerCase(Locale.ROOT).contains(lowerCaseText) ||
                        role.getTeam().getDisplayName().toLowerCase(Locale.ROOT).contains(lowerCaseText))
                .collect(Collectors.toList());
        this.roleGridWidget.populateRoles(this.filteredRoles);
        this.roleGridWidget.setScrollAmount(0);
    }

    private void addScriptRoleReminderAndClose(ScriptRole scriptRole) {
        Reminder reminder;

        if (scriptRole.isCustom()) {
            // Custom role reminder
            CustomRole customRole = scriptRole.asCustomRole();
            // For associated custom role reminders, use the custom role ID format
            reminder = Reminder.forCustomRole(customRole.id(), customRole.getDisplayName());
        } else {
            // Official role reminder
            Role role = scriptRole.asRole();
            String reminderText = Reminders.roleMarker(role);
            reminder = new Reminder(reminderText, Optional.of(role));
        }

        // Add reminder to target player
        StorytellerState.REMINDERS
                .computeIfAbsent(targetPlayerUUID, k -> new ArrayList<>())
                .add(reminder);

        // Handle special reminder logic
        if (minecraft != null && minecraft.player != null && minecraft.player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
            PendingRoleAssignment assignment = StorytellerState.PENDING_ROLES.get(targetPlayerUUID);
            Role assignedRole = (assignment != null) ? assignment.role() : Role.NO_ROLE;

            if (scriptRole.isCustom()) {
                // Custom role associated reminder
                CustomRole customRole = scriptRole.asCustomRole();
                boolean markByDefault = customRole.otherNight() > 0;

                // For Philosopher, Cannibal, and Pixie: set marked status to match the associated role's default
                if (assignedRole == Role.PHILOSOPHER || assignedRole == Role.CANNIBAL || assignedRole == Role.PIXIE) {
                    if (markByDefault) {
                        StorytellerState.markedPlayers.add(targetPlayerUUID);
                    } else {
                        StorytellerState.markedPlayers.remove(targetPlayerUUID);
                    }
                } else {
                    // For other roles: only add mark if needed
                    if (markByDefault && !StorytellerState.markedPlayers.contains(targetPlayerUUID)) {
                        boolean wasMarkableBefore = (assignment != null && hasOtherNightsAbility(assignment.role()));
                        if (!wasMarkableBefore) {
                            StorytellerState.markedPlayers.add(targetPlayerUUID);
                        }
                    }
                }
                // Custom roles are handled via processCustomRoles in NightOrderBuilder
                // which now checks for associated custom role reminders
            } else {
                // Official role associated reminder
                Role role = scriptRole.asRole();
                boolean markByDefault = isMarkedByDefault(role);

                // For Philosopher, Cannibal, and Pixie: set marked status to match the associated role's default
                if (assignedRole == Role.PHILOSOPHER || assignedRole == Role.CANNIBAL || assignedRole == Role.PIXIE) {
                    if (markByDefault) {
                        StorytellerState.markedPlayers.add(targetPlayerUUID);
                    } else {
                        StorytellerState.markedPlayers.remove(targetPlayerUUID);
                    }
                } else {
                    // For other roles: only add mark if needed
                    if (markByDefault && !StorytellerState.markedPlayers.contains(targetPlayerUUID)) {
                        boolean wasMarkableBefore = (assignment != null && hasOtherNightsAbility(assignment.role()));

                        if (!wasMarkableBefore) {
                            StorytellerState.markedPlayers.add(targetPlayerUUID);
                        }
                    }
                }

                // Check if this is an FN-only or FN-triggered or godfather (they have a unique N1 visit)
                boolean isFNOnly = NightOrderHudManager.hasFirstNightsAbility(role) &&
                        (!NightOrderHudManager.hasOtherNightsAbility(role) ||
                                NightOrderHudManager.isTriggeredRole(role) || role == Role.GODFATHER);

                if (isFNOnly) {
                    // Special case: Pixie's own FN visit always triggers immediately (even as associated role)
                    boolean isPixieItself = (role == Role.PIXIE);

                    // Check if player has Pixie (assigned or associated)
                    boolean hasPixie = false;
                    if (assignedRole == Role.PIXIE) {
                        hasPixie = true;
                    } else {
                        // Check for Pixie associated role
                        hasPixie = StorytellerState.REMINDERS.getOrDefault(targetPlayerUUID, Collections.emptyList()).stream()
                                .anyMatch(r -> r.role().isPresent() && r.role().get() == Role.PIXIE &&
                                        Reminders.isRoleMarker(r.text(), Role.PIXIE));
                    }

                    // Determine if we can create the trigger
                    boolean canCreateTrigger = true;
                    if (!isPixieItself && hasPixie) {
                        canCreateTrigger = StorytellerState.REMINDERS.getOrDefault(targetPlayerUUID, Collections.emptyList()).stream()
                                .anyMatch(r -> r.text().equals(Reminders.HAS_ABILITY) && r.role().isPresent() && r.role().get() == Role.PIXIE);
                    }

                    // Skip on night 1 - associated roles already get the normal FN visit
                    if (canCreateTrigger && ClientState.currentNight != 1) {
                        NightOrderHudManager.createFirstNightTriggeredVisitForPlayer(targetPlayerUUID, role);
                    }
                }
            }
        }

        // Always rebuild the HUD if a reminder was added
        NightOrderHudManager.rebuildActiveNightOrder();

        // Sync grimoire with other storytellers
        StorytellerState.syncGrimoire();

        this.minecraft.gui.setScreen(this.parentScreen);
    }

    private boolean hasOtherNightsAbility(Role role) {
        return NightOrder.getOtherNightOrder().stream()
                .anyMatch(info -> info.isRole() && info.getRole() == role);
    }

    private boolean isMarkedByDefault(Role role) {
        return NightOrder.getOtherNightOrder().stream()
                .filter(info -> info.isRole() && info.getRole() == role)
                .findFirst()
                .map(NightOrder.NightOrderInfo::isMarkedByDefault)
                .orElse(false);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractRenderState(context, mouseX, mouseY, delta);
        context.centeredText(this.font, this.title, this.width / 2, 15, 0xFFFFFFFF);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int keyCode = event.key();
        int scanCode = event.scancode();
        int modifiers = event.modifiers();

        // Don't close screen if typing in search field
        if ((KeyInputHandler.openAssignGui.matches(event) || keyCode == GLFW.GLFW_KEY_E)
                && !this.searchField.isFocused()) {
            this.minecraft.gui.setScreen(this.parentScreen);
            return true;
        }
        return super.keyPressed(event);
    }

    // Role grid widget - supports both official and custom roles via ScriptRole
    private class RoleGridWidget extends ContainerObjectSelectionList<RoleGridWidget.RoleGridEntry> {

        public RoleGridWidget(Minecraft client, int width, int height, int y) {
            super(client, width, height, y, 70); // 70px height for icon + text
        }

        public void populateRoles(List<ScriptRole> roles) {
            this.clearEntries();
            final int columns = 5;
            for (int i = 0; i < roles.size(); i += columns) {
                List<ScriptRole> rowRoles = roles.subList(i, Math.min(i + columns, roles.size()));
                if (!rowRoles.isEmpty()) {
                    this.addEntry(new RoleGridEntry(rowRoles));
                }
            }
        }

        @Override
        public int getRowWidth() {
            return 75 * 5;
        }

        @Override
        protected int scrollBarX() {
            return super.scrollBarX() + 30;
        }

        public class RoleGridEntry extends ContainerObjectSelectionList.Entry<RoleGridEntry> {
            private final List<ScriptRole> rolesInRow;
            private int entryY;

            public RoleGridEntry(List<ScriptRole> roles) {
                this.rolesInRow = roles;
            }

            @Override
            public void extractContent(GuiGraphicsExtractor context, int mouseX, int mouseY, boolean hovered, float tickDelta) {
                int x = getContentX();
                int y = getContentY();

                this.entryY = y;
                int itemWidth = 75;

                for (int i = 0; i < this.rolesInRow.size(); i++) {
                    ScriptRole scriptRole = this.rolesInRow.get(i);
                    int roleX = x + i * itemWidth;

                    int borderWidth = 40;
                    int borderX = roleX + (itemWidth - borderWidth) / 2;

                    boolean isMouseOver = mouseX >= borderX && mouseX < borderX + borderWidth &&
                            mouseY >= y + 5 && mouseY < y + 5 + borderWidth;

                    int borderColor = scriptRole.getTeam().getColor();
                    Identifier icon = scriptRole.getIcon();

                    // Draw icon
                    context.outline(borderX, y + 5, borderWidth, 40, borderColor);
                    context.blit(RenderPipelines.GUI_TEXTURED, icon, borderX + 1, y + 6, 0, 0, 38, 38, 38, 38);

                    int textCenterX = borderX + (borderWidth / 2);
                    String roleNameString = scriptRole.getDisplayName();

                    // Use wider margin for single words, narrower for multi-word names
                    int wrapWidth = roleNameString.contains(" ") ? itemWidth - 4 : itemWidth + 1;

                    List<Component> textLines = minecraft.font.getSplitter()
                            .splitLines(roleNameString, wrapWidth, Style.EMPTY)
                            .stream()
                            .map(line -> Component.literal(line.getString()))
                            .collect(Collectors.toList());

                    int startY = y + 50;

                    for (int j = 0; j < textLines.size(); j++) {
                        Component line = textLines.get(j);
                        int currentLineY = startY + (j * minecraft.font.lineHeight);
                        context.centeredText(minecraft.font, line, textCenterX, currentLineY, 0xFFFFFFFF);
                    }

                    // Show role description on hover
                    if (isMouseOver) {
                        int tooltipMaxWidth = 170;
                        List<FormattedText> wrappedLines = minecraft.font.getSplitter()
                                .splitLines(AbilityText.of(scriptRole), tooltipMaxWidth, Style.EMPTY);
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
                    int itemWidth = 75;
                    int rowX = RoleGridWidget.this.getRowLeft();

                    for (int i = 0; i < this.rolesInRow.size(); i++) {
                        int roleX = rowX + i * itemWidth;
                        int roleY = this.entryY;
                        int roleHeight = RoleGridWidget.this.defaultEntryHeight;

                        if (mouseX >= roleX && mouseX < roleX + itemWidth && mouseY >= roleY && mouseY < roleY + roleHeight) {
                            ScriptRole selectedRole = this.rolesInRow.get(i);

                            // Shift+left_click opens role details screen (only for official roles)
                            if (Minecraft.getInstance().hasShiftDown() && !selectedRole.isCustom()) {
                                RoleReminderScreen.this.savedScrollAmount = RoleGridWidget.this.scrollAmount();
                                Minecraft.getInstance().gui.setScreen(new CharacterDetailsScreen(selectedRole.asRole(), RoleReminderScreen.this));
                                return true;
                            }

                            addScriptRoleReminderAndClose(selectedRole);
                            return true;
                        }
                    }
                }
                return false;
            }

            @Override
            public List<? extends GuiEventListener> children() {
                return Collections.emptyList();
            }

            @Override
            public List<? extends NarratableEntry> narratables() {
                return Collections.emptyList();
            }
        }
    }
}
