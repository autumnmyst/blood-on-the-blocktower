package com.autumnwind.botb.gui;

import com.autumnwind.botb.BloodOnTheBlocktower;
import com.autumnwind.botb.event.KeyInputHandler;
import com.autumnwind.botb.hud.NightOrderHudManager;
import com.autumnwind.botb.states.ClientState;
import com.autumnwind.botb.states.StorytellerState;
import com.autumnwind.botb.util.*;
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
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.*;
import java.util.stream.Collectors;
import com.autumnwind.botb.hud.nightorderhud.TriggerManager;

public class RoleSelectionScreen extends Screen {
    @Nullable // --- MODIFIED ---
    private final UUID targetPlayerUUID;
    private final Screen parentScreen;
    private List<ScriptRole> filteredRoles;
    private AlignmentOverride alignmentOverride;
    private TextFieldWidget searchField;
    private RoleListWidget roleListWidget;
    private List<ScriptRole> sourceRoles;
    private double savedScrollAmount = 0.0; // For restoring scroll position when returning from CharacterDetailsScreen

    @Nullable
    private final Integer bluffIndex;

    Identifier barrierTexture = Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/icons/barrier.png");

    /**
     * Constructor for assigning a role to a PLAYER.
     */
    public RoleSelectionScreen(Text title, UUID targetPlayerUUID, Screen parentScreen) {
        super(title);
        this.parentScreen = parentScreen;
        this.targetPlayerUUID = targetPlayerUUID;
        this.bluffIndex = null; // Not a bluff

        PendingRoleAssignment existingAssignment = StorytellerState.PENDING_ROLES.get(targetPlayerUUID);
        if (existingAssignment != null) {
            this.alignmentOverride = existingAssignment.override();
        } else {
            this.alignmentOverride = AlignmentOverride.DEFAULT;
        }
    }

    /**
     * Constructor for assigning a role to a BLUFF slot.
     */
    public RoleSelectionScreen(Text title, int bluffIndex, Screen parentScreen) {
        super(title);
        this.parentScreen = parentScreen;
        this.targetPlayerUUID = null; // Not a player
        this.bluffIndex = bluffIndex;
        this.alignmentOverride = AlignmentOverride.DEFAULT; // Bluffs have no alignment
    }


