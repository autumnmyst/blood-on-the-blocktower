package com.autumnwind.botb.gui.assignroles;

import com.autumnwind.botb.hud.NightOrderHudManager;
import com.autumnwind.botb.networking.*;
import com.autumnwind.botb.states.ClientState;
import com.autumnwind.botb.states.StorytellerState;
import com.autumnwind.botb.util.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import java.util.*;
import net.minecraft.server.permissions.Permissions;

/**
 * Action methods for the AssignRolesScreen (shuffle, randomize, send roles, etc.)
 */
public class AssignRolesActions {

    private AssignRolesActions() {} // Prevent instantiation

    /**
     * Shuffles role assignments among players.
     * When called mid-game (not setup), fires role-switch triggers for each player whose
     * official role actually changed, subject to {@link StorytellerState#createRoleSwitchTriggersOnRoleChange}
     * and the replacement rule inside {@code TriggerManager.createRoleSwitchTrigger}.
     * @return true if animation should be triggered
     */
    public static boolean shuffleRoles() {
        Minecraft client = Minecraft.getInstance();
        boolean isMidGame = !(ClientState.currentDay == 0 && ClientState.currentNight == 0);

        // Snapshot pre-shuffle roles so we can fire triggers only for players who actually changed.
        Map<UUID, Role> oldRoles = new HashMap<>();
        for (Map.Entry<UUID, PendingRoleAssignment> e : StorytellerState.PENDING_ROLES.entrySet()) {
            oldRoles.put(e.getKey(), e.getValue() != null ? e.getValue().role() : null);
        }

        // Get players with roles and the roles themselves
        List<UUID> assignedPlayers = new ArrayList<>(StorytellerState.PENDING_ROLES.keySet());
        List<PendingRoleAssignment> assignments = new ArrayList<>(StorytellerState.PENDING_ROLES.values());
        Collections.shuffle(assignments); // Shuffle the roles
        StorytellerState.PENDING_ROLES.clear(); // Clear existing assignments and marks (only for operators)
        if (client.player != null && client.player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
            StorytellerState.markedPlayers.clear(); // Clear all marks before re-evaluating
        }

        StorytellerState.REMINDERS.clear();

        // Re-assign shuffled roles and update marks
        for (int i = 0; i < assignedPlayers.size(); i++) {
            UUID playerUUID = assignedPlayers.get(i);
            PendingRoleAssignment newAssignment = assignments.get(i);
            StorytellerState.PENDING_ROLES.put(playerUUID, newAssignment);

            if (client.player != null && client.player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
                // Find the role in the "Other Nights" list to check its default mark status
                boolean markedByDefault = NightOrder.getOtherNightOrder().stream()
                        .filter(info -> info.isRole() && info.getRole() == newAssignment.role())
                        .findFirst()
                        .map(NightOrder.NightOrderInfo::isMarkedByDefault)
                        .orElse(false);

                if (markedByDefault) {
                    StorytellerState.markedPlayers.add(playerUUID);
                }
            }
        }

        // Rebuild the HUD list (only needed for operators)
        if (client.player != null && client.player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
            NightOrderHudManager.rebuildActiveNightOrder();
        }

        // Mid-game: every role change is surfaced as a triggered visit. createRoleSwitchTrigger
        // self-gates on the storyteller toggle and enforces the "replace upcoming trigger for
        // the same player" rule, so every change can be announced unconditionally here.
        if (isMidGame && client.player != null && client.player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
            fireRoleSwitchTriggersForChangedPlayers(oldRoles);
        }

        Collections.shuffle(StorytellerState.DEMON_BLUFFS);

        // Sync grimoire with other storytellers
        StorytellerState.syncGrimoire();

        return true; // Trigger animation
    }

