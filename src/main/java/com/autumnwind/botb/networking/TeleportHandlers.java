package com.autumnwind.botb.networking;

import com.autumnwind.botb.config.ServerConfig;
import com.autumnwind.botb.daytime.*;
import com.autumnwind.botb.states.ServerState;
import com.autumnwind.botb.voicechat.VoiceChatServerCompat;
import java.util.*;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

/** Server-bound packet handlers: Moving players between seats, homes, the town square, and the storyteller. */
final class TeleportHandlers {

    private TeleportHandlers() {}

    static void register() {
        ModPackets.registerGuarded(TeleportToSeatC2SPayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            if (player.hasPermissionLevel(2)) {
                int seat = payload.seatNumber();
                BlockPos pos = ServerConfig.SEAT_HOMES.get(seat);
                ServerWorld world = player.getServerWorld();

                if (pos != null) {
                    player.teleport(world, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, player.getYaw(), player.getPitch());

                    // Find the player assigned to this seat and play doorbell/doorknock sound for them
                    String soundType = payload.useDoorknock() ? PlaySoundS2CPayload.DOORKNOCK : PlaySoundS2CPayload.DOORBELL;
                    for (ServerPlayerEntity onlinePlayer : context.server().getPlayerManager().getPlayerList()) {
                        Integer playerSeat = ServerState.PLAYER_SEAT_NUMBERS.get(onlinePlayer.getUuid());
                        if (playerSeat != null && playerSeat == seat) {
                            ServerPlayNetworking.send(onlinePlayer, new PlaySoundS2CPayload(soundType));
                            break;
                        }
                    }
                } else {
                    player.sendMessage(Text.translatable("message.blood-on-the-blocktower.teleport.seat_home_not_set", seat).formatted(Formatting.RED), false);
                }
            }
        });

        ModPackets.registerGuarded(TeleportPlayersToSeatC2SPayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            if (player.hasPermissionLevel(2)) {
                int seat = payload.seatNumber();
                BlockPos pos = ServerConfig.SEAT_HOMES.get(seat);
                ServerWorld world = player.getServerWorld();

                if (pos != null) {
                    // Teleport all specified players to the seat
                    for (UUID playerUuid : payload.playerUuids()) {
                        ServerPlayerEntity targetPlayer = context.server().getPlayerManager().getPlayer(playerUuid);
                        if (targetPlayer != null) {
                            targetPlayer.teleport(world, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, targetPlayer.getYaw(), targetPlayer.getPitch());
                        }
                    }
                } else {
                    player.sendMessage(Text.translatable("message.blood-on-the-blocktower.teleport.seat_home_not_set", seat).formatted(Formatting.RED), false);
                }
            }
        });

        ModPackets.registerGuarded(TeleportPlayersToTownSquareSeatC2SPayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            if (player.hasPermissionLevel(2)) {
                int seat = payload.seatNumber();
                BlockPos pos = ServerConfig.TOWN_SQUARE_SEATS.get(seat);
                ServerWorld world = player.getServerWorld();

                if (pos != null) {
                    // Teleport all specified players to their town square seat
                    for (UUID playerUuid : payload.playerUuids()) {
                        ServerPlayerEntity targetPlayer = context.server().getPlayerManager().getPlayer(playerUuid);
                        if (targetPlayer != null) {
                            // Remove from voice chat group before teleporting (silently)
                            context.server().getCommandManager().executeWithPrefix(
                                    targetPlayer.getCommandSource().withSilent(),
                                    "voicechat leave"
                            );

                            targetPlayer.teleport(world, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, targetPlayer.getYaw(), targetPlayer.getPitch());
                        }
                    }
                } else {
                    player.sendMessage(Text.translatable("message.blood-on-the-blocktower.teleport.town_square_seat_not_set", seat).formatted(Formatting.RED), false);
                }
            }
        });

        ModPackets.registerGuarded(TeleportToTownSquareC2SPayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            if (player.hasPermissionLevel(2)) {
                BlockPos pos = ServerConfig.TOWN_SQUARE;
                ServerWorld world = player.getServerWorld();

                if (pos != null) {
                    // Leave voice chat group before teleporting
                    ModPackets.leaveVoiceChatGroup(player);

                    player.teleport(world, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, player.getYaw(), player.getPitch());
                } else {
                    player.sendMessage(Text.translatable("message.blood-on-the-blocktower.teleport.town_square_not_set").formatted(Formatting.RED), false);
                }
            }
        });

        ModPackets.registerGuarded(TeleportPlayerToStorytellerC2SPayload.ID, (payload, context) -> {
            ServerPlayerEntity storyteller = context.player();
            if (storyteller.hasPermissionLevel(2)) {
                UUID targetUuid = payload.playerUuid();
                ServerPlayerEntity targetPlayer = context.server().getPlayerManager().getPlayer(targetUuid);

                if (targetPlayer != null) {
                    // Put the target into the storyteller's voice chat group (or out of any group)
                    VoiceChatServerCompat.matchGroup(storyteller.getUuid(), targetUuid);

                    ServerWorld world = storyteller.getServerWorld();
                    targetPlayer.teleport(world,
                        storyteller.getX(), storyteller.getY(), storyteller.getZ(),
                        targetPlayer.getYaw(), targetPlayer.getPitch());
                } else {
                    storyteller.sendMessage(Text.translatable("message.blood-on-the-blocktower.teleport.target_not_found").formatted(Formatting.RED), false);
                }
            }
        });

        ModPackets.registerGuarded(CallBackC2SPayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            if (player.hasPermissionLevel(2)) {
                // Play call back sound for all players
                for (ServerPlayerEntity onlinePlayer : context.server().getPlayerManager().getPlayerList()) {
                    ServerPlayNetworking.send(onlinePlayer, new PlaySoundS2CPayload(PlaySoundS2CPayload.CALL_BACK));
                }
            }
        });
    }
}