    @Override
    protected void init() {
        // Source list generation - official roles, custom roles and travelers.
        if (ClientState.currentScript != null && !ClientState.currentScript.allRoles().isEmpty()) {
            this.sourceRoles = new ArrayList<>(ClientState.currentScript.allRoles());
        } else {
            // Fallback to selectable roles wrapped as ScriptRole.Official
            this.sourceRoles = Role.SELECTABLE_ROLES.stream()
                    .map(ScriptRole.Official::new)
                    .collect(Collectors.toList());
        }

        // Pope allows duplicate character claims, so assigned roles stay selectable.
        boolean popeActive = ClientState.currentScript != null
                && ClientState.currentScript.hasFabledOrLoric(Role.POPE.getId());

        if (this.bluffIndex != null) {
            // Bluff slot: filter out any roles already in play or used as other bluffs.

            // Collect assigned role IDs (both official and custom). Official IDs strip
            // underscores to match ScriptRole.Official.getId(). Otherwise multi-word roles
            // like FORTUNE_TELLER (id "fortuneteller") wouldn't match the assigned name
            // "fortune_teller" and would still appear in bluffs.
            Set<String> assignedRoleIds = popeActive
                    ? Set.of()
                    : StorytellerState.PENDING_ROLES.values().stream()
                            .map(a -> a.isCustomRole() && a.customRole().isPresent()
                                    ? a.customRole().get().id()
                                    : a.role().getId())
                            .collect(Collectors.toSet());

            // Collect other bluff role IDs (unified ScriptRole list)
            Set<String> otherBluffRoleIds = new HashSet<>();
            for (int i = 0; i < StorytellerState.DEMON_BLUFFS.size(); i++) {
                if (i != this.bluffIndex) {
                    ScriptRole bluffRole = StorytellerState.DEMON_BLUFFS.get(i);
                    if (bluffRole != null) {
                        otherBluffRoleIds.add(bluffRole.getId().toLowerCase(Locale.ROOT));
                    }
                }
            }

            // Filter out assigned roles and other bluffs (both official and custom)
            this.sourceRoles = this.sourceRoles.stream()
                    .filter(sr -> {
                        String roleId = sr.getId().toLowerCase(Locale.ROOT);
                        return !assignedRoleIds.contains(roleId) && !otherBluffRoleIds.contains(roleId);
                    })
                    .collect(Collectors.toList());
        } else if (this.targetPlayerUUID != null && ClientState.currentNight == 0 && ClientState.currentDay == 0 && !popeActive) {
            // Setup-time player assignment: forbid duplicate roles unless the role's own
            // rules permit duplicates (e.g. Village Idiot, Legion). Pope Loric being active
            // disables this check entirely. Custom roles use their id, and official roles strip
            // underscores to match ScriptRole.Official.getId().
            Set<String> assignedRoleIds = StorytellerState.PENDING_ROLES.entrySet().stream()
                    .filter(e -> !e.getKey().equals(this.targetPlayerUUID))
                    .map(e -> {
                        PendingRoleAssignment a = e.getValue();
                        if (a.isCustomRole() && a.customRole().isPresent()) {
                            return a.customRole().get().id();
                        }
                        Role r = a.role();
                        if (r == Role.NO_ROLE || Role.ALLOWS_DUPLICATES_AT_SETUP.contains(r)) {
                            return null;
                        }
                        return r.getId();
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            this.sourceRoles = this.sourceRoles.stream()
                    .filter(sr -> !assignedRoleIds.contains(sr.getId().toLowerCase(Locale.ROOT)))
                    .collect(Collectors.toList());
        }

        // This must come AFTER sourceRoles is finalized
        this.filteredRoles = new ArrayList<>(this.sourceRoles);

        // --- Layout Logic ---
        int topBarY = 30;
        int searchWidth = 150;
        int alignWidth = 60;
        int unassignWidth = 70;
        int spacing = 5;

        // --- MODIFIED --- Layout changes based on mode
        if (this.targetPlayerUUID != null) {
            // Player Mode: Show Search, Align, and Unassign
            int totalWidth = searchWidth + alignWidth + unassignWidth + (2 * spacing);
            int startX = this.width / 2 - totalWidth / 2;

            this.searchField = new TextFieldWidget(this.textRenderer, startX, topBarY, searchWidth, 20, Text.empty());
            this.addDrawableChild(this.searchField);

            // Alignment Button
            this.addDrawableChild(ButtonWidget.builder(
                    Text.literal(alignmentOverride.getDisplayName()).styled(style -> style.withColor(TextColor.fromRgb(getAlignmentColor()))),
                    button -> {
                        this.alignmentOverride = this.alignmentOverride.next();
                        // Save scroll position before reinitializing screen
                        if (this.roleListWidget != null) {
                            this.savedScrollAmount = this.roleListWidget.getScrollAmount();
                        }
                        this.client.setScreen(this);
                    }
            ).dimensions(startX + searchWidth + spacing, topBarY, alignWidth, 20).build());

            // Unassign Button (for players)
            this.addDrawableChild(ButtonWidget.builder(
                    Text.literal("Unassign").formatted(Formatting.RED),
                    button -> unassignAndClose() // Calls player unassign
            ).dimensions(startX + searchWidth + alignWidth + (2 * spacing), topBarY, unassignWidth, 20).build());

        } else {
            // Bluff Mode: Show Search and Unassign
            int totalWidth = searchWidth + unassignWidth + spacing;
            int startX = this.width / 2 - totalWidth / 2;

            this.searchField = new TextFieldWidget(this.textRenderer, startX, topBarY, searchWidth, 20, Text.empty());
            this.addDrawableChild(this.searchField);

            // Unassign Button (for bluffs)
            this.addDrawableChild(ButtonWidget.builder(
                    Text.literal("Unassign").formatted(Formatting.RED),
                    button -> unassignBluffAndClose() // Calls bluff unassign
            ).dimensions(startX + searchWidth + spacing, topBarY, unassignWidth, 20).build());
        }

        this.searchField.setChangedListener(this::filterRoles);


        // --- Role List Widget ---
        int listTopY = topBarY + 20 + 10;
        int footerHeight = 40;
        this.roleListWidget = new RoleListWidget(this.client, this.width, this.height - listTopY - footerHeight, listTopY);
        this.roleListWidget.populateRoles(this.filteredRoles);
        this.roleListWidget.setScrollAmount(this.savedScrollAmount); // Restore scroll position
        this.addDrawableChild(this.roleListWidget);

        // --- Footer ---
        this.addDrawableChild(ButtonWidget.builder(Text.translatable("gui.done"), (button) -> this.client.setScreen(this.parentScreen))
                .dimensions(this.width / 2 - 100, this.height - 28, 200, 20)
                .build());
    }

    private void filterRoles(String searchText) {
        String lowerCaseText = searchText.toLowerCase(Locale.ROOT);
        this.filteredRoles = this.sourceRoles.stream()
                .filter(sr -> sr.getDisplayName().toLowerCase(Locale.ROOT).contains(lowerCaseText) ||
                        sr.getTeam().name().toLowerCase(Locale.ROOT).contains(lowerCaseText))
                .collect(Collectors.toList());
        this.roleListWidget.populateRoles(this.filteredRoles);
        this.roleListWidget.setScrollAmount(0);
    }

    private int getAlignmentColor() {
        return switch (this.alignmentOverride) {
            case FORCE_GOOD -> 0xFF00AAFF;
            case FORCE_BAD -> 0xFFFF5555;
            default -> 0xFFFFFFFF;
        };
    }

    private int getButtonColor(ScriptRole scriptRole) {
        if (this.bluffIndex != null) {
            return scriptRole.getTeam().getColor();
        }
        // Player assignment logic
        RoleType team = scriptRole.getTeam();
        boolean isDefaultGood = team.isDefaultGood();
        boolean isTraveler = team == RoleType.TRAVELER;
        boolean isFinalGood = switch (this.alignmentOverride) {
            case FORCE_GOOD -> true;
            case FORCE_BAD -> false;
            case DEFAULT -> isDefaultGood;
        };

        // Special handling for travelers: always show alignment color when forced
        if (isTraveler) {
            return switch (this.alignmentOverride) {
                case FORCE_GOOD -> 0xFF00AAFF; // Townsfolk blue
                case FORCE_BAD -> 0xFFFF5555; // Minion red
                default -> team.getColor(); // Purple
            };
        }

        if (isFinalGood && !isDefaultGood) return 0xFF00AAFF; // Townsfolk blue
        if (!isFinalGood && isDefaultGood) return 0xFFFF5555; // Minion red

        return team.getColor();
    }

    private void assignRoleAndClose(ScriptRole selectedScriptRole) {

        // Check if we are assigning to a bluff or player
        if (this.bluffIndex != null) {
            // Assign to bluff slot using unified ScriptRole
            StorytellerState.DEMON_BLUFFS.set(this.bluffIndex, selectedScriptRole);

        } else if (this.targetPlayerUUID != null) {
            // Assign to player
            boolean wasAssigned = StorytellerState.PENDING_ROLES.containsKey(targetPlayerUUID);

            // Track old role for potential role switch trigger
            Role oldRole = null;
            if (wasAssigned) {
                PendingRoleAssignment oldAssignment = StorytellerState.PENDING_ROLES.get(targetPlayerUUID);
                if (oldAssignment != null && !oldAssignment.isCustomRole()) {
                    oldRole = oldAssignment.role();
                }
            }

            // Create appropriate PendingRoleAssignment based on role type
            PendingRoleAssignment newAssignment;
            if (selectedScriptRole.isCustom()) {
                CustomRole customRole = ((ScriptRole.Custom) selectedScriptRole).customRole();
                newAssignment = new PendingRoleAssignment(customRole, this.alignmentOverride);
            } else {
                Role role = ((ScriptRole.Official) selectedScriptRole).role();
                newAssignment = new PendingRoleAssignment(role, this.alignmentOverride);
            }
            StorytellerState.PENDING_ROLES.put(targetPlayerUUID, newAssignment);

            // Reconcile evil-traveler MINION_INFO trigger against the new assignment. Remove
            // any stale trigger from a prior evil-traveler assignment, then add a fresh one
            // if the new assignment is an evil traveler. Both calls are idempotent / no-op
            // when not applicable.
            TriggerManager.removeEvilTravelerDemonInfoTrigger(targetPlayerUUID);
            TriggerManager.createEvilTravelerDemonInfoTrigger(targetPlayerUUID);

            // Create role switch trigger if: different role, was previously assigned, and not day 0 night 0
            // Only for official roles
            if (!selectedScriptRole.isCustom()) {
                Role selectedRole = ((ScriptRole.Official) selectedScriptRole).role();
                boolean isStartOfGame = ClientState.currentDay == 0 && ClientState.currentNight == 0;
                if (wasAssigned && oldRole != null && oldRole != selectedRole && !isStartOfGame && this.client.player != null && this.client.player.hasPermissionLevel(2)) {
                    NightOrderHudManager.createRoleSwitchTrigger(targetPlayerUUID, selectedRole);
                }
            }

            if (StorytellerState.PENDING_SEAT_NUMBERS.getOrDefault(targetPlayerUUID, -1) <= 0) {
                StorytellerState.PENDING_SEAT_NUMBERS.put(targetPlayerUUID, StorytellerState.nextSeatNumber);
                StorytellerState.nextSeatNumber++;
            }

            // Auto-add outsider reminders when assigning Hermit (official role only)
            if (!selectedScriptRole.isCustom()) {
                Role selectedRole = ((ScriptRole.Official) selectedScriptRole).role();
                if (selectedRole == Role.HERMIT && ClientState.currentScript != null) {
                    List<Reminder> hermitReminders = StorytellerState.REMINDERS.getOrDefault(targetPlayerUUID, new ArrayList<>());
                    List<Reminder> newReminders = new ArrayList<>(hermitReminders);

                    // Add all outsiders on script as associated role reminders (except Hermit itself)
                    for (Role scriptRole : ClientState.currentScript.roles()) {
                        if (scriptRole.getType() == RoleType.OUTSIDER && scriptRole != Role.HERMIT) {
                            Reminder outsiderReminder = new Reminder(scriptRole.name().replace('_', ' '), Optional.of(scriptRole));
                            if (!newReminders.contains(outsiderReminder)) {
                                newReminders.add(outsiderReminder);
                            }
                        }
                    }

                    StorytellerState.REMINDERS.put(targetPlayerUUID, newReminders);
                }
            }

            if (this.client.player != null && this.client.player.hasPermissionLevel(2)) {
                // Handle marking for night order
                if (selectedScriptRole.isCustom()) {
                    // Custom roles are mark-based on other nights
                    CustomRole customRole = ((ScriptRole.Custom) selectedScriptRole).customRole();
                    if (customRole.wakesOtherNights()) {
                        StorytellerState.markedPlayers.add(targetPlayerUUID);
                    } else {
                        StorytellerState.markedPlayers.remove(targetPlayerUUID);
                    }
                } else {
                    Role selectedRole = ((ScriptRole.Official) selectedScriptRole).role();
                    Optional<NightOrder.NightOrderInfo> infoOpt = NightOrder.getOtherNightOrder().stream()
                            .filter(info -> info.isRole() && info.getRole() == selectedRole)
                            .findFirst();

                    if (infoOpt.isPresent()) {
                        if (infoOpt.get().isMarkedByDefault()) {
                            StorytellerState.markedPlayers.add(targetPlayerUUID);
                        } else {
                            StorytellerState.markedPlayers.remove(targetPlayerUUID);
                        }
                    } else {
                        StorytellerState.markedPlayers.remove(targetPlayerUUID);
                    }
                }
                NightOrderHudManager.rebuildActiveNightOrder();
            }
        }

        // Sync grimoire with other storytellers
        StorytellerState.syncGrimoire();

        this.client.setScreen(this.parentScreen);
    }

    /**
     * Unassigns a bluff slot.
     */
    private void unassignBluffAndClose() {
        if (this.bluffIndex != null) {
            StorytellerState.DEMON_BLUFFS.set(this.bluffIndex, null);
        }
        // Sync grimoire with other storytellers
        StorytellerState.syncGrimoire();
        this.client.setScreen(this.parentScreen);
    }

    /**
     * Unassigns a PLAYER.
     */
    private void unassignAndClose() {
        if (this.targetPlayerUUID == null) return; // Safety check

        boolean isOperator = this.client.player != null && this.client.player.hasPermissionLevel(2);

        StorytellerState.PENDING_ROLES.remove(this.targetPlayerUUID);

        // Drop any stale evil-traveler MINION_INFO trigger for this player.
        TriggerManager.removeEvilTravelerDemonInfoTrigger(targetPlayerUUID);

        if (isOperator) {
            StorytellerState.markedPlayers.remove(this.targetPlayerUUID);
            NightOrderHudManager.rebuildActiveNightOrder();
        }

        int removedSeat = StorytellerState.PENDING_SEAT_NUMBERS.getOrDefault(targetPlayerUUID, -1);
        if (removedSeat > 0) {
            StorytellerState.PENDING_SEAT_NUMBERS.remove(targetPlayerUUID);
            StorytellerState.nextSeatNumber--;

            Map<UUID, Integer> updatedSeats = new HashMap<>();
            for (Map.Entry<UUID, Integer> entry : StorytellerState.PENDING_SEAT_NUMBERS.entrySet()) {
                int currentSeat = entry.getValue();
                if (currentSeat > removedSeat) {
                    updatedSeats.put(entry.getKey(), currentSeat - 1);
                } else {
                    updatedSeats.put(entry.getKey(), currentSeat);
                }
            }
            StorytellerState.PENDING_SEAT_NUMBERS.clear();
            StorytellerState.PENDING_SEAT_NUMBERS.putAll(updatedSeats);
        }

        // Sync grimoire with other storytellers
        StorytellerState.syncGrimoire();

        this.client.setScreen(this.parentScreen);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 15, 0xFFFFFF);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Don't close screen if typing in search field
        if ((KeyInputHandler.openAssignGui.matchesKey(keyCode, scanCode) || keyCode == GLFW.GLFW_KEY_E)
                && !this.searchField.isFocused()) {
            this.client.setScreen(this.parentScreen);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    // --- Inner Classes for the Scrollable List ---
    private class RoleListWidget extends ElementListWidget<RoleListWidget.RoleGridEntry> {

        public RoleListWidget(MinecraftClient client, int width, int height, int y) {
            super(client, width, height, y, 70); // 70px height for icon + text
        }

        public void populateRoles(List<ScriptRole> roles) {
            this.clearEntries();
            final int columns = 5; // Same as catalog
            for (int i = 0; i < roles.size(); i += columns) {
                List<ScriptRole> rowRoles = roles.subList(i, Math.min(i + columns, roles.size()));
                if (!rowRoles.isEmpty()) {
                    this.addEntry(new RoleGridEntry(rowRoles));
                }
            }
        }

        @Override public int getRowWidth() { return 75 * 5; } // 75px width * 5 columns
        @Override protected int getScrollbarX() { return super.getScrollbarX() + 30; }

        public class RoleGridEntry extends ElementListWidget.Entry<RoleGridEntry> {
            private final List<ScriptRole> rolesInRow;
            private int entryY;

            public RoleGridEntry(List<ScriptRole> roles) {
                this.rolesInRow = roles;
            }

            @Override
            public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
                this.entryY = y;
                int itemWidth = 75; // Same as catalog

                for (int i = 0; i < this.rolesInRow.size(); i++) {
                    ScriptRole scriptRole = this.rolesInRow.get(i);
                    int roleX = x + i * itemWidth;

                    int borderWidth = 40;
                    int borderX = roleX + (itemWidth - borderWidth) / 2;

                    boolean isMouseOver = mouseX >= borderX && mouseX < borderX + borderWidth &&
                            mouseY >= y + 5 && mouseY < y + 5 + borderWidth;

                    int borderColor = getButtonColor(scriptRole);

                    // Draw Icon
                    context.drawBorder(borderX, y + 5, borderWidth, 40, borderColor);
                    context.drawTexture(scriptRole.getIcon(), borderX + 1, y + 6, 0, 0, 38, 38, 38, 38);

                    // Draw barrier overlay if role is crossed out (but not in bluff mode)
                    if (RoleSelectionScreen.this.bluffIndex == null &&
                            ClientState.crossedOutRoles.contains(scriptRole.getId())) {
                        context.drawTexture(barrierTexture, borderX + 1, y + 6, 0, 0, 38, 38, 38, 38);
                    }

                    int textCenterX = borderX + (borderWidth / 2);
                    String roleNameString = scriptRole.getDisplayName();

                    // Use wider margin for single words, narrower for multi-word names
                    int wrapWidth = roleNameString.contains(" ") ? itemWidth - 4 : itemWidth + 1;

                    List<Text> textLines = client.textRenderer.getTextHandler()
                            .wrapLines(roleNameString, wrapWidth, Style.EMPTY)
                            .stream()
                            .map(line -> Text.literal(line.getString()))
                            .collect(Collectors.toList());

                    int startY = y + 50; // Y position for text

                    for (int j = 0; j < textLines.size(); j++) {
                        Text line = textLines.get(j);
                        int currentLineY = startY + (j * client.textRenderer.fontHeight);
                        context.drawCenteredTextWithShadow(client.textRenderer, line, textCenterX, currentLineY, 0xFFFFFF);
                    }

                    // Show role description on hover
                    if (isMouseOver) {
                        int tooltipMaxWidth = 170;
                        List<StringVisitable> wrappedLines = client.textRenderer.getTextHandler()
                                .wrapLines(AbilityText.of(scriptRole), tooltipMaxWidth, Style.EMPTY);
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
                    int itemWidth = 75;
                    int rowX = RoleListWidget.this.getRowLeft();

                    for (int i = 0; i < this.rolesInRow.size(); i++) {
                        int roleX = rowX + i * itemWidth;
                        int roleY = this.entryY;
                        int roleHeight = RoleListWidget.this.itemHeight;

                        if (mouseX >= roleX && mouseX < roleX + itemWidth && mouseY >= roleY && mouseY < roleY + roleHeight) {
                            ScriptRole selectedScriptRole = this.rolesInRow.get(i);

                            // Ctrl+left_click toggles crossed out status
                            if (Screen.hasControlDown()) {
                                String roleId = selectedScriptRole.getId();
                                if (ClientState.crossedOutRoles.contains(roleId)) {
                                    ClientState.crossedOutRoles.remove(roleId);
                                } else {
                                    ClientState.crossedOutRoles.add(roleId);
                                }
                                return true;
                            }

                            // Shift+left_click opens role details screen
                            if (Screen.hasShiftDown()) {
                                RoleSelectionScreen.this.savedScrollAmount = RoleListWidget.this.getScrollAmount();
                                MinecraftClient.getInstance().setScreen(new CharacterDetailsScreen(selectedScriptRole, RoleSelectionScreen.this));
                                return true;
                            }

                            assignRoleAndClose(selectedScriptRole);
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