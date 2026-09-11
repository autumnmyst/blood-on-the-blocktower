package com.autumnwind.botb.networking;

import com.autumnwind.botb.config.ServerConfig;
import com.autumnwind.botb.daytime.*;
import com.autumnwind.botb.states.ServerState;
import com.autumnwind.botb.world.VoteIndicators;
import java.util.*;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import com.autumnwind.botb.util.ServerCommands;

/** Server-bound packet handlers: Grimoire sync between storytellers, sending a grimoire to a player, and seat swaps. */
final class GrimoireHandlers {

    private GrimoireHandlers() {}

    static void register() {
        ModPackets.registerGuarded(SyncGrimoireC2SPayload.ID, (payload, context) -> {
            if (!context.player().hasPermissions(2)) {
                return; // Only operators can sync grimoire
            }

            // Count how many operators are online
            List<ServerPlayer> operators = context.server().getPlayerList().getPlayers().stream()
                    .filter(p -> p.hasPermissions(2))
                    .toList();

            // If only one operator (the sender), no need to broadcast
            if (operators.size() <= 1) {
                return;
            }

            // Broadcast to all OTHER operators
            SyncGrimoireS2CPayload syncPayload = new SyncGrimoireS2CPayload(
                    payload.roles(),
                    payload.seatNumbers(),
                    payload.reminders(),
                    payload.script(),
                    payload.markedPlayers(),
                    payload.demonBluffs(),
                    payload.setupOutsiderCount()
            );

            for (ServerPlayer operator : operators) {
                if (!operator.getUUID().equals(context.player().getUUID())) {
                    ServerPlayNetworking.send(operator, syncPayload);
                }
            }
        });

        // Register send grimoire to specific player receiver
        ModPackets.registerGuarded(SendGrimoireToPlayerC2SPayload.ID, (payload, context) -> {
            if (context.player().hasPermissions(2)) {
                UUID targetUuid = payload.targetPlayer();
                ServerPlayer targetPlayer = context.server().getPlayerList().getPlayer(targetUuid);

                if (targetPlayer != null) {
                    // Send grimoire data to the specific player
                    // isTargetedSend = true because this is a targeted send (e.g., for Spy role)
                    ServerPlayNetworking.send(targetPlayer, new SendGrimoireS2CPayload(
                            payload.roles(),
                            payload.seatNumbers(),
                            payload.reminders(),
                            payload.demonBluffs(),
                            true
                    ));
                }
            }
        });

        ModPackets.registerGuarded(SwapPlayersC2SPayload.ID, (payload, context) -> {
            if (!context.player().hasPermissions(2)) {
                return; // Only operators can swap seats
            }

            UUID player1 = payload.player1();
            UUID player2 = payload.player2();

            // ServerState's seat map is only populated by Send Roles. Until then the storyteller
            // is arranging the table against their own map, which the payload carries, so that
            // one is used whenever the server doesn't know both players yet.
            boolean serverSeated = ServerState.PLAYER_SEAT_NUMBERS.containsKey(player1)
                    && ServerState.PLAYER_SEAT_NUMBERS.containsKey(player2);
            Map<UUID, Integer> seats = serverSeated
                    ? ServerState.PLAYER_SEAT_NUMBERS
                    : new HashMap<>(payload.seatNumbers());

            Integer seat1 = seats.get(player1);
            Integer seat2 = seats.get(player2);

            if (seat1 == null || seat2 == null) {
                context.player().displayClientMessage(Component.translatable("message.blood-on-the-blocktower.grimoire.cannot_swap_unseated").withStyle(ChatFormatting.RED), true);
                return;
            }

            // Block if vote/nomination/exile in progress (this is also checked client-side, but double-check here)
            if (DaytimeState.getCurrentNominee() != null ||
                DaytimeState.getCurrentExileTarget() != null ||
                DaytimeState.isExileSupportInProgress()) {
                context.player().displayClientMessage(Component.translatable("message.blood-on-the-blocktower.grimoire.cannot_swap_during_vote").withStyle(ChatFormatting.RED), true);
                return;
            }

            ServerLevel world = context.server().overworld();

            // Get indicator positions for both seats
            BlockPos indicator1 = ServerConfig.SEAT_VOTE_INDICATOR_POSITIONS.get(seat1);
            BlockPos indicator2 = ServerConfig.SEAT_VOTE_INDICATOR_POSITIONS.get(seat2);

            // Reset both indicators to clean state (remove ghost used blocks)
            // We leave pistons in place - they are NEVER removed
            if (indicator1 != null) {
                BlockPos belowIndicator1 = indicator1.below();
                BlockState blockState1 = world.getBlockState(belowIndicator1);
                Block ghostUsedBlock = VotingManager.getBlockFromString(ServerConfig.VOTE_INDICATOR_BLOCK_GHOST_USED);
                if (blockState1.getBlock().equals(ghostUsedBlock)) {
                    world.setBlockAndUpdate(belowIndicator1, Blocks.AIR.defaultBlockState());
                }
            }
            if (indicator2 != null) {
                BlockPos belowIndicator2 = indicator2.below();
                BlockState blockState2 = world.getBlockState(belowIndicator2);
                Block ghostUsedBlock = VotingManager.getBlockFromString(ServerConfig.VOTE_INDICATOR_BLOCK_GHOST_USED);
                if (blockState2.getBlock().equals(ghostUsedBlock)) {
                    world.setBlockAndUpdate(belowIndicator2, Blocks.AIR.defaultBlockState());
                }
            }

            // Writes through to ServerState when that map is the authoritative one, and only
            // into the payload's local copy otherwise, since ServerState stays empty until Send Roles.
            seats.put(player1, seat2);
            seats.put(player2, seat1);

            // Get death status for both players
            boolean player1Dead = ServerState.PLAYER_DEATH_STATUS.getOrDefault(player1, false);
            boolean player2Dead = ServerState.PLAYER_DEATH_STATUS.getOrDefault(player2, false);

            // Reconcile vote indicators for player1 at their NEW seat (seat2)
            VoteIndicators.reconcileVoteIndicator(context.server(), player1, seat2, player1Dead);

            // Reconcile vote indicators for player2 at their NEW seat (seat1)
            VoteIndicators.reconcileVoteIndicator(context.server(), player2, seat1, player2Dead);

            // Broadcast updated seats to all players
            SendSeatsS2CPayload seatsPayload = new SendSeatsS2CPayload(new HashMap<>(seats));
            for (ServerPlayer player : context.server().getPlayerList().getPlayers()) {
                ServerPlayNetworking.send(player, seatsPayload);
            }

            // Teleport both players to their new town square seats
            BlockPos newPos1 = ServerConfig.TOWN_SQUARE_SEATS.get(seat2); // player1 goes to seat2
            BlockPos newPos2 = ServerConfig.TOWN_SQUARE_SEATS.get(seat1); // player2 goes to seat1

            ServerPlayer p1 = context.server().getPlayerList().getPlayer(player1);
            ServerPlayer p2 = context.server().getPlayerList().getPlayer(player2);

            if (p1 != null && newPos1 != null) {
                p1.teleportTo(world, newPos1.getX() + 0.5, newPos1.getY(), newPos1.getZ() + 0.5, p1.getYRot(), p1.getXRot());

                // Update spawnpoint to new seat home
                String spawnpointCommand = String.format("spawnpoint @s %d %d %d",
                    newPos1.getX(), newPos1.getY(), newPos1.getZ());
                ServerCommands.runAs(context.server(), p1.getStringUUID(), spawnpointCommand);
            }
            if (p2 != null && newPos2 != null) {
                p2.teleportTo(world, newPos2.getX() + 0.5, newPos2.getY(), newPos2.getZ() + 0.5, p2.getYRot(), p2.getXRot());

                // Update spawnpoint to new seat home
                String spawnpointCommand = String.format("spawnpoint @s %d %d %d",
                    newPos2.getX(), newPos2.getY(), newPos2.getZ());
                ServerCommands.runAs(context.server(), p2.getStringUUID(), spawnpointCommand);
            }
        });
    }
}
