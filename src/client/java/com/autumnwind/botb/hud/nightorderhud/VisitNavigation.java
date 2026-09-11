package com.autumnwind.botb.hud.nightorderhud;

import com.autumnwind.botb.networking.ExecuteDuskDawnC2SPayload;
import com.autumnwind.botb.util.RoleVisit;
import net.minecraft.text.Text;
import net.minecraft.text.MutableText;
import com.autumnwind.botb.networking.OpenNominationsC2SPayload;
import com.autumnwind.botb.networking.TeleportPlayersToSeatC2SPayload;
import com.autumnwind.botb.networking.TeleportToSeatC2SPayload;
import com.autumnwind.botb.networking.UpdateDeadPlayersC2SPayload;
import com.autumnwind.botb.states.ClientState;
import com.autumnwind.botb.states.StorytellerState;
import com.autumnwind.botb.util.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;

import java.util.*;

import static com.autumnwind.botb.hud.nightorderhud.RoleHelpers.*;

/**
 * Handles navigation through the night order and teleportation.
 */
public class VisitNavigation {

    /**
     * Moves the current visit index by 'direction' (1 or -1).
     */
    public static void advance(int direction) {
        if (StorytellerState.activeNightOrder.isEmpty()) return;

        int size = StorytellerState.activeNightOrder.size();
        RoleVisit currentVisit = StorytellerState.activeNightOrder.get(StorytellerState.currentNightVisitIndex);

        // Find Dusk and Dawn indices
        int duskIndex = -1;
        int dawnIndex = -1;
        for (int i = 0; i < size; i++) {
            RoleVisit visit = StorytellerState.activeNightOrder.get(i);
            if (visit.staticAction() != null) {
                if (visit.staticAction() == NightOrder.StaticAction.DUSK) {
                    duskIndex = i;
                } else if (visit.staticAction() == NightOrder.StaticAction.DAWN) {
                    dawnIndex = i;
                }
            }
        }

        // Check if we're currently on Dusk or Dawn
        boolean currentlyOnDusk = currentVisit.staticAction() != null &&
                                   currentVisit.staticAction() == NightOrder.StaticAction.DUSK;
        boolean currentlyOnDawn = currentVisit.staticAction() != null &&
                                   currentVisit.staticAction() == NightOrder.StaticAction.DAWN;

        // Check if it's nighttime (dusk was activated: night > day)
        boolean isNightTime = ClientState.currentNight > ClientState.currentDay;
        // Check if it's daytime (dawn was activated or setup: night == day, and night > 0 means dawn was activated)
        boolean isDayTime = ClientState.currentNight == ClientState.currentDay && ClientState.currentDay > 0;
        // Check if we're in setup phase (day 0, night 0)
        boolean isSetupPhase = ClientState.currentDay == 0 && ClientState.currentNight == 0;

        MinecraftClient client = MinecraftClient.getInstance();

        // Rule 0: During setup, cannot move off Dusk at all (must activate Dusk to start the game)
        if (isSetupPhase && currentlyOnDusk) {
            if (client.player != null) {
                client.player.sendMessage(
                    Text.literal("Must activate Dusk to start the game")
                        .formatted(Formatting.RED),
                    true
                );
            }
            return;
        }

        // Rule 1: Don't allow progressing past Dusk if it's not nighttime
        if (direction > 0 && currentlyOnDusk && !isNightTime) {
            if (client.player != null) {
                client.player.sendMessage(
                    Text.literal("Must activate Dusk before progressing night order")
                        .formatted(Formatting.RED),
                    true
                );
            }
            return;
        }

        // Rule 2: Don't allow progressing past Dawn if it's not daytime
        if (direction > 0 && currentlyOnDawn && !isDayTime) {
            if (client.player != null) {
                client.player.sendMessage(
                    Text.literal("Must activate Dawn before progressing night order")
                        .formatted(Formatting.RED),
                    true
                );
            }
            return;
        }

        // Calculate next index
        int nextIndex = (StorytellerState.currentNightVisitIndex + direction + size) % size;

        // Rule 3: Don't allow moving backwards FROM Dusk once nighttime has started
        if (direction < 0 && currentlyOnDusk && isNightTime) {
            if (client.player != null) {
                client.player.sendMessage(
                    Text.literal("Cannot move backwards from Dusk")
                        .formatted(Formatting.RED),
                    true
                );
            }
            return;
        }

        // Rule 4: Don't allow moving backwards FROM Dawn once daytime has started
        if (direction < 0 && currentlyOnDawn && isDayTime) {
            if (client.player != null) {
                client.player.sendMessage(
                    Text.literal("Cannot move backwards from Dawn")
                        .formatted(Formatting.RED),
                    true
                );
            }
            return;
        }

        StorytellerState.currentNightVisitIndex = nextIndex;

        // Update semantic position tracking
        NightOrderBuilder.updateSemanticTracking();

        // Always show visit info when changing to a new visit
        showCurrentVisitInfo();

        // Auto-teleport only if Auto TP is enabled, the visit allows it, and the players changed
        RoleVisit nextVisit = StorytellerState.activeNightOrder.get(StorytellerState.currentNightVisitIndex);
        boolean samePlayers = new HashSet<>(currentVisit.players()).equals(new HashSet<>(nextVisit.players()));
        if (StorytellerState.autoTeleportEnabled && nextVisit.seatTeleport() && !samePlayers) {
            performTeleport();
        }
    }

