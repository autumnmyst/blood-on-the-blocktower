package com.autumnwind.botb.networking;

import com.autumnwind.botb.config.ServerConfig;
import com.autumnwind.botb.daytime.*;
import com.autumnwind.botb.item.ModItems;
import com.autumnwind.botb.states.ServerState;
import com.autumnwind.botb.util.AlignmentOverride;
import com.autumnwind.botb.util.PendingRoleAssignment;
import com.autumnwind.botb.util.Role;
import com.autumnwind.botb.util.RoleType;
import com.autumnwind.botb.util.Script;
import com.autumnwind.botb.world.VoteIndicators;
import java.util.*;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import com.autumnwind.botb.util.ServerCommands;
import com.autumnwind.botb.world.TeamManager;

/** Server-bound packet handlers: Role assignment and item distribution from the storyteller's grimoire. */
final class RoleHandlers {

    private RoleHandlers() {}

    static void register() {
        ModPackets.registerGuarded(AssignRolesC2SPayload.ID, (payload, context) -> {
            if (context.player().hasPermissions(2)) {
                Map<UUID, PendingRoleAssignment> pendingRoles = payload.roles();
                int activePlayerCount = payload.activePlayerCount();
                Map<UUID, Integer> seatNumbers = payload.seatNumbers();
                Script script = payload.script().orElse(null);

                // Calculate traveler count and collect traveler UUIDs from pending roles
                // Must resolve custom roles from script first (placeholders have wrong team)
                // Count travelers based on assignment, not on whether travelers are enabled
                Set<UUID> travelerUuids = new HashSet<>();
                for (Map.Entry<UUID, PendingRoleAssignment> entry : pendingRoles.entrySet()) {
                    PendingRoleAssignment resolved = script != null ? entry.getValue().resolveCustomRole(script) : entry.getValue();
                    if (resolved.getRoleType() == RoleType.TRAVELER) {
                        travelerUuids.add(entry.getKey());
                    }
                }
                final int travelerCount = travelerUuids.size();

                // Unassignment: anyone who had a role and doesn't now.
                Set<UUID> previouslyAssignedPlayers = new HashSet<>(ServerState.PLAYER_ROLES.keySet());
                Set<UUID> newlyAssignedPlayers = pendingRoles.keySet();

                previouslyAssignedPlayers.removeAll(newlyAssignedPlayers);
                Set<UUID> playersToUnassign = previouslyAssignedPlayers;

                playersToUnassign.forEach(uuid -> {
                    ServerPlayer player = context.server().getPlayerList().getPlayer(uuid);
                    if (player != null) {
                        // NO_ROLE clears their role, and carries the new active player count.
                        ServerPlayNetworking.send(player, SendRoleS2CPayload.ofRole(Role.NO_ROLE, true, activePlayerCount, travelerCount, false));
                    }
                });

                // If the storyteller sent a script, cache it on the server (so a (re)joining
                // player can request it via RequestScriptC2SPayload) and broadcast to all
                // currently-online players.
                if (payload.script().isPresent()) {
                    ServerState.currentScript = payload.script().get();
                }

                payload.script().ifPresent(s -> {
                    SendScriptS2CPayload scriptPayload = new SendScriptS2CPayload(s);
                    for (ServerPlayer player : context.server().getPlayerList().getPlayers()) {
                        ServerPlayNetworking.send(player, scriptPayload);
                    }
                    // Script and Grimoire items are handed out separately, by the Storyteller
                    // Tools "Distribute Items" button.
                });

                Map<UUID, Integer> previousSeats = new HashMap<>(ServerState.PLAYER_SEAT_NUMBERS);
                Set<UUID> previouslySeated = previousSeats.keySet();
                Set<Integer> previouslySeatedSeats = new HashSet<>(previousSeats.values());
                ServerState.updateSeats(seatNumbers);

                // Stored whole rather than reduced to (role, alignment): a homebrew assignment
                // carries its identity in the custom role id, which a Role enum can't hold, and
                // this is what a reconnecting player gets re-sent.
                ServerState.updateRoles(pendingRoles);

                // The keyset of canBeExiled is the authoritative server-side traveler set, so it
                // is rebuilt unconditionally, since a player reassigned away from a traveler role has
                // to drop out even when travelerUuids is empty.
                DaytimeState.initializeExileEligibility(travelerUuids);

                // Reconcile the unseated cage with the new occupancy. Only seats that
                // transitioned assigned→unassigned get the piston rewritten, while newly-seated
                // and unchanged seats leave their piston alone.
                VoteIndicators.syncUnseatedVoteIndicators(context.server(), previouslySeatedSeats);

                // If nominations are already open, fold any newly-seated players into the
                // eligibility maps. Without this, a player added mid-day would be excluded
                // from canBeNominated and the storyteller's UI would silently misbehave for
                // the seat right before them in render order.
                if (DaytimeState.areNominationsOpen()) {
                    Map<UUID, Boolean> deathStatus = ServerState.PLAYER_DEATH_STATUS;
                    for (UUID uuid : seatNumbers.keySet()) {
                        if (previouslySeated.contains(uuid)) continue;
                        boolean isTraveler = travelerUuids.contains(uuid);
                        if (!isTraveler) {
                            DaytimeState.setCanBeNominated(uuid, true);
                        }
                        boolean isDead = deathStatus.getOrDefault(uuid, false);
                        boolean isMayNotNominate = DaytimeState.isMayNotNominate(uuid);
                        if (!isDead && !isMayNotNominate && !isTraveler) {
                            DaytimeState.setCanNominate(uuid, true);
                        }
                    }
                }

                // Sync daytime state to clients so canBeExiled / canBeNominated / canNominate
                // changes from the block above are reflected in everyone's UI.
                StateBroadcaster.broadcastDaytimeState(context.server());

                // Broadcast seat numbers to all players
                SendSeatsS2CPayload seatsPayload = new SendSeatsS2CPayload(seatNumbers);
                for (ServerPlayer player : context.server().getPlayerList().getPlayers()) {
                    ServerPlayNetworking.send(player, seatsPayload);
                }

                // Note: Death status is handled separately via UpdateDeadPlayersC2SPayload

                // Send roles to newly assigned players and execute seat assignment commands
                // Also add all assigned players and the storyteller to botb_player team for nametag hiding
                // Travelers only go to botb_traveler team when called for exile (for purple glow color)
                Scoreboard scoreboard = context.server().getScoreboard();
                PlayerTeam playerTeam = scoreboard.getPlayerTeam(TeamManager.PLAYER_TEAM);
                if (playerTeam == null) {
                    playerTeam = scoreboard.addPlayerTeam(TeamManager.PLAYER_TEAM);
                }
                playerTeam.setColor(ChatFormatting.WHITE);

                // Create traveler team (purple) - used only during exile calls for purple glow
                PlayerTeam travelerTeam = scoreboard.getPlayerTeam(TeamManager.TRAVELER_TEAM);
                if (travelerTeam == null) {
                    travelerTeam = scoreboard.addPlayerTeam(TeamManager.TRAVELER_TEAM);
                }
                travelerTeam.setColor(ChatFormatting.LIGHT_PURPLE);
                // Nametag visibility is set to NEVER at Dusk, ALWAYS at Dawn (both teams)

                // Add the storyteller to the player team
                scoreboard.addPlayerToTeam(context.player().getGameProfile().getName(), playerTeam);

                final PlayerTeam finalPlayerTeam = playerTeam;
                pendingRoles.forEach((uuid, assignment) -> {
                    ServerPlayer player = context.server().getPlayerList().getPlayer(uuid);
                    if (player != null) {
                        // Whatever the storyteller decided this player should be told, including
                        // any Tor / Drunk / Lunatic substitution, which is applied client-side.
                        ServerPlayNetworking.send(player, SendRoleS2CPayload.ofAssignment(assignment, activePlayerCount, travelerCount, false));

                        // Add all players to botb_player team
                        // Travelers only go to botb_traveler team when called for exile (for purple glow color)
                        scoreboard.addPlayerToTeam(player.getGameProfile().getName(), finalPlayerTeam);

                        // Seat command and spawnpoint only when the seat is new or changed
                        Integer seatNumber = seatNumbers.get(uuid);
                        if (seatNumber != null && !seatNumber.equals(previousSeats.get(uuid))) {
                            String seatCommand = ServerConfig.SEAT_ASSIGNMENT_COMMANDS.get(seatNumber);
                            if (seatCommand != null && !seatCommand.isEmpty()) {
                                ServerCommands.runAs(context.server(), player.getStringUUID(), seatCommand);
                            }
                            BlockPos seatHome = ServerConfig.SEAT_HOMES.get(seatNumber);
                            if (seatHome != null) {
                                ServerCommands.runAs(context.server(), player.getStringUUID(),
                                        String.format("spawnpoint @s %d %d %d", seatHome.getX(), seatHome.getY(), seatHome.getZ()));
                            }
                        }
                    }
                });

                // Send madnesses to players
                MadnessSync.sendMadnessesToPlayers(context.server(), payload.reminders(), pendingRoles);

                // Note: Storytellers don't need traveler counts sent to them - they calculate it
                // from their shared grimoire. Non-storyteller players get counts via SendRoleS2CPayload.

                // Broadcast travelers to all players (so everyone can see traveler roles in grimoire)
                if (script != null && !travelerUuids.isEmpty()) {
                    for (Map.Entry<UUID, PendingRoleAssignment> entry : pendingRoles.entrySet()) {
                        UUID travelerUuid = entry.getKey();
                        PendingRoleAssignment originalAssignment = entry.getValue();
                        PendingRoleAssignment resolved = originalAssignment.resolveCustomRole(script);

                        // Only broadcast if this is a traveler role
                        if (resolved.getRoleType() == RoleType.TRAVELER) {
                            // Create a new assignment with DEFAULT alignment (unknown to other players)
                            PendingRoleAssignment travelerBroadcast = new PendingRoleAssignment(
                                    originalAssignment.role(),
                                    originalAssignment.customRole(),
                                    AlignmentOverride.DEFAULT);

                            // Send to non-operator players only (except the traveler themselves)
                            // Operators (storytellers) already have the full grimoire with true alignments
                            // The traveler already got their full role via SendRoleS2CPayload
                            SendTravelerUpdateS2CPayload travelerPayload =
                                    new SendTravelerUpdateS2CPayload(travelerUuid, travelerBroadcast);
                            for (ServerPlayer recipient : context.server().getPlayerList().getPlayers()) {
                                boolean isTraveler = recipient.getUUID().equals(travelerUuid);
                                boolean isOperator = recipient.hasPermissions(2);
                                if (!isTraveler && !isOperator) {
                                    ServerPlayNetworking.send(recipient, travelerPayload);
                                }
                            }
                        }
                    }
                }
            }
        });

        // Targeted Send Roles: the full send for one player and server state updated for only them
        ModPackets.registerGuarded(SendRolesToPlayerC2SPayload.ID, (payload, context) -> {
            if (!context.player().hasPermissions(2)) {
                return;
            }
            UUID targetUuid = payload.targetPlayer();
            ServerPlayer target = context.server().getPlayerList().getPlayer(targetUuid);
            if (target == null) {
                context.player().displayClientMessage(Component.translatable("message.blood-on-the-blocktower.roles.cannot_send_offline").withStyle(ChatFormatting.RED), true);
                return;
            }

            Map<UUID, PendingRoleAssignment> pendingRoles = payload.roles();
            Map<UUID, Integer> seatNumbers = payload.seatNumbers();
            int activePlayerCount = payload.activePlayerCount();
            Script script = payload.script().orElse(null);

            // Same traveler derivation as the full send, so the counts the player sees match.
            Set<UUID> travelerUuids = new HashSet<>();
            for (Map.Entry<UUID, PendingRoleAssignment> entry : pendingRoles.entrySet()) {
                PendingRoleAssignment resolved = script != null ? entry.getValue().resolveCustomRole(script) : entry.getValue();
                if (resolved.getRoleType() == RoleType.TRAVELER) {
                    travelerUuids.add(entry.getKey());
                }
            }
            int travelerCount = travelerUuids.size();

            if (script != null) {
                ServerState.currentScript = script;
                ServerPlayNetworking.send(target, new SendScriptS2CPayload(script));
            }

            // The seat HUD needs the whole table, so send every seat like the full send does.
            Integer seatNumber = seatNumbers.get(targetUuid);
            if (seatNumber != null) {
                ServerState.PLAYER_SEAT_NUMBERS.put(targetUuid, seatNumber);
            } else {
                ServerState.PLAYER_SEAT_NUMBERS.remove(targetUuid);
            }
            ServerPlayNetworking.send(target, new SendSeatsS2CPayload(seatNumbers));

            PendingRoleAssignment assignment = pendingRoles.get(targetUuid);
            if (assignment == null) {
                ServerState.PLAYER_ROLES.remove(targetUuid);
                DaytimeState.removeTraveler(targetUuid);
                ServerPlayNetworking.send(target, SendRoleS2CPayload.ofRole(Role.NO_ROLE, true, activePlayerCount, travelerCount, false));
                MadnessSync.sendMadnessesToPlayer(context.server(), targetUuid, payload.reminders(), pendingRoles);
                context.player().displayClientMessage(Component.translatable("message.blood-on-the-blocktower.roles.cleared_role", target.getGameProfile().getName()).withStyle(ChatFormatting.YELLOW), true);
                return;
            }

            ServerState.PLAYER_ROLES.put(targetUuid, assignment);
            if (travelerUuids.contains(targetUuid)) {
                DaytimeState.addTraveler(targetUuid);
            } else {
                DaytimeState.removeTraveler(targetUuid);
            }

            ServerPlayNetworking.send(target, SendRoleS2CPayload.ofAssignment(assignment, activePlayerCount, travelerCount, false));

            MadnessSync.sendMadnessesToPlayer(context.server(), targetUuid, payload.reminders(), pendingRoles);
            context.player().displayClientMessage(Component.translatable("message.blood-on-the-blocktower.roles.sent_roles", target.getGameProfile().getName()).withStyle(ChatFormatting.GREEN), true);
        });

        ModPackets.registerGuarded(DistributeItemsC2SPayload.ID, (payload, context) -> {
            ServerPlayer player = context.player();
            if (player.hasPermissions(2)) {
                // Iterate the seat map the storyteller actually sees (carried in the payload),
                // not ServerState, since that map is only populated after Send Roles.
                int itemsGiven = 0;
                for (Map.Entry<UUID, Integer> entry : payload.seatNumbers().entrySet()) {
                    if (entry.getValue() > 0) {
                        ServerPlayer seatedPlayer = context.server().getPlayerList().getPlayer(entry.getKey());
                        if (seatedPlayer != null) {
                            // Give Script item if player doesn't have one
                            if (!ModPackets.playerHasItem(seatedPlayer, ModItems.SCRIPT)) {
                                seatedPlayer.addItem(new ItemStack(ModItems.SCRIPT));
                                itemsGiven++;
                            }
                            // Give Grimoire item if player doesn't have one
                            if (!ModPackets.playerHasItem(seatedPlayer, ModItems.GRIMOIRE)) {
                                seatedPlayer.addItem(new ItemStack(ModItems.GRIMOIRE));
                                itemsGiven++;
                            }
                        }
                    }
                }
                // Send feedback to storyteller
                if (itemsGiven > 0) {
                    player.displayClientMessage(Component.translatable("message.blood-on-the-blocktower.roles.distributed_items", itemsGiven).withStyle(ChatFormatting.GREEN), false);
                } else {
                    player.displayClientMessage(Component.translatable("message.blood-on-the-blocktower.roles.all_have_items").withStyle(ChatFormatting.YELLOW), false);
                }
            }
        });
    }
}