    /**
     * Re-draws the three demon bluffs from the script's good characters that aren't in play.
     * Pope lets in-play characters be bluffs.
     */
    public static void randomizeBluffs() {
        Script script = ClientState.currentScript;
        if (script == null) {
            return;
        }

        boolean popeActive = script.hasFabledOrLoric("pope");
        Set<String> inPlayIds = new HashSet<>();
        if (!popeActive) {
            for (PendingRoleAssignment assignment : StorytellerState.PENDING_ROLES.values()) {
                if (assignment == null) continue;
                if (assignment.isCustomRole() && assignment.customRole().isPresent()) {
                    inPlayIds.add(assignment.customRole().get().id().toLowerCase(Locale.ROOT));
                } else if (assignment.role() != null) {
                    inPlayIds.add(assignment.role().getId());
                }
            }
        }

        List<ScriptRole> pool = new ArrayList<>();
        Set<String> pooledIds = new HashSet<>();
        for (ScriptRole scriptRole : script.allRoles()) {
            RoleType team = scriptRole.getTeam();
            if (team != RoleType.TOWNSFOLK && team != RoleType.OUTSIDER) continue;
            String id = scriptRole.getId().toLowerCase(Locale.ROOT);
            if (inPlayIds.contains(id) || !pooledIds.add(id)) continue;
            pool.add(scriptRole);
        }
        Collections.shuffle(pool);

        for (int i = 0; i < StorytellerState.DEMON_BLUFFS.size(); i++) {
            StorytellerState.DEMON_BLUFFS.set(i, i < pool.size() ? pool.get(i) : null);
        }
    }

    /**
     * For each player whose official role differs from the pre-shuffle snapshot, fire a
     * role-switch trigger. Custom-role and NO_ROLE transitions are skipped to match the
     * existing single-player role-change pathway (which only triggers on official roles).
     */
    private static void fireRoleSwitchTriggersForChangedPlayers(Map<UUID, Role> oldRoles) {
        for (Map.Entry<UUID, PendingRoleAssignment> e : StorytellerState.PENDING_ROLES.entrySet()) {
            UUID playerUUID = e.getKey();
            PendingRoleAssignment newAssignment = e.getValue();
            if (newAssignment == null || newAssignment.isCustomRole()) continue;
            Role newRole = newAssignment.role();
            if (newRole == null || newRole == Role.NO_ROLE) continue;
            Role oldRole = oldRoles.get(playerUUID);
            if (oldRole == newRole) continue;
            NightOrderHudManager.createRoleSwitchTrigger(playerUUID, newRole);
        }
    }

    /**
     * Shuffles seat assignments among players.
     * @return true if animation should be triggered
     */
    public static boolean shuffleSeats() {
        Minecraft client = Minecraft.getInstance();

        // Get players with seats
        List<UUID> seatedPlayers = new ArrayList<>(StorytellerState.PENDING_SEAT_NUMBERS.keySet());
        if (seatedPlayers.isEmpty()) return false;

        // Get the seat numbers
        List<Integer> seatNumbers = new ArrayList<>(StorytellerState.PENDING_SEAT_NUMBERS.values());
        Collections.shuffle(seatNumbers); // Shuffle the seat numbers

        // Clear and re-assign
        StorytellerState.PENDING_SEAT_NUMBERS.clear();
        for (int i = 0; i < seatedPlayers.size(); i++) {
            StorytellerState.PENDING_SEAT_NUMBERS.put(seatedPlayers.get(i), seatNumbers.get(i));
        }

        // Rebuild night order
        if (client.player != null && client.player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
            NightOrderHudManager.rebuildActiveNightOrder();
        }

        // Sync grimoire with other storytellers
        StorytellerState.syncGrimoire();

        return true; // Trigger animation
    }