    /**
     * Shows the visit information (players, instructions) for the current visit.
     * Does NOT perform any teleporting.
     */
    public static void showCurrentVisitInfo() {
        if (StorytellerState.currentNightVisitIndex >= StorytellerState.activeNightOrder.size()) return;

        RoleVisit visit = StorytellerState.activeNightOrder.get(StorytellerState.currentNightVisitIndex);
        List<UUID> players = visit.players();
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        // Update HUD state for night order instructions display
        updateHudState(visit, players, client);

        // Find minstrel player for text suffix
        UUID minstrelPlayer = findMinstrelPlayer();

        if (players.isEmpty()) {
            // No players for this visit (e.g., Dusk, Dawn)
            // Don't show anything when just navigating to Dawn/Dusk
        } else if (players.size() == 1) {
            // Single player (supports distant players)
            UUID playerUUID = players.getFirst();
            int seat = StorytellerState.PENDING_SEAT_NUMBERS.getOrDefault(playerUUID, -1);
            if (seat > 0) {
                String playerName = getPlayerName(client, playerUUID);
                if (playerName != null) {
                    MutableText message = Text.literal("Visiting Seat ").formatted(Formatting.GRAY);

                    String reminderSuffix = getPlayerReminderSuffix(playerUUID, minstrelPlayer);
                    PendingRoleAssignment assignment = StorytellerState.PENDING_ROLES.get(playerUUID);

                    // Build Role Text: (Assigned Role / Associated Role)
                    // Use assignment.getDisplayName() which handles both official and custom roles
                    String visitText = visit.getName().getString();
                    MutableText roleText = Text.literal((assignment != null) ? assignment.getDisplayName() : visitText);
                    if (visit.hasAssociatedRole()) {
                        roleText.append(Text.literal(" / " + visit.getAssociatedRoleDisplayName()).formatted(Formatting.GRAY));
                    }

                    message.append(Text.literal(String.valueOf(seat)).formatted(Formatting.GOLD))
                            .append(Text.literal(": ").formatted(Formatting.GRAY))
                            .append(Text.literal(playerName).formatted(Formatting.WHITE))
                            .append(Text.literal(" (").formatted(Formatting.GRAY))
                            .append(roleText.styled(style -> style.withColor(TextColor.fromRgb(getAlignedRoleColor(assignment)))))
                            .append(Text.literal(")").formatted(Formatting.GRAY))
                            .append(Text.literal(reminderSuffix).formatted(Formatting.GRAY));

                    client.player.sendMessage(message, false);
                }
            }
        } else {
            // Multiple players (supports distant players)
            MutableText message = Text.literal("Multiple players for ").append(visit.getName().copy().formatted(Formatting.YELLOW)).append(":");

            for (UUID playerUUID : players) {
                String playerName = getPlayerName(client, playerUUID);
                if (playerName == null) continue;

                int seat = StorytellerState.PENDING_SEAT_NUMBERS.getOrDefault(playerUUID, -1);

                String reminderSuffix = getPlayerReminderSuffix(playerUUID, minstrelPlayer);
                PendingRoleAssignment assignment = StorytellerState.PENDING_ROLES.get(playerUUID);

                // Build Role Text: (Assigned Role / Associated Role)
                // Use assignment.getDisplayName() which handles both official and custom roles
                String visitText = visit.getName().getString();
                MutableText roleText = Text.literal((assignment != null) ? assignment.getDisplayName() : visitText);
                if (visit.hasAssociatedRole()) {
                    roleText.append(Text.literal(" / " + visit.getAssociatedRoleDisplayName()).formatted(Formatting.GRAY));
                }

                Style teleportStyle = Style.EMPTY
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/botb teleportToSeat " + seat))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("Teleport to seat " + seat))).withColor(Formatting.GREEN);

                message.append(Text.literal("\n- ").formatted(Formatting.GRAY))
                        .append(Text.literal(playerName).formatted(Formatting.WHITE))
                        .append(Text.literal(" (").formatted(Formatting.GRAY))
                        .append(roleText.styled(style -> style.withColor(TextColor.fromRgb(getAlignedRoleColor(assignment)))))
                        .append(Text.literal(reminderSuffix).formatted(Formatting.GRAY))
                        .append(Text.literal(", Seat ").formatted(Formatting.GRAY))
                        .append(Text.literal(String.valueOf(seat)).formatted(Formatting.GOLD))
                        .append(Text.literal(") [").formatted(Formatting.GRAY))
                        .append(Text.literal("TELEPORT").setStyle(teleportStyle))
                        .append(Text.literal("]").formatted(Formatting.GRAY));
            }
            client.player.sendMessage(message, false);
        }
    }

