package com.autumnwind.botb.networking;

import com.autumnwind.botb.config.GrimoirePersistence;
import com.autumnwind.botb.gui.AssignRolesScreen;
import com.autumnwind.botb.hud.NightOrderHudManager;
import com.autumnwind.botb.states.ClientState;
import com.autumnwind.botb.states.StorytellerState;
import com.autumnwind.botb.util.PendingRoleAssignment;
import com.autumnwind.botb.util.Reminder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/** Client-bound packet receivers for grimoire contents, for the storyteller and for a player shown one. */
final class GrimoireReceivers {

    private GrimoireReceivers() {}

    static void register() {
        // Client receives clear grimoire trigger (reset game)
        ClientPlayNetworking.registerGlobalReceiver(ClearGrimoireS2CPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                StorytellerState.clear();
                // Clear crossed out roles when game is reset
                ClientState.crossedOutRoles.clear();
                // Drop the persisted grimoire too, since a finished game's state shouldn't
                // be restored on the next reconnect.
                GrimoirePersistence.clear();
            });
        });

        // Client receives grimoire data (all roles and reminders)
        ClientPlayNetworking.registerGlobalReceiver(SendGrimoireS2CPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                // Update StorytellerState with all player roles, seat numbers, and reminders
                // NOTE: This is used for game end animation AND mid-game grimoire sends (e.g., Spy)
                // It should NOT affect ClientState (player's actual role/counts)
                StorytellerState.PENDING_ROLES.clear();
                // Resolve custom roles from the current script
                for (Map.Entry<UUID, PendingRoleAssignment> entry : payload.roles().entrySet()) {
                    PendingRoleAssignment assignment = entry.getValue();
                    if (assignment.isCustomRole() && ClientState.currentScript != null) {
                        assignment = assignment.resolveCustomRole(ClientState.currentScript);
                    }
                    StorytellerState.PENDING_ROLES.put(entry.getKey(), assignment);
                }

                StorytellerState.PENDING_SEAT_NUMBERS.clear();
                StorytellerState.PENDING_SEAT_NUMBERS.putAll(payload.seatNumbers());

                StorytellerState.REMINDERS.clear();
                StorytellerState.REMINDERS.putAll(payload.reminders());

                // Update demon bluffs - convert from string format to ScriptRole
                StorytellerState.DEMON_BLUFFS.clear();
                StorytellerState.DEMON_BLUFFS.addAll(
                        StorytellerState.stringsToBluffs(payload.demonBluffs()));

                // Do NOT update ClientState.activePlayerCount here!
                // Non-storyteller clients should only get their count from SendRoleS2CPayload

                // If this is a targeted grimoire send (e.g., for Spy role), show message and refresh screen
                if (payload.isTargetedSend()) {
                    // Show message to player
                    if (context.client().player != null) {
                        context.client().player.sendMessage(
                                Text.translatable("message.blood-on-the-blocktower.client.grimoire_received")
                                        .formatted(Formatting.LIGHT_PURPLE),
                                false
                        );
                    }

                    // Refresh AssignRolesScreen if it's currently open
                    if (context.client().currentScreen instanceof AssignRolesScreen) {
                        context.client().setScreen(new AssignRolesScreen(
                                Text.translatable("message.blood-on-the-blocktower.client.title_grimoire")
                        ));
                    }
                }
            });
        });

        // Client receives grimoire sync from another storyteller
        ClientPlayNetworking.registerGlobalReceiver(SyncGrimoireS2CPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                // Only operators should process this
                if (context.client().player == null || !context.client().player.hasPermissionLevel(2)) {
                    return;
                }

                // Update script first if present (needed for resolving custom roles)
                payload.script().ifPresent(script -> {
                    ClientState.currentScript = script;
                });

                // Update StorytellerState with synced data
                StorytellerState.PENDING_ROLES.clear();
                // Resolve custom roles from the current script
                for (Map.Entry<UUID, PendingRoleAssignment> entry : payload.roles().entrySet()) {
                    PendingRoleAssignment assignment = entry.getValue();
                    if (assignment.isCustomRole() && ClientState.currentScript != null) {
                        assignment = assignment.resolveCustomRole(ClientState.currentScript);
                    }
                    StorytellerState.PENDING_ROLES.put(entry.getKey(), assignment);
                }

                StorytellerState.PENDING_SEAT_NUMBERS.clear();
                StorytellerState.PENDING_SEAT_NUMBERS.putAll(payload.seatNumbers());

                StorytellerState.REMINDERS.clear();
                for (Map.Entry<UUID, List<Reminder>> entry : payload.reminders().entrySet()) {
                    StorytellerState.REMINDERS.put(entry.getKey(), new ArrayList<>(entry.getValue()));
                }

                // Update marked players
                StorytellerState.markedPlayers.clear();
                StorytellerState.markedPlayers.addAll(payload.markedPlayers());

                // Update demon bluffs - convert from string format to ScriptRole
                StorytellerState.DEMON_BLUFFS.clear();
                StorytellerState.DEMON_BLUFFS.addAll(
                        StorytellerState.stringsToBluffs(payload.demonBluffs()));

                StorytellerState.setupOutsiderCount = payload.setupOutsiderCount();

                // Update nextSeatNumber based on current seat assignments
                int maxSeat = 0;
                for (int seat : StorytellerState.PENDING_SEAT_NUMBERS.values()) {
                    if (seat > maxSeat) {
                        maxSeat = seat;
                    }
                }
                StorytellerState.nextSeatNumber = maxSeat + 1;

                // Rebuild night order with new data
                NightOrderHudManager.rebuildActiveNightOrder();

                // If on AssignRolesScreen, refresh it
                if (context.client().currentScreen instanceof AssignRolesScreen assignRolesScreen) {
                    assignRolesScreen.refreshFromSync();
                }
            });
        });
    }
}
