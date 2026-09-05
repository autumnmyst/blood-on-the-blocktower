package com.autumnwind.botb.gui;

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
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

import java.util.*;
import java.util.stream.Collectors;
import com.autumnwind.botb.hud.nightorderhud.RoleHelpers;
import net.minecraft.util.Identifier;

public class ReminderChooseScreen extends Screen {

    private final UUID targetPlayerUUID;
    private final Screen parentScreen;
    private TextFieldWidget customTextField;
    private ReminderGridWidget listWidget;
    private double savedScrollAmount = 0.0; // For restoring scroll position when returning from CharacterDetailsScreen

    public ReminderChooseScreen(Text title, UUID targetPlayerUUID, Screen parentScreen) {
        super(title);
        this.targetPlayerUUID = targetPlayerUUID;
        this.parentScreen = parentScreen;
    }

    @Override
    protected void init() {
        int contentWidth = 370; // Increased to fit Roles button
        int startX = (this.width - contentWidth) / 2;
        int topY = 30;

        // Custom Reminder Input
        this.customTextField = new TextFieldWidget(this.textRenderer, startX, topY, 200, 20, Text.literal("Custom Reminder..."));
        this.addDrawableChild(this.customTextField);

        // "Save Custom" Button
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Save Custom").formatted(Formatting.GREEN),
                this::saveCustomReminder
        ).dimensions(startX + 200 + 10, topY, 100, 20).build());

        // "Roles" Button - opens role reminder selection screen
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Roles").formatted(Formatting.AQUA),
                button -> this.client.setScreen(new RoleReminderScreen(Text.literal("Role Reminders"), this.targetPlayerUUID, this.parentScreen))
        ).dimensions(startX + 200 + 10 + 100 + 10, topY, 50, 20).build());

        // --- MODIFIED --- Use the new ReminderGridWidget
        int listTopY = topY + 20 + 10;
        int footerHeight = 40;
        this.listWidget = new ReminderGridWidget(this.client, this.width, this.height - listTopY - footerHeight, listTopY);
        this.listWidget.populateEntries(getValidReminderDefinitions()); // Populate with valid reminders
        this.listWidget.setScrollAmount(this.savedScrollAmount); // Restore scroll position
        this.addDrawableChild(this.listWidget);

        // "Done" Button
        this.addDrawableChild(ButtonWidget.builder(Text.translatable("gui.done"), (button) -> this.client.setScreen(this.parentScreen))
                .dimensions(this.width / 2 - 100, this.height - 28, 200, 20)
                .build());
    }

    private boolean isLilMonstaHolder(UUID uuid) {
        return StorytellerState.REMINDERS.getOrDefault(uuid, Collections.emptyList())
                .stream().anyMatch(r -> r.text().equals("Is The Demon"));
    }

    private static final Set<String> REDUNDANT_GLOBALS = Set.of(
            "Is The Drunk",
            "Is The Marionette",
            "Is The Philosopher",
            "Is The Alchemist",
            "Good",
            "Evil"
    );

    /**
     * Gets the list of reminders that should be available for selection.
     */
    private List<ReminderCatalog.ReminderDefinition> getValidReminderDefinitions() {
        List<ReminderCatalog.ReminderDefinition> valid = new ArrayList<>();
        Set<Role> scriptRoles = new HashSet<>();
        if (ClientState.currentScript != null) {
            scriptRoles.addAll(ClientState.currentScript.roles());
        }

        // --- 1. Add standard reminders ---
        Set<Role> assignedRoles = new HashSet<>();
        Set<String> assignedCustomRoleIds = new HashSet<>();
        for (PendingRoleAssignment assignment : StorytellerState.PENDING_ROLES.values()) {
            assignedRoles.add(assignment.role());
            if (assignment.isCustomRole() && assignment.customRole().isPresent()) {
                assignedCustomRoleIds.add(assignment.customRole().get().id());
            }
        }
        for (ScriptRole bluff : StorytellerState.DEMON_BLUFFS) {
            if (bluff != null) {
                if (!bluff.isCustom()) {
                    assignedRoles.add(bluff.asRole());
                } else {
                    assignedCustomRoleIds.add(bluff.getId());
                }
            }
        }

        boolean isOperator = client != null && client.player != null && client.player.hasPermissionLevel(2);

        // Get custom role IDs from script (including custom travelers)
        Set<String> scriptCustomRoleIds = new HashSet<>();
        if (ClientState.currentScript != null && ClientState.currentScript.hasCustomRolesOrTravelers()) {
            for (CustomRole cr : ClientState.currentScript.allCustomRoles()) {
                scriptCustomRoleIds.add(cr.id());
            }
        }

        for (ReminderCatalog.ReminderDefinition def : ReminderCatalog.getDefinitions()) {
            if (def.isCustomRole()) {
                // Custom role reminder
                if (def.isGlobal()) {
                    // Global custom role reminders are available if custom role is on script
                    if (scriptCustomRoleIds.contains(def.customRoleId())) valid.add(def);
                } else {
                    // Non-global custom role reminders require role to be assigned
                    if (assignedCustomRoleIds.contains(def.customRoleId())) valid.add(def);
                }
            } else if (def.role() != null && (def.role().getType() == RoleType.FABLED || def.role().getType() == RoleType.LORIC)) {
                // Official fabled/loric reminder - available if the fabled is on the script
                String fabledId = def.role().name().toLowerCase().replace("_", "");
                if (ClientState.currentScript != null && ClientState.currentScript.hasFabledOrLoric(fabledId)) {
                    valid.add(def);
                }
            } else if (def.role() != null && def.role().getType() == RoleType.TRAVELER) {
                // Official traveler reminder - check if traveler is assigned
                String travelerId = def.role().name().toLowerCase().replace("_", "");
                if (assignedRoles.contains(def.role()) || assignedCustomRoleIds.contains(travelerId)) {
                    valid.add(def);
                }
            } else if (def.isGlobal()) {
                if (isOperator && REDUNDANT_GLOBALS.contains(def.text())) {
                    continue;
                }
                // Global reminders with NO_ROLE are always available (e.g., Good, Evil)
                // Other global reminders require the role to be on script
                if (def.role() == Role.NO_ROLE || scriptRoles.contains(def.role())) valid.add(def);
            } else {
                if (assignedRoles.contains(def.role())) valid.add(def);
            }
        }

        // --- 1b. Add reminders for CUSTOM fabled characters ---
        // Only custom fabled/loric (ScriptRole.Fabled) carry reminders in the script. The
        // official ones are already in ReminderCatalog and handled by the loop above.
        if (ClientState.currentScript != null && ClientState.currentScript.hasFabledOrLoric()) {
            for (ScriptRole sr : ClientState.currentScript.allFabledAndLoric()) {
                if (sr instanceof ScriptRole.Fabled fabled) {
                    NonPlayerCharacter npc = fabled.fabledCharacter();
                    for (String reminderText : npc.reminders()) {
                        valid.add(ReminderCatalog.ReminderDefinition.forFabled(npc.id(), reminderText, false));
                    }
                    for (String reminderText : npc.remindersGlobal()) {
                        valid.add(ReminderCatalog.ReminderDefinition.forFabled(npc.id(), reminderText, true));
                    }
                }
            }
        }

        // --- 2. Add reminders for associated roles ---
        // For each player who has an associated role reminder, add that associated role's standard reminders.
        // ST:-prefixed minion reminders (Plague Doctor's gained abilities) also unlock the underlying
        // minion's standard reminders so the storyteller can track that ability's state.
        for (Map.Entry<UUID, List<Reminder>> entry : StorytellerState.REMINDERS.entrySet()) {
            for (Reminder reminder : entry.getValue()) {
                if ((isSpecialReminder(reminder) || reminder.isStorytellerMinionReminder()) && reminder.role().isPresent()) {
                    Role associatedRole = reminder.role().get();
                    // Add all standard (non-global) reminders for this associated role
                    for (ReminderCatalog.ReminderDefinition def : ReminderCatalog.getDefinitions()) {
                        if (def.role() == associatedRole && !def.isGlobal()) {
                            // Check if not already in the list to avoid duplicates
                            boolean alreadyAdded = valid.stream()
                                    .anyMatch(d -> d.role() == def.role() && d.text().equals(def.text()));
                            if (!alreadyAdded) {
                                valid.add(def);
                            }
                        }
                    }
                }
            }
        }

        // --- 3. Add "Special Reminders" based on target player's role (Req 5) ---
        PendingRoleAssignment targetAssignment = StorytellerState.PENDING_ROLES.get(targetPlayerUUID);
        if (targetAssignment == null) {
            valid.sort(Comparator.comparingInt(ReminderChooseScreen::teamOrderFor));
            return valid; // No target, no special reminders
        }

        Role targetRole = targetAssignment.role();

        Set<Role> assignedRolesInPlay = StorytellerState.PENDING_ROLES.values().stream()
                .map(PendingRoleAssignment::role)
                .collect(Collectors.toSet());
        Set<Role> notInPlayRoles = scriptRoles.stream()
                .filter(r -> !assignedRolesInPlay.contains(r))
                .collect(Collectors.toSet());

        // --- 4. Add minions for ANY player with "Storyteller Ability" reminder (Plague Doctor) ---
        boolean hasStorytellerAbility = StorytellerState.REMINDERS.getOrDefault(targetPlayerUUID, Collections.emptyList()).stream()
                .anyMatch(r -> r.text().equals("Storyteller Ability"));

        if (hasStorytellerAbility) {
            for (Role r : scriptRoles) {
                if (r.getType() == RoleType.MINION) {
                    // Check if not already in the list to avoid duplicates
                    boolean alreadyAdded = valid.stream()
                            .anyMatch(d -> d.role() == r && d.text().equals(r.name().replace('_', ' ')));
                    if (!alreadyAdded) {
                        valid.add(new ReminderCatalog.ReminderDefinition(r, r.name().replace('_', ' '), false));
                    }
                }
            }
        }

        switch (targetRole) {
            case DRUNK: // b. Drunk: Not-in-play Townsfolk
                for (Role r : notInPlayRoles) {
                    if (r.getType() == RoleType.TOWNSFOLK) {
                        valid.add(new ReminderCatalog.ReminderDefinition(r, r.name().replace('_', ' '), false));
                    }
                }
                break;
            case MARIONETTE: // c. Marionette: Not-in-play Good
                for (Role r : notInPlayRoles) {
                    if (r.isDefaultGood()) {
                        valid.add(new ReminderCatalog.ReminderDefinition(r, r.name().replace('_', ' '), false));
                    }
                }
                break;
            case ALCHEMIST: // d. Alchemist: All Minions on script
                for (Role r : scriptRoles) {
                    if (r.getType() == RoleType.MINION) {
                        valid.add(new ReminderCatalog.ReminderDefinition(r, r.name().replace('_', ' '), false));
                    }
                }
                break;
            case PHILOSOPHER: // e. Philosopher: All Good characters on script
                for (Role r : scriptRoles) {
                    if (r.isDefaultGood()) {
                        valid.add(new ReminderCatalog.ReminderDefinition(r, r.name().replace('_', ' '), false));
                    }
                }
                break;
            case LUNATIC: // a. Lunatic: All Demons on script
                for (Role r : scriptRoles) {
                    if (r.getType() == RoleType.DEMON) {
                        valid.add(new ReminderCatalog.ReminderDefinition(r, r.name().replace('_', ' '), false));
                    }
                }
                break;
            case CANNIBAL: // Cannibal: All Good characters on script
                for (Role r : scriptRoles) {
                    if (r.isDefaultGood()) {
                        valid.add(new ReminderCatalog.ReminderDefinition(r, r.name().replace('_', ' '), false));
                    }
                }
                break;
            case PIXIE: // Pixie: All Townsfolk on script
                for (Role r : scriptRoles) {
                    if (r.getType() == RoleType.TOWNSFOLK) {
                        valid.add(new ReminderCatalog.ReminderDefinition(r, r.name().replace('_', ' '), false));
                    }
                }
                break;
            case HERMIT: // Hermit: All Outsiders on script (except Hermit), plus conditionals
                // Get current reminders to check for conditional outsiders
                List<Reminder> hermitReminders = StorytellerState.REMINDERS.getOrDefault(targetPlayerUUID, Collections.emptyList());
                boolean hasDrunkAssociated = hermitReminders.stream()
                        .anyMatch(r -> r.role().isPresent() && r.role().get() == Role.DRUNK &&
                                       r.text().equals(Role.DRUNK.name().replace('_', ' ')));
                boolean hasLunaticAssociated = hermitReminders.stream()
                        .anyMatch(r -> r.role().isPresent() && r.role().get() == Role.LUNATIC &&
                                       r.text().equals(Role.LUNATIC.name().replace('_', ' ')));

                for (Role r : scriptRoles) {
                    if (r.getType() == RoleType.OUTSIDER && r != Role.HERMIT) {
                        valid.add(new ReminderCatalog.ReminderDefinition(r, r.name().replace('_', ' '), false));
                    }
                }
                // If Drunk associated, add all Townsfolk
                if (hasDrunkAssociated) {
                    for (Role r : scriptRoles) {
                        if (r.getType() == RoleType.TOWNSFOLK) {
                            valid.add(new ReminderCatalog.ReminderDefinition(r, r.name().replace('_', ' '), false));
                        }
                    }
                }
                // If Lunatic associated, add all Demons
                if (hasLunaticAssociated) {
                    for (Role r : scriptRoles) {
                        if (r.getType() == RoleType.DEMON) {
                            valid.add(new ReminderCatalog.ReminderDefinition(r, r.name().replace('_', ' '), false));
                        }
                    }
                }
                break;
            default:
                // f. Boffined Demon
                if (targetRole.getType() == RoleType.DEMON || isLilMonstaHolder(targetPlayerUUID)) {
                    if (assignedRoles.contains(Role.BOFFIN)) {
                        // Boffin gives Good characters on script
                        for (Role r : scriptRoles) {
                            if (r.isDefaultGood()) {
                                valid.add(new ReminderCatalog.ReminderDefinition(r, r.name().replace('_', ' '), false));
                            }
                        }
                    }
                }
                break;
        }

        // Check if player has "Mad" reminder from Harpy
        List<Reminder> targetReminders = StorytellerState.REMINDERS.getOrDefault(targetPlayerUUID, Collections.emptyList());
        boolean hasHarpyMadReminder = targetReminders.stream()
                .anyMatch(r -> r.text().equals("Mad") && r.role().isPresent() && r.role().get() == Role.HARPY);

        if (hasHarpyMadReminder) {
            addPlayerRemindersForHarpy(valid);
        }

        // Check if player has "Mad" reminder from Cerenovus
        boolean hasMadReminder = targetReminders.stream()
                .anyMatch(r -> r.text().equals("Mad") && r.role().isPresent() && r.role().get() == Role.CERENOVUS);

        if (hasMadReminder) {
            addMadRoleRemindersForCerenovus(valid, scriptRoles);
        }

        // Check if player is assigned Plague Doctor or has Plague Doctor associated role
        PendingRoleAssignment assignment = StorytellerState.PENDING_ROLES.get(targetPlayerUUID);
        boolean isPlagueDoctor = assignment != null && assignment.role() == Role.PLAGUE_DOCTOR;
        boolean hasPlagueDoctorAssociated = targetReminders.stream()
                .anyMatch(r -> r.role().isPresent() && r.role().get() == Role.PLAGUE_DOCTOR &&
                               r.text().equals(Role.PLAGUE_DOCTOR.name().replace('_', ' ')));

        if (isPlagueDoctor || hasPlagueDoctorAssociated) {
            addStorytellerMinionRemindersForPlagueDoctor(valid, scriptRoles);
        }

        // --- 5. Check if ANY player has special associated roles that grant additional reminders ---
        // Check all players' reminders for Pixie/Alchemist/Philosopher/Cannibal associated roles
        boolean anyPlayerHasPixieAssociated = false;
        boolean anyPlayerHasAlchemistAssociated = false;
        boolean anyPlayerHasPhilosopherAssociated = false;
        boolean anyPlayerHasCannibalAssociated = false;

        for (List<Reminder> reminders : StorytellerState.REMINDERS.values()) {
            for (Reminder reminder : reminders) {
                if (isSpecialReminder(reminder) && reminder.role().isPresent()) {
                    Role role = reminder.role().get();
                    if (role == Role.PIXIE) anyPlayerHasPixieAssociated = true;
                    if (role == Role.ALCHEMIST) anyPlayerHasAlchemistAssociated = true;
                    if (role == Role.PHILOSOPHER) anyPlayerHasPhilosopherAssociated = true;
                    if (role == Role.CANNIBAL) anyPlayerHasCannibalAssociated = true;
                }
            }
        }

        // Pixie associated: add all Townsfolk
        if (anyPlayerHasPixieAssociated) {
            for (Role r : scriptRoles) {
                if (r.getType() == RoleType.TOWNSFOLK) {
                    valid.add(new ReminderCatalog.ReminderDefinition(r, r.name().replace('_', ' '), false));
                }
            }
        }

        // Alchemist associated: add all Minions
        if (anyPlayerHasAlchemistAssociated) {
            for (Role r : scriptRoles) {
                if (r.getType() == RoleType.MINION) {
                    valid.add(new ReminderCatalog.ReminderDefinition(r, r.name().replace('_', ' '), false));
                }
            }
        }

        // Philosopher associated: add all Good
        if (anyPlayerHasPhilosopherAssociated) {
            for (Role r : scriptRoles) {
                if (r.isDefaultGood()) {
                    valid.add(new ReminderCatalog.ReminderDefinition(r, r.name().replace('_', ' '), false));
                }
            }
        }

        // Cannibal associated: add all Good
        if (anyPlayerHasCannibalAssociated) {
            for (Role r : scriptRoles) {
                if (r.isDefaultGood()) {
                    valid.add(new ReminderCatalog.ReminderDefinition(r, r.name().replace('_', ' '), false));
                }
            }
        }

        // --- 6. Remove duplicates ---
        List<ReminderCatalog.ReminderDefinition> uniqueValid = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (ReminderCatalog.ReminderDefinition def : valid) {
            // Handle both official and custom role reminders
            String roleKey = def.isCustomRole() ? "custom:" + def.customRoleId() :
                    (def.role() != null ? def.role().name() : "none");
            String key = roleKey + ":" + def.text();
            if (!seen.contains(key)) {
                seen.add(key);
                uniqueValid.add(def);
            }
        }

        // Sort by role team so the listing always reads Townsfolk → Outsider → Minion →
        // Demon → Traveler → other, regardless of how the script JSON declared roles.
        // Stable sort preserves the within-team order from the catalog/script.
        uniqueValid.sort(Comparator.comparingInt(ReminderChooseScreen::teamOrderFor));
        return uniqueValid;
    }

    /**
     * Resolves the team-order key for a reminder definition. Globals with NO_ROLE
     * (e.g. "Good"/"Evil") and anything we can't classify fall to the end.
     *
     * <p>Note on the "fabled:" prefix: it's a namespace marker added by
     * {@link ReminderCatalog.ReminderDefinition#forFabled} to keep fabled-flavored
     * definitions distinct from regular custom-role ones. Regular custom role IDs
     * carry no type prefix. The {@code forFabled} factory is also used for custom
     * <em>loric</em> reminders (see the loop at the top of {@link #getValidReminderDefinitions}),
     * so we look up the actual team via {@code Script.getFabledOrLoric} rather than
     * assuming FABLED.
     */
    private static int teamOrderFor(ReminderCatalog.ReminderDefinition def) {
        if (def.role() != null && def.role() != Role.NO_ROLE) {
            return Script.getTypeOrder(def.role().getType());
        }
        if (def.isCustomRole() && ClientState.currentScript != null) {
            return ClientState.currentScript.getCustomRole(def.customRoleId())
                    .map(cr -> Script.getTypeOrder(cr.team()))
                    .orElse(Integer.MAX_VALUE);
        }
        if (def.customRoleId() != null && def.customRoleId().startsWith("fabled:")
                && ClientState.currentScript != null) {
            String npcId = def.customRoleId().substring("fabled:".length());
            return ClientState.currentScript.getFabledOrLoric(npcId)
                    .map(sr -> Script.getTypeOrder(sr.getTeam()))
                    .orElse(Integer.MAX_VALUE);
        }
        return Integer.MAX_VALUE;
    }

    /**
     * Adds player reminders for Harpy madness (all assigned players including self).
     */
    private void addPlayerRemindersForHarpy(List<ReminderCatalog.ReminderDefinition> valid) {
        for (Map.Entry<UUID, PendingRoleAssignment> entry : StorytellerState.PENDING_ROLES.entrySet()) {
            UUID playerUuid = entry.getKey();
            // Get player name for the reminder text (supports distant players)
            String playerName = "Unknown Player";
            if (client != null) {
                var player = client.world != null ? client.world.getPlayerByUuid(playerUuid) : null;
                if (player != null) {
                    playerName = player.getName().getString();
                } else {
                    PlayerListUtil.PlayerInfo info = PlayerListUtil.getPlayerOrCached(client, playerUuid);
                    if (info != null) playerName = info.name();
                }
            }
            // Player reminders use HARPY role with the player's UUID
            valid.add(new ReminderCatalog.ReminderDefinition(
                Role.HARPY,
                null,
                playerName,
                false,
                Optional.of(playerUuid)
            ));
        }
    }

    /**
     * Adds mad role reminders for Cerenovus madness (all good roles + Goblin on script).
     */
    private void addMadRoleRemindersForCerenovus(List<ReminderCatalog.ReminderDefinition> valid, Set<Role> scriptRoles) {
        for (Role r : scriptRoles) {
            if (r.isDefaultGood() || r == Role.GOBLIN) {
                // Mad role reminders use the format "Mad: RoleName"
                valid.add(new ReminderCatalog.ReminderDefinition(
                    r,
                    "Mad: " + r.getDisplayName(),
                    false
                ));
            }
        }
    }

    /**
     * Adds storyteller-minion reminders for Plague Doctor (all minions on script).
     */
    private void addStorytellerMinionRemindersForPlagueDoctor(List<ReminderCatalog.ReminderDefinition> valid, Set<Role> scriptRoles) {
        for (Role r : scriptRoles) {
            if (r.getType() == RoleType.MINION) {
                // Storyteller-minion reminders use the format "ST: MinionName"
                valid.add(new ReminderCatalog.ReminderDefinition(
                    r,
                    "ST: " + r.getDisplayName(),
                    false
                ));
            }
        }
    }

    private void saveCustomReminder(ButtonWidget button) {
        String text = this.customTextField.getText();
        if (text.isEmpty() || text.equals("Custom Reminder...")) {
            return;
        }

        // Check if custom text matches a role name (case-insensitive)
        Optional<Role> matchedRole = Optional.empty();
        if (text.equals(text.toUpperCase()) && text.length() > 0) {
            for (Role role : Role.values()) {
                if (text.trim().equalsIgnoreCase(role.name().replace('_', ' '))) {
                    RoleType type = role.getType();
                    if (type == RoleType.TOWNSFOLK || type == RoleType.OUTSIDER ||
                            type == RoleType.MINION || type == RoleType.DEMON) {
                        matchedRole = Optional.of(role);
                        break;
                    }
                }
            }
        }

        // "Night N" on a Xaan is the Xaan's night marker
        if (matchedRole.isEmpty() && text.matches("Night \\d+")
                && RoleHelpers.isRealXaanHolder(this.targetPlayerUUID)) {
            matchedRole = Optional.of(Role.XAAN);
        }

        // Create a custom reminder (with role if matched)
        Reminder reminder = new Reminder(text, matchedRole);
        addReminderAndClose(reminder);
    }

    private void addReminderAndClose(Reminder reminder) {
        UUID targetUUID = this.targetPlayerUUID; // Capture for lambda

        StorytellerState.REMINDERS
                .computeIfAbsent(targetUUID, k -> new ArrayList<>())
                .add(reminder);

        if (client != null && client.player != null && client.player.hasPermissionLevel(2) &&
                isSpecialReminder(reminder) && reminder.role().isPresent()) {

            Role associatedRole = reminder.role().get();
            PendingRoleAssignment assignment = StorytellerState.PENDING_ROLES.get(targetUUID);
            Role assignedRole = (assignment != null) ? assignment.role() : Role.NO_ROLE;

            boolean markByDefault = isMarkedByDefault(associatedRole);

            // For Philosopher, Cannibal, and Pixie: set marked status to match the associated role's default
            if (assignedRole == Role.PHILOSOPHER || assignedRole == Role.CANNIBAL || assignedRole == Role.PIXIE) {
                if (markByDefault) {
                    StorytellerState.markedPlayers.add(targetUUID);
                } else {
                    StorytellerState.markedPlayers.remove(targetUUID);
                }
            } else {
                // For other roles: only add mark if needed
                if (markByDefault && !StorytellerState.markedPlayers.contains(targetUUID)) {
                    boolean wasMarkableBefore = (assignment != null && hasOtherNightsAbility(assignment.role()));

                    if (!wasMarkableBefore) {
                        StorytellerState.markedPlayers.add(targetUUID);
                    }
                }
            }

            // Check if this is a first-night-only role (FN-only or FN-triggered)
            boolean isFNOnly = NightOrderHudManager.hasFirstNightsAbility(associatedRole) &&
                (!NightOrderHudManager.hasOtherNightsAbility(associatedRole) ||
                 NightOrderHudManager.isTriggeredRole(associatedRole));

            if (isFNOnly) {
                // Special case: Pixie's own FN visit always triggers immediately (even as associated role)
                boolean isPixieItself = (associatedRole == Role.PIXIE);

                // Check if player has Pixie (assigned or associated)
                boolean hasPixie = false;
                if (assignedRole == Role.PIXIE) {
                    hasPixie = true;
                } else {
                    // Check for Pixie associated role
                    hasPixie = StorytellerState.REMINDERS.getOrDefault(targetUUID, Collections.emptyList()).stream()
                        .anyMatch(r -> r.role().isPresent() && r.role().get() == Role.PIXIE &&
                                       r.text().equals(Role.PIXIE.name().replace('_', ' ')));
                }

                // Determine if we can create the trigger:
                // - Always create if it's Pixie itself
                // - Otherwise, only create if player doesn't have Pixie, OR has Pixie with "Has Ability"
                boolean canCreateTrigger = true;
                if (!isPixieItself && hasPixie) {
                    canCreateTrigger = StorytellerState.REMINDERS.getOrDefault(targetUUID, Collections.emptyList()).stream()
                        .anyMatch(r -> r.text().equals("Has Ability") && r.role().isPresent() && r.role().get() == Role.PIXIE);
                }

                // Skip only setup or true Night 1 (natural FN processing handles those).
                // Day 1+ must create the trigger so it survives Dusk into the next night, because
                // currentNight stays at 1 through Day 1 and only increments at the next Dusk.
                // Also skip if we're currently visiting this player (no redundant trigger needed).
                boolean isSetupOrNightOne = ClientState.currentDay == 0 && ClientState.currentNight <= 1;
                if (canCreateTrigger && !isSetupOrNightOne && !isPlayerInCurrentVisit(targetUUID)) {
                    // Create the FN-only triggered visit (no auto-teleport for anyone)
                    NightOrderHudManager.createFirstNightTriggeredVisitForPlayer(targetUUID, associatedRole);
                }
            }

            // If player is marked, create mark triggers for this new associated role
            if (StorytellerState.markedPlayers.contains(targetUUID)) {
                NightOrderHudManager.createMarkTriggersForAssociatedRole(targetUUID, assignedRole, associatedRole);
            }
        }

        // Special case: If adding "Has Ability" reminder to a Pixie, create FN-only triggers for existing FN-only associated roles
        if (reminder.text().equals("Has Ability") && reminder.role().isPresent() && reminder.role().get() == Role.PIXIE) {
            PendingRoleAssignment assignment = StorytellerState.PENDING_ROLES.get(targetUUID);
            if (assignment != null) {
                Role assignedRole = assignment.role();

                // Check if player has Pixie (assigned or associated)
                boolean hasPixie = assignedRole == Role.PIXIE;
                if (!hasPixie) {
                    hasPixie = StorytellerState.REMINDERS.getOrDefault(targetUUID, Collections.emptyList()).stream()
                        .anyMatch(r -> r.role().isPresent() && r.role().get() == Role.PIXIE &&
                                       r.text().equals(Role.PIXIE.name().replace('_', ' ')));
                }

                if (hasPixie) {
                    // Find all FN-only associated roles and create triggers for them
                    for (Reminder existingReminder : StorytellerState.REMINDERS.getOrDefault(targetUUID, Collections.emptyList())) {
                        if (existingReminder.role().isEmpty()) continue;
                        if (existingReminder == reminder) continue; // Skip the Has Ability reminder itself

                        Role associatedRole = existingReminder.role().get();

                        // Check if it's a special reminder (associated role)
                        if (isSpecialReminder(existingReminder)) {
                            // Skip Pixie itself - it already triggered when it was added
                            if (associatedRole == Role.PIXIE) continue;

                            boolean isFNOnly = NightOrderHudManager.hasFirstNightsAbility(associatedRole) &&
                                (!NightOrderHudManager.hasOtherNightsAbility(associatedRole) ||
                                 NightOrderHudManager.isTriggeredRole(associatedRole));

                            // Skip only setup or true Night 1 (natural FN processing handles those).
                            // currentNight stays at 1 through Day 1, so a plain `>1` check would
                            // miss the common case of Pixie gaining Has Ability via a Day 1 execution.
                            boolean isSetupOrNightOne = ClientState.currentDay == 0 && ClientState.currentNight <= 1;
                            if (isFNOnly && !isSetupOrNightOne && !isPlayerInCurrentVisit(targetUUID)) {
                                NightOrderHudManager.createFirstNightTriggeredVisitForPlayer(targetUUID, associatedRole);
                            }
                        }
                    }
                }
            }
        }
        // Always rebuild the HUD if a reminder was added
        NightOrderHudManager.rebuildActiveNightOrder();

        // Sync grimoire with other storytellers
        StorytellerState.syncGrimoire();

        this.client.setScreen(this.parentScreen);
    }

    /**
     * Checks if a role has an ability that triggers on "First Night".
     */
    private boolean hasFirstNightsAbility(Role role) {
        return NightOrder.getFirstNightOrder().stream()
                .anyMatch(info -> info.isRole() && info.getRole() == role);
    }

    /**
     * Checks if a role has an ability that triggers on "Other Nights".
     */
    private boolean hasOtherNightsAbility(Role role) {
        return NightOrder.getOtherNightOrder().stream()
                .anyMatch(info -> info.isRole() && info.getRole() == role);
    }

    /**
     * Gets the "markedByDefault" status for a given role.
     */
    private boolean isMarkedByDefault(Role role) {
        return NightOrder.getOtherNightOrder().stream()
                .filter(info -> info.isRole() && info.getRole() == role)
                .findFirst()
                .map(NightOrder.NightOrderInfo::isMarkedByDefault)
                .orElse(false); // Default to false if not found
    }

    /**
     * Checks if a reminder is a "Special Reminder" that grants an ability.
     * (Duplicate from AssignRolesScreen for use here)
     */
    private boolean isSpecialReminder(Reminder reminder) {
        if (reminder.role().isEmpty()) return false; // Must have a role

        Role role = reminder.role().get();
        String text = reminder.text();

        // Standard associated role: text=[RoleName], role=[Role]
        if (text.equals(role.name().replace('_', ' '))) {
            RoleType type = role.getType();
            // Check if it's an ability-granting type
            return type == RoleType.TOWNSFOLK || type == RoleType.OUTSIDER ||
                    type == RoleType.MINION || type == RoleType.DEMON;
        }

        return false;
    }

    /**
     * Checks if a player is part of the current visit.
     * Used to skip creating redundant triggers when we're already visiting that player.
     */
    private boolean isPlayerInCurrentVisit(UUID playerUUID) {
        if (StorytellerState.currentNightVisitIndex < 0 ||
            StorytellerState.currentNightVisitIndex >= StorytellerState.activeNightOrder.size()) {
            return false;
        }
        RoleVisit currentVisit = StorytellerState.activeNightOrder.get(StorytellerState.currentNightVisitIndex);
        return currentVisit.players().contains(playerUUID);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 15, 0xFFFFFF);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Don't close screen if typing in text field
        if ((KeyInputHandler.openAssignGui.matchesKey(keyCode, scanCode) || keyCode == GLFW.GLFW_KEY_E)
                && !this.customTextField.isFocused()) {
            this.client.setScreen(this.parentScreen);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }


    // This class is modeled after RoleCatalogListWidget from RoleCatalogScreen.java
    private class ReminderGridWidget extends ElementListWidget<ReminderGridWidget.ReminderGridEntry> {

        public ReminderGridWidget(MinecraftClient client, int width, int height, int y) {
            super(client, width, height, y, 70); // 70px height for icon + text
        }

        public void populateEntries(List<ReminderCatalog.ReminderDefinition> definitions) {
            this.clearEntries();
            final int columns = 5; // Same as catalog
            for (int i = 0; i < definitions.size(); i += columns) {
                List<ReminderCatalog.ReminderDefinition> rowDefs = definitions.subList(i, Math.min(i + columns, definitions.size()));
                if (!rowDefs.isEmpty()) {
                    this.addEntry(new ReminderGridEntry(rowDefs));
                }
            }
        }

        @Override public int getRowWidth() { return 75 * 5; } // 75px width * 5 columns
        @Override protected int getScrollbarX() { return super.getScrollbarX() + 30; }

        public class ReminderGridEntry extends ElementListWidget.Entry<ReminderGridEntry> {
            private final List<ReminderCatalog.ReminderDefinition> defsInRow;
            private int entryY;

            public ReminderGridEntry(List<ReminderCatalog.ReminderDefinition> defs) {
                this.defsInRow = defs;
            }

            @Override
            public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
                this.entryY = y;
                int itemWidth = 75; // Same as catalog

                for (int i = 0; i < this.defsInRow.size(); i++) {
                    ReminderCatalog.ReminderDefinition def = this.defsInRow.get(i);
                    Role role = def.role();
                    int roleX = x + i * itemWidth;

                    int borderWidth = 40;
                    int borderX = roleX + (itemWidth - borderWidth) / 2;

                    boolean isMouseOver = mouseX >= borderX && mouseX < borderX + borderWidth &&
                            mouseY >= y + 5 && mouseY < y + 5 + borderWidth;

                    // Check if this is a player reminder (Harpy)
                    boolean isPlayerReminder = def.playerUuid().isPresent();
                    boolean isCustomRole = def.isCustomRole();
                    boolean isFabled = def.isFabled();

                    // Determine border color and icon
                    int borderColor;
                    Identifier iconTexture = null;

                    if (isFabled) {
                        // Fabled reminder - get color and icon from fabled character
                        ScriptRole fabledRole = ClientState.currentScript != null ?
                                ClientState.currentScript.getFabledOrLoric(def.fabledId()).orElse(null) : null;
                        if (fabledRole != null) {
                            borderColor = fabledRole.getTeam().getColor();
                            iconTexture = fabledRole.getIcon();
                        } else {
                            borderColor = RoleType.FABLED.getColor(); // Default fabled gold
                            iconTexture = Reminder.CUSTOM_ICON;
                        }
                    } else if (isCustomRole) {
                        // Custom role reminder - get color and icon from custom role
                        CustomRole customRole = ClientState.currentScript != null ?
                                ClientState.currentScript.getCustomRole(def.customRoleId()).orElse(null) : null;
                        if (customRole != null) {
                            borderColor = customRole.team().getColor();
                            iconTexture = UrlTextureLoader.getTexture(customRole);
                        } else {
                            borderColor = RoleType.NONE.getColor();
                            iconTexture = Reminder.CUSTOM_ICON;
                        }
                    } else if (isPlayerReminder) {
                        borderColor = RoleType.MINION.getColor();
                    } else {
                        borderColor = role.getType().getColor();
                    }

                    // Draw Icon
                    context.drawBorder(borderX, y + 5, borderWidth, 40, borderColor);

                    if (isPlayerReminder) {
                        // Render player head (handles disconnect fallback internally)
                        PlayerListUtil.drawPlayerHead(context, client, def.playerUuid().get(), borderX + 1, y + 6, 38);
                    } else if (isFabled && iconTexture != null) {
                        // Render fabled character icon
                        context.drawTexture(iconTexture, borderX + 1, y + 6, 0, 0, 38, 38, 38, 38);
                    } else if (isCustomRole && iconTexture != null) {
                        // Render custom role icon (includes custom role Good/Evil reminders)
                        context.drawTexture(iconTexture, borderX + 1, y + 6, 0, 0, 38, 38, 38, 38);
                    } else if (def.text().equals("Good")) {
                        // Render Good alignment icon (only for global Good reminder with no associated role)
                        context.drawTexture(Reminder.GOOD_ICON, borderX + 1, y + 6, 0, 0, 38, 38, 38, 38);
                    } else if (def.text().equals("Evil")) {
                        // Render Evil alignment icon (only for global Evil reminder with no associated role)
                        context.drawTexture(Reminder.EVIL_ICON, borderX + 1, y + 6, 0, 0, 38, 38, 38, 38);
                    } else {
                        // Render official role icon
                        context.drawTexture(role.getIcon(), borderX + 1, y + 6, 0, 0, 38, 38, 38, 38);
                    }

                    int textCenterX = borderX + (borderWidth / 2);

                    // --- MODIFIED --- Use reminder text instead of role name
                    String reminderTextString = def.text();

                    // Use wider margin for single words, narrower for multi-word names
                    int wrapWidth = reminderTextString.contains(" ") ? itemWidth - 4 : itemWidth + 1;

                    List<Text> textLines = client.textRenderer.getTextHandler()
                            .wrapLines(reminderTextString, wrapWidth, Style.EMPTY)
                            .stream()
                            .map(line -> Text.literal(line.getString()))
                            .collect(Collectors.toList());

                    int startY = y + 50; // Y position for text

                    for (int j = 0; j < textLines.size(); j++) {
                        Text line = textLines.get(j);
                        int currentLineY = startY + (j * client.textRenderer.fontHeight);
                        context.drawCenteredTextWithShadow(client.textRenderer, line, textCenterX, currentLineY, 0xFFFFFF);
                    }

                    // Show role description on hover (still useful)
                    if (isMouseOver) {
                        int tooltipMaxWidth = 170;
                        String description;
                        if (isFabled) {
                            ScriptRole fabledRole = ClientState.currentScript != null ?
                                    ClientState.currentScript.getFabledOrLoric(def.fabledId()).orElse(null) : null;
                            description = fabledRole != null ? fabledRole.getAbility() : "Fabled character";
                        } else if (isCustomRole) {
                            CustomRole customRole = ClientState.currentScript != null ?
                                    ClientState.currentScript.getCustomRole(def.customRoleId()).orElse(null) : null;
                            description = customRole != null ? customRole.ability() : "Custom role";
                        } else if (role == Role.NO_ROLE) {
                            // Good/Evil alignment markers sit on the placeholder role
                            description = "Marks this player as " + def.text().toLowerCase() + ".";
                        } else {
                            description = role.getDescription();
                        }
                        List<StringVisitable> wrappedLines = client.textRenderer.getTextHandler()
                                .wrapLines(description, tooltipMaxWidth, Style.EMPTY);
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
                    int rowX = ReminderGridWidget.this.getRowLeft();

                    for (int i = 0; i < this.defsInRow.size(); i++) {
                        int roleX = rowX + i * itemWidth;
                        int roleY = this.entryY;
                        int roleHeight = ReminderGridWidget.this.itemHeight;

                        if (mouseX >= roleX && mouseX < roleX + itemWidth && mouseY >= roleY && mouseY < roleY + roleHeight) {
                            ReminderCatalog.ReminderDefinition def = this.defsInRow.get(i);

                            // Shift+left_click opens role details screen (only for official roles)
                            if (Screen.hasShiftDown() && !def.isCustomRole() && !def.isFabled() && def.role() != null) {
                                ReminderChooseScreen.this.savedScrollAmount = ReminderGridWidget.this.getScrollAmount();
                                MinecraftClient.getInstance().setScreen(new CharacterDetailsScreen(def.role(), ReminderChooseScreen.this));
                                return true;
                            }

                            // --- MODIFIED --- Create reminder from definition and close
                            Reminder reminder;
                            if (def.playerUuid().isPresent()) {
                                // Player reminder (Harpy)
                                reminder = new Reminder(def.text(), Optional.of(def.role()), def.playerUuid());
                            } else if (def.isFabled()) {
                                // Fabled reminder - use factory method
                                reminder = Reminder.forFabled(def.fabledId(), def.text());
                            } else if (def.isCustomRole()) {
                                // Custom role reminder - use factory method
                                reminder = Reminder.forCustomRole(def.customRoleId(), def.text());
                            } else {
                                // Regular reminder
                                reminder = new Reminder(def.text(), Optional.of(def.role()));
                            }
                            addReminderAndClose(reminder);

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