    /**
     * Refreshes the HUD state for the current visit without sending chat messages.
     * Call this after rebuilding the night order to update the instructions box.
     */
    public static void refreshCurrentVisitHud() {
        if (StorytellerState.currentNightVisitIndex >= StorytellerState.activeNightOrder.size()) return;

        RoleVisit visit = StorytellerState.activeNightOrder.get(StorytellerState.currentNightVisitIndex);
        List<UUID> players = visit.players();
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        updateHudState(visit, players, client);
    }

    private static void updateHudState(RoleVisit visit, List<UUID> players, MinecraftClient client) {
        // Pure phase transitions have nothing to display in the role HUD, so clear it.
        // MINION_INFO / DEMON_INFO are info phases that DO carry an instruction and (for
        // triggered evil-traveler visits) a role, so they fall through to the regular
        // role/custom-role branches below.
        NightOrder.StaticAction sa = visit.staticAction();
        if (visit.role() != null) {
            // Official role visit - show instructions in HUD
            Role visitRole = visit.role();
            StorytellerState.currentVisitScriptRole = new ScriptRole.Official(visitRole);
            StorytellerState.currentVisitIcon = visit.getIcon();
            StorytellerState.currentVisitInstructions = visit.instruction();

            // Determine alignment based on the actual role assignment if available
            if (!players.isEmpty()) {
                PendingRoleAssignment assignment = StorytellerState.PENDING_ROLES.get(players.getFirst());
                StorytellerState.currentVisitIsGood = assignment != null ? assignment.isFinalGood() : visitRole.isDefaultGood();
            } else {
                StorytellerState.currentVisitIsGood = visitRole.isDefaultGood();
            }

            // Generate extra info for this role visit (for Storyteller HUD).
            // For associated-role visits (e.g. Drunk-as-Empath, Philosopher-as-Oracle), the
            // info must follow the ABILITY being used, not the player's assigned role.
            // Otherwise an Oracle-as-Empath would show "Dead evil" instead of "Evil neighbors",
            // and a Drunk-as-Empath would show nothing.
            Role infoRole = visit.associatedRole().orElse(visitRole);
            StorytellerState.currentVisitExtraInfo = withVortoxLine(visit,
                    NightOrderInfoGenerator.generateInfo(infoRole, players));

            // Build role text for HUD with proper coloring
            UUID minstrelPlayerForRoleText = findMinstrelPlayer();

            if (!players.isEmpty()) {
                UUID firstPlayerUUID = players.getFirst();
                PendingRoleAssignment firstAssignment = StorytellerState.PENDING_ROLES.get(firstPlayerUUID);

                // Build role text with colors
                MutableText roleText = Text.literal((firstAssignment != null) ? firstAssignment.getDisplayName() : visitRole.getDisplayName())
                        .styled(style -> style.withColor(TextColor.fromRgb(getAlignedRoleColor(firstAssignment))));
                if (visit.hasAssociatedRole()) {
                    roleText.append(Text.literal(" / " + visit.getAssociatedRoleDisplayName()).formatted(Formatting.GRAY));
                }

                // Add reminder suffix (Drunk/Poisoned) in gray
                String reminderSuffix = getPlayerReminderSuffix(firstPlayerUUID, minstrelPlayerForRoleText);
                if (!reminderSuffix.isEmpty()) {
                    roleText.append(Text.literal(reminderSuffix).formatted(Formatting.GRAY));
                }

                StorytellerState.currentVisitRoleText = roleText;
                StorytellerState.currentVisitPlayerNames = buildPlayerNamesText(players, client);
            } else {
                StorytellerState.currentVisitRoleText = Text.literal(visitRole.getDisplayName())
                        .styled(style -> style.withColor(TextColor.fromRgb(visitRole.getType().getColor())));
                StorytellerState.currentVisitPlayerNames = null;
            }
        } else if (visit.isCustomRole()) {
            // Custom role visit - show instructions in HUD
            CustomRole customRole = visit.customRole();
            StorytellerState.currentVisitScriptRole = new ScriptRole.Custom(customRole);
            StorytellerState.currentVisitIcon = visit.getIcon();
            StorytellerState.currentVisitInstructions = visit.instruction();

            // Determine alignment based on the actual role assignment if available
            if (!players.isEmpty()) {
                PendingRoleAssignment assignment = StorytellerState.PENDING_ROLES.get(players.getFirst());
                StorytellerState.currentVisitIsGood = assignment != null ? assignment.isFinalGood() : customRole.isDefaultGood();
            } else {
                StorytellerState.currentVisitIsGood = customRole.isDefaultGood();
            }

            // No extra info generator for custom roles themselves yet, but if the custom
            // role's ability is an *official* role (e.g. custom Philosopher-like role
            // with the Empath ability), surface info for that associated official role.
            StorytellerState.currentVisitExtraInfo = withVortoxLine(visit, visit.associatedRole().isPresent()
                    ? NightOrderInfoGenerator.generateInfo(visit.associatedRole().get(), players)
                    : null);

            if (!players.isEmpty()) {
                UUID firstPlayerUUID = players.getFirst();
                PendingRoleAssignment firstAssignment = StorytellerState.PENDING_ROLES.get(firstPlayerUUID);

                // Build role text with colors - use assigned role display name if this is an associated visit
                String assignedDisplayName = (firstAssignment != null) ? firstAssignment.getDisplayName() : customRole.getDisplayName();
                int assignedColor = (firstAssignment != null) ? getAlignedRoleColor(firstAssignment) : customRole.team().getColor();

                MutableText roleText = Text.literal(assignedDisplayName)
                        .styled(style -> style.withColor(TextColor.fromRgb(assignedColor)));

                // Add associated role text if this is an associated visit
                if (visit.hasAssociatedRole()) {
                    roleText.append(Text.literal(" / " + visit.getAssociatedRoleDisplayName()).formatted(Formatting.GRAY));
                }

                StorytellerState.currentVisitRoleText = roleText;
                StorytellerState.currentVisitPlayerNames = buildPlayerNamesText(players, client);
            } else {
                MutableText roleText = Text.literal(customRole.getDisplayName())
                        .styled(style -> style.withColor(TextColor.fromRgb(customRole.team().getColor())));
                if (visit.hasAssociatedRole()) {
                    roleText.append(Text.literal(" / " + visit.getAssociatedRoleDisplayName()).formatted(Formatting.GRAY));
                }
                StorytellerState.currentVisitRoleText = roleText;
                StorytellerState.currentVisitPlayerNames = null;
            }
        } else if (sa == NightOrder.StaticAction.MINION_INFO || sa == NightOrder.StaticAction.DEMON_INFO) {
            // Static info bundle (regular minion/demon-info phases with no specific role attached).
            // Show the phase's instruction, the affected players, the static-action icon
            // (minion_info.png / demon_info.png), and a helper line listing who the demons
            // (or minions) are, including any Magicians.
            //
            // Layout note: putting the player names into currentVisitRoleText (line 1) and
            // leaving currentVisitPlayerNames null makes RoleHUD render the names where the
            // role name normally goes, then collapse the empty role-type line below. There's
            // no role to display for these bundles, so "NONE" would just be noise.
            StorytellerState.currentVisitScriptRole = null;
            StorytellerState.currentVisitIcon = visit.getIcon();
            StorytellerState.currentVisitInstructions = visit.instruction();
            StorytellerState.currentVisitIsGood = false;
            StorytellerState.currentVisitRoleText = players.isEmpty() ? null : buildPlayerNamesText(players, client);
            StorytellerState.currentVisitPlayerNames = null;
            StorytellerState.currentVisitExtraInfo = sa == NightOrder.StaticAction.MINION_INFO
                    ? NightOrderInfoGenerator.generateMinionInfoBundleInfo()
                    : NightOrderInfoGenerator.generateDemonInfoBundleInfo();
        } else if (sa != null && visit.iconReminders() != null && !visit.iconReminders().isEmpty()
                && visit.instruction() != null && !visit.instruction().isBlank()) {
            // Other static visits (Dawn, Nominations, Dusk) are shown only when a modifier is
            // on them: Organ Grinder, Bishop, Legion or Riot on Nominations, Buddhist on Dawn.
            // Every modifier icon gets its paragraph in the instruction text, so the box
            // shows the phase's own icon; the modifier icons stay on the sidebar entry.
            StorytellerState.currentVisitScriptRole = null;
            StorytellerState.currentVisitIcon = visit.getIcon();
            StorytellerState.currentVisitInstructions = visit.instruction();
            StorytellerState.currentVisitIsGood = true;
            StorytellerState.currentVisitRoleText = visit.getName().copy().formatted(Formatting.GOLD);
            StorytellerState.currentVisitPlayerNames = players.isEmpty() ? null : buildPlayerNamesText(players, client);
            StorytellerState.currentVisitExtraInfo = null;
        } else {
            // Nothing to show for this visit (Dawn/Nominations/Dusk with no modifiers), clear HUD
            clearVisitHudState();
        }
    }

