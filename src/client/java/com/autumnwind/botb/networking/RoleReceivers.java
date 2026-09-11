package com.autumnwind.botb.networking;

import com.autumnwind.botb.config.PlayerConfig;
import com.autumnwind.botb.gui.AssignRolesScreen;
import com.autumnwind.botb.hud.NightOrderHudManager;
import com.autumnwind.botb.sound.CustomSounds;
import com.autumnwind.botb.sound.ModSounds;
import com.autumnwind.botb.states.ClientState;
import com.autumnwind.botb.states.StorytellerState;
import com.autumnwind.botb.util.PendingRoleAssignment;
import com.autumnwind.botb.util.Role;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/** Client-bound packet receivers for the player's own role, traveler updates, seats, death status, and madnesses. */
final class RoleReceivers {

    private RoleReceivers() {}

    static void register() {
        ClientPlayNetworking.registerGlobalReceiver(SendRoleS2CPayload.ID, (payload, context) -> {
            // Update the ClientState with the full assignment (supports custom roles)
            ClientState.updatePlayerState(payload.assignment(), payload.activePlayerCount(), payload.travelerCount(), payload.silent());

            // Refresh AssignRolesScreen if currently open (so local grimoire updates)
            context.client().execute(() -> {
                if (context.client().screen instanceof AssignRolesScreen) {
                    context.client().setScreen(context.client().screen);
                }
            });
        });

        // Client receives traveler update (broadcast to all players)
        ClientPlayNetworking.registerGlobalReceiver(SendTravelerUpdateS2CPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                UUID travelerUuid = payload.travelerUuid();
                PendingRoleAssignment assignment = payload.assignment();

                // Skip if this is our own role (we already got it via SendRoleS2CPayload)
                if (context.client().player != null && travelerUuid.equals(context.client().player.getUUID())) {
                    return;
                }

                // Update grimoire with traveler's role
                // For non-operators, this goes into StorytellerState.PENDING_ROLES
                // (which is shared between operators and players for local grimoire state)
                if (assignment.role() == Role.NO_ROLE && !assignment.isCustomRole()) {
                    // Player is no longer a traveler - remove from grimoire
                    StorytellerState.PENDING_ROLES.remove(travelerUuid);
                } else {
                    // Resolve custom role from current script (the network only sends the ID)
                    PendingRoleAssignment resolvedAssignment = assignment;
                    if (assignment.isCustomRole() && ClientState.currentScript != null) {
                        resolvedAssignment = assignment.resolveCustomRole(ClientState.currentScript);
                    }
                    // Update/add traveler role
                    StorytellerState.PENDING_ROLES.put(travelerUuid, resolvedAssignment);
                }

                // Refresh AssignRolesScreen if currently open (so local grimoire updates)
                if (context.client().screen instanceof AssignRolesScreen) {
                    context.client().setScreen(context.client().screen);
                }
            });
        });

        // Client receives seat number map
        ClientPlayNetworking.registerGlobalReceiver(SendSeatsS2CPayload.ID, (payload, context) -> {
            ClientState.playerSeatNumbers = payload.seatNumbers();

            // Also update StorytellerState for operators (grimoire seat locations)
            context.client().execute(() -> {
                boolean isOperator = context.client().player != null && context.client().player.hasPermissions(2);
                if (isOperator) {
                    // Update pending seat numbers to match
                    StorytellerState.PENDING_SEAT_NUMBERS.clear();
                    StorytellerState.PENDING_SEAT_NUMBERS.putAll(payload.seatNumbers());

                    // Refresh AssignRolesScreen if currently open
                    if (context.client().screen instanceof AssignRolesScreen) {
                        context.client().setScreen(context.client().screen);
                    }
                }
            });
        });

        // Client receives death status map
        ClientPlayNetworking.registerGlobalReceiver(SendDeathStatusS2CPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                Map<UUID, Boolean> previousDeathStatus = new HashMap<>(ClientState.playerDeathStatus);
                ClientState.playerDeathStatus = payload.deadPlayers();

                boolean isOperator = context.client().player != null && context.client().player.hasPermissions(2);
                boolean anyDeathChange = false;

                // When a player dies or is revived, update their canNominate status
                for (Map.Entry<UUID, Boolean> entry : payload.deadPlayers().entrySet()) {
                    UUID playerUuid = entry.getKey();
                    boolean isDead = entry.getValue();
                    boolean wasDeadBefore = previousDeathStatus.getOrDefault(playerUuid, false);

                    if (isDead && !wasDeadBefore) {
                        // Player just died - remove from canNominate
                        ClientState.canNominate.put(playerUuid, false);
                        anyDeathChange = true;

                        // Storyteller: Create death triggers for this player
                        if (isOperator) {
                            NightOrderHudManager.createDeathTriggersForPlayer(playerUuid);

                            // Unmark player when they die, unless they have triggered ability
                            if (!NightOrderHudManager.hasAnyTriggeredRole(playerUuid)) {
                                StorytellerState.markedPlayers.remove(playerUuid);
                            }

                            // If this was an execution (executionToday is true), track executed role and add Cannibal reminder
                            if (ClientState.executionToday) {
                                // Track executed player's role for Undertaker/Cannibal info
                                PendingRoleAssignment executedAssignment =
                                        StorytellerState.PENDING_ROLES.get(playerUuid);
                                if (executedAssignment != null) {
                                    StorytellerState.lastExecutedRole = executedAssignment.role();
                                }

                                NightOrderHudManager.createCannibalExecutionReminder();
                            }
                        }
                    } else if (!isDead && wasDeadBefore) {
                        // Player was revived - restore canNominate (if nominations are open)
                        if (ClientState.nominationsOpen) {
                            ClientState.canNominate.put(playerUuid, true);
                        }
                        anyDeathChange = true;

                        // Storyteller: Remove death-based triggers and create resurrection trigger
                        if (isOperator) {
                            NightOrderHudManager.removeDeathTriggers(playerUuid);
                            NightOrderHudManager.createResurrectionTrigger(playerUuid);
                        }
                    }
                }

                // Rebuild night order if any death status changed (for storyteller)
                if (anyDeathChange && isOperator) {
                    NightOrderHudManager.rebuildActiveNightOrder();
                }
            });
        });

        // Client receives madness data
        ClientPlayNetworking.registerGlobalReceiver(SendMadnessS2CPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                // Check if we're gaining new madnesses (madness count increased)
                int oldCount = ClientState.myMadnesses != null ? ClientState.myMadnesses.size() : 0;
                int newCount = payload.madnesses().size();
                boolean gainingMadness = newCount > oldCount;

                // Update ClientState with received madnesses
                ClientState.myMadnesses = payload.madnesses();

                // Only show the HUD when gaining madness, not when losing it
                if (gainingMadness) {
                    ClientState.isRoleHudVisible = true;
                    PlayerConfig.save();

                    // Play role receive sound - Role Receive category
                    float madnessVolume = ClientState.BASE_VOLUME_ROLE_RECEIVE * ClientState.volumeRoleReceive;
                    if (context.client().player != null && context.client().level != null && madnessVolume > 0) {
                        CustomSounds.playOneShot(context.client(), CustomSounds.madnessReceiveCandidates(ClientState.myAssignment),
                                ModSounds.ROLE_RECEIVE, madnessVolume);
                    }
                }
            });
        });
    }
}