    /**
     * Randomly assigns roles from the script based on the number of eligible players.
     * Supports both official roles and custom roles.
     * Mid-game, fires role-switch triggers for each player whose official role actually
     * changed, with the same gating/replacement rules as {@link #shuffleRoles()}.
     * @return true if animation should be triggered
     */
    public static boolean randomizeRoles() {
        Minecraft client = Minecraft.getInstance();
        boolean isMidGame = !(ClientState.currentDay == 0 && ClientState.currentNight == 0);

        // Snapshot pre-randomize roles for trigger-firing diff after the assignment is done.
        Map<UUID, Role> oldRoles = new HashMap<>();
        for (Map.Entry<UUID, PendingRoleAssignment> e : StorytellerState.PENDING_ROLES.entrySet()) {
            oldRoles.put(e.getKey(), e.getValue() != null ? e.getValue().role() : null);
        }

        // Check if script is loaded
        if (ClientState.currentScript == null) {
            return false;
        }

        // Build list of all roles (official + custom) as ScriptRole
        List<ScriptRole> allScriptRoles = new ArrayList<>();

        // Add official roles
        List<Role> officialRoles = ClientState.currentScript.roles();
        if (officialRoles != null) {
            for (Role role : officialRoles) {
                allScriptRoles.add(new ScriptRole.Official(role));
            }
        }

        // Add custom roles
        if (ClientState.currentScript.hasCustomRoles()) {
            for (CustomRole customRole : ClientState.currentScript.customRoles()) {
                allScriptRoles.add(new ScriptRole.Custom(customRole));
            }
        }

        if (allScriptRoles.isEmpty()) {
            return false;
        }

        // Get eligible players - first try seated players (seat > 0)
        List<UUID> eligiblePlayers = new ArrayList<>();
        boolean needsSeating = false;
        for (Map.Entry<UUID, Integer> entry : StorytellerState.PENDING_SEAT_NUMBERS.entrySet()) {
            if (entry.getValue() > 0) {
                eligiblePlayers.add(entry.getKey());
            }
        }

        // If no seated players, count all players on server except self (storyteller)
        if (eligiblePlayers.isEmpty() && client != null && client.player != null) {
            UUID selfUUID = client.player.getUUID();
            for (PlayerListUtil.PlayerInfo playerInfo : PlayerListUtil.getAllPlayers(client)) {
                if (!playerInfo.uuid().equals(selfUUID)) {
                    eligiblePlayers.add(playerInfo.uuid());
                }
            }
            needsSeating = true; // These players need to be assigned seats
        }

        if (eligiblePlayers.isEmpty()) {
            return false;
        }

        int playerCount = eligiblePlayers.size();
        RoleCounts.RoleCountInfo counts = RoleCounts.getCounts(playerCount);
        if (counts == null) {
            return false;
        }

        // Separate script roles by type (using ScriptRole.getTeam())
        List<ScriptRole> townsfolkRoles = new ArrayList<>();
        List<ScriptRole> outsiderRoles = new ArrayList<>();
        List<ScriptRole> minionRoles = new ArrayList<>();
        List<ScriptRole> demonRoles = new ArrayList<>();

        for (ScriptRole scriptRole : allScriptRoles) {
            switch (scriptRole.getTeam()) {
                case TOWNSFOLK -> townsfolkRoles.add(scriptRole);
                case OUTSIDER -> outsiderRoles.add(scriptRole);
                case MINION -> minionRoles.add(scriptRole);
                case DEMON -> demonRoles.add(scriptRole);
                default -> {} // Ignore other types (TRAVELER, FABLED, NONE)
            }
        }

        // Shuffle each list for random selection
        Collections.shuffle(townsfolkRoles);
        Collections.shuffle(outsiderRoles);
        Collections.shuffle(minionRoles);
        Collections.shuffle(demonRoles);

        // Select the required number of each type
        List<ScriptRole> selectedRoles = new ArrayList<>();

        // Add townsfolk
        for (int i = 0; i < counts.townsfolk() && i < townsfolkRoles.size(); i++) {
            selectedRoles.add(townsfolkRoles.get(i));
        }

        // Add outsiders
        for (int i = 0; i < counts.outsiders() && i < outsiderRoles.size(); i++) {
            selectedRoles.add(outsiderRoles.get(i));
        }

        // Add minions
        for (int i = 0; i < counts.minions() && i < minionRoles.size(); i++) {
            selectedRoles.add(minionRoles.get(i));
        }

        // Add demons
        for (int i = 0; i < counts.demon() && i < demonRoles.size(); i++) {
            selectedRoles.add(demonRoles.get(i));
        }

        // Shuffle the selected roles before assignment
        Collections.shuffle(selectedRoles);

        // Clear existing assignments and marks
        StorytellerState.PENDING_ROLES.clear();
        if (client.player != null && client.player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
            StorytellerState.markedPlayers.clear();
        }
        StorytellerState.REMINDERS.clear();

        // If players need seating, clear and reset seat assignments
        if (needsSeating) {
            StorytellerState.PENDING_SEAT_NUMBERS.clear();
            StorytellerState.nextSeatNumber = 1;
        }

        // Assign roles (and seats if needed) to players
        int roleIndex = 0;
        for (UUID playerUuid : eligiblePlayers) {
            if (roleIndex < selectedRoles.size()) {
                ScriptRole scriptRole = selectedRoles.get(roleIndex);
                PendingRoleAssignment assignment;

                if (scriptRole.isCustom()) {
                    // Custom role assignment
                    CustomRole customRole = scriptRole.asCustomRole();
                    assignment = new PendingRoleAssignment(customRole, AlignmentOverride.DEFAULT);
                } else {
                    // Official role assignment
                    Role role = scriptRole.asRole();
                    assignment = new PendingRoleAssignment(role, AlignmentOverride.DEFAULT);
                }

                StorytellerState.PENDING_ROLES.put(playerUuid, assignment);

                // Assign seat if needed
                if (needsSeating) {
                    StorytellerState.PENDING_SEAT_NUMBERS.put(playerUuid, StorytellerState.nextSeatNumber);
                    StorytellerState.nextSeatNumber++;
                }

                // Check for default marking
                if (client.player != null && client.player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
                    boolean markedByDefault;

                    if (scriptRole.isCustom()) {
                        // Custom roles: marked by default if they have other night ability
                        CustomRole customRole = scriptRole.asCustomRole();
                        markedByDefault = customRole.otherNight() > 0;
                    } else {
                        // Official roles: check night order
                        Role role = scriptRole.asRole();
                        markedByDefault = NightOrder.getOtherNightOrder().stream()
                                .filter(info -> info.isRole() && info.getRole() == role)
                                .findFirst()
                                .map(NightOrder.NightOrderInfo::isMarkedByDefault)
                                .orElse(false);
                    }

                    if (markedByDefault) {
                        StorytellerState.markedPlayers.add(playerUuid);
                    }
                }

                roleIndex++;
            }
        }

        // Rebuild night order
        if (client.player != null && client.player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
            NightOrderHudManager.rebuildActiveNightOrder();
        }

        // Mid-game: fire triggers for changed roles (same rules as shuffleRoles).
        if (isMidGame && client.player != null && client.player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
            fireRoleSwitchTriggersForChangedPlayers(oldRoles);
        }

        randomizeBluffs();

        // Sync grimoire with other storytellers
        StorytellerState.syncGrimoire();

        return true; // Trigger animation
    }