    private static void clearVisitHudState() {
        StorytellerState.currentVisitScriptRole = null;
        StorytellerState.currentVisitIcon = null;
        StorytellerState.currentVisitInstructions = null;
        StorytellerState.currentVisitIsGood = true;
        StorytellerState.currentVisitExtraInfo = null;
        StorytellerState.currentVisitRoleText = null;
        StorytellerState.currentVisitPlayerNames = null;
    }

    private static MutableText buildPlayerNamesText(List<UUID> players, MinecraftClient client) {
        MutableText playerNamesText = Text.empty();
        boolean first = true;
        for (UUID playerUUID : players) {
            String playerName = getPlayerName(client, playerUUID);
            if (playerName != null) {
                if (!first) {
                    playerNamesText.append(Text.literal(", ").formatted(Formatting.YELLOW));
                }
                playerNamesText.append(Text.literal(playerName).formatted(Formatting.YELLOW));
                first = false;
            }
        }
        return playerNamesText;
    }

    /**
     * Gets player name with fallback for distant players.
     */
    private static String getPlayerName(MinecraftClient client, UUID playerUuid) {
        if (client.world != null) {
            AbstractClientPlayerEntity player = (AbstractClientPlayerEntity) client.world.getPlayerByUuid(playerUuid);
            if (player != null) return player.getName().getString();
        }
        PlayerListUtil.PlayerInfo info = PlayerListUtil.getPlayer(client, playerUuid);
        return info != null ? info.name() : null;
    }

