package com.autumnwind.botb.networking;

import com.autumnwind.botb.config.ServerConfig;
import com.autumnwind.botb.daytime.*;
import com.autumnwind.botb.states.ServerState;
import com.autumnwind.botb.voicechat.VoiceChatServerCompat;
import java.util.*;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;

/** Server-bound packet handlers: Moving players between seats, homes, the town square, and the storyteller. */
final class TeleportHandlers {

    private TeleportHandlers() {}

    static void register() {
        ModPackets.registerGuarded(TeleportToSeatC2SPayload.ID, (payload, context) -> {
            ServerPlayer player = context.player();
            if (player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
                int seat = payload.seatNumber();
                BlockPos pos = ServerConfig.SEAT_HOMES.get(seat);
                ServerLevel world = player.level();

                if (pos != null) {
                    player.teleportTo(world, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, Set.of(), player.getYRot(), player.getXRot(), true);

                    // Find the player assigned to this seat and play doorbell/doorknock sound for them
                    String soundType = payload.useDoorknock() ? PlaySoundS2CPayload.DOORKNOCK : PlaySoundS2CPayload.DOORBELL;
                    for (ServerPlayer onlinePlayer : context.server().getPlayerList().getPlayers()) {
                        Integer playerSeat = ServerState.PLAYER_SEAT_NUMBERS.get(onlinePlayer.getUUID());
                        if (playerSeat != null && playerSeat == seat) {
                            ServerPlayNetworking.send(onlinePlayer, new PlaySoundS2CPayload(soundType));
                            break;
                        }
                    }
                } else {
                    player.sendSystemMessage(Component.translatable("message.blood-on-the-blocktower.teleport.seat_home_not_set", seat).withStyle(ChatFormatting.RED), false);
                }
            }
        });

        ModPackets.registerGuarded(TeleportPlayersToSeatC2SPayload.ID, (payload, context) -> {
            ServerPlayer player = context.player();
            if (player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
                int seat = payload.seatNumber();
                BlockPos pos = ServerConfig.SEAT_HOMES.get(seat);
                ServerLevel world = player.level();

                if (pos != null) {
                    // Teleport all specified players to the seat
                    for (UUID playerUuid : payload.playerUuids()) {
                        ServerPlayer targetPlayer = context.server().getPlayerList().getPlayer(playerUuid);
                        if (targetPlayer != null) {
                            targetPlayer.teleportTo(world, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, Set.of(), targetPlayer.getYRot(), targetPlayer.getXRot(), true);
                        }
                    }
                } else {
                    player.sendSystemMessage(Component.translatable("message.blood-on-the-blocktower.teleport.seat_home_not_set", seat).withStyle(ChatFormatting.RED), false);
                }
            }
        });

        ModPackets.registerGuarded(TeleportPlayersToTownSquareSeatC2SPayload.ID, (payload, context) -> {
            ServerPlayer player = context.player();
            if (player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
                int seat = payload.seatNumber();
                BlockPos pos = ServerConfig.TOWN_SQUARE_SEATS.get(seat);
                ServerLevel world = player.level();

                if (pos != null) {
                    // Teleport all specified players to their town square seat
                    for (UUID playerUuid : payload.playerUuids()) {
                        ServerPlayer targetPlayer = context.server().getPlayerList().getPlayer(playerUuid);
                        if (targetPlayer != null) {
                            // Remove from voice chat group before teleporting (silently)
                            context.server().getCommands().performPrefixedCommand(
                                    targetPlayer.createCommandSourceStack().withSuppressedOutput(),
                                    "voicechat leave"
                            );

                            targetPlayer.teleportTo(world, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, Set.of(), targetPlayer.getYRot(), targetPlayer.getXRot(), true);
                        }
                    }
                } else {
                    player.sendSystemMessage(Component.translatable("message.blood-on-the-blocktower.teleport.town_square_seat_not_set", seat).withStyle(ChatFormatting.RED), false);
                }
            }
        });

        ModPackets.registerGuarded(TeleportToTownSquareC2SPayload.ID, (payload, context) -> {
            ServerPlayer player = context.player();
            if (player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
                BlockPos pos = ServerConfig.TOWN_SQUARE;
                ServerLevel world = player.level();

                if (pos != null) {
                    // Leave voice chat group before teleporting
                    ModPackets.leaveVoiceChatGroup(player);

                    player.teleportTo(world, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, Set.of(), player.getYRot(), player.getXRot(), true);
                } else {
                    player.sendSystemMessage(Component.translatable("message.blood-on-the-blocktower.teleport.town_square_not_set").withStyle(ChatFormatting.RED), false);
                }
            }
        });

        ModPackets.registerGuarded(TeleportPlayerToStorytellerC2SPayload.ID, (payload, context) -> {
            ServerPlayer storyteller = context.player();
            if (storyteller.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
                UUID targetUuid = payload.playerUuid();
                ServerPlayer targetPlayer = context.server().getPlayerList().getPlayer(targetUuid);

                if (targetPlayer != null) {
                    // Put the target into the storyteller's voice chat group (or out of any group)
                    VoiceChatServerCompat.matchGroup(storyteller.getUUID(), targetUuid);

                    ServerLevel world = storyteller.level();
                    targetPlayer.teleportTo(world, storyteller.getX(), storyteller.getY(), storyteller.getZ(), Set.of(), targetPlayer.getYRot(), targetPlayer.getXRot(), true);
                } else {
                    storyteller.sendSystemMessage(Component.translatable("message.blood-on-the-blocktower.teleport.target_not_found").withStyle(ChatFormatting.RED), false);
                }
            }
        });

        ModPackets.registerGuarded(CallBackC2SPayload.ID, (payload, context) -> {
            ServerPlayer player = context.player();
            if (player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
                // Play call back sound for all players
                for (ServerPlayer onlinePlayer : context.server().getPlayerList().getPlayers()) {
                    ServerPlayNetworking.send(onlinePlayer, new PlaySoundS2CPayload(PlaySoundS2CPayload.CALL_BACK));
                }
            }
        });
    }
}