    public static void sendAllPlayersToSeats() {
        for (Map.Entry<UUID, Integer> entry : StorytellerState.PENDING_SEAT_NUMBERS.entrySet()) {
            UUID playerUuid = entry.getKey();
            int seat = entry.getValue();
            if (seat > 0) {
                ClientPlayNetworking.send(new TeleportPlayersToTownSquareSeatC2SPayload(seat, List.of(playerUuid)));
            }
        }
    }

    public static void sendAllPlayersHome() {
        for (Map.Entry<UUID, Integer> entry : StorytellerState.PENDING_SEAT_NUMBERS.entrySet()) {
            UUID playerUuid = entry.getKey();
            int seat = entry.getValue();
            if (seat > 0) {
                ClientPlayNetworking.send(new TeleportPlayersToSeatC2SPayload(seat, List.of(playerUuid)));
            }
        }
    }

    public static void sendDeadPlayersToServer() {
        // Build dead players map from ClientState (the single source of truth)
        Map<UUID, Boolean> deadPlayersMap = new HashMap<>();
        for (UUID uuid : StorytellerState.PENDING_ROLES.keySet()) {
            deadPlayersMap.put(uuid, ClientState.playerDeathStatus.getOrDefault(uuid, false));
        }

        // Voudon mode evaluated against the death status being sent, so killing or
        // reviving the Voudon flips the mode on the server immediately
        Optional<UUID> voudonPlayer = StorytellerState.getVoudonAliveWithAbility(deadPlayersMap);

        // Send the UpdateDeadPlayersC2SPayload. silent=false so a daytime mark-dead
        // produces the cosmetic lightning strike on the server.
        ClientPlayNetworking.send(new UpdateDeadPlayersC2SPayload(
                deadPlayersMap,
                StorytellerState.PENDING_SEAT_NUMBERS,
                false,
                voudonPlayer.isPresent(),
                voudonPlayer
        ));
    }

    /**
     * Static method to send roles with proper reminder checks (Drunk sees Townsfolk, etc.)
     * Called from StorytellerToolsScreen's Send Roles button.
     */
    public static void sendRolesWithReminderChecks() {
        int activePlayerCount = StorytellerState.PENDING_ROLES.size();
        Map<UUID, PendingRoleAssignment> rolesToSend = buildRolesToSendMap();

        ClientPlayNetworking.send(new AssignRolesC2SPayload(
                rolesToSend,
                StorytellerState.PENDING_SEAT_NUMBERS,
                activePlayerCount,
                Optional.ofNullable(ClientState.currentScript),
                StorytellerState.REMINDERS
        ));

    }

    /**
     * Targeted Send Roles: the same data as {@link #sendRolesWithReminderChecks()}, but the
     * server only updates the named player's client. Everyone else is left alone.
     */
    public static void sendRolesToPlayer(UUID targetPlayer) {
        int activePlayerCount = StorytellerState.PENDING_ROLES.size();
        Map<UUID, PendingRoleAssignment> rolesToSend = buildRolesToSendMap();

        ClientPlayNetworking.send(new SendRolesToPlayerC2SPayload(
                targetPlayer,
                rolesToSend,
                StorytellerState.PENDING_SEAT_NUMBERS,
                activePlayerCount,
                Optional.ofNullable(ClientState.currentScript),
                StorytellerState.REMINDERS
        ));
    }