    /**
     * Performs the actual teleportation for the current visit.
     * Does NOT display any messages.
     */
    public static void performTeleport() {
        if (StorytellerState.currentNightVisitIndex >= StorytellerState.activeNightOrder.size()) return;

        RoleVisit visit = StorytellerState.activeNightOrder.get(StorytellerState.currentNightVisitIndex);
        List<UUID> players = visit.players();
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        if (players.isEmpty()) {
            // No players for this visit (e.g., Dusk, Dawn, Nominations)
            // Execute the effects when manually triggered
            if (visit.staticAction() != null) {
                handleStaticActionTeleport(visit, client);
            }
        } else if (players.size() == 1) {
            // Single player - teleport to them
            UUID playerUUID = players.getFirst();
            int seat = StorytellerState.PENDING_SEAT_NUMBERS.getOrDefault(playerUUID, -1);
            if (seat > 0) {
                ClientPlayNetworking.send(new TeleportToSeatC2SPayload(seat, StorytellerState.useDoorknock));

                // Teleport wraith players if target is evil
                List<UUID> wraithsToTeleport = getWraithPlayersToTeleport(playerUUID);
                if (!wraithsToTeleport.isEmpty()) {
                    ClientPlayNetworking.send(new TeleportPlayersToSeatC2SPayload(seat, wraithsToTeleport));
                }
            }
        }
        // For multiple players, don't auto-teleport - they need to click the teleport links
    }

    private static void handleStaticActionTeleport(RoleVisit visit, MinecraftClient client) {
        // Check time of day for Dusk/Dawn activation
        boolean isNightTime = ClientState.currentNight > ClientState.currentDay;
        boolean isDayTime = ClientState.currentNight == ClientState.currentDay;

        if (visit.staticAction() == NightOrder.StaticAction.DUSK) {
            // Dusk can only be activated during the day
            if (isNightTime) {
                client.player.sendMessage(
                    Text.literal("Dusk can only be activated during the day")
                        .formatted(Formatting.RED),
                    true
                );
                return;
            }

            // Setup→Night 1: snapshot current evil travelers and create their MINION_INFO
            // triggers. createEvilTravelerDemonInfoTrigger gates on currentNight >= 1, so
            // during setup individual assignments don't create triggers, and they're
            // materialised here when the storyteller activates the first night.
            if (ClientState.currentNight == 0) {
                TriggerManager.createInitialEvilTravelerTriggers();
            }

            ClientPlayNetworking.send(new ExecuteDuskDawnC2SPayload(ExecuteDuskDawnC2SPayload.DUSK));
        } else if (visit.staticAction() == NightOrder.StaticAction.DAWN) {
            // Dawn can only be activated during the night
            if (isDayTime) {
                client.player.sendMessage(
                    Text.literal("Dawn can only be activated during the night")
                        .formatted(Formatting.RED),
                    true
                );
                return;
            }
            // Get current banshee ability players to update their indicators at dawn
            List<UUID> bansheeHasAbilityPlayers = StorytellerState.getBansheeHasAbilityPlayers();

            // Get players who previously had banshee double vote but lost it (server will compare)
            // The server tracks who has the ability, so we just send empty list here
            // and let the server determine who lost ability by comparing with its internal set
            List<UUID> bansheeLostAbilityPlayers = new ArrayList<>();

            // Check for Voudon mode (alive with ability)
            Optional<UUID> voudonPlayer = StorytellerState.getVoudonAliveWithAbility(ClientState.playerDeathStatus);
            boolean voudonModeActive = voudonPlayer.isPresent();

            ClientPlayNetworking.send(new ExecuteDuskDawnC2SPayload(
                    ExecuteDuskDawnC2SPayload.DAWN,
                    bansheeHasAbilityPlayers,
                    bansheeLostAbilityPlayers,
                    voudonModeActive,
                    voudonPlayer
            ));

            // Send death state updates when Dawn is activated
            sendDeathUpdates();

            // Clear triggered visits and daily tracking when Dawn is activated
            StorytellerState.triggeredVisits.clear();
            StorytellerState.clearDailyTracking();
            // Rebuild to remove triggered visits from active night order
            NightOrderBuilder.rebuildActiveNightOrder();
        } else if (visit.staticAction() == NightOrder.StaticAction.NOMINATIONS) {
            List<UUID> bansheeAbilityPlayers = StorytellerState.getBansheeHasAbilityPlayers();
            List<UUID> mayNotNominatePlayers = StorytellerState.getMayNotNominatePlayers();

            // Check for Voudon mode (alive with ability)
            Optional<UUID> voudonPlayer = StorytellerState.getVoudonAliveWithAbility(ClientState.playerDeathStatus);
            boolean voudonModeActive = voudonPlayer.isPresent();

            ClientPlayNetworking.send(new OpenNominationsC2SPayload(
                    bansheeAbilityPlayers,
                    voudonModeActive,
                    voudonPlayer,
                    mayNotNominatePlayers
            ));
        }
    }