    /**
     * Sends only the current script to all players. Roles, seats, and reminders are untouched.
     */
    public static void sendScriptOnly() {
        if (ClientState.currentScript == null) {
            return;
        }
        ClientPlayNetworking.send(new SendScriptC2SPayload(ClientState.currentScript));
    }

    /**
     * Builds the roles map with special role handling (Drunk sees Townsfolk, etc.)
     */
    public static Map<UUID, PendingRoleAssignment> buildRolesToSendMap() {
        Map<UUID, PendingRoleAssignment> rolesToSend = new HashMap<>();

        // Tor: the living don't know their character or alignment, and learn it when they die.
        boolean torActive = ClientState.currentScript != null
                && ClientState.currentScript.hasFabledOrLoric("tor");

        for (Map.Entry<UUID, PendingRoleAssignment> entry : StorytellerState.PENDING_ROLES.entrySet()) {
            UUID playerUuid = entry.getKey();
            PendingRoleAssignment assignment = entry.getValue();
            Role role = assignment.role();

            if (torActive && !ClientState.playerDeathStatus.getOrDefault(playerUuid, false)) {
                rolesToSend.put(playerUuid, new PendingRoleAssignment(Role.NO_ROLE, AlignmentOverride.DEFAULT));
                continue;
            }

            // Special handling for Drunk, Marionette, Lunatic: send their associated role
            if (role == Role.DRUNK || role == Role.MARIONETTE || role == Role.LUNATIC) {
                List<Reminder> reminders = StorytellerState.REMINDERS.getOrDefault(playerUuid, Collections.emptyList());
                Role associatedRole = null;

                for (Reminder reminder : reminders) {
                    if (AssignRolesUtils.isSpecialReminder(reminder) && reminder.role().isPresent()) {
                        associatedRole = reminder.role().get();
                        break;
                    }
                }

                if (associatedRole != null) {
                    rolesToSend.put(playerUuid, new PendingRoleAssignment(associatedRole, AlignmentOverride.DEFAULT));
                } else {
                    rolesToSend.put(playerUuid, assignment);
                }
            }
            // Special handling for Ogre: send with default alignment (good)
            else if (role == Role.OGRE) {
                rolesToSend.put(playerUuid, new PendingRoleAssignment(Role.OGRE, AlignmentOverride.DEFAULT));
            }
            // Special handling for Hermit
            else if (role == Role.HERMIT) {
                List<Reminder> reminders = StorytellerState.REMINDERS.getOrDefault(playerUuid, Collections.emptyList());

                boolean hasDrunk = reminders.stream().anyMatch(r -> AssignRolesUtils.isSpecialReminder(r) && r.role().isPresent() && r.role().get() == Role.DRUNK);
                boolean hasLunatic = reminders.stream().anyMatch(r -> AssignRolesUtils.isSpecialReminder(r) && r.role().isPresent() && r.role().get() == Role.LUNATIC);
                boolean hasOgre = reminders.stream().anyMatch(r -> AssignRolesUtils.isSpecialReminder(r) && r.role().isPresent() && r.role().get() == Role.OGRE);

                Role associatedRole = null;
                for (Reminder reminder : reminders) {
                    if (AssignRolesUtils.isSpecialReminder(reminder) && reminder.role().isPresent()) {
                        Role reminderRole = reminder.role().get();
                        RoleType type = reminderRole.getType();

                        if (hasDrunk && type == RoleType.TOWNSFOLK) {
                            associatedRole = reminderRole;
                            break;
                        }
                        if (hasLunatic && type == RoleType.DEMON) {
                            associatedRole = reminderRole;
                            break;
                        }
                    }
                }

                if (associatedRole != null) {
                    rolesToSend.put(playerUuid, new PendingRoleAssignment(associatedRole, AlignmentOverride.DEFAULT));
                } else if (hasOgre) {
                    rolesToSend.put(playerUuid, new PendingRoleAssignment(Role.HERMIT, AlignmentOverride.DEFAULT));
                } else {
                    rolesToSend.put(playerUuid, assignment);
                }
            }
            // All other roles: send as-is
            else {
                rolesToSend.put(playerUuid, assignment);
            }
        }

        return rolesToSend;
    }
}