    /**
     * Manually triggers the teleport for the current visit.
     */
    public static void manualTeleport() {
        if (StorytellerState.activeNightOrder.isEmpty()) return;
        performTeleport();
    }

    /**
     * Moves to Dusk visit and activates it (triggers night transition).
     * Called after execution completes.
     */
    public static void goToDuskAndActivate() {
        if (StorytellerState.activeNightOrder.isEmpty()) return;

        int size = StorytellerState.activeNightOrder.size();

        // Find Dusk index
        int duskIndex = -1;
        for (int i = 0; i < size; i++) {
            RoleVisit visit = StorytellerState.activeNightOrder.get(i);
            if (visit.staticAction() != null && visit.staticAction() == NightOrder.StaticAction.DUSK) {
                duskIndex = i;
                break;
            }
        }

        if (duskIndex == -1) return; // No Dusk found

        // Move to Dusk
        StorytellerState.currentNightVisitIndex = duskIndex;
        NightOrderBuilder.updateSemanticTracking();
        showCurrentVisitInfo();

        // Activate Dusk (send the payload to trigger night transition)
        performTeleport();
    }

    /**
     * Determines which wraith players should be teleported to the target seat.
     * Returns a list of player UUIDs with wraith ability that should be teleported.
     */
    private static List<UUID> getWraithPlayersToTeleport(UUID targetPlayerUUID) {
        List<UUID> wraithsToTeleport = new ArrayList<>();

        // Check if target player is evil
        PendingRoleAssignment targetAssignment = StorytellerState.PENDING_ROLES.get(targetPlayerUUID);
        if (targetAssignment == null || targetAssignment.isFinalGood()) {
            return wraithsToTeleport; // Target is good or not assigned, no wraiths teleport
        }

        // Target is evil: every Wraith holder who can still roam comes along. The target
        // themselves is already being teleported by the visit.
        for (UUID playerUUID : StorytellerState.PENDING_ROLES.keySet()) {
            if (!playerUUID.equals(targetPlayerUUID) && wraithHasAbility(playerUUID)) {
                wraithsToTeleport.add(playerUUID);
            }
        }

        return wraithsToTeleport;
    }

    /**
     * Sends death state updates to all players.
     * Called when Dawn static visit is activated.
     */
    private static void sendDeathUpdates() {
        Map<UUID, Boolean> deadPlayersMap = new HashMap<>();
        for (UUID uuid : StorytellerState.PENDING_ROLES.keySet()) {
            deadPlayersMap.put(uuid, ClientState.playerDeathStatus.getOrDefault(uuid, false));
        }

        // Voudon mode evaluated against the death status being sent, so a Voudon
        // killed or revived overnight flips the mode along with the dawn batch
        Optional<UUID> voudonPlayer = StorytellerState.getVoudonAliveWithAbility(deadPlayersMap);

        // silent=true: this is the dawn batch revealing deferred night kills to all
        // players. We don't want the daytime cosmetic lightning to fire for these,
        // since the deaths happened at night.
        ClientPlayNetworking.send(new UpdateDeadPlayersC2SPayload(
                deadPlayersMap,
                StorytellerState.PENDING_SEAT_NUMBERS,
                true,
                voudonPlayer.isPresent(),
                voudonPlayer
        ));
    }

    /**
     * Prepends a red "Tell them lies" line to the visit's helper info when the Vortox icon is on
     * the visit, since the storyteller sees the true info and must give the player a false one.
     */
    private static Text withVortoxLine(RoleVisit visit, Text extraInfo) {
        boolean vortoxOnVisit = visit.iconReminders() != null && visit.iconReminders().stream()
                .anyMatch(r -> r.role().isPresent() && r.role().get() == Role.VORTOX
                        && r.text().equals("Vortox Effect"));
        if (!vortoxOnVisit) return extraInfo;
        // Unstyled root so the helper info keeps its own colors instead of inheriting red
        MutableText result = Text.empty().append(Text.literal("Tell them lies").formatted(Formatting.RED));
        return extraInfo == null ? result : result.append("\n").append(extraInfo);
    }
}
